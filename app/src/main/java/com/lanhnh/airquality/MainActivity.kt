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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.lanhnh.airquality.data.model.Device
import java.io.IOException

class MainActivity : AppCompatActivity(), GoogleMap.OnMarkerClickListener, OnMapReadyCallback {

    lateinit var mapFragment: SupportMapFragment
    lateinit var googleMap: GoogleMap
    private var devices = mutableListOf<Device>()
    private lateinit var marker: Marker
    private lateinit var myLocation: LatLng
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var isAllowAccessLocation = false
    private var locationUpdateState = false
    private lateinit var locationRequest: LocationRequest

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
                    devices.add(Device(id, lat, long))
                }

                if (this@MainActivity::googleMap.isInitialized) {
                    val myDrawable =
                        ResourcesCompat.getDrawable(resources, R.drawable.dvlocation, null)
                    val markerBitmap = (myDrawable as BitmapDrawable?)!!.bitmap

                    devices.forEach {
                        val location = LatLng(it.lat, it.long)
                        googleMap.addMarker(
                            MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                                .position(LatLng(it.lat, it.long))
                                .title(getAddress(location))
                                .snippet(it.id)
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
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
        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnInfoWindowClickListener {
            it.snippet?.let { snippet ->
                val intent = Intent(this@MainActivity, DetailActivity::class.java)
                val bundle = Bundle()
                bundle.putString(EXTRA_TITLE, snippet)
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

        googleMap.addCircle(
            CircleOptions().center(location).radius(5000.0).strokeWidth(3f).strokeColor(Color.GRAY)
                .fillColor(
                    Color.argb(10, 97, 149, 237)
                )
        )
    }

    companion object {
        const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val DEFAULT_ZOOM = 12f
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        private const val INTERVAL = 10000L
        private const val FASTEST_INTERVAL = 5000L
    }
}
