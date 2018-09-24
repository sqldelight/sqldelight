package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.core.integration.Shoots
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlResultSet
import com.squareup.sqldelight.internal.QueryList
import java.lang.Void
import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection

class PlayerQueries(private val queryWrapper: QueryWrapper, private val database: SqlDatabase) : Transacter(database) {
    internal val allPlayers: QueryList = QueryList()

    internal val playersForTeam: QueryList = QueryList()

    internal val playersForNumbers: QueryList = QueryList()

    internal val selectNull: QueryList = QueryList()

    private val insertPlayer: InsertPlayer by lazy {
            InsertPlayer(database.getConnection().prepareStatement("""
            |INSERT INTO player
            |VALUES (?, ?, ?, ?)
            """.trimMargin(), SqlPreparedStatement.Type.INSERT, 4))
            }

    fun <T : Any> allPlayers(mapper: (
        name: String,
        number: Long,
        team: String?,
        shoots: Shoots
    ) -> T): Query<T> = Query(allPlayers, database, """
    |SELECT *
    |FROM player
    """.trimMargin()) { resultSet ->
        mapper(
            resultSet.getString(0)!!,
            resultSet.getLong(1)!!,
            resultSet.getString(2),
            queryWrapper.playerAdapter.shootsAdapter.decode(resultSet.getString(3)!!)
        )
    }

    fun allPlayers(): Query<Player> = allPlayers(Player::Impl)

    fun <T : Any> playersForTeam(team: String?, mapper: (
        name: String,
        number: Long,
        team: String?,
        shoots: Shoots
    ) -> T): Query<T> = PlayersForTeam(team) { resultSet ->
        mapper(
            resultSet.getString(0)!!,
            resultSet.getLong(1)!!,
            resultSet.getString(2),
            queryWrapper.playerAdapter.shootsAdapter.decode(resultSet.getString(3)!!)
        )
    }

    fun playersForTeam(team: String?): Query<Player> = playersForTeam(team, Player::Impl)

    fun <T : Any> playersForNumbers(number: Collection<Long>, mapper: (
        name: String,
        number: Long,
        team: String?,
        shoots: Shoots
    ) -> T): Query<T> = PlayersForNumbers(number) { resultSet ->
        mapper(
            resultSet.getString(0)!!,
            resultSet.getLong(1)!!,
            resultSet.getString(2),
            queryWrapper.playerAdapter.shootsAdapter.decode(resultSet.getString(3)!!)
        )
    }

    fun playersForNumbers(number: Collection<Long>): Query<Player> = playersForNumbers(number, Player::Impl)

    fun <T : Any> selectNull(mapper: (expr: Void?) -> T): Query<T> = Query(selectNull, database, "SELECT NULL") { resultSet ->
        mapper(
            null
        )
    }

    fun selectNull(): Query<SelectNull> = selectNull(SelectNull::Impl)

    fun insertPlayer(
        name: String,
        number: Long,
        team: String?,
        shoots: Shoots
    ): Long = insertPlayer.execute(name, number, team, shoots)

    fun updateTeamForNumbers(team: String?, number: Collection<Long>): Long {
        val numberIndexes = createArguments(count = number.size, offset = 3)
        val statement = database.getConnection().prepareStatement("""
                |UPDATE player
                |SET team = ?1
                |WHERE number IN $numberIndexes
                """.trimMargin(), SqlPreparedStatement.Type.UPDATE, 1 + number.size)
        statement.bindString(1, team)
        number.forEachIndexed { index, number ->
                statement.bindLong(index + 3, number)
                }
        notifyQueries(queryWrapper.playerQueries.allPlayers + queryWrapper.playerQueries.playersForTeam + queryWrapper.playerQueries.playersForNumbers)
        return statement.execute()
    }

    private inner class PlayersForTeam<out T : Any>(private val team: String?, mapper: (SqlResultSet) -> T) : Query<T>(playersForTeam, mapper) {
        override fun createStatement(): SqlPreparedStatement {
            val statement = database.getConnection().prepareStatement("""
                    |SELECT *
                    |FROM player
                    |WHERE team = ?1
                    """.trimMargin(), SqlPreparedStatement.Type.SELECT, 1)
            statement.bindString(1, team)
            return statement
        }
    }

    private inner class PlayersForNumbers<out T : Any>(private val number: Collection<Long>, mapper: (SqlResultSet) -> T) : Query<T>(playersForNumbers, mapper) {
        override fun createStatement(): SqlPreparedStatement {
            val numberIndexes = createArguments(count = number.size, offset = 2)
            val statement = database.getConnection().prepareStatement("""
                    |SELECT *
                    |FROM player
                    |WHERE number IN $numberIndexes
                    """.trimMargin(), SqlPreparedStatement.Type.SELECT, number.size)
            number.forEachIndexed { index, number ->
                    statement.bindLong(index + 2, number)
                    }
            return statement
        }
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
            statement.bindString(3, team)
            statement.bindString(4, queryWrapper.playerAdapter.shootsAdapter.encode(shoots))
            val result = statement.execute()
            notifyQueries(queryWrapper.playerQueries.allPlayers + queryWrapper.playerQueries.playersForTeam + queryWrapper.playerQueries.playersForNumbers)
            return result
        }
    }
}
