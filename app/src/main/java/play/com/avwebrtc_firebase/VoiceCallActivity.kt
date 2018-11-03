package play.com.avwebrtc_firebase

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_recieve_call.*
import kotlinx.android.synthetic.main.audio_call_laout.view.*
import play.com.avwebrtc_firebase.VoiceCall.VoiceCallSession
import play.com.avwebrtc_firebase.VoiceCall.VoiceCallStatus


class VoiceCallActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_AUDIO_PERMISSION_REQUEST = 1
        private const val TAG = "VideoCallActivity"

        fun startCall(context: Context, id: String, callerid: String) {
            val starter = Intent(context, VoiceCallActivity::class.java)
            starter.putExtra("offer", true)
            starter.putExtra("id", id)
            starter.putExtra("callerid", callerid)
            context.startActivity(starter)
        }

        fun receiveCall(context: Context, id: String, callerid: String) {
            val starter = Intent(context, VoiceCallActivity::class.java)
            starter.putExtra("offer", false)
            starter.putExtra("id", id)
            starter.putExtra("callerid", callerid)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container)

        if (savedInstanceState == null) {
            val fragment = VoiceCallFragment()
            fragment.isOffer = intent.getBooleanExtra("offer", false)
            fragment.id = intent.getStringExtra("id")
            fragment.callerid = intent.getStringExtra("callerid")

            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment, "CallFragment")
                    .addToBackStack(null)
                    .commit()
        }
    }


    class VoiceCallFragment : Fragment() {
        private var voiceSession: VoiceCallSession? = null
        private var audioManager: AudioManager? = null
        private var savedMicrophoneState: Boolean? = null
        private var savedAudioMode: Int? = null

        var isOffer = false
        lateinit var id: String
        lateinit var callerid : String
        private lateinit var userRef: DatabaseReference


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            retainInstance = true

            audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            savedAudioMode = audioManager?.mode
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION

            savedMicrophoneState = audioManager?.isMicrophoneMute
            speaker(false)
            microphone(false)


        }
        private val usersListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    root.callername.setText(dataSnapshot.value.toString())
                }

            }

        }
        fun microphone(type: Boolean)
        {
            audioManager?.isMicrophoneMute =type

        }
        fun speaker(type: Boolean)
        {
            audioManager?.isSpeakerphoneOn = type

        }
        lateinit var root :View
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
             root = inflater.inflate(R.layout.audio_call_laout, container, false)

            val hangup: FloatingActionButton = root.findViewById(R.id.hangup_button)
            hangup.setOnClickListener { activity!!.finish() }

            root.speaker.setOnClickListener {
                if (audioManager?.isSpeakerphoneOn==true)
                {
                    speaker(false)
                }
                else
                    speaker(true)

            }
            root.microphone.setOnClickListener {
                if (audioManager?.isMicrophoneMute==true)
                {
                    microphone(false)
                }
                else
                    microphone(true)

            }
            userRef=   FirebaseData.database.getReference("users/$callerid/name")

            userRef.addListenerForSingleValueEvent(usersListener);

            return root
        }

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)
            if (savedInstanceState == null)
                handlePermissions()
        }

        override fun onDestroyView() {
            super.onDestroyView()
        }

        override fun onDestroy() {
            super.onDestroy()
            voiceSession?.terminate()

            if (savedAudioMode !== null) {
                audioManager?.mode = savedAudioMode!!
            }
            if (savedMicrophoneState != null) {
                audioManager?.isMicrophoneMute = savedMicrophoneState!!
            }
        }

        private fun onStatusChanged(newStatus: VoiceCallStatus) {
            Log.d(TAG, "New call status: $newStatus")
            if (!isAdded) {
                Log.w(TAG, "onStatusChanged, but is not added : $newStatus")
                return
            }
            activity?.runOnUiThread {
                when (newStatus) {
                    VoiceCallStatus.FINISHED -> activity!!.finish()

                    else -> {
//                        statusTextView.text = resources.getString(newStatus.label)
//                        statusTextView.setTextColor(ContextCompat.getColor(context!!, newStatus.color))

                        Toast.makeText(activity,resources.getString(newStatus.label),Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private fun handlePermissions() {
            val canRecordAudio = ContextCompat.checkSelfPermission(context!!, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if ( !canRecordAudio) {
                ActivityCompat.requestPermissions(activity!!, arrayOf( Manifest.permission.RECORD_AUDIO), CAMERA_AUDIO_PERMISSION_REQUEST)
            } else {
                startVideoSession()
            }
        }

        private fun startVideoSession() {
            voiceSession = VoiceCallSession.connect(context!!, id, isOffer, this::onStatusChanged)
            initVideoVews()
        }

        private fun initVideoVews() {
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            Log.w(TAG, "onRequestPermissionsResult: $requestCode $permissions $grantResults")
            when (requestCode) {
                CAMERA_AUDIO_PERMISSION_REQUEST -> {
                    if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                        startVideoSession()
                    } else {
                        activity!!.finish()
                    }
                    return
                }
            }
        }

    }
}
