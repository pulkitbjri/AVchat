package play.com.avwebrtc_firebase.NotificationActivities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_start_call.*
import play.com.avwebrtc_firebase.FirebaseData
import play.com.avwebrtc_firebase.R
import play.com.avwebrtc_firebase.VoiceCallActivity

class StartCall : AppCompatActivity() {

    private lateinit var callRef: DatabaseReference
    private lateinit var callInitRef: DatabaseReference

    var pos=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_call)

         pos=intent.getStringExtra("callerId")
        callRef = FirebaseData.database.getReference("calls/${FirebaseData.myID}/id")
        callInitRef = FirebaseData.database.getReference("callInit/$pos/id")
        callInitRef.addValueEventListener(callInitListener)

        FirebaseData.database.getReference("callInit/$pos/id")
            .setValue("${FirebaseData.myID}")

        callername.setText(intent.getStringExtra("name"))

        callRef.addValueEventListener(callListener)

        cancelcall.setOnClickListener {
            callInitRef.removeEventListener(callInitListener)
            FirebaseData.database.getReference("callInit/$pos/id").removeValue()
            callInitRef.removeValue()
            super.onBackPressed()
        }
    }




    private val callListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
        }
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.exists()) {
                receiveVideoCall(dataSnapshot.getValue(String::class.java)!!)
                callRef.removeValue()
//                callInitRef.removeValue()
            }
        }

    }

    private val callInitListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
        }
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (!dataSnapshot.exists() && dataSnapshot.value==null) {
                Log.d("dasd",dataSnapshot.toString())
                back()
            }
        }
    }


    private fun receiveVideoCall(key: String) {
        VoiceCallActivity.receiveCall(this, key,pos)
        this.finish()
    }

    override fun onPause() {
        super.onPause()
        callInitRef.removeEventListener(callInitListener)
        callRef.removeEventListener(callListener)

    }
    private fun back() {
        onBackPressed()
    }
//    override fun onBackPressed() {
//
//    }
}

