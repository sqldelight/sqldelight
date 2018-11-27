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
      |import com.squareup.sqldelight.internal.QueryList
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.collections.List
      |
      |class DataQueries(private val queryWrapper: QueryWrapper, private val database: SqlDatabase) : Transacter(database) {
      |    internal val selectForId: QueryList = QueryList()
      |
      |    private val insertData: InsertData = InsertData()
      |
      |    fun <T : Any> selectForId(id: Long, mapper: (id: Long, value: List?) -> T): Query<T> = SelectForId(id) { cursor ->
      |        mapper(
      |            cursor.getLong(0)!!,
      |            cursor.getString(1)?.let(queryWrapper.dataAdapter.valueAdapter::decode)
      |        )
      |    }
      |
      |    fun selectForId(id: Long): Query<Data> = selectForId(id, Data::Impl)
      |
      |    fun insertData(id: Long?, value: List?) {
      |        insertData.execute(id, value)
      |    }
      |
      |    private inner class SelectForId<out T : Any>(private val id: Long, mapper: (SqlCursor) -> T) : Query<T>(selectForId, mapper) {
      |        override fun createStatement(): SqlPreparedStatement {
      |            val statement = database.getConnection().prepareStatement(""${'"'}
      |                    |SELECT *
      |                    |FROM data
      |                    |WHERE id = ?1
      |                    ""${'"'}.trimMargin(), SqlPreparedStatement.Type.SELECT, 1)
      |            statement.bindLong(1, id)
      |            return statement
      |        }
      |    }
      |
      |    private inner class InsertData {
      |        private val statement: SqlPreparedStatement by lazy {
      |                database.getConnection().prepareStatement(""${'"'}
      |                |INSERT INTO data
      |                |VALUES (?, ?)
      |                ""${'"'}.trimMargin(), SqlPreparedStatement.Type.INSERT, 2)
      |                }
      |
      |        fun execute(id: Long?, value: List?) {
      |            statement.bindLong(1, id)
      |            statement.bindString(2, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |            statement.execute()
      |            notifyQueries(queryWrapper.dataQueries.selectForId)
      |        }
      |    }
      |}
      |""".trimMargin())
  }
}

