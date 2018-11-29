package com.example.dimit.coinz

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mapbox.geojson.Feature
import kotlinx.android.synthetic.main.activity_coin_detail.*
import kotlinx.android.synthetic.main.coin_detail.view.*
import kotlin.math.roundToInt

class CoinDetailFragment : Fragment() {



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
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.coin_detail, container, false)

        // Show the dummy content as text in a TextView.
        view.let {
            rootView.coin_detail.text = it?.tooltipText
        }

        return rootView
    }

    companion object {
        const val ARG_ITEM_ID = "item_id"
        const val tag = "CoinDetailFragment"
    }
}
