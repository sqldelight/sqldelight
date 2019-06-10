package com.example.testmodule

import com.example.GroupQueries
import com.example.Player
import com.example.PlayerQueries
import com.example.SelectNull
import com.example.Team
import com.example.TeamQueries
import com.example.TestDatabase
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.core.integration.Shoots
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.copyOnWriteList
import java.lang.Void
import kotlin.Any
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection
import kotlin.collections.MutableList
import kotlin.reflect.KClass

internal val KClass<TestDatabase>.schema: SqlDriver.Schema
    get() = TestDatabaseImpl.Schema

internal fun KClass<TestDatabase>.newInstance(
    driver: SqlDriver,
    playerAdapter: Player.Adapter,
    teamAdapter: Team.Adapter
): TestDatabase = TestDatabaseImpl(driver, playerAdapter, teamAdapter)

private class TestDatabaseImpl(
    driver: SqlDriver,
    internal val playerAdapter: Player.Adapter,
    internal val teamAdapter: Team.Adapter
) : TransacterImpl(driver), TestDatabase {
    override val groupQueries: GroupQueriesImpl = GroupQueriesImpl(this, driver)

    override val playerQueries: PlayerQueriesImpl = PlayerQueriesImpl(this, driver)

    override val teamQueries: TeamQueriesImpl = TeamQueriesImpl(this, driver)

    object Schema : SqlDriver.Schema {
        override val version: Int
            get() = 1

        override fun create(driver: SqlDriver) {
            driver.execute(null, """
                    |CREATE TABLE team (
                    |  name TEXT PRIMARY KEY NOT NULL,
                    |  captain INTEGER UNIQUE NOT NULL REFERENCES player(number),
                    |  inner_type TEXT,
                    |  coach TEXT NOT NULL
                    |)
                    """.trimMargin(), 0)
            driver.execute(null, """
                    |INSERT INTO team
                    |VALUES ('Anaheim Ducks', 15, NULL, 'Randy Carlyle'),
                    |       ('Ottawa Senators', 65, 'ONE', 'Guy Boucher')
                    """.trimMargin(), 0)
            driver.execute(null, """
                    |CREATE TABLE player (
                    |  name TEXT NOT NULL,
                    |  number INTEGER NOT NULL,
                    |  team TEXT REFERENCES team(name),
                    |  shoots TEXT NOT NULL,
                    |  PRIMARY KEY (team, number)
                    |)
                    """.trimMargin(), 0)
            driver.execute(null, """
                    |INSERT INTO player
                    |VALUES ('Ryan Getzlaf', 15, 'Anaheim Ducks', 'RIGHT'),
                    |       ('Erik Karlsson', 65, 'Ottawa Senators', 'RIGHT')
                    """.trimMargin(), 0)
            driver.execute(null, "CREATE TABLE `group` (`index` INTEGER PRIMARY KEY NOT NULL)", 0)
            driver.execute(null, "INSERT INTO `group` VALUES (1), (2), (3)", 0)
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Int,
            newVersion: Int
        ) {
        }
    }
}

private class TeamQueriesImpl(private val database: TestDatabaseImpl, private val driver: SqlDriver)
        : TransacterImpl(driver), TeamQueries {
    internal val teamForCoach: MutableList<Query<*>> = copyOnWriteList()

    internal val forInnerType: MutableList<Query<*>> = copyOnWriteList()

    override fun <T : Any> teamForCoach(coach: String, mapper: (
        name: String,
        captain: Long,
        inner_type: Shoots.Type?,
        coach: String
    ) -> T): Query<T> = TeamForCoach(coach) { cursor ->
        mapper(
            cursor.getString(0)!!,
            cursor.getLong(1)!!,
            cursor.getString(2)?.let(database.teamAdapter.inner_typeAdapter::decode),
            cursor.getString(3)!!
        )
    }

    override fun teamForCoach(coach: String): Query<Team> = teamForCoach(coach, Team::Impl)

    override fun <T : Any> forInnerType(inner_type: Shoots.Type?, mapper: (
        name: String,
        captain: Long,
        inner_type: Shoots.Type?,
        coach: String
    ) -> T): Query<T> = ForInnerType(inner_type) { cursor ->
        mapper(
            cursor.getString(0)!!,
            cursor.getLong(1)!!,
            cursor.getString(2)?.let(database.teamAdapter.inner_typeAdapter::decode),
            cursor.getString(3)!!
        )
    }

    override fun forInnerType(inner_type: Shoots.Type?): Query<Team> = forInnerType(inner_type,
            Team::Impl)

    private inner class TeamForCoach<out T : Any>(private val coach: String, mapper: (SqlCursor) ->
            T) : Query<T>(teamForCoach, mapper) {
        override fun execute(): SqlCursor = driver.executeQuery(99, """
        |SELECT *
        |FROM team
        |WHERE coach = ?1
        """.trimMargin(), 1) {
            bindString(1, coach)
        }

        override fun toString(): String = "Team.sq:teamForCoach"
    }

    private inner class ForInnerType<out T : Any>(private val inner_type: Shoots.Type?,
            mapper: (SqlCursor) -> T) : Query<T>(forInnerType, mapper) {
        override fun execute(): SqlCursor = driver.executeQuery(null, """
        |SELECT *
        |FROM team
        |WHERE inner_type ${ if (inner_type == null) "IS" else "=" } ?1
        """.trimMargin(), 1) {
            bindString(1, if (inner_type == null) null else
                    database.teamAdapter.inner_typeAdapter.encode(inner_type))
        }

        override fun toString(): String = "Team.sq:forInnerType"
    }
}

private class PlayerQueriesImpl(private val database: TestDatabaseImpl, private val driver:
        SqlDriver) : TransacterImpl(driver), PlayerQueries {
    internal val allPlayers: MutableList<Query<*>> = copyOnWriteList()

    internal val playersForTeam: MutableList<Query<*>> = copyOnWriteList()

    internal val playersForNumbers: MutableList<Query<*>> = copyOnWriteList()

    internal val selectNull: MutableList<Query<*>> = copyOnWriteList()

    override fun <T : Any> allPlayers(mapper: (
        name: String,
        number: Long,
        team: String?,
        shoots: Shoots
    ) -> T): Query<T> = Query(101, allPlayers, driver, "Player.sq", "allPlayers", """
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

    override fun allPlayers(): Query<Player> = allPlayers(Player::Impl)

    override fun <T : Any> playersForTeam(team: String?, mapper: (
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

    override fun playersForTeam(team: String?): Query<Player> = playersForTeam(team, Player::Impl)

    override fun <T : Any> playersForNumbers(number: Collection<Long>, mapper: (
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

    override fun playersForNumbers(number: Collection<Long>): Query<Player> =
            playersForNumbers(number, Player::Impl)

    override fun <T : Any> selectNull(mapper: (expr: Void?) -> T): Query<T> = Query(104, selectNull,
            driver, "Player.sq", "selectNull", "SELECT NULL") { cursor ->
        mapper(
            null
        )
    }

    override fun selectNull(): Query<SelectNull> = selectNull(SelectNull::Impl)

    override fun insertPlayer(
        name: String,
        number: Long,
        team: String?,
        shoots: Shoots
    ) {
        driver.execute(105, """
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

    override fun updateTeamForNumbers(team: String?, number: Collection<Long>) {
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

    override fun foreignKeysOn() {
        driver.execute(107, """PRAGMA foreign_keys = 1""", 0)
    }

    override fun foreignKeysOff() {
        driver.execute(108, """PRAGMA foreign_keys = 0""", 0)
    }

    private inner class PlayersForTeam<out T : Any>(private val team: String?,
            mapper: (SqlCursor) -> T) : Query<T>(playersForTeam, mapper) {
        override fun execute(): SqlCursor = driver.executeQuery(null, """
        |SELECT *
        |FROM player
        |WHERE team ${ if (team == null) "IS" else "=" } ?1
        """.trimMargin(), 1) {
            bindString(1, team)
        }

        override fun toString(): String = "Player.sq:playersForTeam"
    }

    private inner class PlayersForNumbers<out T : Any>(private val number: Collection<Long>,
            mapper: (SqlCursor) -> T) : Query<T>(playersForNumbers, mapper) {
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

        override fun toString(): String = "Player.sq:playersForNumbers"
    }
}

private class GroupQueriesImpl(private val database: TestDatabaseImpl, private val driver:
        SqlDriver) : TransacterImpl(driver), GroupQueries {
    internal val selectAll: MutableList<Query<*>> = copyOnWriteList()

    override fun selectAll(): Query<Long> = Query(109, selectAll, driver, "Group.sq", "selectAll",
            "SELECT `index` FROM `group`") { cursor ->
        cursor.getLong(0)!!
    }
}
