package com.squareup.sqldelight.core.integration

import com.example.Group
import com.example.Player
import com.example.Team
import com.example.TestDatabase
import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.core.integration.Shoots.LEFT
import com.squareup.sqldelight.core.integration.Shoots.RIGHT
import com.squareup.sqldelight.core.integration.Shoots.Type.ONE
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.squareup.sqldelight.test.util.FixtureCompiler
import com.squareup.sqldelight.test.util.fixtureRoot
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class IntegrationTest {
  private lateinit var driver: SqlDriver
  private lateinit var queryWrapper: TestDatabase

  private val playerAdapter = Player.Adapter(EnumColumnAdapter())
  private val teamAdapter = Team.Adapter(EnumColumnAdapter())

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
        |VALUES ('Ryan Getzlaf', 15, 'Anaheim Ducks', 'RIGHT'),
        |       ('Erik Karlsson', 65, 'Ottawa Senators', 'RIGHT');
        |
        |insertPlayer:
        |INSERT INTO player
        |VALUES (?, ?, ?, ?);
        |
        |foreignKeysOn:
        |PRAGMA foreign_keys = 1;
        |
        |foreignKeysOff:
        |PRAGMA foreign_keys = 0;
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
        |
        |selectNull:
        |SELECT NULL;
        |""".trimMargin(), temporaryFolder, "Player.sq")

    FixtureCompiler.writeSql("""
        |import com.squareup.sqldelight.core.integration.Shoots;
        |
        |CREATE TABLE team (
        |  name TEXT PRIMARY KEY NOT NULL,
        |  captain INTEGER UNIQUE NOT NULL REFERENCES player(number),
        |  inner_type TEXT AS Shoots.Type,
        |  coach TEXT NOT NULL
        |);
        |
        |INSERT INTO team
        |VALUES ('Anaheim Ducks', 15, NULL, 'Randy Carlyle'),
        |       ('Ottawa Senators', 65, 'ONE', 'Guy Boucher');
        |
        |teamForCoach:
        |SELECT *
        |FROM team
        |WHERE coach = ?;
        |
        |forInnerType:
        |SELECT *
        |FROM team
        |WHERE inner_type = ?;
        |""".trimMargin(), temporaryFolder, "Team.sq")

    FixtureCompiler.writeSql("""
        |CREATE TABLE `group` (`index` INTEGER PRIMARY KEY NOT NULL);
        |
        |INSERT INTO `group` VALUES (1), (2), (3);
        |
        |selectAll:
        |SELECT `index` FROM `group`;
        |""".trimMargin(), temporaryFolder, "Group.sq")

    val fileWriter: (String) -> Appendable = { fileName ->
      val file = File(fileName)
      file.parentFile.mkdirs()
      file.apply { createNewFile() }.printWriter()
    }

    val result = FixtureCompiler.compileFixture(
        fixtureRoot = temporaryFolder.fixtureRoot().path,
        writer = fileWriter,
        outputDirectory = File("src/test/kotlin")
    )

    temporaryFolder.delete()

    assertThat(result.errors).isEmpty()
  }

  @Before fun setupDb() {
    driver = JdbcSqliteDriver()
    queryWrapper = TestDatabase(driver, playerAdapter, teamAdapter)
    TestDatabase.Schema.create(driver)
  }

  @After fun closeDb() {
    driver.close()
  }

  @Test fun `escaped named are handled correctly`() {
    val allGroups = queryWrapper.groupQueries.selectAll().executeAsList()
    assertThat(allGroups).containsExactly(1L, 2L, 3L)
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
        Player.Impl("Ryan Getzlaf", 15, "Anaheim Ducks", RIGHT),
        Player.Impl("Erik Karlsson", 65, "Ottawa Senators", RIGHT)
    )

    queryWrapper.playerQueries.insertPlayer("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT)

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(allPlayers.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 15, "Anaheim Ducks", RIGHT),
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
        Team.Impl("Anaheim Ducks", 15, null, "Randy Carlyle")
    )

    queryWrapper.playerQueries.insertPlayer("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT)

    assertThat(resultSetChanged.get()).isEqualTo(0)
  }

  @Test fun `playersForNumbers triggers from inserts`() {
    val resultSetChanged = AtomicInteger(0)

    val playersForNumbers = queryWrapper.playerQueries.playersForNumbers(listOf(15, 87))
    playersForNumbers.addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        resultSetChanged.incrementAndGet()
      }
    })

    assertThat(playersForNumbers.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 15, "Anaheim Ducks", RIGHT)
    )

    queryWrapper.playerQueries.insertPlayer("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT)

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(playersForNumbers.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 15, "Anaheim Ducks", RIGHT),
        Player.Impl("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT)
    )
  }

  @Test fun `multiple inserts in a transaction only notifies once`() {
    val resultSetChanged = AtomicInteger(0)

    val playersForNumbers = queryWrapper.playerQueries.playersForNumbers(listOf(10, 87, 15))
    playersForNumbers.addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        resultSetChanged.incrementAndGet()
      }
    })

    assertThat(playersForNumbers.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 15, "Anaheim Ducks", RIGHT)
    )

    queryWrapper.playerQueries.transaction {
      queryWrapper.playerQueries.insertPlayer("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT)
      queryWrapper.playerQueries.insertPlayer("Corey Perry", 10, "Anaheim Ducks", RIGHT)
    }

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(playersForNumbers.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 15, "Anaheim Ducks", RIGHT),
        Player.Impl("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT),
        Player.Impl("Corey Perry", 10, "Anaheim Ducks", RIGHT)
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
        Player.Impl("Ryan Getzlaf", 15, "Anaheim Ducks", RIGHT)
    )

    queryWrapper.playerQueries.transaction {
      queryWrapper.playerQueries.updateTeamForNumbers("Anaheim Ducks", listOf(65, 87))
    }

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(playersForTeam.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 15, "Anaheim Ducks", RIGHT),
        Player.Impl("Erik Karlsson", 65, "Anaheim Ducks", RIGHT)
    )
  }

  @Test fun `multiple mutators in a transaction notify queries listening to both tables once`() {
    val resultSetChanged = AtomicInteger(0)

    val playersForTeam = queryWrapper.playerQueries.playersForTeam("Anaheim Ducks")
    playersForTeam.addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        resultSetChanged.incrementAndGet()
      }
    })

    assertThat(playersForTeam.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 15, "Anaheim Ducks", RIGHT)
    )

    queryWrapper.playerQueries.transaction {
      queryWrapper.playerQueries.insertPlayer("Sidney Crosby", 87, "Pittsburgh Penguins", LEFT)
      queryWrapper.playerQueries.updateTeamForNumbers("Anaheim Ducks", listOf(65, 87))
    }

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(playersForTeam.executeAsList()).containsExactly(
        Player.Impl("Ryan Getzlaf", 15, "Anaheim Ducks", RIGHT),
        Player.Impl("Erik Karlsson", 65, "Anaheim Ducks", RIGHT),
        Player.Impl("Sidney Crosby", 87, "Anaheim Ducks", LEFT)
    )
  }

  @Test fun `bind no arguments to a collection parameter`() {
    assertThat(queryWrapper.playerQueries.playersForNumbers(emptyList()).executeAsList()).isEmpty()
  }

  @Test fun `selecting just null behaves correctly`() {
    assertThat(queryWrapper.playerQueries.selectNull().executeAsOne().expr).isNull()
  }

  @Test fun `inner type query`() {
    assertThat(queryWrapper.teamQueries.forInnerType(ONE).executeAsList()).containsExactly(
        Team.Impl("Ottawa Senators", 65, ONE, "Guy Boucher")
    )
  }
}
