package com.lanhnh.airquality

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.lanhnh.airquality.data.model.Device

class MainActivity : AppCompatActivity(), GoogleMap.OnMarkerClickListener, OnMapReadyCallback {

    lateinit var mapFragment: SupportMapFragment
    lateinit var googleMap: GoogleMap
    private var devices = mutableListOf<Device>()
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
                    devices.forEach {
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(it.lat, it.long))
                                .title(it.id)
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        val intent = Intent(this@MainActivity, DetailActivity::class.java)
        val bundle = Bundle()
        bundle.putString(EXTRA_TITLE, marker?.title ?: "")
        intent.putExtras(bundle)
        startActivity(intent)

        return false
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
        googleMap.setOnMarkerClickListener(this)
    }

    companion object {
        const val EXTRA_TITLE = "EXTRA_TITLE"
    }
}
