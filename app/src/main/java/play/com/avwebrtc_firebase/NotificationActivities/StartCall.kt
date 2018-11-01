package play.com.avwebrtc_firebase.NotificationActivities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_start_call.*
import play.com.avwebrtc_firebase.FirebaseData
import play.com.avwebrtc_firebase.R

class StartCall : AppCompatActivity() {
    var pos="";
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_call)

        var pos=intent.getStringExtra("callerId")

        FirebaseData.database.getReference("callInit/$pos/id")
            .setValue("${FirebaseData.myID}")

        callername.setText(intent.getStringExtra("name"))


    }


    fun reject( v : View)
    {
        FirebaseData.database.getReference("callInit/$pos/id").removeValue()
        super.onBackPressed()
    }

    override fun onBackPressed() {

    }
}

