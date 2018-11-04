package com.example.dimit.coinz

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.util.JsonToken
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.example.dimit.coinz.R.string.sign_out
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity(),OnMapReadyCallback,
      LocationEngineListener,PermissionsListener{

    private val tag = "MainActivity"
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var mAuth : FirebaseAuth? = null
    private lateinit var originLocation : Location
    private lateinit var permissionsManager : PermissionsManager
    private lateinit var locationEngine : LocationEngine
    private lateinit var locationLayerPlugin : LocationLayerPlugin
    private val mapUrl : String = "http://homepages.inf.ed.ac.uk/stg/coinz/"
    private var downloadDate = "" // Format: YYYY/MM/DD
    private var dailyFcData = "" //JsonData that was downloaded for the day
    private val preferencesFile = "MyPrefsFile" // for storing preferences
    private var fc : MutableList<Feature>? = null //daily feature collection list of features
    private var wallet : MutableList<Feature>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        if(mAuth?.currentUser == null){
            startActivity(Intent(this@MainActivity,LoginActivity::class.java))
        }
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

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
            downloadMap()
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

    private fun downloadMap(){
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
        drawCoins(fc)
    }

    private fun drawCoins(fc : MutableList<Feature>?){
        fc?.forEach {
            val coinCoord = it.geometry() as Point
            val coinPos = LatLng(coinCoord.latitude(), coinCoord.longitude())
            val coinColour = it.getStringProperty("marker-color").toString()
            val coinTitle = it.getStringProperty("currency")
            val coinSnippet = it.getStringProperty("marker-symbol")
            //setting icon based on colour
            //var myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.blue_marker)
            /*when (coinColour) {
                "#0000ff" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.blue_marker)
                "#ffdf00" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.yellow_marker)
                "#008000" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.green_marker)
                "#ff0000" -> myIcon = IconFactory.getInstance(this@MainActivity).fromResource(R.drawable.red_marker)
            }*/
            map?.addMarker(MarkerOptions().title(coinTitle).snippet(coinSnippet).position(coinPos))
        }
    }

   /* private fun drawableToIcon(context: Context,drawableId : Int,colour :String): Icon {
        val vectorDrawable = ResourcesCompat.getDrawable(context.resources,drawableId,context.theme)!!
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
                     vectorDrawable.intrinsicHeight,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0,0,canvas.width,canvas.height)
        DrawableCompat.setTint(vectorDrawable,Color.parseColor(colour))
        vectorDrawable.draw(canvas)
        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }*/

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
            fc?.forEach{
                val coinCoord = it.geometry() as Point
                val coinLoc = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = coinCoord.latitude()
                    longitude = coinCoord.longitude()
                }
                if(originLocation.distanceTo(coinLoc)<= 25.0){
                    wallet?.add(it)
                    Toast.makeText(this@MainActivity,"Coin Collected ${it.getStringProperty("id")}",Toast.LENGTH_LONG).show()
                }
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
        Toast.makeText(this@MainActivity,"This app requires permission to access your " +
                "location to proceed.", Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            // Open a dialogue with the user
            Toast.makeText(this@MainActivity,"This app requires permission to access your " +
                    "location to proceed.", Toast.LENGTH_LONG).show()
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
                Toast.makeText(this@MainActivity,"Sign out successful",Toast.LENGTH_LONG).show()
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
        // Write a message to ”logcat” (for debugging purposes)
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
}
