package com.example.sqldelight.hockey.ui

import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.Team
import com.example.sqldelight.hockey.platform.DateFormatHelper

class TeamData(updateNotifier: () -> Unit) {
  private val df = DateFormatHelper("dd/MM/yyyy")
  private var teamsList: List<Team> = emptyList()

  init {
    val db = Db.instance.teamQueries
    teamsList = db.selectAll().executeAsList()
    updateNotifier()
  }

  val size: Int
    get() = teamsList.size

  fun findRow(index: Int): Team = teamsList.get(index)

  fun fillRow(index: Int, cell: TeamCell) {
    val team = teamsList.get(index)
    cell.fillName(team.name)
    cell.fillCoach(team.coach)
    cell.fillFounded(df.format(team.founded))
  }
}