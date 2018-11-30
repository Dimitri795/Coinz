package com.example.dimit.coinz

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.mapbox.geojson.Feature
import kotlinx.android.synthetic.main.activity_coin_detail.*
import kotlinx.android.synthetic.main.coin_detail.view.*
import java.lang.ClassCastException
import kotlin.math.roundToInt

class CoinDetailFragment : Fragment() {

    var activityCallback : CoinDetailFragment.OnFragInteractionListener? = null

    interface OnFragInteractionListener{
        fun makeTrade(rec: String,frag: Fragment)
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
        val rootview = inflater.inflate(R.layout.coin_detail, container, false)

        rootview.coin_detail.tradeButton.setOnClickListener {
            makeTrade(rootview.coin_detail.fieldEmail.text.toString(),this) }

        return rootview
    }

    private fun makeTrade(rec :String,frag: Fragment){
        if(!rec.isEmpty() && rec != senderEmail){
            activityCallback?.makeTrade(rec,frag)
        }
    }


    companion object {
        const val ARG_ITEM_ID = "item_id"
        const val tag = "CoinDetailFragment"
        var senderEmail = ""
    }
}
