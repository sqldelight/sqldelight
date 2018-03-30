package com.example.sqldelight.hockey.ui

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.example.sqldelight.hockey.R
import com.example.sqldelight.hockey.data.ForTeam
import com.example.sqldelight.hockey.data.HockeyOpenHelper
import com.example.sqldelight.hockey.ui.PlayersActivity.PlayersAdapter.ViewHolder

class PlayersActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.list)

    val db = HockeyOpenHelper.getInstance(this).playerQueries
    val players = findViewById<RecyclerView>(R.id.list)
    players.layoutManager = LinearLayoutManager(this)
    players.adapter = PlayersAdapter(db.forTeam(intent.getLongExtra(TEAM_ID, -1)).executeAsList())
  }

  private inner class PlayersAdapter(
    private val data: List<ForTeam>
  ) : RecyclerView.Adapter<ViewHolder>() {
    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      return ViewHolder(layoutInflater.inflate(R.layout.player_row, parent, false) as PlayerRow)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      holder.row.populate(data[position])
    }

    inner class ViewHolder(val row: PlayerRow): RecyclerView.ViewHolder(row)
  }

  companion object {
    val TEAM_ID = "team_id"
  }
}
