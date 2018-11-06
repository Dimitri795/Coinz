package com.example.dimit.coinz

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
import com.google.gson.GsonBuilder
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

class MainActivity : AppCompatActivity(),OnMapReadyCallback,
      LocationEngineListener,PermissionsListener{

    private val tag = "MainActivity"
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var mAuth : FirebaseAuth? = null
    private var db : FirebaseFirestore? = null
    private var wallet : CollectionReference? = null

    private lateinit var originLocation : Location
    private lateinit var permissionsManager : PermissionsManager
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin : LocationLayerPlugin

    private val mapUrl : String = "http://homepages.inf.ed.ac.uk/stg/coinz/"
    private var downloadDate = "" // Format: YYYY/MM/DD
    private var dailyFcData = "" //JsonData that was downloaded for the day
    private val preferencesFile = "MyPrefsFile" // for storing preferences

    private var fc : MutableList<Feature>? = null //daily feature collection list of features
    private var markers = HashMap<String, Marker?>()
    private var mapDrawn = false
    private var purse : MutableList<String>? = mutableListOf()
    private var newfc : MutableList<Feature>? = null

    companion object {
        private const val collection_key ="Users"
        private const val subcollection_key = "Wallet"
        private const val personalWalletDoc = "Personal Wallet"
        private const val friendWalletDoc = "Gift Wallet"
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
                .setTimestampsInSnapshotsEnabled(true).build()
        db?.firestoreSettings = settings
        wallet = db?.collection(collection_key)?.document(mAuth?.uid!!)?.collection(subcollection_key)

        Mapbox.getInstance(this,getString(R.string.access_token))
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        /*fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }*/
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
        if(downloadDate != currentFormatted){
            downloadDate = currentFormatted
            val dailyMap  = "$mapUrl$downloadDate/coinzmap.geojson"
            val myAsyncTask = DownloadFileTask(DownloadCompleteRunner)
            dailyFcData = myAsyncTask.execute(dailyMap).get()
            map?.addSource(GeoJsonSource("geojson",dailyFcData))
        }
        fc = FeatureCollection.fromJson(dailyFcData).features()
        updateMap(fc)
    }

    private fun fillPurse(){
        wallet?.document(personalWalletDoc)?.get()?.addOnCompleteListener { task ->
            if(task.isComplete){
                val doc = task.result
                if(doc?.exists()!!){
                    Log.d(tag, "DocumentSnapshot data: " + doc.data)
                    purse?.addAll(doc.data!!.keys)

                }else{
                    Log.d(tag,"No such document")
                }
            } else{
                Log.d(tag,"get failed with",task.exception)
            }
        }
    }

    private fun updateMap(fc : MutableList<Feature>?){
        fillPurse()
        newfc = fc?.filterNot{purse?.contains(it.getStringProperty("id"))!!} as MutableList<Feature>?
        fc?.forEach {
            val coinCoord = it.geometry() as Point
            val coinPos = LatLng(coinCoord.latitude(), coinCoord.longitude())
            val coinColour = it.getStringProperty("marker-color").toString()
            val coinTitle = it.getStringProperty("currency")
            val coinSymbol = it.getStringProperty("marker-symbol")
            //setting icon based on colour
            val myIcon = makeIcon(coinColour,coinSymbol)
            markers[it.getStringProperty("id")] = map?.addMarker(MarkerOptions().title(coinTitle).icon(myIcon).position(coinPos))
        }
        mapDrawn = true
    }

    private fun makeIcon(colour: String, symbol: String): Icon{
        lateinit var myIcon : Icon
        when(colour){
            "#ff0000" -> when(symbol){
                "1" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.redmarker1)
                "2" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.redmarker2)
                "3" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.redmarker3)
                "4" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.redmarker4)
                "5" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.redmarker5)
                "6" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.redmarker6)
                "7" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.redmarker7)
                "8" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.redmarker8)
                "9" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.redmarker9)
            }
            "#0000ff" -> when(symbol){
                "1" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.bluemarker1)
                "2" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.bluemarker2)
                "3" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.bluemarker3)
                "4" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.bluemarker4)
                "5" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.bluemarker5)
                "6" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.bluemarker6)
                "7" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.bluemarker7)
                "8" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.bluemarker8)
                "9" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.bluemarker9)
            }
            "#ffdf00" -> when(symbol){
                "1" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.yelmarker1)
                "2" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.yelmarker2)
                "3" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.yelmarker3)
                "4" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.yelmarker4)
                "5" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.yelmarker5)
                "6" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.yelmarker6)
                "7" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.yelmarker7)
                "8" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.yelmarker8)
                "9" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.yelmarker9)
            }
            "#008000" -> when(symbol){
                "1" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.greenmarker1)
                "2" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.greenmarker2)
                "3" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.greenmarker3)
                "4" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.greenmarker4)
                "5" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.greenmarker5)
                "6" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.greenmarker6)
                "7" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.greenmarker7)
                "8" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.greenmarker8)
                "9" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.greenmarker9)
            }
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
            collectCoin()
        }
    }

    private fun collectCoin(){
        if(mapDrawn){
            fc?.forEach {
                val coinCoord = it.geometry() as Point
                val coinLoc = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = coinCoord.latitude()
                    longitude = coinCoord.longitude()
                }
                if (originLocation.distanceTo(coinLoc) <= 25.0) {
                    val data = HashMap<String,String>()
                    data[it.getStringProperty("id")] = it.toJson()
                    wallet?.document(personalWalletDoc)?.set(data as Map<String, Any>, SetOptions.merge())
                    makeToast("Coin Collected ${it.getStringProperty("id")}")
                }
            }
        }
        updateMap(fc)
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
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // use ”” as the default value (this might be the first time the app is run)
        downloadDate = settings.getString("lastDownloadDate", "")!!
        dailyFcData = settings.getString("DailyCoinData","")!!
        Log.d(tag, "[onStart] Recalled lastDownloadDate is ’$downloadDate’")
        Log.d(tag, "[onStart] Recalled Daily Coin list is ’$dailyFcData’")
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

        Log.d(tag, "[onStop] Storing lastDownloadDate of $downloadDate")
        Log.d(tag, "[onStop] Storing Daily Coin Data of $dailyFcData")
        //Log.d(tag,"[onStop] Storing Collected Coin List of $wallet")
        // All objects are from android.context.Context
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // We need an Editor object to make preference changes.
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
        editor.putString("DailyCoinData", dailyFcData)
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
        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_LONG).show()
    }
}
