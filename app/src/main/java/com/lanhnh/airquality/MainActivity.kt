package com.lanhnh.airquality

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import com.google.maps.android.PolyUtil
import com.lanhnh.airquality.data.model.Device
import com.lanhnh.airquality.data.model.DirectionResponses
import com.lanhnh.airquality.extentions.safeClick
import com.lanhnh.airquality.google.GoogleAPIService
import com.lanhnh.airquality.google.RetrofitClient
import com.lanhnh.airquality.screen.air.TutorialActivity
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class MainActivity : AppCompatActivity(), GoogleMap.OnMarkerClickListener, OnMapReadyCallback {

    lateinit var mapFragment: SupportMapFragment
    lateinit var googleMap: GoogleMap
    private var devices = mutableListOf<Device>()
    private var markers = mutableListOf<Marker>()
    private lateinit var marker: Marker
    private lateinit var myLocation: LatLng
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var isAllowAccessLocation = false
    private var locationUpdateState = false
    private lateinit var locationRequest: LocationRequest
    private var polylineList = mutableListOf<LatLng>()
    private var v: Float? = null
    private var lat: Double? = null
    private var lng: Double? = null
    private var startPosition: LatLng? = null
    private var endPosition: LatLng? = null
    private var index: Int = 0
    private var next: Int = 0
    private lateinit var polylineOptions: PolylineOptions
    private lateinit var blackPolylineOptions: PolylineOptions
    private lateinit var blackPolyline: Polyline
    private lateinit var greyPolyline: Polyline
    private lateinit var googleAPIService: GoogleAPIService
    private var destination: String = ""
    private var polyline: Polyline? = null
    private var destinationPosition: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("devices")
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    val id = it.child("id").value as String
                    val lat = it.child("lat").value as Double
                    val long = it.child("long").value as Double
                    val airQuality = it.child("lastAir").value?.toString()?.toDouble()
                    val dust = it.child("lastPM25").value?.toString()?.toDouble()
                    devices.add(Device(id, lat, long, airQuality ?: 0.0, dust ?: 0.0))
                }

                if (this@MainActivity::googleMap.isInitialized) {
                    devices.forEach {
                        createMarkers(it);
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val id = dataSnapshot.child("id").value as String
                val lat = dataSnapshot.child("lat").value as Double
                val long = dataSnapshot.child("long").value as Double
                val airQuality = dataSnapshot.child("lastAir").value?.toString()?.toDouble()
                val dust = dataSnapshot.child("lastPM25").value?.toString()?.toDouble()
                val device = Device(id, lat, long, airQuality ?: 0.0, dust ?: 0.0)
                val marker = markers.filter { it.snippet == id }.first()
                marker.remove();
                createMarkers(device)
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        }
        myRef.addChildEventListener(childEventListener)
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                lastLocation = result.lastLocation
                placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
                if (!isAllowAccessLocation) {
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                lastLocation.latitude,
                                lastLocation.longitude
                            ), DEFAULT_ZOOM
                        )
                    )
                    isAllowAccessLocation = true
                }
            }
        }
        createLocationRequest()
        setSupportActionBar(toolbar)
        onClickListener()
        googleAPIService = RetrofitClient.create()
    }

    private fun onClickListener() {
        btn_search.safeClick {
            val text = et_search.text.toString()
            if (text.isNotEmpty()) {
                destination = text
                destination = destination.replace(" ", "+")
                searchDirection(destination)
            }
        }
    }

    private fun searchDirection(destination: String) {
        val requestURL =
            "https://maps.googleapis.com/maps/api/directions/json?" + "mode=drivings&" + "transit_routing_preference=less_driving&" + "origin=" + myLocation.latitude + "," + myLocation.longitude + "&destination=" + destination + "&" + "key=" + resources.getString(
                R.string.google_directions_key
            )
        val from = myLocation.latitude.toString() + "," + myLocation.longitude
        val key = resources.getString(R.string.google_app_id)
        googleAPIService.getDirection(requestURL)
            .enqueue(object : Callback<DirectionResponses> {
                override fun onResponse(
                    call: Call<DirectionResponses>,
                    response: Response<DirectionResponses>
                ) {
                    drawPolyline(response)
                    Log.d("GG MAP API SUCCESS", response.message())
                }

                override fun onFailure(call: Call<DirectionResponses>, t: Throwable) {
                    Log.e("GG MAP API ERROR", t.localizedMessage)
                }
            })
    }

    private fun drawPolyline(response: Response<DirectionResponses>) {
        this.polyline?.remove()
        response.body()?.routes?.let { routes ->
            if (routes.isNotEmpty()) {
                val shape = routes[0]?.overviewPolyline?.points
                val polylineOptions = PolylineOptions()
                    .addAll(PolyUtil.decode(shape))
                    .width(8f)
                    .color(Color.RED)
                this.polyline = googleMap.addPolyline(polylineOptions)
            } else {
                Toast.makeText(this, "Không tìm thấy địa chỉ này!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createMarkers(vararg devices: Device) {
        val myDrawable =
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.location_marker_orange,
                null
            )
        val markerBitmap = (myDrawable as BitmapDrawable?)!!.bitmap
        val myDrawable1 =
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.location_marker_purple,
                null
            )
        val markerBitmapWarmingLevel1 = (myDrawable1 as BitmapDrawable?)!!.bitmap
        val myDrawable2 =
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.location_marker_green,
                null
            )
        val markerBitmapWarmingLevel2 = (myDrawable2 as BitmapDrawable?)!!.bitmap

        devices.forEach {
            val location = LatLng(it.lat, it.long)
            val icon = if (it.lastAir > 50 || it.lastPM25 > 50) {
                BitmapDescriptorFactory.fromBitmap(markerBitmap)
            } else if (it.lastAir > 10 || it.lastPM25 > 10) {
                BitmapDescriptorFactory.fromBitmap(markerBitmapWarmingLevel1)
            } else {
                BitmapDescriptorFactory.fromBitmap(markerBitmapWarmingLevel2)
            }
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .icon(icon)
                    .position(LatLng(it.lat, it.long))
                    .title(getAddress(location))
                    .snippet(it.id)
            )
            markers.add(0, marker)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_tutorial -> {
                startActivity(Intent(this@MainActivity, TutorialActivity::class.java))
                return true
            }
            R.id.action_search -> {
                ll_search_view.visibility =
                    if (ll_search_view.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = INTERVAL
        locationRequest.fastestInterval = FASTEST_INTERVAL
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        return false
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap.isTrafficEnabled = false
        googleMap.isIndoorEnabled = false
        googleMap.isBuildingsEnabled = false
        googleMap.uiSettings.isZoomControlsEnabled = true


        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnInfoWindowClickListener {
            it.let { snippet ->
                val intent = Intent(this@MainActivity, DetailActivity::class.java)
                val bundle = Bundle()
                bundle.putString(EXTRA_DEVICE_ID, it.snippet)
                bundle.putString(EXTRA_TITLE, it.title)
                intent.putExtras(bundle)
                startActivity(intent)
            }
        }
    }

    private fun getAddress(latLng: LatLng): String {
        val geoCoder = Geocoder(this)
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            addresses = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (null != addresses && !addresses.run { isEmpty() }) {
                address = addresses[0]
                try {
                    addressText += address.getAddressLine(0).toString()
                } catch (ignored: Exception) {
                }
            }
        } catch (e: IOException) {
            Log.e("MainActivity", e.localizedMessage ?: "error")
        }
        return addressText
    }

    override fun onStop() {
        super.onStop()
        isAllowAccessLocation = false
    }

    private fun placeMarkerOnMap(location: LatLng) {
        if (this::marker.isInitialized) marker.remove()

        this.myLocation = location
        marker = googleMap.addMarker(
            MarkerOptions().position(location).title(getAddress(location))
        )
    }

    companion object {
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_DEVICE_ID = "EXTRA_DEVICE_ID"
        private const val DEFAULT_ZOOM = 14f
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        private const val INTERVAL = 10000L
        private const val FASTEST_INTERVAL = 5000L
    }
}
