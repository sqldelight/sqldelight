package app.cash.sqldelight.core.integration

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.core.integration.Shoots.LEFT
import app.cash.sqldelight.core.integration.Shoots.RIGHT
import app.cash.sqldelight.core.integration.Shoots.Type.ONE
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.Companion.IN_MEMORY
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.fixtureRoot
import com.example.Player
import com.example.Team
import com.example.TeamForCoach
import com.example.TestDatabase
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IntegrationTest {
  private lateinit var driver: SqlDriver
  private lateinit var queryWrapper: TestDatabase

  private val playerAdapter = Player.Adapter(EnumColumnAdapter())
  private val teamAdapter = Team.Adapter(EnumColumnAdapter())

  init {
    val temporaryFolder = TemporaryFolder()
    temporaryFolder.create()

    FixtureCompiler.writeSql(
      """
        |CREATE TABLE player (
        |  name TEXT AS VALUE NOT NULL,
        |  number INTEGER NOT NULL,
        |  team TEXT REFERENCES team(name),
        |  shoots TEXT AS app.cash.sqldelight.core.integration.Shoots NOT NULL,
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
        |
        |selectStuff:
        |SELECT 1, 2;
        |
        |insertAndReturn {
        |  INSERT INTO player
        |  VALUES (?, ?, ?, ?);
        |
        |  SELECT *
        |  FROM player
        |  WHERE player.rowid = last_insert_rowid();
        |}
        |
        |greaterThanNumberAndName:
        |SELECT *
        |FROM player
        |WHERE (number, name) > (?, ?);
        |
      """.trimMargin(),
      temporaryFolder,
      "Player.sq",
    )

    FixtureCompiler.writeSql(
      """
        |import app.cash.sqldelight.core.integration.Shoots;
        |
        |CREATE TABLE team (
        |  name TEXT AS VALUE PRIMARY KEY NOT NULL,
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
        |SELECT name, captain
        |FROM team
        |WHERE coach = ?;
        |
        |forInnerType:
        |SELECT *
        |FROM team
        |WHERE inner_type = ?;
        |
        |selectStuff:
        |SELECT 1, 2;
        |
      """.trimMargin(),
      temporaryFolder,
      "Team.sq",
    )

    FixtureCompiler.writeSql(
      """
        |CREATE TABLE `group` (`index` INTEGER PRIMARY KEY NOT NULL);
        |
        |INSERT INTO `group` VALUES (1), (2), (3);
        |
        |selectAll:
        |SELECT `index` FROM `group`;
        |
        |CREATE VIRTUAL TABLE myftstable USING fts5(something, nice);
        |
        |CREATE VIRTUAL TABLE myftstable2 USING fts5(something TEXT, nice TEXT);
        |
        |selectFromTable2:
        |SELECT *
        |FROM myftstable2;
        |
      """.trimMargin(),
      temporaryFolder,
      "Group.sq",
    )

    val fileWriter: (String) -> Appendable = { fileName ->
      val file = File(fileName)
      file.parentFile.mkdirs()
      file.apply { createNewFile() }.printWriter()
    }

    val result = FixtureCompiler.compileFixture(
      fixtureRoot = temporaryFolder.fixtureRoot().path,
      writer = fileWriter,
      outputDirectory = File("src/test/kotlin"),
    )

    temporaryFolder.delete()

    assertThat(result.errors).isEmpty()
  }

  @Before fun setupDb() {
    driver = JdbcSqliteDriver(IN_MEMORY)
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
    allPlayers.addListener { resultSetChanged.incrementAndGet() }

    assertThat(allPlayers.executeAsList()).containsExactly(
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
      Player(Player.Name("Erik Karlsson"), 65, Team.Name("Ottawa Senators"), RIGHT),
    )

    queryWrapper.playerQueries.insertPlayer(Player.Name("Sidney Crosby"), 87, Team.Name("Pittsburgh Penguins"), LEFT)

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(allPlayers.executeAsList()).containsExactly(
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
      Player(Player.Name("Erik Karlsson"), 65, Team.Name("Ottawa Senators"), RIGHT),
      Player(Player.Name("Sidney Crosby"), 87, Team.Name("Pittsburgh Penguins"), LEFT),
    )
  }

  @Test fun `teamForCoach doesnt trigger from an insert to players`() {
    val resultSetChanged = AtomicInteger(0)

    val teamForCoach = queryWrapper.teamQueries.teamForCoach("Randy Carlyle")
    teamForCoach.addListener { resultSetChanged.incrementAndGet() }

    assertThat(teamForCoach.executeAsList()).containsExactly(
      TeamForCoach(Team.Name("Anaheim Ducks"), 15),
    )

    queryWrapper.playerQueries.insertPlayer(Player.Name("Sidney Crosby"), 87, Team.Name("Pittsburgh Penguins"), LEFT)

    assertThat(resultSetChanged.get()).isEqualTo(0)
  }

  @Test fun `playersForNumbers triggers from inserts`() {
    val resultSetChanged = AtomicInteger(0)

    val playersForNumbers = queryWrapper.playerQueries.playersForNumbers(listOf(15, 87))
    playersForNumbers.addListener { resultSetChanged.incrementAndGet() }

    assertThat(playersForNumbers.executeAsList()).containsExactly(
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
    )

    queryWrapper.playerQueries.insertPlayer(Player.Name("Sidney Crosby"), 87, Team.Name("Pittsburgh Penguins"), LEFT)

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(playersForNumbers.executeAsList()).containsExactly(
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
      Player(Player.Name("Sidney Crosby"), 87, Team.Name("Pittsburgh Penguins"), LEFT),
    )
  }

  @Test fun `multiple inserts in a transaction only notifies once`() {
    val resultSetChanged = AtomicInteger(0)

    val playersForNumbers = queryWrapper.playerQueries.playersForNumbers(listOf(10, 87, 15))
    playersForNumbers.addListener { resultSetChanged.incrementAndGet() }

    assertThat(playersForNumbers.executeAsList()).containsExactly(
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
    )

    queryWrapper.playerQueries.transaction {
      queryWrapper.playerQueries.insertPlayer(Player.Name("Sidney Crosby"), 87, Team.Name("Pittsburgh Penguins"), LEFT)
      queryWrapper.playerQueries.insertPlayer(Player.Name("Corey Perry"), 10, Team.Name("Anaheim Ducks"), RIGHT)
    }

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(playersForNumbers.executeAsList()).containsExactly(
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
      Player(Player.Name("Sidney Crosby"), 87, Team.Name("Pittsburgh Penguins"), LEFT),
      Player(Player.Name("Corey Perry"), 10, Team.Name("Anaheim Ducks"), RIGHT),
    )
  }

  @Test fun `updateTeamForNumbers properly updates and triggers`() {
    val resultSetChanged = AtomicInteger(0)

    val playersForTeam = queryWrapper.playerQueries.playersForTeam(Team.Name("Anaheim Ducks"))
    playersForTeam.addListener { resultSetChanged.incrementAndGet() }

    assertThat(playersForTeam.executeAsList()).containsExactly(
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
    )

    queryWrapper.playerQueries.transaction {
      queryWrapper.playerQueries.updateTeamForNumbers(Team.Name("Anaheim Ducks"), listOf(65, 87))
    }

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(playersForTeam.executeAsList()).containsExactly(
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
      Player(Player.Name("Erik Karlsson"), 65, Team.Name("Anaheim Ducks"), RIGHT),
    )
  }

  @Test fun `multiple mutators in a transaction notify queries listening to both tables once`() {
    val resultSetChanged = AtomicInteger(0)

    val playersForTeam = queryWrapper.playerQueries.playersForTeam(Team.Name("Anaheim Ducks"))
    playersForTeam.addListener { resultSetChanged.incrementAndGet() }

    assertThat(playersForTeam.executeAsList()).containsExactly(
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
    )

    queryWrapper.playerQueries.transaction {
      queryWrapper.playerQueries.insertPlayer(Player.Name("Sidney Crosby"), 87, Team.Name("Pittsburgh Penguins"), LEFT)
      queryWrapper.playerQueries.updateTeamForNumbers(Team.Name("Anaheim Ducks"), listOf(65, 87))
    }

    assertThat(resultSetChanged.get()).isEqualTo(1)

    assertThat(playersForTeam.executeAsList()).containsExactly(
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
      Player(Player.Name("Erik Karlsson"), 65, Team.Name("Anaheim Ducks"), RIGHT),
      Player(Player.Name("Sidney Crosby"), 87, Team.Name("Anaheim Ducks"), LEFT),
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
      Team(Team.Name("Ottawa Senators"), 65, ONE, "Guy Boucher"),
    )
  }

  @Test fun `grouped statement`() {
    with(queryWrapper.playerQueries) {
      val brady = insertAndReturn(Player.Name("Brady Tkachuk"), 7, Team.Name("Ottawa Senators"), LEFT)
        .executeAsOne()

      assertThat(brady.name).isEqualTo(Player.Name("Brady Tkachuk"))
    }
  }

  @Test fun `multi column expression select`() {
    queryWrapper.playerQueries.insertPlayer(Player.Name("Brady Hockey"), 15, Team.Name("Ottawa Hockey Team"), LEFT)

    assertThat(
      queryWrapper.playerQueries.greaterThanNumberAndName(10, Player.Name("A")).executeAsList(),
    ).containsExactly(
      Player(Player.Name("Brady Hockey"), 15, Team.Name("Ottawa Hockey Team"), LEFT),
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
      Player(Player.Name("Erik Karlsson"), 65, Team.Name("Ottawa Senators"), RIGHT),
    )

    assertThat(
      queryWrapper.playerQueries.greaterThanNumberAndName(10, Player.Name("Z")).executeAsList(),
    ).containsExactly(
      Player(Player.Name("Brady Hockey"), 15, Team.Name("Ottawa Hockey Team"), LEFT),
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
      Player(Player.Name("Erik Karlsson"), 65, Team.Name("Ottawa Senators"), RIGHT),
    )

    assertThat(
      queryWrapper.playerQueries.greaterThanNumberAndName(15, Player.Name("A")).executeAsList(),
    ).containsExactly(
      Player(Player.Name("Brady Hockey"), 15, Team.Name("Ottawa Hockey Team"), LEFT),
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
      Player(Player.Name("Erik Karlsson"), 65, Team.Name("Ottawa Senators"), RIGHT),
    )

    assertThat(
      queryWrapper.playerQueries.greaterThanNumberAndName(15, Player.Name("C")).executeAsList(),
    ).containsExactly(
      Player(Player.Name("Ryan Getzlaf"), 15, Team.Name("Anaheim Ducks"), RIGHT),
      Player(Player.Name("Erik Karlsson"), 65, Team.Name("Ottawa Senators"), RIGHT),
    )

    assertThat(
      queryWrapper.playerQueries.greaterThanNumberAndName(15, Player.Name("Z")).executeAsList(),
    ).containsExactly(
      Player(Player.Name("Erik Karlsson"), 65, Team.Name("Ottawa Senators"), RIGHT),
    )
  }
}
