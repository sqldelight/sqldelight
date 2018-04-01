package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.core.integration.Shoots
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlResultSet
import kotlin.Any
import kotlin.Boolean
import kotlin.Long
import kotlin.String
import kotlin.Unit
import kotlin.collections.Collection
import kotlin.collections.MutableList

class PlayerQueries(private val queryWrapper: QueryWrapper, private val database: SqlDatabase) : Transacter(database) {
    internal val allPlayers: MutableList<Query<*>> = mutableListOf()

    internal val playersForTeam: MutableList<Query<*>> = mutableListOf()

    internal val playersForNumbers: MutableList<Query<*>> = mutableListOf()

    private val insertPlayer: InsertPlayer by lazy {
            InsertPlayer(database.getConnection().prepareStatement("""
            |INSERT INTO player
            |VALUES (?, ?, ?, ?)
            """.trimMargin(), SqlPreparedStatement.Type.INSERT))
            }

    fun <T : Any> allPlayers(mapper: (
            name: String,
            number: Long,
            team: String?,
            shoots: Shoots
    ) -> T): Query<T> {
        val statement = database.getConnection().prepareStatement("""
                |SELECT *
                |FROM player
                """.trimMargin(), SqlPreparedStatement.Type.SELECT)
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

    fun <T : Any> playersForTeam(team: String?, mapper: (
            name: String,
            number: Long,
            team: String?,
            shoots: Shoots
    ) -> T): Query<T> {
        val statement = database.getConnection().prepareStatement("""
                |SELECT *
                |FROM player
                |WHERE team = ?1
                """.trimMargin(), SqlPreparedStatement.Type.SELECT)
        statement.bindString(1, team)
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

    fun <T : Any> playersForNumbers(number: Collection<Long>, mapper: (
            name: String,
            number: Long,
            team: String?,
            shoots: Shoots
    ) -> T): Query<T> {
        val numberIndexes = number.mapIndexed { index, _ ->
                "?${ index + 2 }"
                }.joinToString(prefix = "(", postfix = ")")
        val statement = database.getConnection().prepareStatement("""
                |SELECT *
                |FROM player
                |WHERE number IN $numberIndexes
                """.trimMargin(), SqlPreparedStatement.Type.SELECT)
        number.forEachIndexed { index, number ->
                statement.bindLong(index + 2, number)
                }
        return PlayersForNumbers(number, statement) { resultSet ->
            mapper(
                resultSet.getString(0)!!,
                resultSet.getLong(1)!!,
                resultSet.getString(2),
                queryWrapper.playerAdapter.shootsAdapter.decode(resultSet.getString(3)!!)
            )
        }
    }

    fun playersForNumbers(number: Collection<Long>): Query<Player> = playersForNumbers(number, Player::Impl)

    fun insertPlayer(
            name: String,
            number: Long,
            team: String?,
            shoots: Shoots
    ): Long = insertPlayer.execute(name, number, team, shoots)

    fun updateTeamForNumbers(team: String?, number: Collection<Long>): Long {
        val numberIndexes = number.mapIndexed { index, _ ->
                "?${ index + 3 }"
                }.joinToString(prefix = "(", postfix = ")")
        val statement = database.getConnection().prepareStatement("""
                |UPDATE player
                |SET team = ?1
                |WHERE number IN $numberIndexes
                """.trimMargin(), SqlPreparedStatement.Type.UPDATE)
        statement.bindString(1, team)
        number.forEachIndexed { index, number ->
                statement.bindLong(index + 3, number)
                }
        return statement.execute()
    }

    private inner class PlayersForTeam<out T : Any>(
            private val team: String?,
            statement: SqlPreparedStatement,
            mapper: (SqlResultSet) -> T
    ) : Query<T>(statement, playersForTeam, mapper) {
        fun dirtied(team: String?): Boolean = true
    }

    private inner class PlayersForNumbers<out T : Any>(
            private val number: Collection<Long>,
            statement: SqlPreparedStatement,
            mapper: (SqlResultSet) -> T
    ) : Query<T>(statement, playersForNumbers, mapper) {
        fun dirtied(number: Long): Boolean = true
    }

    private inner class InsertPlayer(private val statement: SqlPreparedStatement) {
        private val notify: () -> Unit = {
                (queryWrapper.playerQueries.allPlayers + queryWrapper.playerQueries.playersForTeam + queryWrapper.playerQueries.playersForNumbers)
                .forEach { it.notifyResultSetChanged() }
                }

        fun execute(
                name: String,
                number: Long,
                team: String?,
                shoots: Shoots
        ): Long {
            statement.bindString(1, name)
            statement.bindLong(2, number)
            statement.bindString(3, team)
            statement.bindString(4, queryWrapper.playerAdapter.shootsAdapter.encode(shoots))
            val result = statement.execute()
            deferAction(notify)
            return result
        }
    }
}
