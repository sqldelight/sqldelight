package com.example

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDriver
import kotlin.Int

class TestDatabase(
    driver: SqlDriver,
    internal val playerAdapter: Player.Adapter,
    internal val teamAdapter: Team.Adapter
) : Transacter(driver) {
    val playerQueries: PlayerQueries = PlayerQueries(this, driver)

    val teamQueries: TeamQueries = TeamQueries(this, driver)

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
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Int,
            newVersion: Int
        ) {
        }
    }
}
