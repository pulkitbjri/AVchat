package play.com.avwebrtc_firebase

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import play.com.avwebrtc_firebase.FirebaseData.myID
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import play.com.avwebrtc_firebase.NotificationActivities.StartCall


class LobbyActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<Pair<String, ContactData>>

    private lateinit var callRef: DatabaseReference
    private lateinit var callInitRef: DatabaseReference

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        val listView: ListView = findViewById(R.id.list)
        adapter = ContactsAdapter(this, ArrayList<Pair<String, ContactData>>(0))
        adapter.setNotifyOnChange(true)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            Log.w("TAG", "clicked: " + adapter.getItem(position))
            if(adapter.getItem(position).second.online)
            {

                var pos=adapter.getItem(position).first
                //startVideoCall(adapter.getItem(position).first)
                Toast.makeText(this@LobbyActivity,"Calling...",Toast.LENGTH_LONG).show()

//                FirebaseData.database.getReference("callInit/$pos/id")
//                    .setValue("$myID")

                startActivity(Intent(this@LobbyActivity,StartCall::class.java)
                    .putExtra("id","$myID")
                    .putExtra("callerId","$pos")
                    .putExtra("name",adapter.getItem(position).second.name))

            }
        }
        val emptyTextView =  TextView(this)
        emptyTextView.setText("No contacts available")
        listView.emptyView = emptyTextView

        init()
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        FirebaseData.init()

        callRef = FirebaseData.database.getReference("calls/$myID/id")

        callInitRef = FirebaseData.database.getReference("callInit/$myID/id")

        val textView: TextView = findViewById(R.id.textView)
        textView.text = "My id: $myID"
    }


    override fun onResume() {
        super.onResume()
        callRef.addValueEventListener(callListener)

        callInitRef.addValueEventListener(callInitListener)

        val usersRef = FirebaseData.database.getReference("users")
        usersRef.addValueEventListener(usersListener)
    }

    override fun onPause() {
        super.onPause()
        callRef.removeEventListener(callListener)

        callInitRef.removeEventListener(callInitListener)

        val usersRef = FirebaseData.database.getReference("users")
        usersRef.addValueEventListener(usersListener)
    }


    private fun startVideoCall(key: String) {
        FirebaseData.getCallStatusReference(myID).setValue(true)
        FirebaseData.getCallIdReference(key).onDisconnect().removeValue()
        FirebaseData.getCallIdReference(key).setValue(myID)
        VideoCallActivity.startCall(this@LobbyActivity, key)
    }

    private fun receiveVideoCall(key: String) {
        VideoCallActivity.receiveCall(this, key)
    }


    private val callListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.exists()) {
                receiveVideoCall(dataSnapshot.getValue(String::class.java)!!)
                callRef.removeValue()
            }
        }


    }

    private val callInitListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.exists()) {
                var dialog : AlertDialog.Builder =AlertDialog.Builder(this@LobbyActivity)
                dialog.setMessage("Call Incomming")
                dialog.setPositiveButton("Yes") { dialog, id ->
                    startVideoCall(dataSnapshot.getValue(String::class.java)!!)
                    callRef.removeValue()
                }
                dialog.setNegativeButton("No",null)
                dialog.show()


                callInitRef.removeValue()

            }
        }
    }

    private val usersListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
        }

        override fun onDataChange(dataSnapshot: DataSnapshot) {
            adapter.clear()
            if (!dataSnapshot.exists()) {
                return
            }
            dataSnapshot.children.forEach {
                if (it.exists() && it.key != myID)
                    adapter.add(Pair(it.key, it.getValue(ContactData::class.java)!!) as Pair<String, ContactData>?)
            }
        }

    }


    inner class ContactsAdapter(context: Context, contacts: List<Pair<String, ContactData>>) : ArrayAdapter<Pair<String, ContactData>>(context, 0, contacts) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val contact = getItem(position)
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_item_contact, parent, false)
            }

            val txtName = convertView!!.findViewById(R.id.txtName) as TextView
            val imageView = convertView.findViewById(R.id.imageView) as ImageView
            // Populate the data into the template view using the data object
            txtName.text = contact.second.name
            imageView.setImageResource(if(contact.second.online) R.drawable.round_green else R.drawable.round_red)

            return convertView
        }
    }

}
