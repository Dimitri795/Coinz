package com.example.dimit.coinz

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.activity_trade.*

class TradeActivity : AppCompatActivity(),TradeFragment.OnFragInteractionListener {

    private val tag = "TradeActivity"           // tag for logging
    private var db : FirebaseFirestore?  = null // the Firebase cloud storage variable
    private var mAuth : FirebaseAuth? = null    // the Firebase authentication variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trade) // tells the app which layout to use
        setSupportActionBar(detail_toolbar)

        db = FirebaseFirestore.getInstance()   // get the current instance of the Firebase cloud storage linked to the app
        mAuth = FirebaseAuth.getInstance()     // get the current instance of the Firebase Authentication linked to the app

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            val fragment = TradeFragment().apply {
                arguments = Bundle().apply {
                    putString(TradeFragment.ARG_ITEM_ID,
                            intent.getStringExtra(TradeFragment.ARG_ITEM_ID))
                    putString("username",mAuth?.currentUser?.displayName)
                }
            }
            supportFragmentManager.beginTransaction()
                    .add(R.id.coin_detail_container, fragment)
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                android.R.id.home -> {
                    // This ID represents the Home or Up button. Allow clicking it to take you back to the Bank activity
                    navigateUpTo(Intent(this, BankActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun makeTrade(rec: String,frag : Fragment,doc : DocumentSnapshot) {
        // the interface the Trade fragment uses to communicate with this activity
        val colKey = MainActivity.collection_key
        val traded = BankActivity.tradeGold        // the gold value of the coin being traded
        val recId = doc.id                       // the id of the document that the variable doc is a snapshot of
        val gold = (doc.data!![BankActivity.goldkey] as Long).toInt() // that document's current amount of gold
        val user = HashMap<String,Any>()
        user[BankActivity.goldkey] = traded + gold
        // update their current gold value to reflect the increase after the trade
        db?.collection(colKey)?.document(recId)
                ?.set(user, SetOptions.merge())
                ?.addOnSuccessListener{ Log.d(tag,"Document SnapShot added with ID: $recId and gold: ${BankActivity.tradeGold +gold}") }
                ?.addOnFailureListener{ Log.d(tag,"Error adding document",it) }
        val sender = HashMap<String,Any>()
        sender[mAuth?.currentUser?.displayName!!+"$"+traded.toString()] = traded
        // keep a track of the sender so users can see who sent them a coin and possibly return the favour!
        db?.collection(colKey)?.document(recId)?.collection(MainActivity.subcollection_key)?.document(MainActivity.sendersdoc)
                ?.set(sender, SetOptions.merge())
                ?.addOnSuccessListener{ Log.d(tag,"Document SnapShot added with ID: $recId and sender: $sender") }
                ?.addOnFailureListener{ Log.d(tag,"Error adding document",it) }
        Toast.makeText(this,"Coin sent to user: $rec",Toast.LENGTH_SHORT).show()
        supportFragmentManager.beginTransaction().remove(frag).commit()   // remove the trade fragment now that the trade is complete
        navigateUpTo(Intent(this, BankActivity::class.java)) // return to the Bank activity
        BankActivity.tradeValid = true                                    // register the trade as successful
    }

}
