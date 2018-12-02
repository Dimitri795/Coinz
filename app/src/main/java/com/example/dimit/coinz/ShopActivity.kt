package com.example.dimit.coinz

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v4.content.ContextCompat
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

    private var tag = "ShopActivity"
    private var twoPane: Boolean = false
    private var itemList = ArrayList<Items>()
    private var itemCount = 7
    private var db : FirebaseFirestore? = null
    private var mAuth : FirebaseAuth? = null

    companion object {
        private const val avItems = "Available_Items"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        db = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()

        setSupportActionBar(toolbar)
        toolbar.title = title

        fab.setOnClickListener { _ ->
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
                    NavUtils.navigateUpFromSameTask(this)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
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
                    val intent = Intent(v.context, ItemDetailActivity::class.java).apply {
                        putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id)
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            val price = "Price: ${item.cost} gold"
            holder.idView.text = item.name
            holder.contentView.text = price
            holder.imageView.setImageResource(item.img)

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val idView: TextView = view.id_text
            val contentView: TextView = view.content
            val imageView : ImageView = view.imageView
        }
    }

    override fun onStart() {
        super.onStart()

        db?.collection(MainActivity.collection_key)?.document(mAuth?.uid!!)?.get()?.addOnSuccessListener { snap ->
            if (snap.exists()) {
                if (snap.data!![avItems] != null) {
                    val doc = snap.data!![avItems]
                    Log.d(tag, "[onStart] Snapshot exists and available item list is: $doc")
                    composeItemList(doc as IntArray)
                } else {
                    Log.d(tag, "[onStart] Snapshot exists but field available items doesn't")
                    val ints = IntArray(itemCount)
                    for (i in 0 until itemCount) {
                        ints[i] = i
                    }
                    composeItemList(ints)
                }
            } else {
                Log.d(tag, "Snapshot doesn't exist")
            }
        }
    }

    override fun onStop() {
        super.onStop()
    }

    data class Items (val id : Int, val name : String, val descrip : String,val img : Int){
        // class for items that can be bought in the shop for gold
        var cost  = 0 // cost of items in gold
        var available = false
        var original = false
    }

    private fun composeItemList(itemIds : IntArray){
        val names = resources.getStringArray(R.array.Names)
        val descrips = resources.getStringArray(R.array.Descriptions)
        val imgs = resources.obtainTypedArray(R.array.Images)
        val costs = resources.getIntArray(R.array.Costs)
        for(i in itemIds){
            /* Items in order: Coin Magnet1,Magnet2,Wallet1,Wallet2,Wallet3,Wallet4,Wallet5*/
            val item = Items(i,names[i],descrips[i],imgs.getResourceId(i,0))
            item.cost = costs[i]
            if(i == 0 || i == 2 ){
                item.original = true
            }
            itemList.add(item)
        }
        setupRecyclerView(Shop)
    }
}
