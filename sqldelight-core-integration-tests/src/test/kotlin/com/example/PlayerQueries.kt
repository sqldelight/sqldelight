package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.core.integration.Shoots
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlResultSet
import java.lang.ThreadLocal
import kotlin.Boolean
import kotlin.Long
import kotlin.String
import kotlin.collections.MutableList

class PlayerQueries(
        private val queryWrapper: QueryWrapper,
        private val database: SqlDatabase,
        transactions: ThreadLocal<Transacter.Transaction>
) : Transacter(database, transactions) {
    internal val allPlayers: MutableList<Query<*>> = mutableListOf()

    internal val playersForTeam: MutableList<Query<*>> = mutableListOf()

    private val insertPlayer: InsertPlayer by lazy {
            InsertPlayer(database.getConnection().prepareStatement("""
            |INSERT INTO player
            |VALUES (?, ?, ?, ?)
            """.trimMargin()))
            }

    fun <T> allPlayers(mapper: (
            name: String,
            number: Long,
            team: String?,
            shoots: Shoots
    ) -> T): Query<T> {
        val statement = database.getConnection().prepareStatement("""
                |SELECT *
                |FROM player
                """.trimMargin())
        return Query(statement, allPlayers) { resultSet ->
            mapper(
                resultSet.getString(0)!!,
                resultSet.getLong(1)!!,
                resultSet.getString(2),
                queryWrapper.playerAdapter.shootsAdapter.decode(resultSet.getString(3)!!)
            )
        }
    }

    fun allPlayers(): Query<Player> = allPlayers(Player::Impl)
    fun <T> playersForTeam(team: String?, mapper: (
            name: String,
            number: Long,
            team: String?,
            shoots: Shoots
    ) -> T): Query<T> {
        val statement = database.getConnection().prepareStatement("""
                |SELECT *
                |FROM player
                |WHERE team = ?
                """.trimMargin())
        statement.bindString(1, if (team == null) null else team)
        return PlayersForTeam(team, statement) { resultSet ->
            mapper(
                resultSet.getString(0)!!,
                resultSet.getLong(1)!!,
                resultSet.getString(2),
                queryWrapper.playerAdapter.shootsAdapter.decode(resultSet.getString(3)!!)
            )
        }
    }

    fun playersForTeam(team: String?): Query<Player> = playersForTeam(team, Player::Impl)
    fun insertPlayer(
            name: String,
            number: Long,
            team: String?,
            shoots: Shoots
    ): Long = insertPlayer.execute(name, number, team, shoots)

    private inner class PlayersForTeam<out T>(
            private val team: String?,
            statement: SqlPreparedStatement,
            mapper: (SqlResultSet) -> T
    ) : Query<T>(statement, playersForTeam, mapper) {
        fun dirtied(team: String?): Boolean = true
    }

    private inner class InsertPlayer(private val statement: SqlPreparedStatement) {
        fun execute(
                name: String,
                number: Long,
                team: String?,
                shoots: Shoots
        ): Long {
            statement.bindString(1, name)
            statement.bindLong(2, number)
            statement.bindString(3, if (team == null) null else team)
            statement.bindString(4, queryWrapper.playerAdapter.shootsAdapter.encode(shoots))
            val result = statement.execute()
            deferAction {
                (queryWrapper.playerQueries.allPlayers + queryWrapper.playerQueries.playersForTeam)
                        .forEach { it.notifyResultSetChanged() }
            }
            return result
        }
    }
}
