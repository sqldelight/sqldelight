package com.example

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import kotlin.Int

class QueryWrapper(
    database: SqlDatabase,
    internal val teamAdapter: Team.Adapter,
    internal val playerAdapter: Player.Adapter
) : Transacter(database) {
    val teamQueries: TeamQueries = TeamQueries(this, database)

    val playerQueries: PlayerQueries = PlayerQueries(this, database)

    object Schema : SqlDatabase.Schema {
        override val version: Int
            get() = 1

        override fun create(database: SqlDatabase) {
            database.execute(null, """
                    |CREATE TABLE team (
                    |  name TEXT PRIMARY KEY NOT NULL,
                    |  captain INTEGER UNIQUE NOT NULL REFERENCES player(number),
                    |  inner_type TEXT,
                    |  coach TEXT NOT NULL
                    |)
                    """.trimMargin(), 0)
            database.execute(null, """
                    |INSERT INTO team
                    |VALUES ('Anaheim Ducks', 15, NULL, 'Randy Carlyle'),
                    |       ('Ottawa Senators', 65, 'ONE', 'Guy Boucher')
                    """.trimMargin(), 0)
            database.execute(null, """
                    |CREATE TABLE player (
                    |  name TEXT NOT NULL,
                    |  number INTEGER NOT NULL,
                    |  team TEXT REFERENCES team(name),
                    |  shoots TEXT NOT NULL,
                    |  PRIMARY KEY (team, number)
                    |)
                    """.trimMargin(), 0)
            database.execute(null, """
                    |INSERT INTO player
                    |VALUES ('Ryan Getzlaf', 15, 'Anaheim Ducks', 'RIGHT'),
                    |       ('Erik Karlsson', 65, 'Ottawa Senators', 'RIGHT')
                    """.trimMargin(), 0)
        }

        override fun migrate(
            database: SqlDatabase,
            oldVersion: Int,
            newVersion: Int
        ) {
        }
    }
}
