package com.lanhnh.airquality

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.lanhnh.airquality.data.Air
import com.lanhnh.airquality.screen.air.AirAdapter
import kotlinx.android.synthetic.main.activity_detail.*

class DetailActivity : AppCompatActivity() {

    private var title: String = "device-id-1"
    private var itemList = mutableListOf<Air>()
    private var airAdapter: AirAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val bundle = intent.extras
        if (bundle != null) {
            title = bundle.getString(MainActivity.EXTRA_TITLE, "")
        }
        airAdapter = AirAdapter(this, itemList) { air ->
            itemClick(air)
        }

        airAdapter?.let {
            rv_air_quality.apply {
                adapter = it
            }
        }

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("data").child(title).orderByKey()
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    val id = it.child("id").value as Long
                    val deviceId = it.child("device-id").value as String
                    val time = it.child("time").value as String
                    val dust = it.child("data-device").child("0").value as Double
                    val airQuality = it.child("data-device").child("1").value
                    itemList.add(Air(id, deviceId, dust, airQuality.toString().toDouble(), time))
                }
                airAdapter?.notifyDataSetChanged();
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun itemClick(air: Air?) {
        Toast.makeText(this, air?.time ?: "", Toast.LENGTH_SHORT).show()
    }
}
