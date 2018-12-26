package com.example

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.core.integration.Shoots
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlPreparedStatement
import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.collections.MutableList

class TeamQueries(private val queryWrapper: QueryWrapper, private val database: SqlDatabase) :
        Transacter(database) {
    internal val teamForCoach: MutableList<Query<*>> =
            com.squareup.sqldelight.internal.copyOnWriteList()

    internal val forInnerType: MutableList<Query<*>> =
            com.squareup.sqldelight.internal.copyOnWriteList()

    fun <T : Any> teamForCoach(coach: String, mapper: (
        name: String,
        captain: Long,
        inner_type: Shoots.Type?,
        coach: String
    ) -> T): Query<T> = TeamForCoach(coach) { cursor ->
        mapper(
            cursor.getString(0)!!,
            cursor.getLong(1)!!,
            cursor.getString(2)?.let(queryWrapper.teamAdapter.inner_typeAdapter::decode),
            cursor.getString(3)!!
        )
    }

    fun teamForCoach(coach: String): Query<Team> = teamForCoach(coach, Team::Impl)

    fun <T : Any> forInnerType(inner_type: Shoots.Type?, mapper: (
        name: String,
        captain: Long,
        inner_type: Shoots.Type?,
        coach: String
    ) -> T): Query<T> = ForInnerType(inner_type) { cursor ->
        mapper(
            cursor.getString(0)!!,
            cursor.getLong(1)!!,
            cursor.getString(2)?.let(queryWrapper.teamAdapter.inner_typeAdapter::decode),
            cursor.getString(3)!!
        )
    }

    fun forInnerType(inner_type: Shoots.Type?): Query<Team> = forInnerType(inner_type, Team::Impl)

    private inner class TeamForCoach<out T : Any>(private val coach: String, mapper: (SqlCursor) ->
            T) : Query<T>(teamForCoach, mapper) {
        override fun createStatement(): SqlPreparedStatement {
            val statement = database.getConnection().prepareStatement(64, """
                    |SELECT *
                    |FROM team
                    |WHERE coach = ?1
                    """.trimMargin(), SqlPreparedStatement.Type.SELECT, 1)
            statement.bindString(1, coach)
            return statement
        }
    }

    private inner class ForInnerType<out T : Any>(private val inner_type: Shoots.Type?, mapper:
            (SqlCursor) -> T) : Query<T>(forInnerType, mapper) {
        override fun createStatement(): SqlPreparedStatement {
            val statement = database.getConnection().prepareStatement(65, """
                    |SELECT *
                    |FROM team
                    |WHERE inner_type ${ if (inner_type == null) "IS" else "=" } ?1
                    """.trimMargin(), SqlPreparedStatement.Type.SELECT, 1)
            statement.bindString(1, if (inner_type == null) null else
                    queryWrapper.teamAdapter.inner_typeAdapter.encode(inner_type))
            return statement
        }
    }
}
