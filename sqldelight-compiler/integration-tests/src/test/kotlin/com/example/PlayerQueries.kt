package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.core.integration.Shoots
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import java.lang.Void
import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection
import kotlin.collections.MutableList

class PlayerQueries(private val database: TestDatabase, private val driver: SqlDriver) :
        Transacter(driver) {
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
    ) -> T): Query<T> = Query(2, allPlayers, driver, """
    |SELECT *
    |FROM player
    """.trimMargin()) { cursor ->
        mapper(
            cursor.getString(0)!!,
            cursor.getLong(1)!!,
            cursor.getString(2),
            database.playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
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
            database.playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
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
            database.playerAdapter.shootsAdapter.decode(cursor.getString(3)!!)
        )
    }

    fun playersForNumbers(number: Collection<Long>): Query<Player> = playersForNumbers(number,
            Player::Impl)

    fun <T : Any> selectNull(mapper: (expr: Void?) -> T): Query<T> = Query(5, selectNull, driver,
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
        driver.execute(6, """
        |INSERT INTO player
        |VALUES (?1, ?2, ?3, ?4)
        """.trimMargin(), 4) {
            bindString(1, name)
            bindLong(2, number)
            bindString(3, team)
            bindString(4, database.playerAdapter.shootsAdapter.encode(shoots))
        }
        notifyQueries(database.playerQueries.allPlayers + database.playerQueries.playersForTeam +
                database.playerQueries.playersForNumbers)
    }

    fun updateTeamForNumbers(team: String?, number: Collection<Long>) {
        val numberIndexes = createArguments(count = number.size, offset = 2)
        driver.execute(null, """
        |UPDATE player
        |SET team = ?1
        |WHERE number IN $numberIndexes
        """.trimMargin(), 1 + number.size) {
            bindString(1, team)
            number.forEachIndexed { index, number ->
                    bindLong(index + 2, number)
                    }
        }
        notifyQueries(database.playerQueries.allPlayers + database.playerQueries.playersForTeam +
                database.playerQueries.playersForNumbers)
    }

    fun foreignKeysOn() {
        driver.execute(9, """PRAGMA foreign_keys = 1""", 0)
    }

    fun foreignKeysOff() {
        driver.execute(10, """PRAGMA foreign_keys = 0""", 0)
    }

    private inner class PlayersForTeam<out T : Any>(private val team: String?, mapper:
            (SqlCursor) -> T) : Query<T>(playersForTeam, mapper) {
        override fun execute(): SqlCursor = driver.executeQuery(null, """
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
            val numberIndexes = createArguments(count = number.size, offset = 1)
            return driver.executeQuery(null, """
            |SELECT *
            |FROM player
            |WHERE number IN $numberIndexes
            """.trimMargin(), number.size) {
                number.forEachIndexed { index, number ->
                        bindLong(index + 1, number)
                        }
            }
        }
    }
}
