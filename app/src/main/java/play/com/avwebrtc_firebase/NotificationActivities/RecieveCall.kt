package play.com.avwebrtc_firebase.NotificationActivities

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_recieve_call.*
import play.com.avwebrtc_firebase.ContactData
import play.com.avwebrtc_firebase.FirebaseData
import play.com.avwebrtc_firebase.R
import play.com.avwebrtc_firebase.VideoCallActivity

class RecieveCall : AppCompatActivity() {

    private lateinit var callRef: DatabaseReference
    private lateinit var callInitRef: DatabaseReference
    private lateinit var userRef: DatabaseReference


    override fun onResume() {
        super.onResume()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recieve_call)

        var id=intent.getStringExtra("callerId")
        callRef = FirebaseData.database.getReference("calls/${FirebaseData.myID}/id")
        callInitRef = FirebaseData.database.getReference("callInit/${FirebaseData.myID}/id")
        userRef=   FirebaseData.database.getReference("users/$id/name")
        userRef.addListenerForSingleValueEvent(usersListener);
        callInitRef.addValueEventListener(callInitListener)


        acceptcall.setOnClickListener {
            startVideoCall(id)
            callRef.removeValue()
            callInitRef.removeEventListener(callInitListener)
            callInitRef.removeValue()
        }

        cancelcall.setOnClickListener {
            callInitRef.removeEventListener(callInitListener)
            callInitRef.removeValue()
            onBackPressed()
        }

    }

    private val usersListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.exists()) {
                callername.setText(dataSnapshot.value.toString())
            }

        }

    }
    private val callInitListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
        }
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (!dataSnapshot.exists()) {
                back()
            }
        }
    }

    private fun back() {
        onBackPressed()
    }
    override fun onPause() {
        super.onPause()
        callInitRef.removeEventListener(callInitListener)

    }
    private fun startVideoCall(key: String) {
        FirebaseData.getCallStatusReference(FirebaseData.myID).setValue(true)
        FirebaseData.getCallIdReference(key).onDisconnect().removeValue()
        FirebaseData.getCallIdReference(key).setValue(FirebaseData.myID)
        VideoCallActivity.startCall(this@RecieveCall, key)
        this.finish()
    }


}
