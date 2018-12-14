package com.example.dimit.coinz

import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.Timestamp.now
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_chat.*

// adapted and based off of code provided by lecturer
class ChatActivity:AppCompatActivity() {

    private var db : FirebaseFirestore? = null           // the Firebase Cloud Storage variable
    private var mAuth: FirebaseAuth? = null              // the Firebase authentication variable
    private var fChat : DocumentReference? = null        // document reference to the chat for easier use
    private var chatList : ListenerRegistration? = null  // the real time listener to the chat
    private var username = ""                            // the user's username
    private var chatLimit = 20                           // the number of sentences of the chat to show

    companion object  { // globally accessible companion to this Activity
        // firebase collection and document names
        private const val tag = "ChatActivity"
        private const val colKey = "Chat"
        private const val dockey = "Message"
        private const val nameField = "UserName"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat) // tells app which layout to use
        setSupportActionBar(toolbar)
        toolbar.title = title

        db = FirebaseFirestore.getInstance()  // get the current instance of the Firebase Firestore linked to the app
        mAuth = FirebaseAuth.getInstance()    // get the current instance of the Firebase authentication link to the app
        // Use com.google.firebase.Timestamp objects instead of java.util.Date objects
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true).setPersistenceEnabled(false).build()
        db?.firestoreSettings = settings      // apply the settings to the Firestore
        val doc = db?.collection(MainActivity.collection_key)?.document(mAuth?.uid!!)
        doc?.get()?.addOnSuccessListener { snap ->
            // get the username of the message sender and allow them to send messages
            if(snap.exists()){
                username = snap.data!![nameField] as String
                sendMessageButton.setOnClickListener { sendMessage()}
            }
        }
        fChat = db?.collection(colKey)?.document(dockey)
        realTimeUpdateListener() // set a listener for changes to Chat/Message document

        // Show the Up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun sendMessage(){
        // send messages with timeStamps to allow ordering in the display to users
        val timeStamp = now().seconds
        val newMessage = mapOf(
              username to text_field.text.toString()
        )
        val stampedMessage = mapOf(
                timeStamp.toString() to newMessage
        )
        // send message and listen for success or failure
        fChat?.set(stampedMessage, SetOptions.merge())
                ?.addOnSuccessListener { Toast.makeText(this,"Message Sent!",Toast.LENGTH_SHORT).show()}
                ?.addOnFailureListener { Log.e(tag,it.message) }
    }

    private fun realTimeUpdateListener(){
        val updates = HashMap<String, Any>()
        chatList = fChat?.addSnapshotListener{docSnap,e ->
            when{
                e != null -> Log.e(tag,e.message) // if there's an error then log it
                docSnap != null && docSnap.exists() ->{
                    // otherwise sort the document by the timeStamps and display the first 20 ( chatLimit)
                    with(docSnap){
                        var incoming = ""
                        var maxLines = 0
                        data?.toSortedMap()?.forEach {
                            val message = it.value as Map<*, *>
                            if(maxLines< chatLimit){
                                incoming = "${message.keys.first()}: ${message.values.first()}\n" + incoming
                                maxLines++
                            } else{
                                // anything not displayed is added to the HashMap updates for deletion to prevent clutter on database
                                updates[it.key] = FieldValue.delete()
                            }
                        }
                        incoming_message.text = incoming // display the chat to the user
                    }
                }
            }
        }
        // delete the excess messages
        fChat?.update(updates)
                ?.addOnCompleteListener { Log.d(tag,"Deleted messages with timestamps ${updates.keys}") }
                ?.addOnFailureListener { Log.d(tag,"Error updating document",it) }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                android.R.id.home -> {
                    // This ID represents the Home or Up button. Allow clicking it to take you back to the Main activity
                    NavUtils.navigateUpFromSameTask(this)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun onStop() {
        super.onStop()
        chatList?.remove() // remove the listener to prevent memory leaks
    }
}