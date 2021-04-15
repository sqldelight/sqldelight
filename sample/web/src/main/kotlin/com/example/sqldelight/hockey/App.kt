package com.example.sqldelight.hockey

import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.getInstance
import com.example.sqldelight.hockey.platform.defaultFormatter
import kotlinx.html.dom.create
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr
import kotlinx.browser.document

fun main() {
  Db.getInstance().then { db ->

    val players = db.playerQueries.forTeam(-1).executeAsList()
    val playersTable = document.create.table {
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
    document.getElementById("players")?.append(playersTable)

    val teams = db.teamQueries.selectAll().executeAsList()
    val teamsTable = document.create.table {
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
            td { +defaultFormatter.format(team.founded) }
          }
        }
      }
    }
    document.getElementById("teams")?.append(teamsTable)
  }
}
