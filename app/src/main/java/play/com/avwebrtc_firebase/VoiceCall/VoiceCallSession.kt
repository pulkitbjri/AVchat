package play.com.avwebrtc_firebase.VoiceCall


import android.content.Context
import android.util.Log
import android.widget.Toast
import play.com.avwebrtc_firebase.FirebaseData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import org.webrtc.*
import play.com.avwebrtc_firebase.R
import play.com.avwebrtc_firebase.videocall.*
import java.util.concurrent.Executors



enum class VoiceCallStatus(val label: Int, val color: Int) {
    UNKNOWN(R.string.status_unknown, R.color.colorUnknown),
    CONNECTING(R.string.status_connecting, R.color.colorConnecting),
    DIALING(R.string.status_dialing, R.color.colorMatching),
    FAILED(R.string.status_failed, R.color.colorFailed),
    CONNECTED(R.string.status_connected, R.color.colorConnected),
    FINISHED(R.string.status_finished, R.color.colorConnected);
}

data class VoiceRenderers(private var localView: SurfaceViewRenderer?, private var remoteView: SurfaceViewRenderer?) {
    val localRenderer: (VideoRenderer.I420Frame) -> Unit = { f ->
        localView?.renderFrame(f) ?: sink(f)
    }
    //            if (localView == null) this::sink else { f -> localView!!.renderFrame(f) }
    val remoteRenderer: (VideoRenderer.I420Frame) -> Unit = { f ->
        remoteView?.renderFrame(f) ?: sink(f)
    }
//            if (remoteView == null) this::sink else { f -> remoteView!!.renderFrame(f) }


    fun updateViewRenders(localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer) {
        this.localView = localView
        this.remoteView = remoteView
    }

    private fun sink(frame: VideoRenderer.I420Frame) {
        Log.w("VideoRenderer", "Missing surface view, dropping frame")
        VideoRenderer.renderFrameDone(frame)
    }
}

class VoiceCallSession(
    private val context: Context,
    private val isOfferingPeer: Boolean,
    private val onStatusChangedListener: (VoiceCallStatus) -> Unit,
    private val signaler: FirebaseSignaler
)
{

    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null

    private var mediaStream: MediaStream? = null

    private val eglBase = EglBase.create()

    val renderContext: EglBase.Context
        get() = eglBase.eglBaseContext

    class SimpleRTCEventHandler(
            private val onIceCandidateCb: (IceCandidate) -> Unit,
            private val onAddStreamCb: (MediaStream) -> Unit,
            private val onRemoveStreamCb: (MediaStream) -> Unit) : PeerConnection.Observer {

        override fun onIceCandidate(candidate: IceCandidate?) {
            if (candidate != null) onIceCandidateCb(candidate)
        }

        override fun onAddStream(stream: MediaStream?) {
            if (stream != null) onAddStreamCb(stream)
        }

        override fun onRemoveStream(stream: MediaStream?) {
            if (stream != null) onRemoveStreamCb(stream)
        }

        override fun onDataChannel(chan: DataChannel?) {
            Log.w(TAG, "onDataChannel: $chan")
        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {
            Log.w(TAG, "onIceConnectionReceivingChange: $p0")
        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
            Log.w(TAG, "onIceConnectionChange: $newState")
        }

        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
            Log.w(TAG, "onIceGatheringChange: $newState")
        }

        override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
            Log.w(TAG, "onSignalingChange: $newState")
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
            Log.w(TAG, "onIceCandidatesRemoved: $candidates")
        }

        override fun onRenegotiationNeeded() {
            Log.w(TAG, "onRenegotiationNeeded")
        }

        override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
    }

    private val factory: PeerConnectionFactory by lazy {
        //Initialize PeerConnectionFactory globals.
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .setEnableVideoHwAcceleration(true)
                .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        val options = PeerConnectionFactory.Options()
        options.networkIgnoreMask = 0
        options.disableNetworkMonitor=false
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
                renderContext, /* enableIntelVp8Encoder */true, /* enableH264HighProfile */true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(renderContext)
        PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory()
    }


    init {
        signaler.messageHandler = this::onMessage
        this.onStatusChangedListener(VoiceCallStatus.DIALING)
        executor.execute(this::init)
    }

    private fun init() {
        val iceServers = arrayListOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        createPeerConnection(iceServers)
        setupMediaDevices()

        call()
    }

    private fun call() {
        val ref = FirebaseData.getCallStatusReference(signaler.callerID)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue(Boolean::class.java)!!) {
                    ref.removeEventListener(this)
                    onStatusChangedListener(VoiceCallStatus.CONNECTING)
                    start()
                }
            }

            override fun onCancelled(e: DatabaseError) {
                Log.e(TAG, "databaseError:", e.toException())
                ref.removeEventListener(this)
            }
        })
    }


    /**
     * Creating the local peerconnection instance
     */
    private fun createPeerConnection(iceServers: List<PeerConnection.IceServer>) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.apply {
            // TCP candidates are only useful when connecting to a server that supports ICE-TCP.
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY

            enableCpuOveruseDetection = true
            enableDtlsSrtp = true
            // Use ECDSA encryption.
            keyType = PeerConnection.KeyType.ECDSA
        }

        val rtcEvents = SimpleRTCEventHandler(
            this::handleLocalIceCandidate,
            this::addRemoteStream,
            this::removeRemoteStream
        )

        peerConnection = factory.createPeerConnection(rtcConfig, rtcEvents)
    }


    private fun start() {
        signaler.init()
        executor.execute(this::maybeCreateOffer)
    }

    private fun maybeCreateOffer() {
        if (isOfferingPeer) {
            peerConnection?.createOffer(SDPCreateCallback(this::createDescriptorCallback), defaultPcConstraints())
        }
    }

    private fun defaultPcConstraints(): MediaConstraints {
        val pcConstraints = MediaConstraints()
        pcConstraints.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
//        pcConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        return pcConstraints
    }
    private fun handleLocalIceCandidate(candidate: IceCandidate) {
        Log.w(TAG, "Local ICE candidate: $candidate")
        signaler.sendCandidate(candidate.sdpMLineIndex, candidate.sdpMid, candidate.sdp)
    }

    private fun addRemoteStream(stream: MediaStream) {
        onStatusChangedListener(VoiceCallStatus.CONNECTED)
        Log.i(TAG, "Got remote stream: $stream")
//        executor.execute {
//            if (stream.videoTracks.isNotEmpty()) {
//                val remoteVideoTrack = stream.videoTracks.first()
//                remoteVideoTrack.setEnabled(true)
////                remoteVideoTrack.addRenderer(VideoRenderer(videoRenderers.remoteRenderer))
//            }
//        }
    }

    private fun removeRemoteStream(@Suppress("UNUSED_PARAMETER") _stream: MediaStream) {
        // We lost the stream, lets finish
        Log.w(TAG, "Bye")
        onStatusChangedListener(VoiceCallStatus.FINISHED)
    }

    private fun handleRemoteCandidate(label: Int, id: String, strCandidate: String) {
        Log.i(TAG, "Got remote ICE candidate $strCandidate")
        executor.execute {
            val candidate = IceCandidate(id, label, strCandidate)
            peerConnection?.addIceCandidate(candidate)
        }
    }

    private fun setupMediaDevices() {
        mediaStream = factory.createLocalMediaStream(STREAM_LABEL)

        audioSource = factory.createAudioSource(createAudioConstraints())
        val audioTrack = factory.createAudioTrack(AUDIO_TRACK_LABEL, audioSource)

        mediaStream?.addTrack(audioTrack)

        peerConnection?.addStream(mediaStream)
    }


    private fun createAudioConstraints(): MediaConstraints {
        val audioConstraints = MediaConstraints()
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "false"))
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "false"))
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        return audioConstraints
    }

    private fun handleRemoteDescriptor(sdp: String) {
        if (isOfferingPeer) {
            peerConnection?.setRemoteDescription(SDPSetCallback { setError ->
                if (setError != null) {
                    Log.e(
                        TAG,
                        "setRemoteDescription failed: $setError"
                    )
                }
            }, SessionDescription(SessionDescription.Type.ANSWER, sdp))
        } else {
            peerConnection?.setRemoteDescription(SDPSetCallback { setError ->
                if (setError != null) {
                    Log.e(
                        TAG,
                        "setRemoteDescription failed: $setError"
                    )
                } else {
                    peerConnection?.createAnswer(
                        SDPCreateCallback(this::createDescriptorCallback),
                        MediaConstraints()
                    )
                }
            }, SessionDescription(SessionDescription.Type.OFFER, sdp))
        }
    }

    private fun createDescriptorCallback(result: SDPCreateResult) {
        when (result) {
            is SDPCreateSuccess -> {
                peerConnection?.setLocalDescription(SDPSetCallback({ setResult ->
                    Log.i(
                        TAG,
                        "SetLocalDescription: $setResult"
                    )
                }), result.descriptor)
                signaler.sendSDP(result.descriptor.description)
            }
            is SDPCreateFailure -> Log.e(TAG, "Error creating offer: ${result.reason}")
        }
    }

    private fun onMessage(message: ClientMessage) {
        when (message) {
            is SDPMessage -> {
                handleRemoteDescriptor(message.sdp)
            }
            is ICEMessage -> {
                handleRemoteCandidate(message.label, message.id, message.candidate)
            }
            is PeerLeft -> {
                Log.w(TAG, "Bye2")
                onStatusChangedListener(VoiceCallStatus.FINISHED)

            }
        }
    }


    fun terminate() {
        signaler.close()


        audioSource?.dispose()

        peerConnection?.dispose()

        factory.dispose()

        eglBase.release()
    }



    companion object {

        fun connect(context: Context, id: String, isOffer: Boolean, callback: (VoiceCallStatus) -> Unit): VoiceCallSession {
            val firebaseHandler = FirebaseSignaler(id)
            return VoiceCallSession(
                context,
                isOffer,
                callback,
                firebaseHandler
            )
        }

        private const val STREAM_LABEL = "remoteStream"
        private const val VIDEO_TRACK_LABEL = "remoteVideoTrack"
        private const val AUDIO_TRACK_LABEL = "remoteAudioTrack"
        private const val TAG = "VideoCallSession"
        private val executor = Executors.newSingleThreadExecutor()
    }


}