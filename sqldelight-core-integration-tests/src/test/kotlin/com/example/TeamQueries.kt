package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.core.integration.Shoots
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlResultSet
import com.squareup.sqldelight.internal.QueryList
import kotlin.Any
import kotlin.Boolean
import kotlin.Long
import kotlin.String

class TeamQueries(private val queryWrapper: QueryWrapper, private val database: SqlDatabase) : Transacter(database) {
    internal val teamForCoach: QueryList = QueryList()

    internal val forInnerType: QueryList = QueryList()

    fun <T : Any> teamForCoach(coach: String, mapper: (
            name: String,
            captain: Long,
            inner_type: Shoots.Type?,
            coach: String
    ) -> T): Query<T> {
        val statement = database.getConnection().prepareStatement("""
                |SELECT *
                |FROM team
                |WHERE coach = ?1
                """.trimMargin(), SqlPreparedStatement.Type.SELECT)
        statement.bindString(1, coach)
        return TeamForCoach(coach, statement) { resultSet ->
            mapper(
                resultSet.getString(0)!!,
                resultSet.getLong(1)!!,
                resultSet.getString(2)?.let(queryWrapper.teamAdapter.inner_typeAdapter::decode),
                resultSet.getString(3)!!
            )
        }
    }

    fun teamForCoach(coach: String): Query<Team> = teamForCoach(coach, Team::Impl)

    fun <T : Any> forInnerType(inner_type: Shoots.Type?, mapper: (
            name: String,
            captain: Long,
            inner_type: Shoots.Type?,
            coach: String
    ) -> T): Query<T> {
        val statement = database.getConnection().prepareStatement("""
                |SELECT *
                |FROM team
                |WHERE inner_type = ?1
                """.trimMargin(), SqlPreparedStatement.Type.SELECT)
        statement.bindString(1, if (inner_type == null) null else queryWrapper.teamAdapter.inner_typeAdapter.encode(inner_type))
        return ForInnerType(inner_type, statement) { resultSet ->
            mapper(
                resultSet.getString(0)!!,
                resultSet.getLong(1)!!,
                resultSet.getString(2)?.let(queryWrapper.teamAdapter.inner_typeAdapter::decode),
                resultSet.getString(3)!!
            )
        }
    }

    fun forInnerType(inner_type: Shoots.Type?): Query<Team> = forInnerType(inner_type, Team::Impl)

    private inner class TeamForCoach<out T : Any>(
            private val coach: String,
            statement: SqlPreparedStatement,
            mapper: (SqlResultSet) -> T
    ) : Query<T>(statement, teamForCoach, mapper) {
        fun dirtied(coach: String): Boolean = true
    }

    private inner class ForInnerType<out T : Any>(
            private val inner_type: Shoots.Type?,
            statement: SqlPreparedStatement,
            mapper: (SqlResultSet) -> T
    ) : Query<T>(statement, forInnerType, mapper) {
        fun dirtied(inner_type: Shoots.Type?): Boolean = true
    }
}
