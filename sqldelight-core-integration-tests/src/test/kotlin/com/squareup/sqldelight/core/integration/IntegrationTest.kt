package com.squareup.sqldelight.core.integration

import com.example.Player
import com.example.QueryWrapper
import com.example.Team
import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.core.integration.Shoots.LEFT
import com.squareup.sqldelight.core.integration.Shoots.RIGHT
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.sqlite.driver.SqliteJdbcOpenHelper
import com.squareup.sqldelight.test.util.FixtureCompiler
import com.squareup.sqldelight.test.util.fixtureRoot
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class IntegrationTest {
  private lateinit var database: SqlDatabase
  private lateinit var queryWrapper: QueryWrapper

  private val playerAdapter: Player.Adapter = Player.Adapter(EnumColumnAdapter.create(Shoots.values()))

  init {
    val temporaryFolder = TemporaryFolder()
    temporaryFolder.create()

    FixtureCompiler.writeSql("""
        |CREATE TABLE player (
        |  name TEXT NOT NULL,
        |  number INTEGER NOT NULL,
        |  team TEXT REFERENCES team(name),
        |  shoots TEXT AS com.squareup.sqldelight.core.integration.Shoots NOT NULL,
        |  PRIMARY KEY (team, number)
        |);
        |
        |INSERT INTO player
        |VALUES ('Ryan Getzlaf', 10, 'Anaheim Ducks', 'RIGHT'),
        |       ('Erik Karlsson', 65, 'Ottawa Senators', 'RIGHT');
        |
        |insertPlayer:
        |INSERT INTO player
        |VALUES (?, ?, ?, ?);
        |
        |allPlayers:
        |SELECT *
        |FROM player;
        |
        |playersForTeam:
        |SELECT *
        |FROM player
        |WHERE team = ?;
        |
        |playersForNumbers:
        |SELECT *
        |FROM player
        |WHERE number IN ?;
        |
        |updateTeamForNumbers:
        |UPDATE player
        |SET team = ?
        |WHERE number IN ?;
        |""".trimMargin(), temporaryFolder, "Player.sq")

    FixtureCompiler.writeSql("""
        |CREATE TABLE team (
        |  name TEXT PRIMARY KEY NOT NULL,
        |  captain INTEGER UNIQUE NOT NULL REFERENCES player(number),
        |  coach TEXT NOT NULL
        |);
        |
        |INSERT INTO team
        |VALUES ('Anaheim Ducks', 10, 'Randy Carlyle'),
        |       ('Ottawa Senators', 65, 'Guy Boucher');
        |
        |teamForCoach:
        |SELECT *
        |FROM team
        |WHERE coach = ?;
        |""".trimMargin(), temporaryFolder, "Team.sq")

    val fileWriter: (String) -> Appendable = { fileName ->
      val file = File("src/test/kotlin", fileName)
      file.parentFile.mkdirs()
      file.apply { createNewFile() }.printWriter()
    }

    val result = FixtureCompiler.compileFixture(
        fixtureRoot = temporaryFolder.fixtureRoot().path,
        writer = fileWriter
    )

    temporaryFolder.delete()

    assertThat(result.errors).isEmpty()
  }

  @Before fun setupDb() {
    database = SqliteJdbcOpenHelper()
    queryWrapper = QueryWrapper(database, playerAdapter)
    QueryWrapper.onCreate(database.getConnection())
  }

  @After fun closeDb() {
    database.close()
  }

  @Test fun `allPlayers properly triggers from inserts`() {
    val resultSetChanged = AtomicInteger(0)

    val allPlayers = queryWrapper.playerQueries.allPlayers()
    allPlayers.addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        resultSetChanged.incrementAndGet()
      }
    })

    assertThat(allPlayers.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 10, "Anaheim Ducks", RIGHT),
        Player.Impl("Erik Karlsson", 65, "Ottawa Senators", RIGHT)
    )

    queryWrapper.playerQueries.insertPlayer("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT)

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(allPlayers.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 10, "Anaheim Ducks", RIGHT),
        Player.Impl("Erik Karlsson", 65, "Ottawa Senators", RIGHT),
        Player.Impl("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT)
    )
  }

  @Test fun `teamForCoach doesnt trigger from an insert to players`() {
    val resultSetChanged = AtomicInteger(0)

    val teamForCoach = queryWrapper.teamQueries.teamForCoach("Randy Carlyle")
    teamForCoach.addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        resultSetChanged.incrementAndGet()
      }
    })

    assertThat(teamForCoach.executeAsList()).containsExactly(
        Team.Impl("Anaheim Ducks", 10, "Randy Carlyle")
    )

    queryWrapper.playerQueries.insertPlayer("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT)

    assertThat(resultSetChanged.get()).isEqualTo(0)
  }

  @Test fun `playersForNumbers triggers from inserts`() {
    val resultSetChanged = AtomicInteger(0)

    val playersForNumbers = queryWrapper.playerQueries.playersForNumbers(listOf(10, 87))
    playersForNumbers.addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        resultSetChanged.incrementAndGet()
      }
    })

    assertThat(playersForNumbers.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 10, "Anaheim Ducks", RIGHT)
    )

    queryWrapper.playerQueries.insertPlayer("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT)

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(playersForNumbers.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 10, "Anaheim Ducks", RIGHT),
        Player.Impl("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT)
    )
  }

  @Test fun `updateTeamForNumbers properly updates and triggers`() {
     val resultSetChanged = AtomicInteger(0)

    val playersForTeam = queryWrapper.playerQueries.playersForTeam("Anaheim Ducks")
    playersForTeam.addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        resultSetChanged.incrementAndGet()
      }
    })

    assertThat(playersForTeam.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 10, "Anaheim Ducks", RIGHT)
    )

    queryWrapper.playerQueries.transaction {
      queryWrapper.playerQueries.insertPlayer("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT)
      queryWrapper.playerQueries.updateTeamForNumbers("Anaheim Ducks", listOf(65, 87))
    }

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(playersForTeam.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 10, "Anaheim Ducks", RIGHT),
        Player.Impl("Erik Karlsson", 65, "Anaheim Ducks", RIGHT),
        Player.Impl("Sidney Crosby", 87, "Anaheim Ducks", LEFT)
    )
  }
}