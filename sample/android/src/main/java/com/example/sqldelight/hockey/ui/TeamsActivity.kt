package com.example.sqldelight.hockey.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sqldelight.hockey.R
import com.example.sqldelight.hockey.data.Team
import com.example.sqldelight.hockey.ui.TeamsActivity.Adapter.ViewHolder

class TeamsActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.list)

    val teams = findViewById<RecyclerView>(R.id.list)

    val data = TeamData {
      val adapter = teams.adapter
      adapter?.notifyDataSetChanged()
    }

    val adapter = Adapter(data) { teamClicked ->
      val intent = Intent(this@TeamsActivity, PlayersActivity::class.java)
      intent.putExtra(PlayersActivity.TEAM_ID, teamClicked.id)
      startActivity(intent)
    }

    teams.layoutManager = LinearLayoutManager(this)
    teams.adapter = adapter
  }

  private inner class Adapter(
    private val data: TeamData,
    private val clickListener: (Team) -> Unit
  ) : RecyclerView.Adapter<ViewHolder>() {
    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      return ViewHolder(layoutInflater.inflate(R.layout.team_row, parent, false) as TeamRow)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      data.fillRow(position, holder.row)
      holder.row.setOnClickListener { clickListener(data.findRow(position)) }
    }

    inner class ViewHolder(val row: TeamRow) : RecyclerView.ViewHolder(row)
  }
}
