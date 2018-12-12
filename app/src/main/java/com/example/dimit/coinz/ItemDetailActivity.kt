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

    private var tag = "ItemDetailActivity"         // tag for logging
    private var db : FirebaseFirestore? = null     // the Firebase cloud storage  variable
    private var mAuth : FirebaseAuth? = null       // the Firebase authentication  variable
    private var docRef : DocumentReference? = null // reference to user's document
    private var totalGold = 0                      // Amount of gold in bank

    companion object{ // globally accessible companion to this activity
        //Firebase collection and document names
        var walletSizeKey = "WalletSize"
        var coinReachKey = "CoinReach"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail) // tell the app which layout to use
        setSupportActionBar(detail_toolbar)

        db = FirebaseFirestore.getInstance()   // get the current instance of the Firebase Firestore linked to the app
        mAuth = FirebaseAuth.getInstance()     // get the current instance of the Firebase Authentication linked to the app
        docRef = db?.collection(MainActivity.collection_key)?.document(mAuth?.uid!!)
        docRef?.get()?.addOnSuccessListener { it ->
            // get a document snapshot of the user's document in the database to get their current gold
            if(it.exists() && it.data!![BankActivity.goldkey]!= null){
                totalGold = (it.data!![BankActivity.goldkey] as Long).toInt()
                Log.d(tag,"Retrieving Total Gold count of $totalGold")
            } else{
                Log.d(tag,"Document not found or field empty!")
            }
            if (savedInstanceState == null) {
                // Create the detail fragment and add it to the activity
                // using a fragment transaction.
                val item = intent.getIntExtra(ItemDetailFragment.ARG_ITEM_ID,0) // get the arguments from the bundle
                val price = resources.getIntArray(R.array.Costs)[item] // get the item cost out of the linked typed arrays
                if(totalGold >= price){
                    // if the user has more gold than the item's price then it can be bought
                    buyItemButton.isEnabled = true
                    buyItemButton.setOnClickListener { buyItem(item,price) }
                }

                // start the fragment to show item details
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

        fab.setOnClickListener { _ -> //Start Bank activity on click
            startActivity(Intent(this@ItemDetailActivity,BankActivity::class.java))
        }

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun buyItem(i: Int,price : Int){
        // Items are a data class constructed of a name and integer. Get those values from a typed array to make the item again
        // and access its other properties
        val names = resources.getStringArray(R.array.Names)
        val item = ShopActivity.Items(i,names[i])
        item.cost = price
        var nextItem = ""
        if(i < ShopActivity.itemCount - 1){
            // if i is the index of any item except the last item then it has a possible upgrade which is the next item
            // this gets the name of the possible upgrade
            nextItem = names[i+1]
        }
        itemBought(item.id) // process the buying of the item
        updateAvItems(item.id,nextItem) // update the available item list
        // update the user's total gold
        val user = HashMap<String,Any>()
        user[BankActivity.goldkey] = totalGold - price
        docRef?.set(user, SetOptions.merge())
                ?.addOnSuccessListener{ Log.d(tag,"Document SnapShot added with gold: ${totalGold - price}") }
                ?.addOnFailureListener{ Log.d(tag,"Error adding document",it) }

        navigateUpTo(Intent(this, ShopActivity::class.java)) // return to Shop activity
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                android.R.id.home -> {
                    // up returns us to Shop activity
                    navigateUpTo(Intent(this, ShopActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun itemBought(i:Int) {
        // determine the effect of buying this item and put the changes in the database
        val user = HashMap<String, Any>()
        if (i == 0 || i == 1) { //Coin Magnets Bought - Increase Coin Reach
            val coinReach = 25 + (5 * (i + 1))
            user[coinReachKey] = coinReach
            MainActivity.coinReach = coinReach
        } else { // until more items are added all remaining ids are wallets - Increase Wallet Size
            val walletSize = 24 + i
            user[walletSizeKey] = walletSize
            MainActivity.walletSize = walletSize
        }
        // update database
        docRef?.set(user, SetOptions.merge())
                ?.addOnSuccessListener { Log.d(tag, "Updated Document with ID ${docRef?.id} with ${user.keys} : ${user.values}") }
                ?.addOnFailureListener { Log.d(tag, "Error updating document", it) }
    }

    private fun updateAvItems(i: Int,name: String){
        // store updated version of available items after the item is bought
        val preferences = "MyPrefsFile${mAuth?.uid}"
        val settings = getSharedPreferences(preferences, Context.MODE_PRIVATE)
        // get current available item list
        val items = settings.getString("AvailableItemList", "")?.split(delimiters = *arrayOf("$"))
        val newItems = mutableListOf<String>()
        items?.forEach {
            if (it.toInt() != i) {
                // if it was an available item and it hasn't been bought then it's still available
                newItems.add(it)
            } else { // it's been bought therefore check to see if the next item is an upgrade of this one
                if (name != "") { // name == "" means the last item in the item list. it has no upgrade
                    val item = ShopActivity.Items(i + 1, name)
                    if (!ShopActivity.avItemMap[item]!!) {
                        // if the next item in the list is not an original item then it must be an upgrade
                        // this is determined by the boolean value of avItemMap
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
            // when all items are bought this ensures that the Shop activity doesn't recreate the original items as available.
            // This index is guaranteed not to be the ID of any item as long as the itemCount variable and the number of items are in sync.
            editor.putString("AvailableItemList", ShopActivity.itemCount.toString())
        }
        editor.apply()
        Log.d(tag, "[onStop] Storing Available Item list $newItems")
    }
}
