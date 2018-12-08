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
        var dailyLimit = 0 // The amount of coins that can be deposited into the bank daily
        var goldCount : DocumentReference? = null
        var goldkey = "GoldCount"
        var used : MutableList<String>? = mutableListOf()
        var tradeGold = 0
        var tradeValid = false
        var tradeItemId = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)

        setSupportActionBar(toolbar)
        toolbar.title = title

        db = FirebaseFirestore.getInstance()
        mAuth = FirebaseAuth.getInstance()
        goldCount = db?.collection(MainActivity.collection_key)?.document(mAuth?.uid!!)
        goldListener = goldCount?.addSnapshotListener{docSnap, e ->
            when{
                e != null -> Log.d(tag,e.message)
                docSnap != null && docSnap.exists() -> {
                    totalGold = (docSnap.data!![goldkey] as Long).toInt()
                    findViewById<TextView>(R.id.balance).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.balanceVal).visibility = View.VISIBLE
                    updateTextview()
                    val doc = docSnap.reference
                    val senderDoc = doc.collection(MainActivity.subcollection_key).document(MainActivity.sendersdoc)
                    senderDoc.get().addOnSuccessListener { snap ->
                        if(snap.exists() && snap != null){
                            snap.data?.forEach { sender ->
                                Toast.makeText(this,"Coin worth ${sender.value} sent by " +
                                        sender.key.substringBefore("$"),Toast.LENGTH_SHORT).show()
                            }
                            senderDoc.delete()// all senders have been acknowledged. Clear list
                        }
                    }
                    Log.d(tag,"Snapshot listen successful")
                }
            }
        }

        fab.setOnClickListener { _->
            startActivity(Intent(this@BankActivity,ShopActivity::class.java))
        }
        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
            recyclerView.adapter = SimpleItemRecyclerViewAdapter(this@BankActivity,newfc)
        }

    }

    class SimpleItemRecyclerViewAdapter(private val parentActivity: BankActivity, private val values: ArrayList<Feature>?) :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

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
                    tradeGold = goldVal
                    tradeCoin(item)
                    tradeItemId = item.getStringProperty("id")
                }
                if(dailyLimit>= MainActivity.walletSize){
                    depositButton.visibility = View.GONE
                }
                depositButton.setOnClickListener {
                    addGold(goldVal)
                    removeItem(holder.adapterPosition,item.getStringProperty("id")) }
            }
        }

        private fun tradeCoin(item: Feature){
            val intent = Intent(this.parentActivity, TradeActivity::class.java).apply {
                putExtra(TradeFragment.ARG_ITEM_ID, item.toJson())
            }
            this.parentActivity.startActivity(intent)
        }

        private fun removeItem(pos : Int, id : String){
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
            dailyLimit++
            if(dailyLimit >= MainActivity.walletSize){
                Toast.makeText(this.parentActivity,"Daily Deposit limit has been reached!",Toast.LENGTH_SHORT).show()
                parentActivity.resetRecView()
            }
            this.parentActivity.updateTextview()
            Toast.makeText(this.parentActivity,"$gold gold deposited!", Toast.LENGTH_SHORT).show()
        }

        override fun getItemCount() = values!!.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val idView: TextView = view.id_text
            val contentView: TextView = view.content
        }
    }

    private fun resetRecView(){
        setupRecyclerView(Wallet)
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
        dailyLimit = settings.getInt("DailyLimit",0)
        val deposCoins = settings.getString("UsedCoinList","")
        if(deposCoins != ""){
            used = deposCoins?.split(delimiters = *arrayOf("$"))?.asSequence()?.toMutableList()
        }
        if(MainActivity.newDay){
            BankActivity.used?.clear()
            BankActivity.dailyLimit = 0
            MainActivity.newDay = false
        }
        if(tradeValid){
            used?.add(tradeItemId)
            tradeValid = false
            val updates = HashMap<String, Any>()
            updates[tradeItemId] = FieldValue.delete()
            goldCount?.collection(MainActivity.subcollection_key)?.document(MainActivity.personalwalletdoc)?.update(updates)
                    ?.addOnCompleteListener { Log.d(tag,"Deleted Coin with id : $tradeItemId") }
                    ?.addOnFailureListener { Log.d(tag,"Error updating document",it) }
        }
        Log.d(tag,"[On Start] Restoring daily limit of $dailyLimit")
        Log.d(tag,"[On Start] Restoring used coin list of $used")
        fc = FeatureCollection.fromJson(MainActivity.dailyFcData).features()
        val wallet = MainActivity.collected?.filterNot { used?.contains(it)!! }
        newfc = fc?.filter{wallet?.contains(it.getStringProperty("id"))!!} as ArrayList<Feature>?
        setupRecyclerView(Wallet)
    }

    public override fun onStop() {
        super.onStop()
        val user = HashMap<String,Any>()
        user["GoldCount"] = totalGold
        goldCount?.set(user , SetOptions.merge())?.addOnSuccessListener{
            Log.d(tag,"Document SnapShot added with ID: ${mAuth?.uid} and gold: $totalGold")
        }?.addOnFailureListener {
            Log.d(tag,"Error adding document",it)
        }
        goldListener?.remove()

        Log.d(tag, "[onStop] Storing Used Coin List of $used")
        Log.d(tag,"[onStop] Storing daily limit of $dailyLimit")
        // All objects are from android.context.Context
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // We need an Editor object to make preference changes.
        val editor = settings.edit()
        editor.putString("UsedCoinList", used?.joinToString("$"))
        editor.putInt("DailyLimit", dailyLimit)
        // Apply the edits!
        editor.apply()
    }
}
