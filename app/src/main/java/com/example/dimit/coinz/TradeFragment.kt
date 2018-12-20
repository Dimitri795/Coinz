package com.example.dimit.coinz

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.Feature
import kotlinx.android.synthetic.main.activity_trade.*
import kotlinx.android.synthetic.main.trade_view.view.*
import java.lang.ClassCastException
import kotlin.math.roundToInt

class TradeFragment : Fragment() {

    private var activityCallback : TradeFragment.OnFragInteractionListener? = null // Fragment listener interface
    private var db : FirebaseFirestore? = null                                     // Firebase cloud storage variable

    companion object { //globablly accessible companion to this Fragment
        const val ARG_ITEM_ID = "item_id"  // the key for the arguments bundle
        var senderName = ""               // the username of the trade maker
    }

    interface OnFragInteractionListener{
        // define an interface that the TradeActivity can extend so that methods in this Fragment can be implemented in the activity
        fun makeTrade(rec: String,frag: Fragment,doc: DocumentSnapshot)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try{
            // when attached to an activity ensure it extends the interface
            activityCallback = context as OnFragInteractionListener
        }catch (e : ClassCastException){
            // error if the activity doesn't
            throw ClassCastException("${context?.toString()} must implement OnFragInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance() // get the current instance of the Firebase cloud storage linked to the app
        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                // Get relevant details of the coin to display as the header for the trade. In case users forget which coin they are trading.
                val item = Feature.fromJson(it.getString(ARG_ITEM_ID)!!)
                val coinTitle = item.getStringProperty("currency")
                val coinVal = item.getStringProperty("value")?.toDouble()
                val goldVal = ((MainActivity().getGold(coinTitle!!))*coinVal!!).roundToInt()
                val displayText = "$coinTitle ${item.getStringProperty("marker-symbol")} Worth: $goldVal"
                activity?.toolbar_layout?.title = displayText
            }
            if(it.containsKey("username")){
                // get the trader's username
                senderName = it.getString("username")!!
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // create the view to hold the text fields and button
        val rootview = inflater.inflate(R.layout.trade_view, container, false)

        rootview.trade_view.sendCoinButton.setOnClickListener {
            // A trade is being made, validate it and then pass it back to TradeActivity via the interface
            makeTrade(rootview.trade_view.fieldUsername.text.toString(),this) }

        return rootview
    }

    private fun makeTrade(rec :String,frag: Fragment){
        if (rec != senderName) { // Can't send coins to yourself
            db?.collection(MainActivity.collection_key)?.whereEqualTo(ChatActivity.nameField,rec)?.get()?.addOnSuccessListener { it ->
                // Make sure the subject of the trade is an actual user of the app
                if(!it.isEmpty){
                    activityCallback?.makeTrade(rec, frag,it.documents.first()) // implement the validated trade in TradeActivity
                } else{ // error message
                    Toast.makeText(this.context,"That username does not belong to another player",Toast.LENGTH_SHORT).show()
                }
            }
        } else{ // error message
            Toast.makeText(this.context,"You can't send coins to yourself!",Toast.LENGTH_SHORT).show()
        }
    }

}
