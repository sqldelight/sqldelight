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
      |import com.squareup.sqldelight.db.SqlDatabase
      |import com.squareup.sqldelight.db.SqlPreparedStatement
      |import com.squareup.sqldelight.db.SqlResultSet
      |import kotlin.Any
      |import kotlin.Boolean
      |import kotlin.Long
      |import kotlin.collections.List
      |import kotlin.collections.MutableList
      |
      |class DataQueries(private val queryWrapper: QueryWrapper, private val database: SqlDatabase) : Transacter(database) {
      |    internal val selectForId: MutableList<Query<*>> = mutableListOf()
      |
      |    private val insertData: InsertData by lazy {
      |            InsertData(database.getConnection().prepareStatement(""${'"'}
      |            |INSERT INTO data
      |            |VALUES (?, ?)
      |            ""${'"'}.trimMargin(), SqlPreparedStatement.Type.INSERT))
      |            }
      |
      |    fun <T : Any> selectForId(id: Long, mapper: (id: Long, value: List?) -> T): Query<T> {
      |        val statement = database.getConnection().prepareStatement(""${'"'}
      |                |SELECT *
      |                |FROM data
      |                |WHERE id = ?1
      |                ""${'"'}.trimMargin(), SqlPreparedStatement.Type.SELECT)
      |        statement.bindLong(1, id)
      |        return SelectForId(id, statement) { resultSet ->
      |            mapper(
      |                resultSet.getLong(0)!!,
      |                queryWrapper.dataAdapter.valueAdapter.decode(resultSet.getString(1))
      |            )
      |        }
      |    }
      |
      |    fun selectForId(id: Long): Query<Data> = selectForId(id, Data::Impl)
      |
      |    fun insertData(id: Long?, value: List?): Long = insertData.execute(id, value)
      |
      |    private inner class SelectForId<out T : Any>(
      |            private val id: Long,
      |            statement: SqlPreparedStatement,
      |            mapper: (SqlResultSet) -> T
      |    ) : Query<T>(statement, selectForId, mapper) {
      |        fun dirtied(id: Long): Boolean = true
      |    }
      |
      |    private inner class InsertData(private val statement: SqlPreparedStatement) {
      |        fun execute(id: Long?, value: List?): Long {
      |            statement.bindLong(1, id)
      |            statement.bindString(2, if (value == null) null else queryWrapper.dataAdapter.valueAdapter.encode(value))
      |            val result = statement.execute()
      |            deferAction {
      |                (queryWrapper.dataQueries.selectForId)
      |                        .forEach { it.notifyResultSetChanged() }
      |            }
      |            return result
      |        }
      |    }
      |}
      |""".trimMargin())
  }
}

