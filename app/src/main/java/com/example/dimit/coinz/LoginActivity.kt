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
import kotlinx.android.synthetic.main.activity_login.*

//Implementation adapted from https://github.com/firebase/quickstart-android and slides
class LoginActivity : AppCompatActivity(),View.OnClickListener {

    private val tag = "LoginActivity"
    private var mAuth : FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mAuth = FirebaseAuth.getInstance()

        emailSignInButton.setOnClickListener(this)
        emailCreateAccountButton.setOnClickListener(this)
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
                        Toast.makeText(this@LoginActivity,"Welcome To Coinz",Toast.LENGTH_LONG).show()
                        //Experimenting With allowing users to have displaynames
                        /*val user = mAuth?.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName("bob").build()
                        user?.updateProfile(profileUpdates)*/
                        updateUI(mAuth?.currentUser)
                    } else {
                        // Sign in failed, display a message to the user
                        Log.w(tag, "createUserWithEmailAndPassword:failure", task.exception)
                        Toast.makeText(this@LoginActivity, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@LoginActivity,"Welcome Back To Coinz",Toast.LENGTH_LONG).show()
                        updateUI(mAuth?.currentUser)
                    } else {
                        // Sign in failed, display a message to the user
                        Log.w(tag, "signInUserWithEmailAndPassword:failure", task.exception)
                        Toast.makeText(this@LoginActivity, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
    }

    private fun updateUI(user : FirebaseUser?){
        if (user != null) {
           startActivity(Intent(this@LoginActivity,MainActivity::class.java))
        } else {
            emailPasswordButtons.visibility = View.VISIBLE
            emailPasswordFields.visibility = View.VISIBLE
        }
    }

    override fun onClick(v: View) {
        val i = v.id
        when (i) {
            R.id.emailCreateAccountButton -> createAccount(fieldEmail.text.toString(), fieldPassword.text.toString())
            R.id.emailSignInButton -> signIn(fieldEmail.text.toString(), fieldPassword.text.toString())
        }
    }
}