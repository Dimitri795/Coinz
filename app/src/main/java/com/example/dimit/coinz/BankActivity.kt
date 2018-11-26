package com.example.dimit.coinz

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import kotlinx.android.synthetic.main.activity_bank.*
import kotlinx.android.synthetic.main.coin_list.*
import kotlinx.android.synthetic.main.coin_list_content.view.*
import kotlin.math.roundToInt

/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [CoinDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class BankActivity : AppCompatActivity() {

    private val tag = "BankActivity"
    private var twoPane: Boolean = false // whether the activity is in two pane mode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)

        setSupportActionBar(toolbar)
        toolbar.title = title

        fab.setOnClickListener { _->
            startActivity(Intent(this@BankActivity,MainActivity::class.java))
        }
        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (coin_detail_container != null) {
            // The detail container view will be present only in the large-screen layouts (res/values-w900dp).
            twoPane = true
        }
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
        val fc = FeatureCollection.fromJson(MainActivity.dailyFcData).features()
        val newfc = fc?.filter{MainActivity.collected?.contains(it.getStringProperty("id"))!!} as ArrayList<Feature>?
        Wallet.apply {
            layoutManager = LinearLayoutManager(this@BankActivity)
            recyclerView.adapter = SimpleItemRecyclerViewAdapter(this@BankActivity,newfc,twoPane)
        }

    }

    class SimpleItemRecyclerViewAdapter(private val parentActivity: BankActivity, private val values: ArrayList<Feature>?, private val twoPane: Boolean) :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

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
        }

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
            val displayContent = "Coin worth: ${((MainActivity().getGold(coinTitle!!))*coinVal!!).roundToInt()}"
            holder.idView.text = displayText
            holder.contentView.text = displayContent

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values!!.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val idView: TextView = view.id_text
            val contentView: TextView = view.content
        }
    }

    public override fun onStart() {
        super.onStart()
    }

    public override fun onStop() {
        super.onStop()
    }
}
