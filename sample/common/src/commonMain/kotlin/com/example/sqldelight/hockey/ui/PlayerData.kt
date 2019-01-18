package com.example.sqldelight.hockey.ui

import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.ForTeam

class PlayerData(teamId:Long, updateNotifier:()->Unit){

    private var playerList:List<ForTeam> = emptyList()

    init {
        val db = Db.instance.playerQueries
        playerList = db.forTeam(teamId).executeAsList()
        updateNotifier()
    }

    val size:Int
        get() = playerList.size

    fun fillRow(index:Int, cell: PlayerCell){
        val player = playerList.get(index)
        cell.fillName("${player.first_name} ${player.last_name}")
        cell.fillNumber(player.number)
        cell.fillTeamName(player.teamName)
    }
}