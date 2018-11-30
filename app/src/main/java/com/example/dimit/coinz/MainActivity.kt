package com.example.dimit.coinz

import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(),OnMapReadyCallback,
      LocationEngineListener,PermissionsListener{

    private val tag = "MainActivity"
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var mAuth : FirebaseAuth? = null
    private var db : FirebaseFirestore? = null

    private lateinit var originLocation : Location
    private lateinit var permissionsManager : PermissionsManager
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin : LocationLayerPlugin
    private lateinit var locationLifecycle : Lifecycle

    private val mapUrl : String = "http://homepages.inf.ed.ac.uk/stg/coinz/"
    private var downloadDate = "" // Format: YYYY/MM/DD
    private var wallet : CollectionReference? = null // firebase storage of collected coins
    private var walletListener : ListenerRegistration? = null
    private lateinit var preferencesFile : String // for storing preferences

    private var fc : MutableList<Feature>? = null //daily feature collection list of features
    private var markers = HashMap<String, Marker?>()
    private var mapDrawn = false
    private var newfc : MutableList<Feature>? = mutableListOf()

    companion object {
        const val collection_key = "Users"
        const val subcollection_key = "Wallet"
        const val personalwalletdoc = "Personal Wallet"
        const val sendersdoc = "Senders"
        var dailyFcData = "" //JsonData that was downloaded for the day
        var collected : MutableList<String>? = mutableListOf() // list of collected coins
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        if(mAuth?.currentUser == null){
            startActivity(Intent(this@MainActivity,LoginActivity::class.java))
        }
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        db = FirebaseFirestore.getInstance()
        // Use com.google.firebase.Timestamp objects instead of java.util.Date objects
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true).setPersistenceEnabled(false).build()
        db?.firestoreSettings = settings
        if(mAuth?.currentUser != null){
            wallet = db?.collection(collection_key)?.document(mAuth?.uid!!)?.collection(subcollection_key)
        }

        Mapbox.getInstance(this,getString(R.string.access_token))
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        fab.setOnClickListener { _->
            startActivity(Intent(this@MainActivity,BankActivity::class.java))
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapboxMap is null")
        } else {
            map = mapboxMap
            // Set user interface options
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            // Make location information available
            enableLocation()
            downloadFeatures()
        }
    }

    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "Permissions are granted")
            initialiseLocationEngine()
            initialiseLocationLayer()
        } else {
            Log.d(tag, "Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun downloadFeatures(){
        val current = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        val currentFormatted = current.substring(0,4)+"/"+ current.substring(4,6)+"/"+ current.substring(6)
        if(downloadDate != currentFormatted) {
            downloadDate = currentFormatted
            val dailyMap = "$mapUrl$downloadDate/coinzmap.geojson"
            val myAsyncTask = DownloadFileTask(DownloadCompleteRunner)
            dailyFcData = myAsyncTask.execute(dailyMap).get()
            map?.addSource(GeoJsonSource("geojson", dailyFcData))
            wallet?.document(personalwalletdoc)?.delete() // day has changed so empty wallet and begin anew
            collected?.clear()
            BankActivity.used?.clear() // day has changed so avoid clutter by removing previous days depositedd coins.
            BankActivity.dailyLimit = 0
        }
        fc = FeatureCollection.fromJson(dailyFcData).features()
        addMarkers(fc)
    }

    private fun addMarkers(fc : MutableList<Feature>?){
        newfc = fc?.filterNot{collected?.contains(it.getStringProperty("id"))!!} as MutableList<Feature>?
        newfc?.forEach {
            val coinCoord = it.geometry() as Point
            val coinPos = LatLng(coinCoord.latitude(), coinCoord.longitude())
            val coinColour = it.getStringProperty("marker-color").toString()
            val coinTitle = it.getStringProperty("currency")
            val coinVal = it.getStringProperty("value").toDouble()
            val coinSymbol = it.getStringProperty("marker-symbol")
            // getting coin value in gold and rounded
            val goldVal = (getGold(coinTitle)*coinVal).roundToInt().toString()
            //setting icon based on colour
            val mycon = makeIcon(coinColour,coinSymbol)
            markers[it.getStringProperty("id")] = map?.addMarker(MarkerOptions().title(coinTitle).snippet("$goldVal gold")
            .icon(mycon).position(coinPos))
        }
        mapDrawn = true
    }

    fun getGold(currency : String) : Double {
       return JSONObject("${JSONObject(dailyFcData).get("rates")}").getDouble(currency)
    }

    private fun makeIcon(colour: String, symbol: String): Icon{
        lateinit var myIcon : Icon
        val ifact = IconFactory.getInstance(this@MainActivity)
        when(colour){
            "#ff0000" -> myIcon = ifact.fromResource(resources.obtainTypedArray(R.array.RedMarkers).getResourceId(symbol.toInt(),R.drawable.redmarker0))
            "#0000ff" -> myIcon = ifact.fromResource(resources.obtainTypedArray(R.array.BlueMarkers).getResourceId(symbol.toInt(),R.drawable.bluemarker0))
            "#ffdf00" -> myIcon = ifact.fromResource(resources.obtainTypedArray(R.array.YellowMarkers).getResourceId(symbol.toInt(),R.drawable.yelmarker0))
            "#008000" -> myIcon = ifact.fromResource(resources.obtainTypedArray(R.array.GreenMarkers).getResourceId(symbol.toInt(),R.drawable.greenmarker0))
        }
        return myIcon
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationEngine() {
        locationEngine = LocationEngineProvider(this)
                .obtainBestLocationEngineAvailable()
        locationEngine.apply {
            interval = 5000 // preferably every 5 seconds
            fastestInterval = 1000 // at most every second
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }
        val lastLocation = locationEngine.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else { locationEngine.addLocationEngineListener(this) }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationLayer() {
        if (mapView == null) { Log.d(tag, "mapView is null") }
        else {
            if (map == null) { Log.d(tag, "map is null") }
            else {
                locationLayerPlugin = LocationLayerPlugin(mapView!!,
                        map!!, locationEngine)
                locationLayerPlugin.apply {
                    setLocationLayerEnabled(true)
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                }
                locationLifecycle = lifecycle
                locationLifecycle.addObserver(locationLayerPlugin)
            }
        }
    }

    private fun setCameraPosition(location: Location) {
        val latlng = LatLng(location.latitude, location.longitude)
        map?.animateCamera(CameraUpdateFactory.newLatLng(latlng))
    }

    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            Log.d(tag, "[onLocationChanged] location is null")
        } else {
            originLocation = location
            setCameraPosition(originLocation)
            if(mapDrawn){
                collectCoin()
            }
        }
    }

    private fun collectCoin(){
        newfc = fc?.filterNot{collected?.contains(it.getStringProperty("id"))!!} as MutableList<Feature>?
        newfc?.forEach {
            val coinCoord = it.geometry() as Point
            val coinLoc = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = coinCoord.latitude()
                longitude = coinCoord.longitude()
            }
            if (originLocation.distanceTo(coinLoc) <= 25.0) {
                val id = it.getStringProperty("id")
                collected?.add(id)
                val data = HashMap<String,String>()
                data[id] = it.toJson()
                wallet?.document(personalwalletdoc)?.set(data as Map<String, Any>, SetOptions.merge())
                makeToast("Collected a ${it.getStringProperty("currency")} ${it.getStringProperty("marker-symbol")} coin!")
                map?.removeMarker(markers[id]!!)
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    override fun onExplanationNeeded(permissionsToExplain:
                                     MutableList<String>?) {
        Log.d(tag, "Permissions: $permissionsToExplain")
        // Present popup message or dialog
        makeToast("This app requires permission to access your " +
                "location to proceed.")
    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            // Open a dialogue with the user
            makeToast("This app requires permission to access your " +
                    "location to proceed.")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.sign_out -> {
                mAuth?.signOut()
                makeToast("Sign out successful")
                startActivity(Intent(this@MainActivity,LoginActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    public override fun onStart() {
        super.onStart()
        mapView?.onStart()
        // Restore preferences
        preferencesFile = "MyPrefsFile${mAuth?.uid}"
        Log.d(tag, "Preference file is $preferencesFile")
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // use ”” as the default value (this might be the first time the app is run)
        downloadDate = settings.getString("lastDownloadDate", "")!!
        dailyFcData = settings.getString("DailyCoinData","")!!
        val colcCoins = settings.getString("CollectedCoinList","")
        if(colcCoins != ""){
            collected = colcCoins?.split(delimiters = *arrayOf("$"))?.asSequence()?.toSet()?.toMutableList()
        } else {
            walletListener = wallet?.document(personalwalletdoc)?.addSnapshotListener { docSnap, e ->
                when {
                    e != null -> Log.d(tag, e.message)
                    docSnap != null && docSnap.exists() -> {
                        collected?.addAll(docSnap.data!!.keys)
                        collected?.asSequence()?.toSet()?.toMutableList()
                        Log.d(tag, "Snapshot listen successful")
                        updateMap()
                    }
                }
            }
        }

        Log.d(tag, "[onStart] Recalled Last Download Date is $downloadDate")
        Log.d(tag, "[onStart] Recalled Daily Coin list")
        Log.d(tag, "[onStart] Recalled Collected Coin List is $collected")

    }
    private fun updateMap(){
        val newFc = fc?.filter { collected?.contains(it.getStringProperty("id"))!! } as MutableList<Feature>?
        newFc?.forEach {
            if(markers[it.getStringProperty("id")] != null){
                map?.removeMarker(markers[it.getStringProperty("id")]!!)
            }
        }
    }
    public override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mapView?.onStop()
        walletListener?.remove()
        if(::locationEngine.isInitialized){
            locationEngine.removeLocationEngineListener(this)
            locationEngine.removeLocationUpdates()
        }

        Log.d(tag, "Writing to Preferences file $preferencesFile")
        Log.d(tag, "[onStop] Storing Last Download Date of $downloadDate")
        Log.d(tag, "[onStop] Storing Daily Coin Data")
        Log.d(tag, "[onStop] Storing Collected Coin List of $collected")
        // All objects are from android.context.Context
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // We need an Editor object to make preference changes.
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
        editor.putString("DailyCoinData", dailyFcData)
        editor.putString("CollectedCoinList", collected?.joinToString("$"))
        // Apply the edits!
        editor.apply()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    public override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    public override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null){
            mapView?.onSaveInstanceState(outState)
        }
    }
    // Helper function to make writing toasts faster/less wordy
    private fun makeToast(msg : String){
        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_SHORT).show()
    }
}
