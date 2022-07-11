package app.cash.sqldelight.core.queries.async

import app.cash.sqldelight.core.TestDialect
import app.cash.sqldelight.core.compiler.ExecuteQueryGenerator
import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import app.cash.sqldelight.core.dialects.intType
import app.cash.sqldelight.core.dialects.textType
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AsyncSelectQueryTypeTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `returning clause correctly generates an async query function`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  val1 TEXT,
      |  val2 TEXT
      |);
      |
      |insertReturning:
      |INSERT INTO data
      |VALUES ('sup', 'dude')
      |RETURNING *;
      |
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
      generateAsync = true,
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> insertReturning(mapper: (val1: kotlin.String?, val2: kotlin.String?) -> T): app.cash.sqldelight.ExecutableQuery<T> = app.cash.sqldelight.Query(${query.id}, driver, "Test.sq", "insertReturning", ""${'"'}
      ||INSERT INTO data
      ||VALUES ('sup', 'dude')
      ||RETURNING *
      |""${'"'}.trimMargin()) { cursor ->
      |  check(cursor is app.cash.sqldelight.driver.r2dbc.R2dbcCursor)
      |  mapper(
      |    cursor.getString(0),
      |    cursor.getString(1)
      |  )
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `returning clause in an update correctly generates an async query function`() {
    val dialect = TestDialect.POSTGRESQL
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE IF NOT EXISTS users(
      |    id ${dialect.intType} PRIMARY KEY,
      |    firstname ${dialect.textType} NOT NULL,
      |    lastname ${dialect.textType} NOT NULL
      |);
      |
      |update:
      |UPDATE users SET
      |    firstname = :firstname,
      |    lastname = :lastname
      |WHERE id = :id
      |RETURNING id, firstname, lastname;
      |
      """.trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect(),
      generateAsync = true,
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> update(
      |  firstname: kotlin.String,
      |  lastname: kotlin.String,
      |  id: kotlin.Int,
      |  mapper: (
      |    id: kotlin.Int,
      |    firstname: kotlin.String,
      |    lastname: kotlin.String,
      |  ) -> T,
      |): app.cash.sqldelight.ExecutableQuery<T> = UpdateQuery(firstname, lastname, id) { cursor ->
      |  check(cursor is app.cash.sqldelight.driver.r2dbc.R2dbcCursor)
      |  mapper(
      |    cursor.getLong(0)!!.toInt(),
      |    cursor.getString(1)!!,
      |    cursor.getString(2)!!
      |  )
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `async query type generates properly`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      |
      """.trimMargin(),
      tempFolder,
      generateAsync = true,
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  public val id: kotlin.Long,
      |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
      |) : app.cash.sqldelight.Query<T>(mapper) {
      |  public override fun addListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
      |    driver.addListener(listener, arrayOf("data"))
      |  }
      |
      |  public override fun removeListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
      |    driver.removeListener(listener, arrayOf("data"))
      |  }
      |
      |  public override fun <R> execute(mapper: (app.cash.sqldelight.db.SqlCursor) -> R): app.cash.sqldelight.db.QueryResult<R> = driver.executeQuery(${query.id}, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE id = ?
      |  ""${'"'}.trimMargin(), mapper, 1) {
      |    bindLong(0, id)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |
      """.trimMargin(),
    )
  }

  @Test
  fun `grouped statements same parameter`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  value INTEGER NOT NULL
      |);
      |
      |insertTwice {
      |  INSERT INTO data (value)
      |  VALUES (:value)
      |  ;
      |  INSERT INTO data (value)
      |  VALUES (:value)
      |  ;
      |}
      |
      """.trimMargin(),
      tempFolder,
      generateAsync = true,
    )

    val query = file.namedExecutes.first()
    val generator = ExecuteQueryGenerator(query)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public suspend fun insertTwice(`value`: kotlin.Long): kotlin.Unit {
      |  transaction {
      |    driver.execute(${query.idForIndex(0)}, ""${'"'}
      |        |INSERT INTO data (value)
      |        |  VALUES (?)
      |        ""${'"'}.trimMargin(), 1) {
      |          bindLong(0, value)
      |        }.await()
      |    driver.execute(${query.idForIndex(1)}, ""${'"'}
      |        |INSERT INTO data (value)
      |        |  VALUES (?)
      |        ""${'"'}.trimMargin(), 1) {
      |          bindLong(0, value)
      |        }.await()
      |  }
      |  notifyQueries(-609468782) { emit ->
      |    emit("data")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test
  fun `grouped statement with result`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  value INTEGER NOT NULL
      |);
      |
      |insertAndReturn {
      |  INSERT INTO data (value)
      |  VALUES (?1);
      |
      |  SELECT value
      |  FROM data
      |  WHERE id = last_insert_rowid();
      |}
      |
      """.trimMargin(),
      tempFolder,
      generateAsync = true,
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class InsertAndReturnQuery<out T : kotlin.Any>(
      |  public val value_: kotlin.Long,
      |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
      |) : app.cash.sqldelight.ExecutableQuery<T>(mapper) {
      |  public override fun <R> execute(mapper: (app.cash.sqldelight.db.SqlCursor) -> R): app.cash.sqldelight.db.QueryResult<R> = transactionWithResult {
      |    driver.execute(${query.idForIndex(0)}, ""${'"'}
      |        |INSERT INTO data (value)
      |        |  VALUES (?)
      |        ""${'"'}.trimMargin(), 1) {
      |          bindLong(0, value_)
      |        }.await()
      |    driver.executeQuery(${query.idForIndex(1)}, ""${'"'}
      |        |SELECT value
      |        |  FROM data
      |        |  WHERE id = last_insert_rowid()
      |        ""${'"'}.trimMargin(), mapper, 0)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:insertAndReturn"
      |}
      |
      """.trimMargin(),
    )
  }
}
