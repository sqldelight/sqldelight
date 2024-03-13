package com.example.sqldelight.hockey

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.example.sqldelight.hockey.db.DbHelper
import com.example.sqldelight.hockey.db.ForTeam
import com.example.sqldelight.hockey.db.Team
import com.example.sqldelight.hockey.db.formatted
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.html.dom.create
import kotlinx.html.js.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import org.w3c.dom.HTMLTableElement

fun main() {
  val scope = MainScope()
  val dbHelper = DbHelper()

  scope.launch {
    dbHelper.withDatabase { database ->
      val players = database.playerQueries.forTeam(-1).awaitAsList()
      document.getElementById("players")?.append(buildPlayersTable(players))

      val teams = database.teamQueries.selectAll().awaitAsList()
      document.getElementById("teams")?.append(buildCoachesTable(teams))
    }
  }
}

private fun buildPlayersTable(players: List<ForTeam>): HTMLTableElement = document.create.table {
  thead {
    tr {
      th { +"First Name" }
      th { +"Last Name" }
      th { +"Team Name" }
      th { +"Number" }
    }
  }
  tbody {
    players.forEach { player ->
      tr {
        td { +player.first_name }
        td { +player.last_name }
        td { +player.teamName }
        td { +player.number }
      }
    }
  }
}

private fun buildCoachesTable(teams: List<Team>): HTMLTableElement = document.create.table {
  thead {
    tr {
      th { +"Name" }
      th { +"Coach" }
      th { +"Founded" }
    }
  }
  tbody {
    teams.forEach { team ->
      tr {
        td { +team.name }
        td { +team.coach }
        td { +team.founded.formatted("dd/MM/yyyy") }
      }
    }
  }
}
