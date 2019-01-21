package com.example.sqldelight.hockey.ui

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sqldelight.hockey.R
import com.example.sqldelight.hockey.data.Date
import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.ForTeam
import com.example.sqldelight.hockey.data.PlayerVals.Position
import com.example.sqldelight.hockey.data.PlayerVals.Shoots
import com.example.sqldelight.hockey.ui.PlayersActivity.PlayersAdapter.ViewHolder
import java.util.GregorianCalendar

class PlayersActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.list)

    val db = Db.getInstance(this).playerQueries
    val players = findViewById<RecyclerView>(R.id.list)
    players.layoutManager = LinearLayoutManager(this)
    players.adapter = PlayersAdapter(db.forTeam(intent.getLongExtra(TEAM_ID, -1)).executeAsList())

    // Inserting a Player will trigger an AbstractMethodError.
    db.insertPlayer(
        "R8", ":)", 15, "Foo", 30, 221F, Date(1985, 5, 10),
        Shoots.RIGHT, Position.CENTER
    )
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
