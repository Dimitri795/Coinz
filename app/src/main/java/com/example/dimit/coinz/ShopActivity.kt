package com.example.dimit.coinz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_shop.*
import kotlinx.android.synthetic.main.item_list.*
import kotlinx.android.synthetic.main.item_list_content.view.*

class ShopActivity : AppCompatActivity() {

    private var tag = "ShopActivity"              // tag for logging
    private var twoPane: Boolean = false          // boolean which determines what size screen is being used
    private var itemList = ArrayList<Items>()     // List of available Items
    private var db : FirebaseFirestore? = null    // the Firebase cloud storage variable
    private var mAuth : FirebaseAuth? = null      // the Firebase authentication variable
    private lateinit var preferencesFile : String // for storing preferences

    companion object { //globally accessible companion to this activity
        var itemCount = 7                                 // Number of Items possible
        var avItemMap = HashMap<Items,Boolean>(itemCount) // Hashmap of items and their initial availability
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop) // tells app which layout to use

        db = FirebaseFirestore.getInstance()   // get the current instance of the Firebase cloud storage linked to the app
        mAuth = FirebaseAuth.getInstance()     // get the current instance of the Firebase Authentication linked to the app

        setSupportActionBar(toolbar)
        toolbar.title = title

        fab.setOnClickListener { _ -> // start the Bank activity on click
            startActivity(Intent(this@ShopActivity,BankActivity::class.java))
        }
        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (item_detail_container != null) {
            // The detail container view will be present only in the
            // large-screen layouts so the activity should be in two-pane mode.
            twoPane = true
        }

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

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        // apply this adapter to the recycler view
        recyclerView.adapter = SimpleItemRecyclerViewAdapter(this, itemList, twoPane)
    }

    class SimpleItemRecyclerViewAdapter(private val parentActivity: ShopActivity,
                                        private val values: ArrayList<Items>,
                                        private val twoPane: Boolean) :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as Items
                if (twoPane) {
                    // clicking on the view items in the recycler in twopane mode puts the fragment next to the list on the same screen
                    val fragment = ItemDetailFragment().apply {
                        arguments = Bundle().apply {
                            putInt(ItemDetailFragment.ARG_ITEM_ID, item.id)
                        }
                    }
                    parentActivity.supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.item_detail_container, fragment)
                            .commit()
                } else {
                    // screen too small so start a new activity for the fragment (Item Detail Activity)
                    val intent = Intent(v.context, ItemDetailActivity::class.java).apply {
                        putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id)
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // creates view holders for the items in the list
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // setting up what to display about the Items
            val item = values[position]
            val price = "Price: ${item.cost} gold"
            holder.idView.text = item.name
            holder.contentView.text = price
            holder.imageView.setImageResource(item.img)

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener) // allow this view to have the properties of the onclickListener defined above
            }
        }

        override fun getItemCount() = values.size   // used in internal workings of the recycler

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            // assigns views in the layout to variables
            val idView: TextView = view.id_text
            val contentView: TextView = view.content
            val imageView : ImageView = view.imageView
        }
    }

    override fun onStart() {
        super.onStart()
        // restore preferences
        preferencesFile = "MyPrefsFile${mAuth?.uid}"
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // use ”” as the default value (this might be the first time the app is run)
        composeItemList() // creates all possible items
        val items = settings.getString("AvailableItemList","")
        Log.d(tag, "[onStart] Restoring available item list is: $items")
        if(items != ""){ // their exists available items or all available items have been bought
            if(items == itemCount.toString()){ // all available items have been bought
                return
            }else{
                items?.split(delimiters = *arrayOf("$"))?.forEach {id ->
                    avItemMap.forEach{
                        // if the key in the list matches an item in the hashmap then add it to the list to be displayed
                        // in the recycler
                        if(it.key.id == id.toInt())
                            itemList.add(it.key)
                    }
                }
            }
        } else{ // first time setting up shop so use original items. These have a value of true in avItemMap
            avItemMap.forEach {
                if(it.value){
                    itemList.add(it.key)
                }
            }
        }
        setupRecyclerView(Shop) // set up the recycler now that the data is ready
    }

    override fun onStop() {
        super.onStop()

        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // We need an Editor object to make preference changes.
        val editor = settings.edit()
        var items = listOf<String>()
        itemList.toSet().forEach {// prevents duplicates
            items += it.id.toString()
        }
        if(items.isEmpty()){ // empty list has a specific string != empty string
            items += itemCount.toString()
        }
        editor.putString("AvailableItemList",items.joinToString("$"))
        editor.apply()
        Log.d(tag,"[onStop] Storing Available Item list $items")
    }

    data class Items (val id : Int, val name : String){
        // class for items that can be bought in the shop for gold
        var original = false // is this an original item or an upgrade of a previous item. Can be put into an array if more items added
        var cost  = 0        // cost of items in gold
        var img = 0          //  resource int for the item picture\
        var descrip = ""     // description to display to user when describing item
    }

    private fun composeItemList(){
        // everything to create items are in ordered typed arrays in Items.xml
        // In this way a single index is all we need to completely create every aspect of an item.
        val names = resources.getStringArray(R.array.Names)
        val descrips = resources.getStringArray(R.array.Descriptions)
        val imgs = resources.obtainTypedArray(R.array.Images)
        val costs = resources.getIntArray(R.array.Costs)
        for(i in 0 until itemCount){
            /* Items in order: Coin Magnet1,Magnet2,Wallet1,Wallet2,Wallet3,Wallet4,Wallet5*/
            val item = Items(i,names[i])
            item.descrip = descrips[i]
            item.img = imgs.getResourceId(i,0)
            item.cost = costs[i]
            if(i == 0 || i == 2 ){ // until more items are added we know exactly which two items are original
                item.original = true
            }
            avItemMap[item] = item.original
        }
    }
}
