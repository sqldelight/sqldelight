package app.cash.sqldelight.core

import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CompositeCollectionBindTest {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test fun `multi column IN bind generates a typed collection and expands every field`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE data (
      |  department_id INTEGER NOT NULL,
      |  status TEXT,
      |  priority INTEGER NOT NULL
      |);
      |
      |selectByPairs:
      |SELECT *
      |FROM data
      |WHERE (department_id, status) IN :pairs
      |  AND (department_id, status, priority) IN ?;
      """.trimMargin(),
      temporaryFolder,
      fileName = "Data.sq",
    )

    assertThat(result.errors).isEmpty()
    val queries = File(result.outputDirectory, "com/example/DataQueries.kt")
    assertThat(result.compilerOutput).containsKey(queries)
    val generated = result.compilerOutput.getValue(queries).toString()

    assertThat(generated).isEqualTo(
      """
      |@file:Suppress("REDUNDANT_VISIBILITY_MODIFIER", "ASSIGNED_VALUE_IS_NEVER_READ")
      |
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.String
      |import kotlin.collections.Collection
      |
      |public data class SelectByPairsPairs(
      |  public val department_id: Long,
      |  public val status: String?,
      |)
      |
      |public data class SelectByPairsDepartment_id(
      |  public val department_id: Long,
      |  public val status: String?,
      |  public val priority: Long,
      |)
      |
      |public class DataQueries(
      |  driver: SqlDriver,
      |) : TransacterImpl(driver) {
      |  public fun <T : Any> selectByPairs(
      |    pairs: Collection<SelectByPairsPairs>,
      |    department_id: Collection<SelectByPairsDepartment_id>,
      |    mapper: (
      |      department_id: Long,
      |      status: String?,
      |      priority: Long,
      |    ) -> T,
      |  ): Query<T> = SelectByPairsQuery(pairs, department_id) { cursor ->
      |    mapper(
      |      cursor.getLong(0)!!,
      |      cursor.getString(1),
      |      cursor.getLong(2)!!
      |    )
      |  }
      |
      |  public fun selectByPairs(pairs: Collection<SelectByPairsPairs>, department_id: Collection<SelectByPairsDepartment_id>): Query<Data_> = selectByPairs(pairs, department_id, ::Data_)
      |
      |  private inner class SelectByPairsQuery<out T : Any>(
      |    public val pairs: Collection<SelectByPairsPairs>,
      |    public val department_id: Collection<SelectByPairsDepartment_id>,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    override fun addListener(listener: Query.Listener) {
      |      driver.addListener("data", listener = listener)
      |    }
      |
      |    override fun removeListener(listener: Query.Listener) {
      |      driver.removeListener("data", listener = listener)
      |    }
      |
      |    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
      |      val pairsIndexes = pairs.joinToString(prefix = "(", postfix = ")") {
      |        createArguments(count = 2)
      |      }
      |      val department_idIndexes = department_id.joinToString(prefix = "(", postfix = ")") {
      |        createArguments(count = 3)
      |      }
      |      return driver.executeQuery(null, ""${'"'}
      |          |SELECT data.department_id, data.status, data.priority
      |          |FROM data
      |          |WHERE (department_id, status) IN ${"$"}pairsIndexes
      |          |  AND (department_id, status, priority) IN ${"$"}department_idIndexes
      |          ""${'"'}.trimMargin(), mapper, pairs.size * 2 + department_id.size * 3) {
      |            var parameterIndex = 0
      |            pairs.forEach { pairs_ ->
      |              bindLong(parameterIndex++, pairs_.department_id)
      |              bindString(parameterIndex++, pairs_.status)
      |            }
      |            department_id.forEach { department_id_ ->
      |              bindLong(parameterIndex++, department_id_.department_id)
      |              bindString(parameterIndex++, department_id_.status)
      |              bindLong(parameterIndex++, department_id_.priority)
      |            }
      |          }
      |    }
      |
      |    override fun toString(): String = "Data.sq:selectByPairs"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `multi column IN bind preserves value types adapters and mixed bind order`() {
    val result = FixtureCompiler.compileSql(
      """
      |import com.example.Status;
      |
      |CREATE TABLE data (
      |  id INTEGER AS VALUE NOT NULL,
      |  status TEXT AS Status NOT NULL,
      |  note TEXT
      |);
      |
      |selectByPairs:
      |SELECT *
      |FROM data
      |WHERE note = :note
      |  AND id IN :ids
      |  AND (id, status) IN :pairs;
      """.trimMargin(),
      temporaryFolder,
      fileName = "Data.sq",
    )

    assertThat(result.errors).isEmpty()
    val queries = File(result.outputDirectory, "com/example/DataQueries.kt")
    val generated = result.compilerOutput.getValue(queries).toString()

    assertThat(generated).isEqualTo(
      """
      |@file:Suppress("REDUNDANT_VISIBILITY_MODIFIER", "ASSIGNED_VALUE_IS_NEVER_READ")
      |
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.String
      |import kotlin.collections.Collection
      |
      |public data class SelectByPairsPairs(
      |  public val id: Data_.Id,
      |  public val status: Status,
      |)
      |
      |public class DataQueries(
      |  driver: SqlDriver,
      |  private val data_Adapter: Data_.Adapter,
      |) : TransacterImpl(driver) {
      |  public fun <T : Any> selectByPairs(
      |    note: String?,
      |    ids: Collection<Data_.Id>,
      |    pairs: Collection<SelectByPairsPairs>,
      |    mapper: (
      |      id: Data_.Id,
      |      status: Status,
      |      note: String?,
      |    ) -> T,
      |  ): Query<T> = SelectByPairsQuery(note, ids, pairs) { cursor ->
      |    mapper(
      |      Data_.Id(cursor.getLong(0)!!),
      |      data_Adapter.statusAdapter.decode(cursor.getString(1)!!),
      |      cursor.getString(2)
      |    )
      |  }
      |
      |  public fun selectByPairs(
      |    note: String?,
      |    ids: Collection<Data_.Id>,
      |    pairs: Collection<SelectByPairsPairs>,
      |  ): Query<Data_> = selectByPairs(note, ids, pairs, ::Data_)
      |
      |  private inner class SelectByPairsQuery<out T : Any>(
      |    public val note: String?,
      |    public val ids: Collection<Data_.Id>,
      |    public val pairs: Collection<SelectByPairsPairs>,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    override fun addListener(listener: Query.Listener) {
      |      driver.addListener("data", listener = listener)
      |    }
      |
      |    override fun removeListener(listener: Query.Listener) {
      |      driver.removeListener("data", listener = listener)
      |    }
      |
      |    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
      |      val idsIndexes = createArguments(count = ids.size)
      |      val pairsIndexes = pairs.joinToString(prefix = "(", postfix = ")") {
      |        createArguments(count = 2)
      |      }
      |      return driver.executeQuery(null, ""${'"'}
      |          |SELECT data.id, data.status, data.note
      |          |FROM data
      |          |WHERE note ${"$"}{ if (note == null) "IS" else "=" } ?
      |          |  AND id IN ${"$"}idsIndexes
      |          |  AND (id, status) IN ${"$"}pairsIndexes
      |          ""${'"'}.trimMargin(), mapper, 1 + ids.size + pairs.size * 2) {
      |            var parameterIndex = 0
      |            bindString(parameterIndex++, note)
      |            ids.forEach { ids_ ->
      |              bindLong(parameterIndex++, ids_.id)
      |            }
      |            pairs.forEach { pairs_ ->
      |              bindLong(parameterIndex++, pairs_.id.id)
      |              bindString(parameterIndex++, data_Adapter.statusAdapter.encode(pairs_.status))
      |            }
      |          }
      |    }
      |
      |    override fun toString(): String = "Data.sq:selectByPairs"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `multi column IN bind allocates duplicate and keyword field names`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE data (
      |  "when" TEXT NOT NULL,
      |  status TEXT NOT NULL
      |);
      |
      |selectReserved:
      |SELECT *
      |FROM data
      |WHERE ("when", status, status) IN :values;
      """.trimMargin(),
      temporaryFolder,
      fileName = "Data.sq",
    )

    assertThat(result.errors).isEmpty()
    val queries = File(result.outputDirectory, "com/example/DataQueries.kt")
    val generated = result.compilerOutput.getValue(queries).toString()

    assertThat(generated).isEqualTo(
      """
      |@file:Suppress("REDUNDANT_VISIBILITY_MODIFIER", "ASSIGNED_VALUE_IS_NEVER_READ")
      |
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.String
      |import kotlin.collections.Collection
      |
      |public data class SelectReservedValues(
      |  public val when_: String,
      |  public val status: String,
      |  public val status_: String,
      |)
      |
      |public class DataQueries(
      |  driver: SqlDriver,
      |) : TransacterImpl(driver) {
      |  public fun <T : Any> selectReserved(values: Collection<SelectReservedValues>, mapper: (when_: String, status: String) -> T): Query<T> = SelectReservedQuery(values) { cursor ->
      |    mapper(
      |      cursor.getString(0)!!,
      |      cursor.getString(1)!!
      |    )
      |  }
      |
      |  public fun selectReserved(values: Collection<SelectReservedValues>): Query<Data_> = selectReserved(values, ::Data_)
      |
      |  private inner class SelectReservedQuery<out T : Any>(
      |    public val values: Collection<SelectReservedValues>,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    override fun addListener(listener: Query.Listener) {
      |      driver.addListener("data", listener = listener)
      |    }
      |
      |    override fun removeListener(listener: Query.Listener) {
      |      driver.removeListener("data", listener = listener)
      |    }
      |
      |    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
      |      val valuesIndexes = values.joinToString(prefix = "(", postfix = ")") {
      |        createArguments(count = 3)
      |      }
      |      return driver.executeQuery(null, ""${'"'}
      |          |SELECT data."when", data.status
      |          |FROM data
      |          |WHERE ("when", status, status) IN ${"$"}valuesIndexes
      |          ""${'"'}.trimMargin(), mapper, values.size * 3) {
      |            var parameterIndex = 0
      |            values.forEach { values_ ->
      |              bindString(parameterIndex++, values_.when_)
      |              bindString(parameterIndex++, values_.status)
      |              bindString(parameterIndex++, values_.status_)
      |            }
      |          }
      |    }
      |
      |    override fun toString(): String = "Data.sq:selectReserved"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `multi column IN bind allocates colliding generated type names`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  status TEXT NOT NULL
      |);
      |
      |selectA:
      |SELECT * FROM data WHERE (id, status) IN :bC;
      |
      |selectAB:
      |SELECT * FROM data WHERE (id, status) IN :c;
      |
      |selectABC:
      |SELECT id + 1 AS next_id FROM data;
      |
      |Data:
      |SELECT * FROM data WHERE (id, status) IN :queries;
      """.trimMargin(),
      temporaryFolder,
      fileName = "Data.sq",
    )

    assertThat(result.errors).isEmpty()
    val queries = File(result.outputDirectory, "com/example/DataQueries.kt")
    val generated = result.compilerOutput.getValue(queries).toString()

    assertThat(generated).isEqualTo(
      """
      |@file:Suppress("REDUNDANT_VISIBILITY_MODIFIER", "ASSIGNED_VALUE_IS_NEVER_READ")
      |
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.QueryResult
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.String
      |import kotlin.collections.Collection
      |
      |public data class SelectABC_(
      |  public val id: Long,
      |  public val status: String,
      |)
      |
      |public data class SelectABC__(
      |  public val id: Long,
      |  public val status: String,
      |)
      |
      |public data class DataQueries_(
      |  public val id: Long,
      |  public val status: String,
      |)
      |
      |public class DataQueries(
      |  driver: SqlDriver,
      |) : TransacterImpl(driver) {
      |  public fun <T : Any> selectA(bC: Collection<SelectABC_>, mapper: (id: Long, status: String) -> T): Query<T> = SelectAQuery(bC) { cursor ->
      |    mapper(
      |      cursor.getLong(0)!!,
      |      cursor.getString(1)!!
      |    )
      |  }
      |
      |  public fun selectA(bC: Collection<SelectABC_>): Query<Data_> = selectA(bC, ::Data_)
      |
      |  public fun <T : Any> selectAB(c: Collection<SelectABC__>, mapper: (id: Long, status: String) -> T): Query<T> = SelectABQuery(c) { cursor ->
      |    mapper(
      |      cursor.getLong(0)!!,
      |      cursor.getString(1)!!
      |    )
      |  }
      |
      |  public fun selectAB(c: Collection<SelectABC__>): Query<Data_> = selectAB(c, ::Data_)
      |
      |  public fun selectABC(): Query<Long> = Query(-493_514_991, arrayOf("data"), driver, "Data.sq", "selectABC", "SELECT id + 1 AS next_id FROM data") { cursor ->
      |    cursor.getLong(0)!!
      |  }
      |
      |  public fun <T : Any> Data(queries: Collection<DataQueries_>, mapper: (id: Long, status: String) -> T): Query<T> = DataQuery(queries) { cursor ->
      |    mapper(
      |      cursor.getLong(0)!!,
      |      cursor.getString(1)!!
      |    )
      |  }
      |
      |  public fun Data(queries: Collection<DataQueries_>): Query<Data_> = Data(queries, ::Data_)
      |
      |  private inner class SelectAQuery<out T : Any>(
      |    public val bC: Collection<SelectABC_>,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    override fun addListener(listener: Query.Listener) {
      |      driver.addListener("data", listener = listener)
      |    }
      |
      |    override fun removeListener(listener: Query.Listener) {
      |      driver.removeListener("data", listener = listener)
      |    }
      |
      |    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
      |      val bCIndexes = bC.joinToString(prefix = "(", postfix = ")") {
      |        createArguments(count = 2)
      |      }
      |      return driver.executeQuery(null, ""${'"'}SELECT data.id, data.status FROM data WHERE (id, status) IN ${"$"}bCIndexes""${'"'}, mapper, bC.size * 2) {
      |            var parameterIndex = 0
      |            bC.forEach { bC_ ->
      |              bindLong(parameterIndex++, bC_.id)
      |              bindString(parameterIndex++, bC_.status)
      |            }
      |          }
      |    }
      |
      |    override fun toString(): String = "Data.sq:selectA"
      |  }
      |
      |  private inner class SelectABQuery<out T : Any>(
      |    public val c: Collection<SelectABC__>,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    override fun addListener(listener: Query.Listener) {
      |      driver.addListener("data", listener = listener)
      |    }
      |
      |    override fun removeListener(listener: Query.Listener) {
      |      driver.removeListener("data", listener = listener)
      |    }
      |
      |    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
      |      val cIndexes = c.joinToString(prefix = "(", postfix = ")") {
      |        createArguments(count = 2)
      |      }
      |      return driver.executeQuery(null, ""${'"'}SELECT data.id, data.status FROM data WHERE (id, status) IN ${"$"}cIndexes""${'"'}, mapper, c.size * 2) {
      |            var parameterIndex = 0
      |            c.forEach { c_ ->
      |              bindLong(parameterIndex++, c_.id)
      |              bindString(parameterIndex++, c_.status)
      |            }
      |          }
      |    }
      |
      |    override fun toString(): String = "Data.sq:selectAB"
      |  }
      |
      |  private inner class DataQuery<out T : Any>(
      |    public val queries: Collection<DataQueries_>,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    override fun addListener(listener: Query.Listener) {
      |      driver.addListener("data", listener = listener)
      |    }
      |
      |    override fun removeListener(listener: Query.Listener) {
      |      driver.removeListener("data", listener = listener)
      |    }
      |
      |    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
      |      val queriesIndexes = queries.joinToString(prefix = "(", postfix = ")") {
      |        createArguments(count = 2)
      |      }
      |      return driver.executeQuery(null, ""${'"'}SELECT data.id, data.status FROM data WHERE (id, status) IN ${"$"}queriesIndexes""${'"'}, mapper, queries.size * 2) {
      |            var parameterIndex = 0
      |            queries.forEach { queries_ ->
      |              bindLong(parameterIndex++, queries_.id)
      |              bindString(parameterIndex++, queries_.status)
      |            }
      |          }
      |    }
      |
      |    override fun toString(): String = "Data.sq:Data"
      |  }
      |}
      |
      """.trimMargin(),
    )
  }
}
