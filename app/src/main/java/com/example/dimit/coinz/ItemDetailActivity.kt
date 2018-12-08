package com.example.dimit.coinz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.activity_item_detail.*

class ItemDetailActivity : AppCompatActivity() {

    private var tag = "ItemDetailActivity"
    private var db : FirebaseFirestore? = null
    private var mAuth : FirebaseAuth? = null
    private var docRef : DocumentReference? = null
    private var totalGold = 0

    companion object {
        var walletSizeKey = "WalletSize"
        var coinReachKey = "CoinReach"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)
        setSupportActionBar(detail_toolbar)
        db = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()
        docRef = db?.collection(MainActivity.collection_key)?.document(mAuth?.uid!!)
        docRef?.get()?.addOnSuccessListener { it ->
            if(it.exists() && it.data!![BankActivity.goldkey]!= null){
                totalGold = (it.data!![BankActivity.goldkey] as Long).toInt()
                Log.d(tag,"Retrieving Total Gold count of $totalGold")
            } else{
                Log.d(tag,"Document not found or field empty!")
            }
            if (savedInstanceState == null) {
                // Create the detail fragment and add it to the activity
                // using a fragment transaction.
                val item = intent.getIntExtra(ItemDetailFragment.ARG_ITEM_ID,0)
                val price = resources.getIntArray(R.array.Costs)[item]
                if(totalGold >= price){
                    buyItemButton.isEnabled = true
                    buyItemButton.setOnClickListener { buyItem(item,price) }
                }
                val fragment = ItemDetailFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ItemDetailFragment.ARG_ITEM_ID,
                                intent.getIntExtra(ItemDetailFragment.ARG_ITEM_ID,0))                }
                }

                supportFragmentManager.beginTransaction()
                        .add(R.id.item_detail_container, fragment)
                        .commit()
            }
        }

        fab.setOnClickListener { _ ->
            startActivity(Intent(this@ItemDetailActivity,BankActivity::class.java))
        }

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun buyItem(i: Int,price : Int){
        val names = resources.getStringArray(R.array.Names)
        val item = ShopActivity.Items(i,names[i])
        item.cost = price
        var nextItem = ""
        if(i < ShopActivity.itemCount - 1){
            nextItem = names[i+1]
        }
        itemBought(item.id)
        updateAvItems(item.id,nextItem)
        val user = HashMap<String,Any>()
        user[BankActivity.goldkey] = totalGold - price
        docRef?.set(user, SetOptions.merge())
                ?.addOnSuccessListener{ Log.d(tag,"Document SnapShot added with gold: ${totalGold - price}") }
                ?.addOnFailureListener{ Log.d(tag,"Error adding document",it) }

        navigateUpTo(Intent(this, ShopActivity::class.java))
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                android.R.id.home -> {
                    navigateUpTo(Intent(this, ShopActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
    private fun itemBought(i:Int) {
        val user = HashMap<String, Any>()
        if (i == 0 || i == 1) { //Coin Magnets Bought - Increase Coin
            val coinReach = 25 + (5 * (i + 1))
            user[coinReachKey] = coinReach
            MainActivity.coinReach = coinReach
        } else { // until more items are added ll remaining ids are wallets - Increase wallet size
            val walletSize = 24 + i
            user[walletSizeKey] = walletSize
            MainActivity.walletSize = walletSize
        }
        docRef?.set(user, SetOptions.merge())
                ?.addOnSuccessListener { Log.d(tag, "Updated Document with ID ${docRef?.id} with ${user.keys} : ${user.values}") }
                ?.addOnFailureListener { Log.d(tag, "Error updating document", it) }
    }

    private fun updateAvItems(i: Int,name: String){
        val preferences = "MyPrefsFile${mAuth?.uid}"
        val settings = getSharedPreferences(preferences, Context.MODE_PRIVATE)
        val items = settings.getString("AvailableItemList", "")?.split(delimiters = *arrayOf("$"))
        val newItems = mutableListOf<String>()
        items?.forEach {
            if (it.toInt() != i) {
                newItems.add(it)
            } else {
                if (name != "") { // name == "" means the last item in the item list. it has no upgrade
                    val item = ShopActivity.Items(i + 1, name)
                    if (!ShopActivity.avItemMap[item]!!) { // if the next item in the list is not an original item then it must be an upgrade
                        newItems.add((i + 1).toString())
                    }
                }
            }
        }
        // We need an Editor object to make preference changes.
        val editor = settings.edit()
        if (newItems.isNotEmpty()) {
            editor.putString("AvailableItemList", newItems.joinToString("$"))
        } else {
            editor.putString("AvailableItemList", ShopActivity.itemCount.toString())
        }
        editor.apply()
        Log.d(tag, "[onStop] Storing Available Item list $newItems")
    }
}
