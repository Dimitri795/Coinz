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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import kotlinx.android.synthetic.main.activity_bank.*
import kotlinx.android.synthetic.main.coin_list.*
import kotlinx.android.synthetic.main.coin_list_content.view.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class BankActivity : AppCompatActivity() {

    private val tag = "BankActivity"                        // tag for logging
    private var fc : MutableList<Feature>? = null           // daily list of features
    private var newfc : ArrayList<Feature>? = null          // filtered list of collected coins to show in bank
    private var mAuth : FirebaseAuth? = null                // the Firebase authentication  variable
    private var db : FirebaseFirestore? = null              // the Firebase cloud storage variable
    private var goldListener : ListenerRegistration? = null // real time update listener for querying gold count
    private lateinit var preferencesFile : String

    companion object { // globally accessible companion to this activity
        var totalGold  = 0                                // The Amount of Gold in your bank
        var dailyLimit = 0                                // The amount of coins that can be deposited into the bank daily
        var goldCount : DocumentReference? = null         // reference to the user's document on Firebase
        var goldkey = "GoldCount"                         // field name for the available gold
        var used : MutableList<String>? = mutableListOf() // list of deposited or traded coins. Subset of Collected Coins
        var tradeGold = 0                                 // gold value of a coin about to be traded
        var tradeValid = false                            // boolean checking whether a trade was successful
        var tradeItemId = ""                              // id of the traded coin
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank) // tells the app which layout to use

        setSupportActionBar(toolbar)
        toolbar.title = title

        db = FirebaseFirestore.getInstance()   // get the current instance of the Firebase Firestore linked to the app
        mAuth = FirebaseAuth.getInstance()     // get the current instance of the Firebase Authentication linked to the app
        goldCount = db?.collection(MainActivity.collection_key)?.document(mAuth?.uid!!)
        goldListener = goldCount?.addSnapshotListener{docSnap, e ->
            // get a document snapshot of the user's document in the database to properly display their current gold
            when{
                e != null -> Log.d(tag,e.message)
                docSnap != null && docSnap.exists() -> {
                    totalGold = (docSnap.data!![goldkey] as Long).toInt()
                    // only make the data visible when ready so they don't notice the time delay as much
                    findViewById<TextView>(R.id.balance).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.balanceVal).visibility = View.VISIBLE
                    updateTextview() // updates available gold often enough that it was made a function to reduce duplication
                    val doc = docSnap.reference
                    val senderDoc = doc.collection(MainActivity.subcollection_key).document(MainActivity.sendersdoc)
                    senderDoc.get().addOnSuccessListener { snap ->
                        // finds any trades that were made to this user and credits their sender as a toast.
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

        fab.setOnClickListener { _-> // Start the Shop activity on click
            startActivity(Intent(this@BankActivity,ShopActivity::class.java))
        }

        // Show the Up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
        // applies this adapter to the recycler view supplied and given newfc as values to display
        recyclerView.adapter = SimpleItemRecyclerViewAdapter(this,newfc)
    }

    class SimpleItemRecyclerViewAdapter(private val parentActivity: BankActivity, private val values: ArrayList<Feature>?) :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // creates view holders for the items in the list
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.coin_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // setting up what to display about collected coins
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

                if(dailyLimit>= MainActivity.walletSize){
                    // if deposit limit is reached changed view so that users can only send spare change
                    depositButton.visibility = View.GONE
                    tradeButton.visibility = View.GONE
                    spareChangeButton.visibility = View.VISIBLE
                }
                val button : Button = if(tradeButton.visibility == View.VISIBLE){
                    // bonus feature allowing trading of coins before limit reached therefore both buttons do
                    // the same thing but appear under different circumstances. This makes it explicit that one is a trade and
                    // the other is sending spare change.
                    tradeButton
                } else{
                    spareChangeButton
                }
                button.setOnClickListener {
                    // sets a trade in motion
                    tradeGold = goldVal
                    tradeCoin(item)
                    tradeItemId = item.getStringProperty("id")
                }
                depositButton.setOnClickListener {
                    // deposits a coin and removes it from the list
                    addGold(goldVal)
                    removeItem(holder.adapterPosition,item.getStringProperty("id")) }
            }
        }

        private fun tradeCoin(item: Feature){
            // starts the Trade activity to accept user input and trade the coin
            val intent = Intent(this.parentActivity, TradeActivity::class.java).apply {
                putExtra(TradeFragment.ARG_ITEM_ID, item.toJson())
            }
            this.parentActivity.startActivity(intent)
        }

        private fun removeItem(pos : Int, id : String){
            // removes an item from the recycler view by removing it from the dataset and alerting the recycler to the data change
            if(pos > -1){
                values?.removeAt(pos)
            }
            notifyItemRemoved(pos)
            notifyItemRangeChanged(pos,values!!.size)

            BankActivity.used?.add(id)  // adds coin to the used coins list
            Log.d(this.parentActivity.tag,"Used coins ${BankActivity.used}")
//            val updates = HashMap<String, Any>()
//            updates[id] = FieldValue.delete()
//            BankActivity.goldCount?.collection(MainActivity.subcollection_key)?.document(MainActivity.personalwalletdoc)?.update(updates)
//                    ?.addOnCompleteListener { Log.d(this.parentActivity.tag,"Deleted Coin with id : $id") }
//                    ?.addOnFailureListener { Log.d(this.parentActivity.tag,"Error updating document",it) }
        }

        private fun addGold(gold : Int){
            // adds gold value to current gold and increments the current amount of coins deposited for the day (dailyLimit)
            totalGold += gold
            dailyLimit++
            if(dailyLimit >= MainActivity.walletSize){
                // if the limit is reached remake the entire recycler so that the UI changes happen seamlessly
                Toast.makeText(this.parentActivity,"Daily Deposit limit has been reached!",Toast.LENGTH_SHORT).show()
                parentActivity.resetRecView()
            }
            this.parentActivity.updateTextview() // show the updated gold value to the user
            Toast.makeText(this.parentActivity,"$gold gold deposited!", Toast.LENGTH_SHORT).show()
        }

        override fun getItemCount() = values!!.size // used in the internal workings of the recycler

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            // assigns names to the views in the xml layout
            val idView: TextView = view.id_text
            val contentView: TextView = view.content
        }
    }

    private fun resetRecView(){
        //refreshes recycler completely. Otherwise the location of the last coin deposited within the list
        // determines which part of the view gets updated with the UI changes.
        setupRecyclerView(Wallet)
    }

    private fun updateTextview(){
        // display to the user their current gold amount
        val goldBal = findViewById<TextView>(R.id.balanceVal)
        goldBal.text = totalGold.toString()
    }

    public override fun onStart() {
        super.onStart()
        // restore preferences
        preferencesFile = "MyPrefsFile${mAuth?.uid}"
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // use ”” as the default value (this might be the first time the app is run)
        dailyLimit = settings.getInt("DailyLimit",0)
        val downloadDate = settings.getString("lastDownloadDate", "")!!
        val deposCoins = settings.getString("UsedCoinList","")
        if(deposCoins != ""){ //if string isn't empty split into a list
            used = deposCoins?.split(delimiters = *arrayOf("$"))?.asSequence()?.toMutableList()
        }
        val current = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        val currentFormatted = current.substring(0,4)+"/"+ current.substring(4,6)+"/"+ current.substring(6)
        if(downloadDate != currentFormatted){
            // if dates are different then it's a new day so reset the limit and empty the used coins list
            BankActivity.used?.clear()
            BankActivity.dailyLimit = 0
        }
        if(tradeValid){
            // a trade was successful so add the traded coin to the used coin list
            used?.add(tradeItemId)
            tradeValid = false
//            val updates = HashMap<String, Any>()
//            updates[tradeItemId] = FieldValue.delete()
//            goldCount?.collection(MainActivity.subcollection_key)?.document(MainActivity.personalwalletdoc)?.update(updates)
//                    ?.addOnCompleteListener { Log.d(tag,"Deleted Coin with id : $tradeItemId") }
//                    ?.addOnFailureListener { Log.d(tag,"Error updating document",it) }
        }
        Log.d(tag,"[On Start] Restoring daily limit of $dailyLimit")
        Log.d(tag,"[On Start] Restoring used coin list of $used")
        fc = FeatureCollection.fromJson(MainActivity.dailyFcData).features() // recover the list of features from the Json string
        // filter the collected coin list to only contain unused but collected coins
        val wallet = MainActivity.collected?.filterNot { used?.contains(it)!! }
        // filter feature list to the features that were collected
        newfc = fc?.filter{wallet?.contains(it.getStringProperty("id"))!!} as ArrayList<Feature>?
        setupRecyclerView(Wallet) //set up the recycler view now
    }

    public override fun onStop() {
        super.onStop()
        // update the Amount of gold the user has in the database
        val user = HashMap<String,Any>()
        user["GoldCount"] = totalGold
        goldCount?.set(user , SetOptions.merge())?.addOnSuccessListener{
            Log.d(tag,"Document SnapShot added with ID: ${mAuth?.uid} and gold: $totalGold")
        }?.addOnFailureListener {
            Log.d(tag,"Error adding document",it)
        }
        goldListener?.remove() // remove the listener to prevent memory leaks

        // update preferences
        Log.d(tag, "[onStop] Storing Used Coin List of $used")
        Log.d(tag,"[onStop] Storing daily limit of $dailyLimit")
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        // We need an Editor object to make preference changes.
        val editor = settings.edit()
        editor.putString("UsedCoinList", used?.joinToString("$"))
        editor.putInt("DailyLimit", dailyLimit)
        // Apply the edits!
        editor.apply()
    }
}
