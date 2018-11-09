package com.example.dimit.coinz

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_login.*

//Implementation adapted from https://github.com/firebase/quickstart-android and slides
class LoginActivity : AppCompatActivity() {

    private val tag = "LoginActivity"
    private var mAuth : FirebaseAuth? = null
    private var db : FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        skipButton.setOnClickListener {signIn("admin@example.com", "adminexample")}

        emailSignInButton.setOnClickListener { signIn(fieldEmail.text.toString(), fieldPassword.text.toString()) }
        emailCreateAccountButton.setOnClickListener { createAccount(fieldEmail.text.toString(), fieldPassword.text.toString()) }
    }

    public override fun onStart() {
        super.onStart()
        updateUI(mAuth?.currentUser)
    }

    private fun createAccount(email : String,password : String){
        mAuth?.createUserWithEmailAndPassword(email, password)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with user info
                        Log.d(tag, "createUserWithEmailAndPassword:success")
                        makeToast("Welcome To Coinz")
                        //Experimenting With allowing users to have displaynames
                        /*val user = mAuth?.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName("bob").build()
                        user?.updateProfile(profileUpdates)*/
                        val user = HashMap<String,String>()
                        user["UserName"] = fieldEmail.text.toString().substringBefore('@')
                        user["Email"] = fieldEmail.text.toString()
                        db?.collection("Users")?.document(mAuth?.uid!!)?.set(user as Map<String, Any>)?.addOnSuccessListener{
                            Log.d(tag,"Document SnapShot added with ID: ${mAuth?.uid}")
                        }?.addOnFailureListener {
                            Log.d(tag,"Error adding document",it)
                        }
                        updateUI(mAuth?.currentUser)
                    } else {
                        // Sign in failed, display a message to the user
                        Log.w(tag, "createUserWithEmailAndPassword:failure", task.exception)
                        makeToast("Authentication failed.")
                        updateUI(null)
                    }
                }
    }

    private fun signIn(email : String, password: String){
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

    private fun updateUI(user : FirebaseUser?){
        if (user != null) {
           startActivity(Intent(this@LoginActivity,MainActivity::class.java))
        } else {
            //do later
            emailPasswordButtons.visibility = View.VISIBLE
            emailPasswordFields.visibility = View.VISIBLE
        }
    }

    private fun makeToast(msg : String){
        Toast.makeText(this@LoginActivity,msg,Toast.LENGTH_LONG).show()
    }
}