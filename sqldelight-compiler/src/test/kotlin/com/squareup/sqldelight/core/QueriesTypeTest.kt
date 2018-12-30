package com.squareup.sqldelight.core

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.SqlDelightCompiler
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class QueriesTypeTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test fun `queries file is generated properly via compilation`() {
    val result = FixtureCompiler.compileSql("""
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |insertData:
      |INSERT INTO data
      |VALUES (?, ?);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
    """.trimMargin(), temporaryFolder, SqlDelightCompiler::writeQueriesType, fileName = "Data.sq")

    val select = result.compiledFile.namedQueries.first()
    val insert = result.compiledFile.namedMutators.first()
    assertThat(result.errors).isEmpty()

    val dataQueries = File(result.outputDirectory, "com/example/DataQueries.kt")
    assertThat(result.compilerOutput[dataQueries].toString()).isEqualTo("""
      |package com.example
      |
      |import com.squareup.sqldelight.Query
      |import com.squareup.sqldelight.Transacter
      |import com.squareup.sqldelight.db.SqlCursor
      |import com.squareup.sqldelight.db.SqlDatabase
      |import com.squareup.sqldelight.db.SqlPreparedStatement
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.collections.List
      |import kotlin.collections.MutableList
      |
      |class DataQueries(private val queryWrapper: QueryWrapper, private val database: SqlDatabase) :
      |        Transacter(database) {
      |    internal val selectForId: MutableList<Query<*>> =
      |            com.squareup.sqldelight.internal.copyOnWriteList()
      |
      |    fun <T : Any> selectForId(id: Long, mapper: (id: Long, value: List?) -> T): Query<T> =
      |            SelectForId(id) { cursor ->
      |        mapper(
      |            cursor.getLong(0)!!,
      |            cursor.getString(1)?.let(queryWrapper.dataAdapter.valueAdapter::decode)
      |        )
      |    }
      |
      |    fun selectForId(id: Long): Query<Data> = selectForId(id, Data::Impl)
      |
      |    fun insertData(id: Long?, value: List?) {
      |        val statement = database.prepareStatement(${insert.id}, ""${'"'}
      |                |INSERT INTO data
      |                |VALUES (?1, ?2)
      |                ""${'"'}.trimMargin(), SqlPreparedStatement.Type.INSERT, 2)
      |        statement.bindLong(1, id)
      |        statement.bindString(2, if (value == null) null else
      |                queryWrapper.dataAdapter.valueAdapter.encode(value))
      |        statement.execute()
      |        notifyQueries(queryWrapper.dataQueries.selectForId)
      |    }
      |
      |    private inner class SelectForId<out T : Any>(private val id: Long, mapper: (SqlCursor) -> T) :
      |            Query<T>(selectForId, mapper) {
      |        override fun createStatement(): SqlPreparedStatement {
      |            val statement = database.prepareStatement(${select.id}, ""${'"'}
      |                    |SELECT *
      |                    |FROM data
      |                    |WHERE id = ?1
      |                    ""${'"'}.trimMargin(), SqlPreparedStatement.Type.SELECT, 1)
      |            statement.bindLong(1, id)
      |            return statement
      |        }
      |    }
      |}
      |""".trimMargin())
  }
}

