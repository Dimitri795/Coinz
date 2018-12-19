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
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(),OnMapReadyCallback,
      LocationEngineListener,PermissionsListener{

    private val tag = "MainActivity"           // tag for logging
    private var mapView: MapView? = null       // the map view in our activity_main
    private var map: MapboxMap? = null         // the actual map object
    private var mAuth : FirebaseAuth? = null   // the Firebase authentication  variable
    private var db : FirebaseFirestore? = null // the Firebase cloud storage variable

    private lateinit var originLocation : Location                  // the current location to centre the map
    private lateinit var permissionsManager : PermissionsManager    // handles permissions
    private lateinit var locationEngine : LocationEngine            // handles location functionality
    private lateinit var locationLayerPlugin : LocationLayerPlugin  // works with engine to handle functionality
    private lateinit var locationLifecycle : Lifecycle              // makes a location object aware of the activity's lifecylce to prevent memory leaks

    private val mapUrl : String = "http://homepages.inf.ed.ac.uk/stg/coinz/"// the unchanging part of the map download url
    private var downloadDate = ""                                           // Format: YYYY/MM/DD
    private var wallet : CollectionReference? = null                        // firebase storage of collected coins
    private var walletListener : ListenerRegistration? = null               // realtime update listener for querying wallet
    private lateinit var preferencesFile : String                           // for storing preferences

    private var fc : MutableList<Feature>? = null               // daily feature collection list of features
    private var markers = HashMap<String, Marker?>()            // hashmap linking markers drawn to feature IDs
    private var mapDrawn = false                                // a boolean tracking if addmarkers() function has completed
    private var newfc : MutableList<Feature>? = mutableListOf() // filtered version of the daily list of features

    companion object { // globally accessible companion to this activity.
        // Firebase Collection and Document names to ensure consistent naming and easy renaming throughout code
        const val collection_key = "Users"
        const val subcollection_key = "Wallet"
        const val personalwalletdoc = "Personal Wallet"
        const val sendersdoc = "Senders"

        var dailyFcData = ""                                   // JsonData that was downloaded for the day
        var collected : MutableList<String>? = mutableListOf() // list of collected coins. Local version of Wallet on Firestore
        var walletSize = 25                                    // initial amount of coins that can be deposited daily
        var coinReach = 25                                     // initial distance from within which you can collect a coin
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()   // get the current instance of the Firebase Authentication linked to the app
        if(mAuth?.currentUser == null){
            // If a user is not signed in the main activity will redirect to the login activity. Since the instance
            // of an Authentication generally persists after the app is closed, this prevents regular users from having
            // to login daily but still ensures first time users don't play the app before making an account.
            startActivity(Intent(this@MainActivity,LoginActivity::class.java))
        }
        setContentView(R.layout.activity_main) // tells the app which layout to use
        setSupportActionBar(toolbar)

        db = FirebaseFirestore.getInstance()  // get the current instance of the Firebase Firestore linked to the app
        // Use com.google.firebase.Timestamp objects instead of java.util.Date objects
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true).setPersistenceEnabled(false).build()
        db?.firestoreSettings = settings      // apply the settings to the Firestore
        if(mAuth?.currentUser != null){
            // If the current user is null, before the app starts the Login Activity, it continues carrying out lines of code briefly.
            // My database uses user IDs as the document name to hold their data. Therefore if the current user is null the
            // attempt to get a document reference will fail as mAuth?.uid!! is null
            val docRef = db?.collection(collection_key)?.document(mAuth?.uid!!) // document reference for this specific user's data
            val crKey = ItemDetailActivity.coinReachKey                                   // field name for storing the current Coin Reach.
            val wsKey = ItemDetailActivity.walletSizeKey                                  // field name for storing the current Wallet Size
            wallet = docRef?.collection(subcollection_key)                                       // Subcollection storing the collected coins
            docRef?.get()?.addOnSuccessListener {             // retrieve data from a snapshot of the document
                val doc = it.data!!
                if(doc[crKey] != null){
                    coinReach = (doc[crKey] as Long).toInt()  // retrieve data from field Coin Reach if it exists
                }
                if(doc[wsKey] != null){
                    walletSize = (doc[wsKey] as Long).toInt() // retrieve data from field Wallet Size if it exists
                }
            }
        }

        Mapbox.getInstance(this,getString(R.string.access_token)) // get mapbox map using access token
        mapView = findViewById(R.id.mapboxMapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)                                // get the map as an async task

        fab.setOnClickListener { _->   // Start Bank activity on click
            startActivity(Intent(this@MainActivity,BankActivity::class.java))
        }
        fab2.setOnClickListener { _->  // Start Shop activity on click
            startActivity(Intent(this@MainActivity,ShopActivity::class.java))
        }
        fab3.setOnClickListener { _->  // Start Shop activity on click
            startActivity(Intent(this@MainActivity,ChatActivity::class.java))
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        // mostly provided code by lecturer
        if (mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapboxMap is null")
        } else {
            map = mapboxMap
            // Set user interface options
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            // Make location information available
            enableLocation()
            downloadFeatures()  // function to start the download of the GeoJson
        }
    }

    private fun enableLocation() {
        // code provided by the lecturer
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
        // gets current date and formats it so it can be used in the download of the GeoJson
        val current = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        val currentFormatted = current.substring(0,4)+"/"+ current.substring(4,6)+"/"+ current.substring(6)
        if(downloadDate != currentFormatted) {                        // if current date is different than the stored download date then it's a new day
            downloadDate = currentFormatted                           // save new date to prevent re-download in future uses of app today
            val dailyMap = "$mapUrl$downloadDate/coinzmap.geojson"    // piece together the URL where the GeoJson is
            val myAsyncTask = DownloadFileTask(DownloadCompleteRunner)// Make a new Async Task
            dailyFcData = myAsyncTask.execute(dailyMap).get()         // Execute the download of the URL Asynchronously. Result is Json String
            map?.addSource(GeoJsonSource("geojson", dailyFcData))  // Add Json to map
            wallet?.document(personalwalletdoc)?.delete()             // day has changed so empty wallet and begin anew
            collected?.clear()                                        // similarly clear the local version of the wallet
        }
        fc = FeatureCollection.fromJson(dailyFcData).features()       // Extract the features
        addMarkers(fc)                                                // Put features on map as markers
        // set the text view of the Coin to Gold Rate key in content_main.xml
        shilRate.text = "${getGold("SHIL").roundToInt()} gold"
        dolrRate.text = "${getGold("DOLR").roundToInt()} gold"
        penyRate.text = "${getGold("PENY").roundToInt()} gold"
        quidRate.text = "${getGold("QUID").roundToInt()} gold"
    }

    private fun addMarkers(fc : MutableList<Feature>?){
        // Filters list of features by the ones already collected to prevent re-drawing
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
            //setting icon based on colour and currency
            val mycon = makeIcon(coinColour,coinSymbol)
            // add markers to map and then store markers drawn in the hashmap with the coin ID as the key. Helps removal later
            markers[it.getStringProperty("id")] = map?.addMarker(MarkerOptions().title(coinTitle).snippet("$goldVal gold")
            .icon(mycon).position(coinPos))
        }
        mapDrawn = true
    }

    fun getGold(currency : String) : Double {
        // Turns the Json string to an object to single out the rates section and then turns that to an object to translate it.
       return JSONObject("${JSONObject(dailyFcData).get("rates")}").getDouble(currency)
    }

    private fun makeIcon(colour: String, symbol: String): Icon{
        lateinit var myIcon : Icon
        val ifact = IconFactory.getInstance(this@MainActivity) // get an instance of an IconFactory
        when(colour){
            // Icons stored in xml file as typed arrays. Currency is the array's index and the arrays are separated by colour
            "#ff0000" -> myIcon = ifact.fromResource(resources.obtainTypedArray(R.array.RedMarkers).getResourceId(symbol.toInt(),R.drawable.redmarker0))
            "#0000ff" -> myIcon = ifact.fromResource(resources.obtainTypedArray(R.array.BlueMarkers).getResourceId(symbol.toInt(),R.drawable.bluemarker0))
            "#ffdf00" -> myIcon = ifact.fromResource(resources.obtainTypedArray(R.array.YellowMarkers).getResourceId(symbol.toInt(),R.drawable.yelmarker0))
            "#008000" -> myIcon = ifact.fromResource(resources.obtainTypedArray(R.array.GreenMarkers).getResourceId(symbol.toInt(),R.drawable.greenmarker0))
        }
        return myIcon
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationEngine() {
        // code provided by lecturer
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
        // code provided by lecturer except lines with comments
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
                // Gives a lifecycle of the activity and tell the location layer plugin observe it and respond to changes in it
                // This allows it to react properly onPause,Stop, or Destroy without explicitly being told
                locationLifecycle = lifecycle
                locationLifecycle.addObserver(locationLayerPlugin)
            }
        }
    }

    private fun setCameraPosition(location: Location) {
        // code provided by lecturer
        val latlng = LatLng(location.latitude, location.longitude)
        map?.animateCamera(CameraUpdateFactory.newLatLng(latlng))
    }

    override fun onLocationChanged(location: Location?) {
        // code provided by lecturer except commented lines
        if (location == null) {
            Log.d(tag, "[onLocationChanged] location is null")
        } else {
            originLocation = location
            setCameraPosition(originLocation)
            if(mapDrawn){
                // if features have been drawn on map then you can begin collecting coins
                collectCoin()
            }
        }
    }

    private fun collectCoin(){
        // once again filter fc by removing coins already collected
        newfc = fc?.filterNot{collected?.contains(it.getStringProperty("id"))!!} as MutableList<Feature>?
        newfc?.forEach {
            val coinCoord = it.geometry() as Point
            val coinLoc = Location(LocationManager.GPS_PROVIDER).apply {
                // convert coin lat and long to a location which can be compared
                latitude = coinCoord.latitude()
                longitude = coinCoord.longitude()
            }
            if (originLocation.distanceTo(coinLoc) <= coinReach) {
                // coin is within the distance from which it can be collected
                val id = it.getStringProperty("id")
                collected?.add(id) // add coin to local list of collected coins
                val data = HashMap<String,String>()
                data[id] = it.toJson()
                // add it to Firestore wallet as a map with the ID as the key and the full coin as the data
                wallet?.document(personalwalletdoc)?.set(data as Map<String, Any>, SetOptions.merge())
                // celebratory toast to user
                makeToast("Collected a ${it.getStringProperty("currency")} ${it.getStringProperty("marker-symbol")} coin!")
                map?.removeMarker(markers[id]!!) // remove the coin's associated marker from the map.
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        // code provided by lecturer
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        // code provided by lecturer
        Log.d(tag, "Permissions: $permissionsToExplain")
        // Present popup message or dialog
        makeToast("This app requires permission to access your " +
                "location to proceed.")
    }

    override fun onPermissionResult(granted: Boolean) {
        // code provided by lecturer
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            // Open a dialogue with the user
            makeToast("This app requires permission to access your " +
                    "location to proceed.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // code provided by lecturer
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.tutorial -> {
                //Rewatch tutorial
                startActivity(Intent(this@MainActivity,TutorialActivity::class.java))
                true
            }
            R.id.sign_out -> {
                // Allow user to sign out of the App and redirects back to Login activity
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
        // Restore preferences from shared preferences file
        preferencesFile = "MyPrefsFile${mAuth?.uid}"
        Log.d(tag, "Preference file is $preferencesFile")
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // use ”” as the default value (this might be the first time the app is run)
        downloadDate = settings.getString("lastDownloadDate", "")!!
        dailyFcData = settings.getString("DailyCoinData","")!!

        // get collected coins from the Firebase wallet instead. Kinda Slow
        collected?.clear()
        walletListener = wallet?.document(personalwalletdoc)?.addSnapshotListener { docSnap, e ->
            when {
                e != null -> Log.d(tag, e.message)
                docSnap != null && docSnap.exists() -> {
                    collected?.addAll(docSnap.data!!.keys)
                    collected?.asSequence()?.toSet()?.toMutableList()
                    Log.d(tag, "Snapshot listen successful")
                    Log.d(tag, "[onStart] Recalled Collected Coin List is $collected")
                    // slow access sometimes so map may need to be updated after data is accessed to remove markers that were redrawn
                    updateMap()
                }
            }
        }

        Log.d(tag, "[onStart] Recalled Last Download Date is $downloadDate")
        Log.d(tag, "[onStart] Recalled Daily Coin list")
    }

    private fun updateMap(){
        // filter fc to get list of collected coins
        val newFc = fc?.filter { collected?.contains(it.getStringProperty("id"))!! } as MutableList<Feature>?
        newFc?.forEach {
            if(markers[it.getStringProperty("id")] != null){
                // if there's a hashmap entry for that key then the marker was redrawn so remove it
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
        walletListener?.remove() // remove listener since activity is stopping. Prevents memory leaks
        if(::locationEngine.isInitialized){
            // the engine is initialized during the running of the activity not onStart so it's possible to call onStop with it uninitialized
            // can't perform the following if it's uninitialized
            locationEngine.removeLocationEngineListener(this)
            locationEngine.removeLocationUpdates()
        }

        Log.d(tag, "Writing to Preferences file $preferencesFile")
        Log.d(tag, "[onStop] Storing Last Download Date of $downloadDate")
        Log.d(tag, "[onStop] Storing Daily Coin Data")
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

    private fun makeToast(msg : String){
        // Helper function to make writing toasts faster/less wordy
        Toast.makeText(this@MainActivity,msg,Toast.LENGTH_SHORT).show()
    }

}
