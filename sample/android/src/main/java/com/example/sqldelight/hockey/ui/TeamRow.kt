package com.example.sqldelight.hockey.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.example.sqldelight.hockey.R
import com.example.sqldelight.hockey.data.Team
import java.text.SimpleDateFormat

class TeamRow(
  context: Context,
  attrs: AttributeSet
) : LinearLayout(context, attrs), TeamCell {
  override fun fillName(name: String) {
    findViewById<TextView>(R.id.team_name).text = name
  }

  override fun fillCoach(coach: String) {
    findViewById<TextView>(R.id.coach_name).text = coach
  }

  override fun fillFounded(founded: String) {
    findViewById<TextView>(R.id.founded).text = founded
  }
}
