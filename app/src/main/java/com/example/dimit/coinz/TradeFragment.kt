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

    private var activityCallback : TradeFragment.OnFragInteractionListener? = null
    private var db : FirebaseFirestore? = null

    interface OnFragInteractionListener{
        fun makeTrade(rec: String,frag: Fragment,doc: DocumentSnapshot)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try{
            activityCallback = context as OnFragInteractionListener
        }catch (e : ClassCastException){
            throw ClassCastException("${context?.toString()} must implement OnFragInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                val item = Feature.fromJson(it.getString(ARG_ITEM_ID)!!)
                val coinTitle = item.getStringProperty("currency")
                val coinVal = item.getStringProperty("value")?.toDouble()
                val goldVal = ((MainActivity().getGold(coinTitle!!))*coinVal!!).roundToInt()
                val displayText = "$coinTitle ${item.getStringProperty("marker-symbol")} Worth: $goldVal"
                activity?.toolbar_layout?.title = displayText
            }
            if(it.containsKey("email")){
                senderEmail = it.getString("email")!!
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootview = inflater.inflate(R.layout.trade_view, container, false)

        rootview.trade_view.tradeButton.setOnClickListener {
            makeTrade(rootview.trade_view.fieldEmail.text.toString(),this) }

        return rootview
    }

    private fun makeTrade(rec :String,frag: Fragment){
        if (rec != senderEmail) {
            db?.collection(MainActivity.collection_key)?.whereEqualTo("Email",rec)?.get()?.addOnSuccessListener { it ->
                if(!it.isEmpty){
                    activityCallback?.makeTrade(rec, frag,it.documents.first())
                } else{
                    Toast.makeText(this.context,"That email does not belong to another player",Toast.LENGTH_SHORT).show()
                }
            }
        } else{
            Toast.makeText(this.context,"You can't send coins to yourself!",Toast.LENGTH_SHORT).show()
        }
    }


    companion object {
        const val ARG_ITEM_ID = "item_id"
        const val tag = "TradeFragment"
        var senderEmail = ""
    }
}
