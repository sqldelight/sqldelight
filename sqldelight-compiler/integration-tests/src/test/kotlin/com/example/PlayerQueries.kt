package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.core.integration.Shoots
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDatabase
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

    fun <T : Any> allPlayers(mapper: (
        name: String,
        number: Long,
        team: String?,
        shoots: Shoots
    ) -> T): Query<T> = Query(82, allPlayers, database, """
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

    fun <T : Any> selectNull(mapper: (expr: Void?) -> T): Query<T> = Query(85, selectNull, database,
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
        database.execute(86, """
        |INSERT INTO player
        |VALUES (?1, ?2, ?3, ?4)
        """.trimMargin(), 4) {
            bindString(1, name)
            bindLong(2, number)
            bindString(3, team)
            bindString(4, queryWrapper.playerAdapter.shootsAdapter.encode(shoots))
        }
        notifyQueries(queryWrapper.playerQueries.allPlayers +
                queryWrapper.playerQueries.playersForTeam +
                queryWrapper.playerQueries.playersForNumbers)
    }

    fun updateTeamForNumbers(team: String?, number: Collection<Long>) {
        val numberIndexes = createArguments(count = number.size, offset = 3)
        database.execute(null, """
        |UPDATE player
        |SET team = ?1
        |WHERE number IN $numberIndexes
        """.trimMargin(), 1 + number.size) {
            bindString(1, team)
            number.forEachIndexed { index, number ->
                    bindLong(index + 3, number)
                    }
        }
        notifyQueries(queryWrapper.playerQueries.allPlayers +
                queryWrapper.playerQueries.playersForTeam +
                queryWrapper.playerQueries.playersForNumbers)
    }

    fun foreignKeysOn() {
        database.execute(88, """PRAGMA foreign_keys = 1""", 0)
    }

    fun foreignKeysOff() {
        database.execute(89, """PRAGMA foreign_keys = 0""", 0)
    }

    private inner class PlayersForTeam<out T : Any>(private val team: String?, mapper:
            (SqlCursor) -> T) : Query<T>(playersForTeam, mapper) {
        override fun execute(): SqlCursor = database.executeQuery(null, """
        |SELECT *
        |FROM player
        |WHERE team ${ if (team == null) "IS" else "=" } ?1
        """.trimMargin(), 1) {
            bindString(1, team)
        }
    }

    private inner class PlayersForNumbers<out T : Any>(private val number: Collection<Long>, mapper:
            (SqlCursor) -> T) : Query<T>(playersForNumbers, mapper) {
        override fun execute(): SqlCursor {
            val numberIndexes = createArguments(count = number.size, offset = 2)
            return database.executeQuery(null, """
            |SELECT *
            |FROM player
            |WHERE number IN $numberIndexes
            """.trimMargin(), number.size) {
                number.forEachIndexed { index, number ->
                        bindLong(index + 2, number)
                        }
            }
        }
    }
}
