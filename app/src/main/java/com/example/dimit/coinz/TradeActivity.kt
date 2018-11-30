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

    private val tag = "TradeActivity"
    private var db : FirebaseFirestore?  = null
    private var mAuth : FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trade)
        setSupportActionBar(detail_toolbar)

        fab.setOnClickListener { _ ->
            supportFragmentManager.findFragmentByTag(TradeFragment.tag)?.onDestroyView()
        }
        db = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            val fragment = TradeFragment().apply {
                arguments = Bundle().apply {
                    putString(TradeFragment.ARG_ITEM_ID,
                            intent.getStringExtra(TradeFragment.ARG_ITEM_ID))
                    putString("email",mAuth?.currentUser?.email)
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
                    navigateUpTo(Intent(this, BankActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    override fun makeTrade(rec: String,frag : Fragment,doc : DocumentSnapshot) {
        val colKey = MainActivity.collection_key
        val traded = BankActivity.tradeGold
        val recId = doc.id
        val gold = (doc.data!!["GoldCount"] as Long).toInt()
        val user = HashMap<String,Any>()
        user["GoldCount"] = traded + gold
        db?.collection(colKey)?.document(recId)
                ?.set(user, SetOptions.merge())
                ?.addOnSuccessListener{ Log.d(tag,"Document SnapShot added with ID: $recId and gold: ${BankActivity.tradeGold +gold}") }
                ?.addOnFailureListener{ Log.d(tag,"Error adding document",it) }
        val sender = HashMap<String,Any>()
        sender[mAuth?.currentUser?.email!!+"$"+traded.toString()] = traded
        db?.collection(colKey)?.document(recId)?.collection(MainActivity.subcollection_key)?.document(MainActivity.sendersdoc)
                ?.set(sender, SetOptions.merge())
                ?.addOnSuccessListener{ Log.d(tag,"Document SnapShot added with ID: $recId and sender: $sender") }
                ?.addOnFailureListener{ Log.d(tag,"Error adding document",it) }
        Toast.makeText(this,"Coin sent to email: $rec",Toast.LENGTH_SHORT).show()
        supportFragmentManager.beginTransaction().remove(frag).commit()
        navigateUpTo(Intent(this, BankActivity::class.java))
        BankActivity.tradeValid = true
    }

}
