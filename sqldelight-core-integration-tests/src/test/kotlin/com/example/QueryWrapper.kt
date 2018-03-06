package com.example

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import java.lang.ThreadLocal

class QueryWrapper(database: SqlDatabase, internal val playerAdapter: Player.Adapter) {
    private val transactions: ThreadLocal<Transacter.Transaction> =
            ThreadLocal<Transacter.Transaction>()

    val teamQueries: TeamQueries = TeamQueries(this, database, transactions)

    val playerQueries: PlayerQueries = PlayerQueries(this, database, transactions)
    companion object {
        fun onCreate(db: SqlDatabaseConnection) {
            db.prepareStatement("""
                    |CREATE TABLE team (
                    |  name TEXT PRIMARY KEY NOT NULL,
                    |  captain INTEGER UNIQUE NOT NULL REFERENCES player(number),
                    |  coach TEXT NOT NULL
                    |)
                    """.trimMargin()).execute()
            db.prepareStatement("""
                    |INSERT INTO team
                    |VALUES ('Anaheim Ducks', 10, 'Randy Carlyle'),
                    |       ('Ottawa Senators', 65, 'Guy Boucher')
                    """.trimMargin()).execute()
            db.prepareStatement("""
                    |CREATE TABLE player (
                    |  name TEXT NOT NULL,
                    |  number INTEGER NOT NULL,
                    |  team TEXT REFERENCES team(name),
                    |  shoots TEXT NOT NULL,
                    |  PRIMARY KEY (team, number)
                    |)
                    """.trimMargin()).execute()
            db.prepareStatement("""
                    |INSERT INTO player
                    |VALUES ('Ryan Getzlaf', 10, 'Anaheim Ducks', 'RIGHT'),
                    |       ('Erik Karlsson', 65, 'Ottawa Senators', 'RIGHT')
                    """.trimMargin()).execute()
        }
    }
}
