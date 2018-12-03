package com.example.dimit.coinz

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.item_detail.view.*

class ItemDetailFragment : Fragment() {

    private var item : ShopActivity.Items? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                val i = it.getInt(ARG_ITEM_ID)
                val name = resources.getStringArray(R.array.Names)[i]
                val descrip = resources.getStringArray(R.array.Descriptions)[i]
                val cost = resources.getIntArray(R.array.Costs)[i]
                item = ShopActivity.Items(i,name)
                item?.descrip = descrip
                item?.cost = cost
                activity?.toolbar_layout?.title = item?.name
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.item_detail, container, false)

        // Show the dummy content as text in a TextView.
        item.let {
            val details = "${it?.descrip} \n \nPrice: ${it?.cost} gold"
            rootView.item_detail.text = details
        }

        return rootView
    }

    companion object {
       const val ARG_ITEM_ID = "item_id"
    }
}
