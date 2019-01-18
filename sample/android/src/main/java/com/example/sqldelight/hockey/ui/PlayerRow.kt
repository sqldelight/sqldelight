package com.example.sqldelight.hockey.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.example.sqldelight.hockey.R
import com.example.sqldelight.hockey.data.ForTeam

class PlayerRow(
  context: Context,
  attrs: AttributeSet
) : LinearLayout(context, attrs), PlayerCell {
  override fun fillName(name: String) {
    findViewById<TextView>(R.id.player_name).text = name
  }

  override fun fillNumber(number: String) {
    findViewById<TextView>(R.id.player_number).text = number
  }

  override fun fillTeamName(teamName: String) {
    findViewById<TextView>(R.id.team_name).text = teamName
  }
}
