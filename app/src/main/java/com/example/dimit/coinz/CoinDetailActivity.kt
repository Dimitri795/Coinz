package com.example.dimit.coinz

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_coin_detail.*

/**
 * An activity representing a single Coin detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a [BankActivity].
 */
class CoinDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coin_detail)
        setSupportActionBar(detail_toolbar)

        fab.setOnClickListener { _->
            startActivity(Intent(this@CoinDetailActivity,MainActivity::class.java))
        }

        // Show the Up button in the action bar.
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            val fragment = CoinDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(CoinDetailFragment.ARG_ITEM_ID,
                            intent.getStringExtra(CoinDetailFragment.ARG_ITEM_ID))
                }
            }

            supportFragmentManager.beginTransaction()
                    .add(R.id.coin_detail_container, fragment)
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                android.R.id.home -> {
                    navigateUpTo(Intent(this, BankActivity::class.java))
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}
