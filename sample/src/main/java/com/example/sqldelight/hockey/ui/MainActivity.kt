package com.example.sqldelight.hockey.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.sqldelight.hockey.R

class MainActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)
    findViewById<Button>(R.id.list).setOnClickListener {
      startActivity(Intent(this, PlayersActivity::class.java))
    }
    findViewById<Button>(R.id.teams).setOnClickListener {
      startActivity(Intent(this, TeamsActivity::class.java))
    }
  }
}
