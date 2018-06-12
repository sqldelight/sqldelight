package com.example

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import kotlin.Int

class QueryWrapper(
        database: SqlDatabase,
        internal val teamAdapter: Team.Adapter,
        internal val playerAdapter: Player.Adapter
) {
    val teamQueries: TeamQueries = TeamQueries(this, database)

    val playerQueries: PlayerQueries = PlayerQueries(this, database)
    companion object Helper : SqlDatabase.Helper {
        override val version: Int
            get() = 1

        override fun onCreate(db: SqlDatabaseConnection) {
            db.prepareStatement("""
                    |CREATE TABLE team (
                    |  name TEXT PRIMARY KEY NOT NULL,
                    |  captain INTEGER UNIQUE NOT NULL REFERENCES player(number),
                    |  inner_type TEXT,
                    |  coach TEXT NOT NULL
                    |)
                    """.trimMargin(), SqlPreparedStatement.Type.EXEC).execute()
            db.prepareStatement("""
                    |INSERT INTO team
                    |VALUES ('Anaheim Ducks', 15, NULL, 'Randy Carlyle'),
                    |       ('Ottawa Senators', 65, 'ONE', 'Guy Boucher')
                    """.trimMargin(), SqlPreparedStatement.Type.EXEC).execute()
            db.prepareStatement("""
                    |CREATE TABLE player (
                    |  name TEXT NOT NULL,
                    |  number INTEGER NOT NULL,
                    |  team TEXT REFERENCES team(name),
                    |  shoots TEXT NOT NULL,
                    |  PRIMARY KEY (team, number)
                    |)
                    """.trimMargin(), SqlPreparedStatement.Type.EXEC).execute()
            db.prepareStatement("""
                    |INSERT INTO player
                    |VALUES ('Ryan Getzlaf', 15, 'Anaheim Ducks', 'RIGHT'),
                    |       ('Erik Karlsson', 65, 'Ottawa Senators', 'RIGHT')
                    """.trimMargin(), SqlPreparedStatement.Type.EXEC).execute()
        }

        override fun onMigrate(
                db: SqlDatabaseConnection,
                oldVersion: Int,
                newVersion: Int
        ) {
        }
    }
}
