package com.lanhnh.airquality

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
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
        val myRef = database.getReference("data").child(title)
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot.value)
                val id = dataSnapshot.child("id").value as Long
                val deviceId = dataSnapshot.child("device-id").value as String
                val time = (dataSnapshot.child("time").value ?: "") as String
                val dust = (dataSnapshot.child("data-device").child("0").value ?: 0.00) as Double
                val airQuality = dataSnapshot.child("data-device").child("1").value ?: 0.00
                airAdapter?.insert(Air(id, deviceId, dust, airQuality.toString().toDouble(), time))
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildChanged: ${dataSnapshot.key}")
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.key!!)
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.key!!)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "postComments:onCancelled", databaseError.toException())
                Toast.makeText(applicationContext, "Failed to load comments.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        myRef.addChildEventListener(childEventListener)

//        myRef.addChildEventListener((object : ChildEventListener) {
//
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                dataSnapshot.children.forEach {
//                    val id = it.child("id").value as Long
//                    val deviceId = it.child("device-id").value as String
//                    val time = it.child("time").value as String
//                    val dust = it.child("data-device").child("0").value as Double
//                    val airQuality = it.child("data-device").child("1").value
//                    itemList.add(Air(id, deviceId, dust, airQuality.toString().toDouble(), time))
//                }
//                airAdapter?.notifyDataSetChanged();
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//            }
//        })
    }

    private fun itemClick(air: Air?) {
        Toast.makeText(this, air?.time ?: "", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "DetailActivity"
    }
}
