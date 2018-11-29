package com.example.dimit.coinz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import kotlinx.android.synthetic.main.activity_bank.*
import kotlinx.android.synthetic.main.coin_list.*
import kotlinx.android.synthetic.main.coin_list_content.view.*
import kotlin.math.roundToInt

class BankActivity : AppCompatActivity() {

    private val tag = "BankActivity"
    private var fc : MutableList<Feature>? = null
    private var newfc : ArrayList<Feature>? = null
    private var mAuth : FirebaseAuth? = null
    private var db : FirebaseFirestore?  = null
    private var goldListener : ListenerRegistration? = null
    private lateinit var preferencesFile : String

    companion object {
        var totalGold  = 0 // The Amount of Gold in your bank
        var goldCount : DocumentReference? = null
        var used : MutableList<String>? = mutableListOf()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)

        setSupportActionBar(toolbar)
        toolbar.title = title

        db = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()
        if(mAuth?.currentUser != null){
            goldCount = db?.collection(MainActivity.collection_key)?.document(mAuth?.uid!!)
        }
        goldListener = goldCount?.addSnapshotListener{docSnap, e ->
            when{
                e != null -> Log.d(tag,e.message)
                docSnap != null && docSnap.exists() -> {
                    totalGold = (docSnap.data!!["GoldCount"] as Long).toInt()
                    findViewById<TextView>(R.id.balance).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.balanceVal).visibility = View.VISIBLE
                    updateTextview()
                    Log.d(tag,"Snapshot listen successful")
                }
            }
        }

        fab.setOnClickListener { _->
            startActivity(Intent(this@BankActivity,MainActivity::class.java))
        }
        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fc = FeatureCollection.fromJson(MainActivity.dailyFcData).features()
        val wallet = MainActivity.collected?.filterNot { used?.contains(it)!! }
        newfc = fc?.filter{wallet?.contains(it.getStringProperty("id"))!!} as ArrayList<Feature>?
        setupRecyclerView(Wallet)
    }


    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                android.R.id.home -> {
                    // This ID represents the Home or Up button.
                    NavUtils.navigateUpFromSameTask(this)
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        Wallet.apply {
            layoutManager = LinearLayoutManager(this@BankActivity)
            recyclerView.adapter = SimpleItemRecyclerViewAdapter(this@BankActivity,newfc)
        }

    }

    class SimpleItemRecyclerViewAdapter(private val parentActivity: BankActivity, private val values: ArrayList<Feature>?) :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        /*private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as Feature
                if (twoPane) {
                    val fragment = CoinDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(CoinDetailFragment.ARG_ITEM_ID, item.toJson())
                        }
                    }
                    parentActivity.supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.coin_detail_container, fragment)
                            .commit()
                } else {
                    val intent = Intent(v.context, CoinDetailActivity::class.java).apply {
                        putExtra(CoinDetailFragment.ARG_ITEM_ID, item.toJson())
                    }
                    v.context.startActivity(intent)
                }
            }
        }*/

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.coin_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values?.get(position)
            val coinTitle = item?.getStringProperty("currency")
            val coinVal = item?.getStringProperty("value")?.toDouble()
            val displayText = "$coinTitle ${item?.getStringProperty("marker-symbol")}"
            val goldVal = ((MainActivity().getGold(coinTitle!!))*coinVal!!).roundToInt()
            val displayContent = "Coin worth: $goldVal gold"
            holder.idView.text = displayText
            holder.contentView.text = displayContent

            with(holder.itemView) {
                tag = item
                tradeButton.setOnClickListener {
                    tradeCoin(item)}
                    //removeItem(holder.adapterPosition,item.getStringProperty("id")) }
                depositButton.setOnClickListener {
                    addGold(goldVal)
                    removeItem(holder.adapterPosition,item.getStringProperty("id")) }
            }
        }

        private fun tradeCoin(item: Feature){
            val intent = Intent(this.parentActivity, CoinDetailActivity::class.java).apply {
                putExtra(CoinDetailFragment.ARG_ITEM_ID, item.toJson())
            }
            this.parentActivity.startActivity(intent)
        }

        private fun removeItem(pos : Int,id : String){
            if(pos > -1){
                values?.removeAt(pos)
            }
            notifyItemRemoved(pos)
            notifyItemRangeChanged(pos,values!!.size)

            BankActivity.used?.add(id)
            Log.d(this.parentActivity.tag,"Used coins ${BankActivity.used}")
            val updates = HashMap<String, Any>()
            updates[id] = FieldValue.delete()
            BankActivity.goldCount?.collection(MainActivity.subcollection_key)?.document(MainActivity.personalwalletdoc)?.update(updates)
                    ?.addOnCompleteListener { Log.d(this.parentActivity.tag,"Deleted Coin with id : $id") }
                    ?.addOnFailureListener { Log.d(this.parentActivity.tag,"Error updating document",it) }
        }

        private fun addGold(gold : Int){
            totalGold += gold
            this.parentActivity.updateTextview()
            Toast.makeText(this.parentActivity,"$gold gold deposited!", Toast.LENGTH_SHORT).show()
        }

        override fun getItemCount() = values!!.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val idView: TextView = view.id_text
            val contentView: TextView = view.content
        }
    }

    private fun updateTextview(){
        val goldBal = findViewById<TextView>(R.id.balanceVal)
        goldBal.text = totalGold.toString()
    }

    public override fun onStart() {
        super.onStart()
        preferencesFile = "MyPrefsFile${mAuth?.uid}"
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // use ”” as the default value (this might be the first time the app is run)
        val deposCoins = settings.getString("UsedCoinList","")
        if(deposCoins != ""){
            used = deposCoins?.split(delimiters = *arrayOf("$"))?.asSequence()?.toMutableList()
        }
    }

    public override fun onStop() {
        super.onStop()
        val user = HashMap<String,Any>()
        user["GoldCount"] = totalGold
        goldCount?.set(user as Map<String,Any>, SetOptions.merge())?.addOnSuccessListener{
            Log.d(tag,"Document SnapShot added with ID: ${mAuth?.uid} and gold: $totalGold")
        }?.addOnFailureListener {
            Log.d(tag,"Error adding document",it)
        }
        goldListener?.remove()

        Log.d(tag, "[onStop] Storing Used Coin List of $used")
        // All objects are from android.context.Context
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // We need an Editor object to make preference changes.
        val editor = settings.edit()
        editor.putString("UsedCoinList", used?.joinToString("$"))
        // Apply the edits!
        editor.apply()
    }
}
