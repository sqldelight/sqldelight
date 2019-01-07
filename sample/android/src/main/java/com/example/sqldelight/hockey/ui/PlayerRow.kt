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
) : LinearLayout(context, attrs) {
  fun populate(row: ForTeam) {
    findViewById<TextView>(R.id.player_name).text = "${row.first_name} ${row.last_name}"
    findViewById<TextView>(R.id.player_number).text = row.number
    findViewById<TextView>(R.id.team_name).text = row.teamName
  }
}
