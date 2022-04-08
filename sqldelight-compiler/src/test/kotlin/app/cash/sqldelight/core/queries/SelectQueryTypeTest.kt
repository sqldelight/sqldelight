package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.TestDialect
import app.cash.sqldelight.core.compiler.ExecuteQueryGenerator
import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import app.cash.sqldelight.core.dialects.cursorCheck
import app.cash.sqldelight.core.dialects.intType
import app.cash.sqldelight.core.dialects.textType
import app.cash.sqldelight.dialects.hsql.HsqlDialect
import app.cash.sqldelight.dialects.mysql.MySqlDialect
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import com.google.common.truth.Truth.assertThat
import com.squareup.burst.BurstJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(BurstJUnit4::class)
class SelectQueryTypeTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `returning clause correctly generates a query function`(dialect: TestDialect) {
    assumeTrue(dialect in listOf(TestDialect.POSTGRESQL, TestDialect.SQLITE_3_35))
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
      |""".trimMargin(),
      tempFolder,
      dialect = PostgreSqlDialect()
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
      |  check(cursor is app.cash.sqldelight.driver.jdbc.JdbcCursor)
      |  mapper(
      |    cursor.getString(0),
      |    cursor.getString(1)
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `query type generates properly`() {
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
      |""".trimMargin(),
      tempFolder
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(${query.id}, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE id = ?
      |  ""${'"'}.trimMargin(), 1) {
      |    bindLong(1, id)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin()
    )
  }

  @Test fun `bind arguments are ordered in generated type`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |select:
      |SELECT *
      |FROM data
      |WHERE id = ?2
      |AND value = ?1;
      """.trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectQuery<out T : kotlin.Any>(
      |  public val value_: kotlin.String,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(${query.id}, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE id = ?
      |  |AND value = ?
      |  ""${'"'}.trimMargin(), 2) {
      |    bindLong(1, id)
      |    bindString(2, value_)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:select"
      |}
      |""".trimMargin()
    )
  }

  @Test fun `array bind argument`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id IN ?;
      |""".trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  public val id: kotlin.collections.Collection<kotlin.Long>,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor {
      |    val idIndexes = createArguments(count = id.size)
      |    return driver.executeQuery(null, ""${'"'}
      |        |SELECT *
      |        |FROM data
      |        |WHERE id IN ${"$"}idIndexes
      |        ""${'"'}.trimMargin(), id.size) {
      |          id.forEachIndexed { index, id_ ->
      |            bindLong(index + 1, id_)
      |          }
      |        }
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin()
    )
  }

  @Test fun `duplicated array bind argument`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  message TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id IN ?1 AND message != ?2 AND id IN ?1;
      |""".trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  public val id: kotlin.collections.Collection<kotlin.Long>,
      |  public val message: kotlin.String,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor {
      |    val idIndexes = createArguments(count = id.size)
      |    return driver.executeQuery(null, ""${'"'}
      |        |SELECT *
      |        |FROM data
      |        |WHERE id IN ${"$"}idIndexes AND message != ? AND id IN ${"$"}idIndexes
      |        ""${'"'}.trimMargin(), 1 + id.size + id.size) {
      |          id.forEachIndexed { index, id_ ->
      |            bindLong(index + 1, id_)
      |          }
      |          bindString(id.size + 1, message)
      |          id.forEachIndexed { index, id__ ->
      |            bindLong(index + id.size + 2, id__)
      |          }
      |        }
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin()
    )
  }

  @Test fun `nullable parameter not escaped`() {
    val file = FixtureCompiler.parseSql(
      """
       |CREATE TABLE socialFeedItem (
       |  message TEXT,
       |  userId TEXT,
       |  creation_time INTEGER
       |);
       |
       |select_news_list:
       |SELECT * FROM socialFeedItem WHERE message IS NOT NULL AND userId = ? ORDER BY datetime(creation_time) DESC;
       |""".trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
       |private inner class Select_news_listQuery<out T : kotlin.Any>(
       |  public val userId: kotlin.String?,
       |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
       |) : app.cash.sqldelight.Query<T>(mapper) {
       |  public override fun addListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
       |    driver.addListener(listener, arrayOf("socialFeedItem"))
       |  }
       |
       |  public override fun removeListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
       |    driver.removeListener(listener, arrayOf("socialFeedItem"))
       |  }
       |
       |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(null, ""${'"'}SELECT * FROM socialFeedItem WHERE message IS NOT NULL AND userId ${"$"}{ if (userId == null) "IS" else "=" } ? ORDER BY datetime(creation_time) DESC""${'"'}, 1) {
       |    bindString(1, userId)
       |  }
       |
       |  public override fun toString(): kotlin.String = "Test.sq:select_news_list"
       |}
       |""".trimMargin()
    )

    val treatNullAsUnknownFile = FixtureCompiler.parseSql(
      """
       |CREATE TABLE socialFeedItem (
       |  message TEXT,
       |  userId TEXT,
       |  creation_time INTEGER
       |);
       |
       |select_news_list:
       |SELECT * FROM socialFeedItem WHERE message IS NOT NULL AND userId = ? ORDER BY datetime(creation_time) DESC;
       |""".trimMargin(),
      tempFolder,
      treatNullAsUnknownForEquality = true
    )

    val treatNullAsUnknownQuery = treatNullAsUnknownFile.namedQueries.first()
    val nullAsUnknownGenerator = SelectQueryGenerator(treatNullAsUnknownQuery)

    assertThat(nullAsUnknownGenerator.querySubtype().toString()).isEqualTo(
      """
       |private inner class Select_news_listQuery<out T : kotlin.Any>(
       |  public val userId: kotlin.String?,
       |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
       |) : app.cash.sqldelight.Query<T>(mapper) {
       |  public override fun addListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
       |    driver.addListener(listener, arrayOf("socialFeedItem"))
       |  }
       |
       |  public override fun removeListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
       |    driver.removeListener(listener, arrayOf("socialFeedItem"))
       |  }
       |
       |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(${treatNullAsUnknownQuery.id}, ""${'"'}SELECT * FROM socialFeedItem WHERE message IS NOT NULL AND userId = ? ORDER BY datetime(creation_time) DESC""${'"'}, 1) {
       |    bindString(1, userId)
       |  }
       |
       |  public override fun toString(): kotlin.String = "Test.sq:select_news_list"
       |}
       |""".trimMargin()
    )
  }

  @Test fun `nullable parameter has spaces`() {
    val file = FixtureCompiler.parseSql(
      """
       |CREATE TABLE IF NOT EXISTS Friend(
       |    _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
       |    username TEXT NOT NULL UNIQUE,
       |    userId TEXT
       |);
       |
       |selectData:
       |SELECT _id, username
       |FROM Friend
       |WHERE userId=? OR username=? LIMIT 2;
       |""".trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()

    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
       |private inner class SelectDataQuery<out T : kotlin.Any>(
       |  public val userId: kotlin.String?,
       |  public val username: kotlin.String,
       |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
       |) : app.cash.sqldelight.Query<T>(mapper) {
       |  public override fun addListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
       |    driver.addListener(listener, arrayOf("Friend"))
       |  }
       |
       |  public override fun removeListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
       |    driver.removeListener(listener, arrayOf("Friend"))
       |  }
       |
       |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(null, ""${'"'}
       |  |SELECT _id, username
       |  |FROM Friend
       |  |WHERE userId${'$'}{ if (userId == null) " IS " else "=" }? OR username=? LIMIT 2
       |  ""${'"'}.trimMargin(), 2) {
       |    bindString(1, userId)
       |    bindString(2, username)
       |  }
       |
       |  public override fun toString(): kotlin.String = "Test.sq:selectData"
       |}
       |""".trimMargin()
    )

    val nullAsUnknownFile = FixtureCompiler.parseSql(
      """
       |CREATE TABLE IF NOT EXISTS Friend(
       |    _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
       |    username TEXT NOT NULL UNIQUE,
       |    userId TEXT
       |);
       |
       |selectData:
       |SELECT _id, username
       |FROM Friend
       |WHERE userId=? OR username=? LIMIT 2;
       |""".trimMargin(),
      tempFolder,
      treatNullAsUnknownForEquality = true
    )

    val nullAsUnknownQuery = nullAsUnknownFile.namedQueries.first()
    val nullAsUnknownGenerator = SelectQueryGenerator(nullAsUnknownQuery)

    assertThat(nullAsUnknownGenerator.querySubtype().toString()).isEqualTo(
      """
       |private inner class SelectDataQuery<out T : kotlin.Any>(
       |  public val userId: kotlin.String?,
       |  public val username: kotlin.String,
       |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
       |) : app.cash.sqldelight.Query<T>(mapper) {
       |  public override fun addListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
       |    driver.addListener(listener, arrayOf("Friend"))
       |  }
       |
       |  public override fun removeListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
       |    driver.removeListener(listener, arrayOf("Friend"))
       |  }
       |
       |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(${nullAsUnknownQuery.id}, ""${'"'}
       |  |SELECT _id, username
       |  |FROM Friend
       |  |WHERE userId=? OR username=? LIMIT 2
       |  ""${'"'}.trimMargin(), 2) {
       |    bindString(1, userId)
       |    bindString(2, username)
       |  }
       |
       |  public override fun toString(): kotlin.String = "Test.sq:selectData"
       |}
       |""".trimMargin()
    )
  }

  @Test fun `nullable bind parameters`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  val TEXT
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE val = ?
      |AND val == ?
      |AND val <> ?
      |AND val != ?
      |AND val IS ?
      |AND val IS NOT ?;
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  public val val_: kotlin.String?,
      |  public val val__: kotlin.String?,
      |  public val val___: kotlin.String?,
      |  public val val____: kotlin.String?,
      |  public val val_____: kotlin.String?,
      |  public val val______: kotlin.String?,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(null, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE val ${"$"}{ if (val_ == null) "IS" else "=" } ?
      |  |AND val ${"$"}{ if (val__ == null) "IS" else "==" } ?
      |  |AND val ${"$"}{ if (val___ == null) "IS NOT" else "<>" } ?
      |  |AND val ${"$"}{ if (val____ == null) "IS NOT" else "!=" } ?
      |  |AND val IS ?
      |  |AND val IS NOT ?
      |  ""${'"'}.trimMargin(), 6) {
      |    bindString(1, val_)
      |    bindString(2, val__)
      |    bindString(3, val___)
      |    bindString(4, val____)
      |    bindString(5, val_____)
      |    bindString(6, val______)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin()
    )

    val nullAsUnknownFile = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  val TEXT
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE val = ?
      |AND val == ?
      |AND val <> ?
      |AND val != ?
      |AND val IS ?
      |AND val IS NOT ?;
      |""".trimMargin(),
      tempFolder,
      treatNullAsUnknownForEquality = true
    )

    val nullAsUnknownQuery = nullAsUnknownFile.namedQueries.first()
    val nullAsUnknownGenerator = SelectQueryGenerator(nullAsUnknownQuery)

    assertThat(nullAsUnknownGenerator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  public val val_: kotlin.String?,
      |  public val val__: kotlin.String?,
      |  public val val___: kotlin.String?,
      |  public val val____: kotlin.String?,
      |  public val val_____: kotlin.String?,
      |  public val val______: kotlin.String?,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(${nullAsUnknownQuery.id}, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE val = ?
      |  |AND val == ?
      |  |AND val <> ?
      |  |AND val != ?
      |  |AND val IS ?
      |  |AND val IS NOT ?
      |  ""${'"'}.trimMargin(), 6) {
      |    bindString(1, val_)
      |    bindString(2, val__)
      |    bindString(3, val___)
      |    bindString(4, val____)
      |    bindString(5, val_____)
      |    bindString(6, val______)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin()
    )
  }

  @Test fun `synthesized column bind arguments`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE VIRTUAL TABLE data USING fts3 (
      |  content TEXT NOT NULL
      |);
      |
      |selectMatching:
      |SELECT *
      |FROM data
      |WHERE data MATCH ? AND rowid = ?;
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectMatchingQuery<out T : kotlin.Any>(
      |  public val `data`: kotlin.String,
      |  public val rowid: kotlin.Long,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(${query.id}, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE data MATCH ? AND rowid = ?
      |  ""${'"'}.trimMargin(), 2) {
      |    bindString(1, data)
      |    bindLong(2, rowid)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectMatching"
      |}
      |""".trimMargin()
    )
  }

  @Test fun `synthesized fts5 column bind arguments`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE VIRTUAL TABLE data USING fts5(
      |  content,
      |  prefix='2 3 4 5 6 7',
      |  content_rowid=id
      |);
      |
      |selectMatching:
      |SELECT *
      |FROM data
      |WHERE data MATCH '"one ' || ? || '" * ';
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectMatchingQuery<out T : kotlin.Any>(
      |  public val `value`: kotlin.String,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(${query.id}, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE data MATCH '"one ' || ? || '" * '
      |  ""${'"'}.trimMargin(), 1) {
      |    bindString(1, value)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectMatching"
      |}
      |""".trimMargin()
    )
  }

  @Test fun `array and named bind arguments are compatible`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  token TEXT NOT NULL,
      |  name TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE token = :token
      |  AND id IN ?
      |  AND (token != :token OR (name = :name OR :name = 'foo'))
      |  AND token IN ?;
      |""".trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  public val token: kotlin.String,
      |  public val id: kotlin.collections.Collection<kotlin.Long>,
      |  public val name: kotlin.String,
      |  public val token_: kotlin.collections.Collection<kotlin.String>,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor {
      |    val idIndexes = createArguments(count = id.size)
      |    val token_Indexes = createArguments(count = token_.size)
      |    return driver.executeQuery(null, ""${'"'}
      |        |SELECT *
      |        |FROM data
      |        |WHERE token = ?
      |        |  AND id IN ${"$"}idIndexes
      |        |  AND (token != ? OR (name = ? OR ? = 'foo'))
      |        |  AND token IN ${"$"}token_Indexes
      |        ""${'"'}.trimMargin(), 4 + id.size + token_.size) {
      |          bindString(1, token)
      |          id.forEachIndexed { index, id_ ->
      |            bindLong(index + 2, id_)
      |          }
      |          bindString(id.size + 2, token)
      |          bindString(id.size + 3, name)
      |          bindString(id.size + 4, name)
      |          token_.forEachIndexed { index, token__ ->
      |            bindString(index + id.size + 5, token__)
      |          }
      |        }
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin()
    )
  }

  @Test fun `nullable parameter type`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER
      |);
      |
      |selectForId:
      |WITH child_ids AS (SELECT id FROM data WHERE id = ?1)
      |SELECT *
      |FROM data
      |WHERE id = ?1 OR id IN child_ids
      |LIMIT :limit
      |OFFSET :offset;
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  public val id: kotlin.Long?,
      |  public val limit: kotlin.Long,
      |  public val offset: kotlin.Long,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(null, ""${'"'}
      |  |WITH child_ids AS (SELECT id FROM data WHERE id ${'$'}{ if (id == null) "IS" else "=" } ?)
      |  |SELECT *
      |  |FROM data
      |  |WHERE id ${'$'}{ if (id == null) "IS" else "=" } ? OR id IN child_ids
      |  |LIMIT ?
      |  |OFFSET ?
      |  ""${'"'}.trimMargin(), 4) {
      |    bindLong(1, id)
      |    bindLong(2, id)
      |    bindLong(3, limit)
      |    bindLong(4, offset)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin()
    )

    val nullAsUnknownFile = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER
      |);
      |
      |selectForId:
      |WITH child_ids AS (SELECT id FROM data WHERE id = ?1)
      |SELECT *
      |FROM data
      |WHERE id = ?1 OR id IN child_ids
      |LIMIT :limit
      |OFFSET :offset;
      |""".trimMargin(),
      tempFolder,
      treatNullAsUnknownForEquality = true
    )

    val nullAsUnknownQuery = nullAsUnknownFile.namedQueries.first()
    val nullAsUnknownGenerator = SelectQueryGenerator(nullAsUnknownQuery)

    assertThat(nullAsUnknownGenerator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectForIdQuery<out T : kotlin.Any>(
      |  public val id: kotlin.Long?,
      |  public val limit: kotlin.Long,
      |  public val offset: kotlin.Long,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(${nullAsUnknownQuery.id}, ""${'"'}
      |  |WITH child_ids AS (SELECT id FROM data WHERE id = ?)
      |  |SELECT *
      |  |FROM data
      |  |WHERE id = ? OR id IN child_ids
      |  |LIMIT ?
      |  |OFFSET ?
      |  ""${'"'}.trimMargin(), 4) {
      |    bindLong(1, id)
      |    bindLong(2, id)
      |    bindLong(3, limit)
      |    bindLong(4, offset)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectForId"
      |}
      |""".trimMargin()
    )
  }

  @Test fun `custom type vararg`() {
    val file = FixtureCompiler.parseSql(
      """
      |import foo.Bar;
      |
      |CREATE TABLE data (
      |  id INTEGER AS Bar
      |);
      |
      |selectForIds:
      |SELECT *
      |FROM data
      |WHERE id IN ?;
      |""".trimMargin(),
      tempFolder
    )

    val generator = SelectQueryGenerator(file.namedQueries.first())

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectForIdsQuery<out T : kotlin.Any>(
      |  public val id: kotlin.collections.Collection<foo.Bar?>,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor {
      |    val idIndexes = createArguments(count = id.size)
      |    return driver.executeQuery(null, ""${'"'}
      |        |SELECT *
      |        |FROM data
      |        |WHERE id IN ${'$'}idIndexes
      |        ""${'"'}.trimMargin(), id.size) {
      |          id.forEachIndexed { index, id_ ->
      |            bindLong(index + 1, id_?.let { data_Adapter.idAdapter.encode(it) })
      |          }
      |        }
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectForIds"
      |}
      |""".trimMargin()
    )
  }

  @Test
  fun `query type generates properly if argument is compared using IS NULL`(dialect: TestDialect) {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  token ${dialect.textType} NOT NULL
      |);
      |
      |selectByTokenOrAll:
      |SELECT *
      |FROM data
      |WHERE token = :token OR :token IS NULL;
      |""".trimMargin(),
      tempFolder, dialect = dialect.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    /**
     * The [check] statement generated for the prepared statement type in the binder lambda for this dialect.
     *
     * See [QueryGenerator].
     */
    val binderCheck = when {
      dialect.dialect.isSqlite -> ""
      else -> when (dialect.dialect) {
        is PostgreSqlDialect, is HsqlDialect, is MySqlDialect -> "check(this is app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement)\n    "
        else -> throw IllegalStateException("Unknown dialect: $this")
      }
    }

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectByTokenOrAllQuery<out T : kotlin.Any>(
      |  public val token: kotlin.String?,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(null, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE token ${"$"}{ if (token == null) "IS" else "=" } ? OR ? IS NULL
      |  ""${'"'}.trimMargin(), 2) {
      |    ${binderCheck}bindString(1, token)
      |    bindString(2, token)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectByTokenOrAll"
      |}
      |""".trimMargin()
    )

    val nullAsUnknownFile = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  token ${dialect.textType} NOT NULL
      |);
      |
      |selectByTokenOrAll:
      |SELECT *
      |FROM data
      |WHERE token = :token OR :token IS NULL;
      |""".trimMargin(),
      tempFolder, dialect = dialect.dialect,
      treatNullAsUnknownForEquality = true
    )

    val nullAsUnknownQuery = nullAsUnknownFile.namedQueries.first()
    val nullAsUnknownGenerator = SelectQueryGenerator(nullAsUnknownQuery)

    assertThat(nullAsUnknownGenerator.querySubtype().toString()).isEqualTo(
      """
      |private inner class SelectByTokenOrAllQuery<out T : kotlin.Any>(
      |  public val token: kotlin.String?,
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
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor = driver.executeQuery(${nullAsUnknownQuery.id}, ""${'"'}
      |  |SELECT *
      |  |FROM data
      |  |WHERE token = ? OR ? IS NULL
      |  ""${'"'}.trimMargin(), 2) {
      |    ${binderCheck}bindString(1, token)
      |    bindString(2, token)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:selectByTokenOrAll"
      |}
      |""".trimMargin()
    )
  }

  @Test
  fun `proper exposure of greatest function`(dialect: TestDialect) {
    assumeTrue(dialect in listOf(TestDialect.MYSQL, TestDialect.POSTGRESQL))
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  token ${dialect.textType} NOT NULL,
      |  value ${dialect.intType} NOT NULL
      |);
      |
      |selectGreatest:
      |SELECT greatest(token, value)
      |FROM data;
      |""".trimMargin(),
      tempFolder, dialect = dialect.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun selectGreatest(): app.cash.sqldelight.Query<kotlin.String> = app.cash.sqldelight.Query(${query.id}, arrayOf("data"), driver, "Test.sq", "selectGreatest", ""${'"'}
      ||SELECT greatest(token, value)
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |  check(cursor is ${dialect.dialect.cursorType})
      |  cursor.getString(0)!!
      |}
      |""".trimMargin()
    )
  }

  @Test
  fun `proper exposure of concat function`(dialect: TestDialect) {
    assumeTrue(dialect in listOf(TestDialect.MYSQL, TestDialect.POSTGRESQL))
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE people (
      |  first_name ${dialect.textType} NOT NULL,
      |  last_name ${dialect.intType} NOT NULL
      |);
      |
      |selectFullNames:
      |SELECT CONCAT(first_name, last_name)
      |FROM people;
      |""".trimMargin(),
      tempFolder, dialect = dialect.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun selectFullNames(): app.cash.sqldelight.Query<kotlin.String> = app.cash.sqldelight.Query(${query.id}, arrayOf("people"), driver, "Test.sq", "selectFullNames", ""${'"'}
      ||SELECT CONCAT(first_name, last_name)
      ||FROM people
      |""${'"'}.trimMargin()) { cursor ->
      |  check(cursor is ${dialect.dialect.cursorType})
      |  cursor.getString(0)!!
      |}
      |""".trimMargin()
    )
  }

  @Test
  fun `compatible java types from different columns checks for adapter equivalence`(dialect: TestDialect) {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE children(
      |  birthday ${dialect.textType} AS java.time.LocalDate NOT NULL,
      |  age ${dialect.textType} NOT NULL
      |);
      |
      |CREATE TABLE teenagers(
      |  birthday ${dialect.textType} AS java.time.LocalDate NOT NULL,
      |  age ${dialect.textType} NOT NULL
      |);
      |
      |CREATE TABLE adults(
      |  birthday ${dialect.textType} AS java.time.LocalDate,
      |  age ${dialect.textType}
      |);
      |
      |birthdays:
      |SELECT birthday, age
      |FROM children
      |UNION
      |SELECT birthday, age
      |FROM teenagers
      |UNION
      |SELECT birthday, age
      |FROM adults;
      |""".trimMargin(),
      tempFolder, dialect = dialect.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> birthdays(mapper: (birthday: java.time.LocalDate?, age: kotlin.String?) -> T): app.cash.sqldelight.Query<T> {
      |  kotlin.check(kotlin.collections.setOf(childrenAdapter.birthdayAdapter, teenagersAdapter.birthdayAdapter, adultsAdapter.birthdayAdapter).size == 1) { "Adapter types are expected to be identical." }
      |  return app.cash.sqldelight.Query(${query.id}, arrayOf("children", "teenagers", "adults"), driver, "Test.sq", "birthdays", ""${'"'}
      |  |SELECT birthday, age
      |  |FROM children
      |  |UNION
      |  |SELECT birthday, age
      |  |FROM teenagers
      |  |UNION
      |  |SELECT birthday, age
      |  |FROM adults
      |  ""${'"'}.trimMargin()) { cursor ->
      |    ${dialect.cursorCheck}mapper(
      |      cursor.getString(0)?.let { childrenAdapter.birthdayAdapter.decode(it) },
      |      cursor.getString(1)
      |    )
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test
  fun `equivalent adapters from different tables are all required`(dialect: TestDialect) {
    val file = FixtureCompiler.compileSql(
      """
      |CREATE TABLE children(
      |  birthday ${dialect.textType} AS java.time.LocalDate NOT NULL,
      |  age ${dialect.textType} NOT NULL
      |);
      |
      |CREATE TABLE teenagers(
      |  birthday ${dialect.textType} AS java.time.LocalDate NOT NULL,
      |  age ${dialect.textType} NOT NULL
      |);
      |
      |CREATE TABLE adults(
      |  birthday ${dialect.textType} AS java.time.LocalDate,
      |  age ${dialect.textType}
      |);
      |
      |birthdays:
      |SELECT birthday, age
      |FROM children
      |UNION
      |SELECT birthday, age
      |FROM teenagers
      |UNION
      |SELECT birthday, age
      |FROM adults;
      |""".trimMargin(),
      tempFolder,
      overrideDialect = dialect.dialect
    )

    val requiredAdapters = file.compiledFile.requiredAdapters.joinToString { it.type.toString() }
    assertThat(requiredAdapters).isEqualTo("com.example.Children.Adapter, com.example.Teenagers.Adapter, com.example.Adults.Adapter")
  }

  @Test
  fun `proper exposure of month and year functions`(dialect: TestDialect) {
    assumeTrue(dialect in listOf(TestDialect.MYSQL))
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE people (
      |  born_at DATETIME NOT NULL
      |);
      |
      |selectBirthMonthAndYear:
      |SELECT MONTH(born_at) AS birthMonth, YEAR(born_at) AS birthYear
      |FROM people;
      |""".trimMargin(),
      tempFolder, dialect = dialect.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectBirthMonthAndYear(mapper: (birthMonth: kotlin.Long, birthYear: kotlin.Long) -> T): app.cash.sqldelight.Query<T> = app.cash.sqldelight.Query(${query.id}, arrayOf("people"), driver, "Test.sq", "selectBirthMonthAndYear", ""${'"'}
      ||SELECT MONTH(born_at) AS birthMonth, YEAR(born_at) AS birthYear
      ||FROM people
      |""${'"'}.trimMargin()) { cursor ->
      |  check(cursor is ${dialect.dialect.cursorType})
      |  mapper(
      |    cursor.getLong(0)!!,
      |    cursor.getLong(1)!!
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test
  fun `proper exposure of math functions`(dialect: TestDialect) {
    assumeTrue(dialect in listOf(TestDialect.MYSQL))
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE math (
      |  angle INTEGER NOT NULL
      |);
      |
      |selectSomeTrigValues:
      |SELECT SIN(angle) AS sin, COS(angle) AS cos, TAN(angle) AS tan
      |FROM math;
      |""".trimMargin(),
      tempFolder, dialect = dialect.dialect
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectSomeTrigValues(mapper: (
      |  sin: kotlin.Double,
      |  cos: kotlin.Double,
      |  tan: kotlin.Double,
      |) -> T): app.cash.sqldelight.Query<T> = app.cash.sqldelight.Query(${query.id}, arrayOf("math"), driver, "Test.sq", "selectSomeTrigValues", ""${'"'}
      ||SELECT SIN(angle) AS sin, COS(angle) AS cos, TAN(angle) AS tan
      ||FROM math
      |""${'"'}.trimMargin()) { cursor ->
      |  check(cursor is ${dialect.dialect.cursorType})
      |  mapper(
      |    cursor.getDouble(0)!!,
      |    cursor.getDouble(1)!!,
      |    cursor.getDouble(2)!!
      |  )
      |}
      |""".trimMargin()
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
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedExecutes.first()
    val generator = ExecuteQueryGenerator(query)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertTwice(`value`: kotlin.Long): kotlin.Unit {
      |  driver.execute(${query.idForIndex(0)}, ""${'"'}
      |      |INSERT INTO data (value)
      |      |  VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindLong(1, value)
      |      }
      |  driver.execute(${query.idForIndex(1)}, ""${'"'}
      |      |INSERT INTO data (value)
      |      |  VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindLong(1, value)
      |      }
      |  notifyQueries(-609468782) { emit ->
      |    emit("data")
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test
  fun `grouped statements same column`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  value INTEGER NOT NULL
      |);
      |
      |insertTwice {
      |  INSERT INTO data (value)
      |  VALUES (?)
      |  ;
      |  INSERT INTO data (value)
      |  VALUES (?)
      |  ;
      |}
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedExecutes.first()
    val generator = ExecuteQueryGenerator(query)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertTwice(value_: kotlin.Long, value__: kotlin.Long): kotlin.Unit {
      |  driver.execute(${query.idForIndex(0)}, ""${'"'}
      |      |INSERT INTO data (value)
      |      |  VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindLong(1, value_)
      |      }
      |  driver.execute(${query.idForIndex(1)}, ""${'"'}
      |      |INSERT INTO data (value)
      |      |  VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindLong(1, value__)
      |      }
      |  notifyQueries(-609468782) { emit ->
      |    emit("data")
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test
  fun `grouped statements same index`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  value INTEGER NOT NULL
      |);
      |
      |insertTwice {
      |  INSERT INTO data (value)
      |  VALUES (?1)
      |  ;
      |  INSERT INTO data (value)
      |  VALUES (?1)
      |  ;
      |}
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedExecutes.first()
    val generator = ExecuteQueryGenerator(query)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertTwice(value_: kotlin.Long): kotlin.Unit {
      |  driver.execute(${query.idForIndex(0)}, ""${'"'}
      |      |INSERT INTO data (value)
      |      |  VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindLong(1, value_)
      |      }
      |  driver.execute(${query.idForIndex(1)}, ""${'"'}
      |      |INSERT INTO data (value)
      |      |  VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindLong(1, value_)
      |      }
      |  notifyQueries(-609468782) { emit ->
      |    emit("data")
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test
  fun `grouped statements different index`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  value INTEGER NOT NULL
      |);
      |
      |insertTwice {
      |  INSERT INTO data (value)
      |  VALUES (?1)
      |  ;
      |  INSERT INTO data (value)
      |  VALUES (?2)
      |  ;
      |}
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedExecutes.first()
    val generator = ExecuteQueryGenerator(query)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertTwice(value_: kotlin.Long, value__: kotlin.Long): kotlin.Unit {
      |  driver.execute(${query.idForIndex(0)}, ""${'"'}
      |      |INSERT INTO data (value)
      |      |  VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindLong(1, value_)
      |      }
      |  driver.execute(${query.idForIndex(1)}, ""${'"'}
      |      |INSERT INTO data (value)
      |      |  VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindLong(1, value__)
      |      }
      |  notifyQueries(-609468782) { emit ->
      |    emit("data")
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test
  fun `grouped statements that notifies`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data (
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  value INTEGER NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data;
      |
      |insertTwice {
      |  INSERT INTO data (value)
      |  VALUES (?1)
      |  ;
      |  INSERT INTO data (value)
      |  VALUES (?2)
      |  ;
      |}
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedExecutes.first()
    val generator = ExecuteQueryGenerator(query)

    assertThat(generator.function().toString()).isEqualTo(
      """
      |public fun insertTwice(value_: kotlin.Long, value__: kotlin.Long): kotlin.Unit {
      |  driver.execute(${query.idForIndex(0)}, ""${'"'}
      |      |INSERT INTO data (value)
      |      |  VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindLong(1, value_)
      |      }
      |  driver.execute(${query.idForIndex(1)}, ""${'"'}
      |      |INSERT INTO data (value)
      |      |  VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        bindLong(1, value__)
      |      }
      |  notifyQueries(${query.id}) { emit ->
      |    emit("data")
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test
  fun `proper exposure of case arguments function`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE data(
      |  r1 TEXT,
      |  r2 TEXT,
      |  r3 TEXT
      |);
      |
      |selectCase:
      |SELECT CASE :param1 WHEN 'test' THEN r1 WHEN 'test1' THEN r2 ELSE r3 END,
      |       CASE WHEN :param2='test' THEN r1 WHEN :param2='test1' THEN r2 ELSE r3 END
      |FROM data;
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo(
      """
      |public fun <T : kotlin.Any> selectCase(
      |  param1: kotlin.String,
      |  param2: kotlin.String,
      |  mapper: (expr: kotlin.String?, expr_: kotlin.String?) -> T,
      |): app.cash.sqldelight.Query<T> = SelectCaseQuery(param1, param2) { cursor ->
      |  mapper(
      |    cursor.getString(0),
      |    cursor.getString(1)
      |  )
      |}
      |""".trimMargin()
    )
  }

  @Test fun `don't extract variable for duplicate array parameters`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE ComboData (
      |  value TEXT AS ComboEnum NOT NULL
      |);
      |
      |CREATE TABLE ComboData2 (
      |  value TEXT AS ComboEnum NOT NULL
      |);
      |
      |countRecords:
      |SELECT (SELECT count(*) FROM ComboData WHERE value IN :values) +
      |(SELECT count(*) FROM ComboData2 WHERE value IN :values);
      |""".trimMargin(),
      tempFolder, fileName = "Data.sq"
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
        |private inner class CountRecordsQuery<out T : kotlin.Any>(
        |  public val values: kotlin.collections.Collection<ComboEnum>,
        |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
        |) : app.cash.sqldelight.Query<T>(mapper) {
        |  public override fun addListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
        |    driver.addListener(listener, arrayOf("ComboData", "ComboData2"))
        |  }
        |
        |  public override fun removeListener(listener: app.cash.sqldelight.Query.Listener): kotlin.Unit {
        |    driver.removeListener(listener, arrayOf("ComboData", "ComboData2"))
        |  }
        |
        |  public override fun execute(): app.cash.sqldelight.db.SqlCursor {
        |    val valuesIndexes = createArguments(count = values.size)
        |    return driver.executeQuery(null, ""${'"'}
        |        |SELECT (SELECT count(*) FROM ComboData WHERE value IN ${"$"}valuesIndexes) +
        |        |(SELECT count(*) FROM ComboData2 WHERE value IN ${"$"}valuesIndexes)
        |        ""${'"'}.trimMargin(), values.size + values.size) {
        |          values.forEachIndexed { index, values_ ->
        |            bindString(index + 1, ComboDataAdapter.value_Adapter.encode(values_))
        |          }
        |          values.forEachIndexed { index, values__ ->
        |            bindString(index + values.size + 1, ComboDataAdapter.value_Adapter.encode(values__))
        |          }
        |        }
        |  }
        |
        |  public override fun toString(): kotlin.String = "Data.sq:countRecords"
        |}
        |""".trimMargin()
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
      |""".trimMargin(),
      tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo(
      """
      |private inner class InsertAndReturnQuery<out T : kotlin.Any>(
      |  public val value_: kotlin.Long,
      |  mapper: (app.cash.sqldelight.db.SqlCursor) -> T,
      |) : app.cash.sqldelight.ExecutableQuery<T>(mapper) {
      |  public override fun execute(): app.cash.sqldelight.db.SqlCursor {
      |    driver.execute(${query.idForIndex(0)}, ""${'"'}
      |        |INSERT INTO data (value)
      |        |  VALUES (?)
      |        ""${'"'}.trimMargin(), 1) {
      |          bindLong(1, value_)
      |        }
      |    return driver.executeQuery(${query.idForIndex(1)}, ""${'"'}
      |        |SELECT value
      |        |  FROM data
      |        |  WHERE id = last_insert_rowid()
      |        ""${'"'}.trimMargin(), 0)
      |  }
      |
      |  public override fun toString(): kotlin.String = "Test.sq:insertAndReturn"
      |}
      |""".trimMargin()
    )
  }
}
