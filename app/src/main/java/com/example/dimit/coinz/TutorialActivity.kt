package com.example.dimit.coinz

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { _ ->
            startActivity(Intent(this@TutorialActivity,MainActivity::class.java))
        }
    }
}
