package com.example.dimit.coinz

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_login.*

//Implementation adapted from https://github.com/firebase/quickstart-android and slides
class LoginActivity : AppCompatActivity() {

    private val tag = "LoginActivity"          // tag used for logging
    private var mAuth : FirebaseAuth? = null   // the Firebase authentication  variable
    private var db : FirebaseFirestore? = null // the Firebase cloud storage  variable
    private var newUser = false                // if this is a first time user
    private val emailField = "Email"           // email field of database named here for easy change


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // tells the app which layout to use
        mAuth = FirebaseAuth.getInstance()      // get the current instance of the Firebase Authentication linked to the app
        db = FirebaseFirestore.getInstance()    // get the current instance of the Firebase cloud storage linked to the app

        //skipButton.setOnClickListener {signIn("admin@example.com", "adminexample")} skip button for quick testing
        // linked buttons to corresponding functions
        emailSignInButton.setOnClickListener { signIn(fieldEmail.text.toString(), fieldPassword.text.toString()) }
        emailCreateAccountButton.setOnClickListener { createAccount(fieldEmail.text.toString(), fieldPassword.text.toString()) }
    }

    public override fun onStart() {
        super.onStart()
        updateUI(mAuth?.currentUser)
    }

    public override fun onStop() {
        super.onStop()
    }

    private fun createAccount(email : String,password : String){
        if (!validateForm()) {
            // if the text for fieldEmail and fieldPassword is empty this throws and error and forces resubmission
            Log.d(tag,"Attempting to create account with email and password...")
            return
        }
        // uses firebase function to create new user with email and password
        mAuth?.createUserWithEmailAndPassword(email, password)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with user info
                        Log.d(tag, "createUserWithEmailAndPassword:success")
                        makeToast("Welcome To Coinz")
                        val user = HashMap<String,Any>()
                        // Initializes user document with email and gold count.
                        user[emailField] = fieldEmail.text.toString()
                        user[BankActivity.goldkey] = 0
                        db?.collection(MainActivity.collection_key)?.document(mAuth?.uid!!)?.set(user)?.addOnSuccessListener{
                            newUser = true
                            updateUI(mAuth?.currentUser)
                            Log.d(tag,"Document SnapShot added with ID: ${mAuth?.uid}")
                        }?.addOnFailureListener {
                            Log.d(tag,"Error adding document",it)
                        }
                    } else {
                        // Sign in failed, display a message to the user
                        Log.w(tag, "createUserWithEmailAndPassword:failure", task.exception)
                        makeToast("Authentication failed.")
                        updateUI(null)
                    }
                }
    }

    private fun signIn(email : String, password: String){
        if (!validateForm()) {
            // if the text for fieldEmail and fieldPassword is empty this throws and error and forces resubmission
            Log.d(tag,"Attempting to sign in with email and password...")
            return
        }
        // uses firebase function to sign in using email and password
        mAuth?.signInWithEmailAndPassword(email, password)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with user info
                        Log.d(tag, "signInUserWithEmailAndPassword:success")
                        makeToast("Welcome Back To Coinz")
                        updateUI(mAuth?.currentUser)
                    } else {
                        // Sign in failed, display a message to the user
                        Log.w(tag, "signInUserWithEmailAndPassword:failure", task.exception)
                        makeToast("Authentication failed.")
                        updateUI(null)
                    }
                }
    }

    private fun validateForm(): Boolean {
        //checks to see if the email and password field have actual text entered.
        var valid = true

        val email = fieldEmail.text.toString()
        if (email.isEmpty()) {
            fieldEmail.error = "Required." // displays cute error circle to user for the field
            valid = false
        } else {
            fieldEmail.error = null
        }

        val password = fieldPassword.text.toString()
        if (password.isEmpty()) {
            fieldPassword.error = "Required." // displays cute error circle to user for the field
            valid = false
        } else {
            fieldPassword.error = null
        }

        return valid
    }

    private fun updateUI(user : FirebaseUser?){
        if (user != null) {
            // if a user is signed in, start the Main activity
           if(newUser){ // new users first pick a unique username and then read the tutorial
               newUser = false
               startActivity(Intent(this@LoginActivity,UsernameActivity::class.java))
           } else{ // returning users skip the tutorial and should already have a username
               startActivity(Intent(this@LoginActivity,MainActivity::class.java))
           }
        }
    }

    private fun makeToast(msg : String){
        // helper function for making writing of toasts shorter.
        Toast.makeText(this@LoginActivity,msg,Toast.LENGTH_LONG).show()
    }
}