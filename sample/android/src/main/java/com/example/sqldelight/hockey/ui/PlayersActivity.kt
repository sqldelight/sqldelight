package com.example.sqldelight.hockey.ui

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sqldelight.hockey.R
import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.ForTeam
import com.example.sqldelight.hockey.ui.PlayersActivity.PlayersAdapter.ViewHolder

class PlayersActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.list)

    val players = findViewById<RecyclerView>(R.id.list)
    players.layoutManager = LinearLayoutManager(this)

    val data = PlayerData(intent.getLongExtra(TEAM_ID, -1)){
      val playersAdapter = players.adapter
      playersAdapter?.notifyDataSetChanged()
    }

    players.adapter = PlayersAdapter(data)
  }

  private inner class PlayersAdapter(
    var data: PlayerData
  ) : RecyclerView.Adapter<ViewHolder>() {
    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      return ViewHolder(layoutInflater.inflate(R.layout.player_row, parent, false) as PlayerRow)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      data.fillRow(position, holder.row)
    }

    inner class ViewHolder(val row: PlayerRow): RecyclerView.ViewHolder(row)
  }

  companion object {
    val TEAM_ID = "team_id"
  }
}
