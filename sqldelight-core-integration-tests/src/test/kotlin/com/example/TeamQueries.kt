package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlResultSet
import java.lang.ThreadLocal
import kotlin.Boolean
import kotlin.Long
import kotlin.String
import kotlin.collections.MutableList

class TeamQueries(
        private val queryWrapper: QueryWrapper,
        private val database: SqlDatabase,
        transactions: ThreadLocal<Transacter.Transaction>
) : Transacter(database, transactions) {
    internal val teamForCoach: MutableList<Query<*>> = mutableListOf()

    fun <T> teamForCoach(coach: String, mapper: (
            name: String,
            captain: Long,
            coach: String
    ) -> T): Query<T> {
        val statement = database.getConnection().prepareStatement("""
                |SELECT *
                |FROM team
                |WHERE coach = ?
                """.trimMargin())
        statement.bindString(1, coach)
        return TeamForCoach(coach, statement) { resultSet ->
            mapper(
                resultSet.getString(0)!!,
                resultSet.getLong(1)!!,
                resultSet.getString(2)!!
            )
        }
    }

    fun teamForCoach(coach: String): Query<Team> = teamForCoach(coach, Team::Impl)
    private inner class TeamForCoach<out T>(
            private val coach: String,
            statement: SqlPreparedStatement,
            mapper: (SqlResultSet) -> T
    ) : Query<T>(statement, teamForCoach, mapper) {
        fun dirtied(coach: String): Boolean = true
    }
}
