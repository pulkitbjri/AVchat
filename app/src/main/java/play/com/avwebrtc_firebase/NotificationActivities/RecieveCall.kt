package play.com.avwebrtc_firebase.NotificationActivities

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_recieve_call.*
import play.com.avwebrtc_firebase.*

class RecieveCall : AppCompatActivity() {

    private lateinit var callRef: DatabaseReference
    private lateinit var callInitRef: DatabaseReference
    private lateinit var userRef: DatabaseReference


    override fun onResume() {
        super.onResume()

    }
    var id=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recieve_call)

         id=intent.getStringExtra("callerId")
        callRef = FirebaseData.database.getReference("calls/${FirebaseData.myID}/id")
        callInitRef = FirebaseData.database.getReference("callInit/${FirebaseData.myID}/id")
        userRef=   FirebaseData.database.getReference("users/$id/name")
        userRef.addListenerForSingleValueEvent(usersListener);
        callInitRef.addValueEventListener(callInitListener)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)



        val h = displayMetrics.heightPixels
        val layoutParams1 = RelativeLayout.LayoutParams(
            ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        layoutParams1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        layoutParams1.addRule(RelativeLayout.CENTER_HORIZONTAL)
        layoutParams1.bottomMargin = h / 9

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
        call2.setLayoutParams(layoutParams1)
        cancelcall.setLayoutParams(layoutParams1)
        var parans : RelativeLayout.LayoutParams=RelativeLayout.LayoutParams(cancelcall.layoutParams)
        parans.removeRule(RelativeLayout.CENTER_HORIZONTAL)
        parans.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        parans.bottomMargin=h/9
        parans.leftMargin= convertDpToPx(this@RecieveCall, 32F).toInt()
        cancelcall.setLayoutParams(parans)


        acceptcall.setLayoutParams(layoutParams1)
        var parans1 : RelativeLayout.LayoutParams=RelativeLayout.LayoutParams(cancelcall.layoutParams)
        parans1.removeRule(RelativeLayout.CENTER_HORIZONTAL)
        parans1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        parans1.addRule(RelativeLayout.ALIGN_PARENT_END)
        parans1.bottomMargin=h/9
        parans1.rightMargin= convertDpToPx(this@RecieveCall, 32F).toInt()
        acceptcall.setLayoutParams(parans1)

        call2.setOnTouchListener { v, event ->
            //            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
            val width = v.getWidth()
            val xPos = event.getRawX()
            val yPos = event.getRawY()

            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                layoutParams1.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                val layoutParams = v.getLayoutParams() as RelativeLayout.LayoutParams
                layoutParams.topMargin = event.getRawY().toInt()
                Log.i(
                    "",
                    "onCreate: " + layoutParams.topMargin + "     " + h + "    " + convertPxToDp(
                        this@RecieveCall,
                        h.toFloat()
                    )
                )
                if (layoutParams.topMargin >= h / 2 && layoutParams.topMargin <= 3 * h / 4) {
                    call2.setLayoutParams(layoutParams)

                }


            }
            if(event.action==MotionEvent.ACTION_UP)
            {
                startVideoCall(id)
                callRef.removeValue()
                callInitRef.removeEventListener(callInitListener)
                callInitRef.removeValue()
            }
            false
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
        VoiceCallActivity.startCall(this@RecieveCall, key,id)
        this.finish()
    }

    fun convertPxToDp(context: Context, px: Float): Float {
        return px / context.resources.displayMetrics.density
    }

    fun convertDpToPx(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

}
