package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.core.integration.Shoots
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlPreparedStatement
import java.lang.Void
import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection
import kotlin.collections.MutableList

class PlayerQueries(private val queryWrapper: QueryWrapper, private val database: SqlDatabase) :
        Transacter(database) {
    internal val allPlayers: MutableList<Query<*>> =
            com.squareup.sqldelight.internal.copyOnWriteList()

    internal val playersForTeam: MutableList<Query<*>> =
            com.squareup.sqldelight.internal.copyOnWriteList()

    internal val playersForNumbers: MutableList<Query<*>> =
            com.squareup.sqldelight.internal.copyOnWriteList()

    internal val selectNull: MutableList<Query<*>> =
            com.squareup.sqldelight.internal.copyOnWriteList()

    private val insertPlayer: InsertPlayer = InsertPlayer()

    fun <T : Any> allPlayers(mapper: (
        name: String,
        number: Long,
        team: String?,
        shoots: Shoots
    ) -> T): Query<T> = Query(66, allPlayers, database, """
    |SELECT *
    |FROM player
    """.trimMargin()) { cursor ->
        mapper(
            cursor.getString(0)!!,
            cursor.getLong(1)!!,
            cursor.getString(2),
            queryWrapper.playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
        )
    }

    fun allPlayers(): Query<Player> = allPlayers(Player::Impl)

    fun <T : Any> playersForTeam(team: String?, mapper: (
        name: String,
        number: Long,
        team: String?,
        shoots: Shoots
    ) -> T): Query<T> = PlayersForTeam(team) { cursor ->
        mapper(
            cursor.getString(0)!!,
            cursor.getLong(1)!!,
            cursor.getString(2),
            queryWrapper.playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
        )
    }

    fun playersForTeam(team: String?): Query<Player> = playersForTeam(team, Player::Impl)

    fun <T : Any> playersForNumbers(number: Collection<Long>, mapper: (
        name: String,
        number: Long,
        team: String?,
        shoots: Shoots
    ) -> T): Query<T> = PlayersForNumbers(number) { cursor ->
        mapper(
            cursor.getString(0)!!,
            cursor.getLong(1)!!,
            cursor.getString(2),
            queryWrapper.playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
        )
    }

    fun playersForNumbers(number: Collection<Long>): Query<Player> = playersForNumbers(number,
            Player::Impl)

    fun <T : Any> selectNull(mapper: (expr: Void?) -> T): Query<T> = Query(69, selectNull, database,
            "SELECT NULL") { cursor ->
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
    ) {
        insertPlayer.execute(name, number, team, shoots)
    }

    fun updateTeamForNumbers(team: String?, number: Collection<Long>) {
        val numberIndexes = createArguments(count = number.size, offset = 3)
        val statement = database.getConnection().prepareStatement(null, """
                |UPDATE player
                |SET team = ?1
                |WHERE number IN $numberIndexes
                """.trimMargin(), SqlPreparedStatement.Type.UPDATE, 1 + number.size)
        statement.bindString(1, team)
        number.forEachIndexed { index, number ->
                statement.bindLong(index + 3, number)
                }
        notifyQueries(queryWrapper.playerQueries.allPlayers +
                queryWrapper.playerQueries.playersForTeam +
                queryWrapper.playerQueries.playersForNumbers)
        statement.execute()
    }

    private inner class PlayersForTeam<out T : Any>(private val team: String?, mapper:
            (SqlCursor) -> T) : Query<T>(playersForTeam, mapper) {
        override fun createStatement(): SqlPreparedStatement {
            val statement = database.getConnection().prepareStatement(67, """
                    |SELECT *
                    |FROM player
                    |WHERE team ${ if (team == null) "IS" else "=" } ?1
                    """.trimMargin(), SqlPreparedStatement.Type.SELECT, 1)
            statement.bindString(1, team)
            return statement
        }
    }

    private inner class PlayersForNumbers<out T : Any>(private val number: Collection<Long>, mapper:
            (SqlCursor) -> T) : Query<T>(playersForNumbers, mapper) {
        override fun createStatement(): SqlPreparedStatement {
            val numberIndexes = createArguments(count = number.size, offset = 2)
            val statement = database.getConnection().prepareStatement(null, """
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

    private inner class InsertPlayer {
        fun execute(
            name: String,
            number: Long,
            team: String?,
            shoots: Shoots
        ) {
            val statement = database.getConnection().prepareStatement(70, """
                |INSERT INTO player
                |VALUES (?, ?, ?, ?)
                """.trimMargin(), SqlPreparedStatement.Type.INSERT, 4)
            statement.bindString(1, name)
            statement.bindLong(2, number)
            statement.bindString(3, team)
            statement.bindString(4, queryWrapper.playerAdapter.shootsAdapter.encode(shoots))
            statement.execute()
            notifyQueries(queryWrapper.playerQueries.allPlayers +
                    queryWrapper.playerQueries.playersForTeam +
                    queryWrapper.playerQueries.playersForNumbers)
        }
    }
}
