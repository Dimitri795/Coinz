package com.example.dimit.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.activity_username.*

class UsernameActivity : AppCompatActivity(){

    private val tag = "UsernameActivity"
    private var mAuth : FirebaseAuth? = null   // the Firebase authentication  variable
    private var db : FirebaseFirestore? = null // the Firebase cloud storage  variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_username)
        mAuth = FirebaseAuth.getInstance()      // get the current instance of the Firebase Authentication linked to the app
        db = FirebaseFirestore.getInstance()    // get the current instance of the Firebase cloud storage linked to the app

        setUsernameButton.setOnClickListener { setUsername(usernameField.text.toString()) }
    }

    private fun setUsername(username: String) {
        db?.collection(MainActivity.collection_key)?.whereEqualTo(ChatActivity.nameField,username)?.get()?.addOnSuccessListener { it ->
            // Make sure the username is available
            if (it.isEmpty) {
                // username is available so update their auth profile to access the username easily
                mAuth?.currentUser?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(username).build())
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d(tag, "User profile updated.")
                            }
                        }?.addOnFailureListener { Log.e(tag,it.message) }
                // add it to user's database to be able to query the database by username
                val user = HashMap<String,Any>()
                user[ChatActivity.nameField] = username
                db?.collection(MainActivity.collection_key)?.document(mAuth?.uid!!)?.set(user, SetOptions.merge())?.addOnSuccessListener{
                    Log.d(tag,"Document SnapShot added with ID: ${mAuth?.uid}")
                }?.addOnFailureListener {
                    Log.d(tag,"Error adding document",it)
                }
                // display the tutorial to the new user
                startActivity(Intent(this@UsernameActivity,TutorialActivity::class.java))
            } else { //username is taken. Display error message
                Toast.makeText(this, "That username already belongs to another player", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
