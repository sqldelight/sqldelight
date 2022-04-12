package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.TestDialect
import app.cash.sqldelight.core.compiler.QueryInterfaceGenerator
import app.cash.sqldelight.core.compiler.SqlDelightCompiler
import app.cash.sqldelight.core.compiler.TableInterfaceGenerator
import app.cash.sqldelight.dialects.postgresql.PostgreSqlDialect
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.withInvariantLineSeparators
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class InterfaceGeneration {
  @get:Rule val temporaryFolder = TemporaryFolder()

  @Test fun noUniqueQueries() {
    checkFixtureCompiles("no-unique-queries")
  }

  @Test fun queryRequiresType() {
    checkFixtureCompiles("query-requires-type")
  }

  @Test fun `left joins apply nullability to columns`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE A(
      |  val1 TEXT NOT NULL
      |);
      |
      |CREATE TABLE B(
      |  val2 TEXT NOT NULL
      |);
      |
      |leftJoin:
      |SELECT *
      |FROM A LEFT OUTER JOIN B;
    """.trimMargin(),
      temporaryFolder
    )

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class LeftJoin(
      |  public val val1: kotlin.String,
      |  public val val2: kotlin.String?,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `duplicated column name uses table prefix`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE A(
      |  value TEXT NOT NULL
      |);
      |
      |CREATE TABLE B(
      |  value TEXT NOT NULL
      |);
      |
      |leftJoin:
      |SELECT *
      |FROM A JOIN B;
    """.trimMargin(),
      temporaryFolder
    )

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class LeftJoin(
      |  public val value_: kotlin.String,
      |  public val value__: kotlin.String,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `incompatible adapter types revert to sqlite types`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |CREATE TABLE B(
      |  value TEXT AS kotlin.collections.Set
      |);
      |
      |unionOfBoth:
      |SELECT value, value
      |FROM A
      |UNION
      |SELECT value, value
      |FROM B;
    """.trimMargin(),
      temporaryFolder
    )

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class UnionOfBoth(
      |  public val value_: kotlin.String?,
      |  public val value__: kotlin.String?,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `compatible adapter types merges nullability`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List NOT NULL
      |);
      |
      |unionOfBoth:
      |SELECT value, value
      |FROM A
      |UNION
      |SELECT value, nullif(value, 1 == 1)
      |FROM A;
    """.trimMargin(),
      temporaryFolder
    )

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class UnionOfBoth(
      |  public val value_: kotlin.collections.List,
      |  public val value__: kotlin.collections.List?,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `compatible adapter types from different columns merges nullability`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List NOT NULL
      |);
      |
      |CREATE TABLE B(
      |  value TEXT AS kotlin.collections.List NOT NULL
      |);
      |
      |unionOfBoth:
      |SELECT value, value
      |FROM A
      |UNION
      |SELECT value, nullif(value, 1 == 1)
      |FROM B;
    """.trimMargin(),
      temporaryFolder
    )

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class UnionOfBoth(
      |  public val value_: kotlin.collections.List,
      |  public val value__: kotlin.collections.List?,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `null type uses the other column in a union`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List
      |);
      |
      |unionOfBoth:
      |SELECT value, NULL
      |FROM A
      |UNION
      |SELECT NULL, value
      |FROM A;
    """.trimMargin(),
      temporaryFolder
    )

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class UnionOfBoth(
      |  public val value_: kotlin.collections.List?,
      |  public val expr: kotlin.collections.List?,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `argument type uses the other column in a union`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE A(
      |  value TEXT AS kotlin.collections.List NOT NULL
      |);
      |
      |unionOfBoth:
      |SELECT value, ?
      |FROM A
      |UNION
      |SELECT value, value
      |FROM A;
    """.trimMargin(),
      temporaryFolder
    )

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class UnionOfBoth(
      |  public val value_: kotlin.collections.List,
      |  public val expr: kotlin.collections.List,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `union with enum adapter required works fine`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE TestBModel (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  name TEXT NOT NULL,
      |  address TEXT NOT NULL
      |);
      |CREATE TABLE TestAModel (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  name TEXT NOT NULL,
      |  address TEXT NOT NULL,
      |  status TEXT AS TestADbModel.Status NOT NULL
      |);
      |
      |select_all:
      |SELECT *
      |FROM TestAModel
      |JOIN TestBModel ON TestAModel.name = TestBModel.name
      |
      |UNION
      |
      |SELECT *
      |FROM TestAModel
      |JOIN TestBModel ON TestAModel.address = TestBModel.address;
    """.trimMargin(),
      temporaryFolder
    )

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class Select_all(
      |  public val _id: kotlin.Long,
      |  public val name: kotlin.String,
      |  public val address: kotlin.String,
      |  public val status: TestADbModel.Status,
      |  public val _id_: kotlin.Long,
      |  public val name_: kotlin.String,
      |  public val address_: kotlin.String,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `non null column unioned with null in view`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE TestAModel (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  name TEXT NOT NULL
      |);
      |CREATE TABLE TestBModel (
      |  _id INTEGER NOT NULL PRIMARY KEY,
      |  nameB TEXT NOT NULL
      |);
      |
      |CREATE VIEW joined AS
      |SELECT _id, name, NULL AS nameB
      |FROM TestAModel
      |
      |UNION
      |
      |SELECT _id, NULL, nameB
      |FROM TestBModel;
      |
      |selectFromView:
      |SELECT name, nameB
      |FROM joined;
    """.trimMargin(),
      temporaryFolder
    )

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class SelectFromView(
      |  public val name: kotlin.String?,
      |  public val nameB: kotlin.String?,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `abstract class doesnt override kotlin functions unprepended by get`() {
    val result = FixtureCompiler.compileSql(
      """
      |someSelect:
      |SELECT '1' AS is_cool, '2' AS get_cheese, '3' AS stuff;
      |""".trimMargin(),
      temporaryFolder
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/SomeSelect.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import kotlin.String
      |
      |public data class SomeSelect(
      |  public val is_cool: String,
      |  public val get_cheese: String,
      |  public val stuff: String,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `adapted column in inner query`() {
    val result = FixtureCompiler.compileSql(
      """
      |import com.example.Test;
      |
      |CREATE TABLE testA (
      |  id TEXT NOT NULL PRIMARY KEY,
      |  status TEXT AS Test.Status,
      |  attr TEXT
      |);
      |
      |someSelect:
      |SELECT *
      |FROM (
      |  SELECT *, 1 AS ordering
      |  FROM testA
      |  WHERE testA.attr IS NOT NULL
      |
      |  UNION
      |
      |  SELECT *, 2 AS ordering
      |         FROM testA
      |  WHERE testA.attr IS NULL
      |);
      |""".trimMargin(),
      temporaryFolder
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/SomeSelect.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import kotlin.Long
      |import kotlin.String
      |
      |public data class SomeSelect(
      |  public val id: String,
      |  public val status: Test.Status?,
      |  public val attr: String?,
      |  public val ordering: Long,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `virtual table with tokenizer has correct types`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE VIRTUAL TABLE entity_fts USING fts4 (
      |  tokenize=simple X "${'$'} *&#%\'""\/(){}\[]|=+-_,:;<>-?!\t\r\n",
      |  text_content TEXT
      |);
      |
      |someSelect:
      |SELECT text_content, 1
      |FROM entity_fts;
      |""".trimMargin(),
      temporaryFolder
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/SomeSelect.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import kotlin.Long
      |import kotlin.String
      |
      |public data class SomeSelect(
      |  public val text_content: String?,
      |  public val expr: Long,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `fts5 virtual table with tokenizer has correct types`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE VIRTUAL TABLE entity_fts USING fts5 (
      |  text_content TEXT,
      |  prefix='2 3 4 5 6 7',
      |  content_rowid=id
      |);
      |
      |someSelect:
      |SELECT text_content, 1
      |FROM entity_fts;
      |""".trimMargin(),
      temporaryFolder
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/SomeSelect.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import kotlin.Long
      |import kotlin.String
      |
      |public data class SomeSelect(
      |  public val text_content: String?,
      |  public val expr: Long,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `adapted column in foreign table exposed properly`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE testA (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  parent_id INTEGER NOT NULL,
      |  child_id INTEGER NOT NULL,
      |  FOREIGN KEY (parent_id) REFERENCES testB(_id),
      |  FOREIGN KEY (child_id) REFERENCES testB(_id)
      |);
      |
      |CREATE TABLE testB(
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  category TEXT AS java.util.List NOT NULL,
      |  type TEXT AS java.util.List NOT NULL,
      |  name TEXT NOT NULL
      |);
      |
      |exact_match:
      |SELECT *
      |FROM testA
      |JOIN testB AS parentJoined ON parent_id = parentJoined._id
      |JOIN testB AS childJoined ON child_id = childJoined._id
      |WHERE parent_id = ? AND child_id = ?;
      """.trimMargin(),
      temporaryFolder
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/Exact_match.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import java.util.List
      |import kotlin.Long
      |import kotlin.String
      |
      |public data class Exact_match(
      |  public val _id: Long,
      |  public val parent_id: Long,
      |  public val child_id: Long,
      |  public val _id_: Long,
      |  public val category: List,
      |  public val type: List,
      |  public val name: String,
      |  public val _id__: Long,
      |  public val category_: List,
      |  public val type_: List,
      |  public val name_: String,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `avg aggregate has proper nullable type`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE test (
      |  integer_value INTEGER NOT NULL,
      |  real_value REAL NOT NULL,
      |  nullable_real_value REAL
      |);
      |
      |average:
      |SELECT
      |  avg(integer_value) AS avg_integer_value,
      |  avg(real_value) AS avg_real_value,
      |  avg(nullable_real_value) AS avg_nullable_real_value
      |FROM test;
      |""".trimMargin(),
      temporaryFolder
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/Average.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import kotlin.Double
      |
      |public data class Average(
      |  public val avg_integer_value: Double?,
      |  public val avg_real_value: Double?,
      |  public val avg_nullable_real_value: Double?,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `group_concat properly inherits nullability with nullable column`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE target (
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  coacheeId INTEGER NOT NULL,
      |  name TEXT NOT NULL
      |);
      |
      |CREATE TABLE challengeTarget (
      |  targetId INTEGER NOT NULL,
      |  challengeId INTEGER NOT NULL
      |);
      |
      |CREATE TABLE challenge (
      |  id INTEGER NOT NULL,
      |  cancelledAt INTEGER,
      |  emoji TEXT NOT NULL
      |);
      |
      |targetWithEmojis:
      |SELECT target.id AS id, target.name AS name, GROUP_CONCAT(challenge.emoji, "") AS emojis
      |  FROM target
      |  LEFT JOIN challengeTarget
      |    ON challengeTarget.targetId = target.id
      |  LEFT JOIN challenge
      |    ON challengeTarget.challengeId = challenge.id AND challenge.cancelledAt IS NULL
      |  WHERE target.coacheeId = ?
      |  GROUP BY 1
      |  ORDER BY target.name COLLATE NOCASE ASC
      |;
    """.trimMargin(),
      temporaryFolder
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/TargetWithEmojis.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import kotlin.Long
      |import kotlin.String
      |
      |public data class TargetWithEmojis(
      |  public val id: Long,
      |  public val name: String,
      |  public val emojis: String?,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `group_concat properly inherits nullability`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE target (
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  coacheeId INTEGER NOT NULL,
      |  name TEXT NOT NULL
      |);
      |
      |CREATE TABLE challengeTarget (
      |  targetId INTEGER NOT NULL,
      |  challengeId INTEGER NOT NULL
      |);
      |
      |CREATE TABLE challenge (
      |  id INTEGER NOT NULL,
      |  cancelledAt INTEGER,
      |  emoji TEXT
      |);
      |
      |targetWithEmojis:
      |SELECT target.id AS id, target.name AS name, GROUP_CONCAT(challenge.emoji, "") AS emojis
      |  FROM target
      |  LEFT JOIN challengeTarget
      |    ON challengeTarget.targetId = target.id
      |  LEFT JOIN challenge
      |    ON challengeTarget.challengeId = challenge.id AND challenge.cancelledAt IS NULL
      |  WHERE target.coacheeId = ?
      |  GROUP BY 1
      |  ORDER BY target.name COLLATE NOCASE ASC
      |;
    """.trimMargin(),
      temporaryFolder
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/TargetWithEmojis.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import kotlin.Long
      |import kotlin.String
      |
      |public data class TargetWithEmojis(
      |  public val id: Long,
      |  public val name: String,
      |  public val emojis: String?,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `cast inherits nullability`() {
    val file = FixtureCompiler.parseSql(
      """
      |CREATE TABLE example (
      |  foo TEXT
      |);
      |
      |selectWithCast:
      |SELECT
      |  foo,
      |  CAST(foo AS BLOB) AS bar
      |FROM example;
    """.trimMargin(),
      temporaryFolder
    )

    val query = file.namedQueries.first()
    assertThat(QueryInterfaceGenerator(query).kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class SelectWithCast(
      |  public val foo: kotlin.String?,
      |  public val bar: kotlin.ByteArray?,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `annotations do not require an adapter`() {
    val file = FixtureCompiler.parseSql(
      """
      |import java.lang.Deprecated;
      |import kotlin.String;
      |
      |CREATE TABLE category (
      |  accent_color TEXT AS @Deprecated String,
      |  other_thing TEXT AS @Deprecated String NOT NULL
      |);
      """.trimMargin(),
      temporaryFolder, dialect = TestDialect.MYSQL.dialect
    )

    val query = file.tables(false).single()
    val generator = TableInterfaceGenerator(query)

    assertThat(generator.kotlinImplementationSpec().toString()).isEqualTo(
      """
      |public data class Category(
      |  @java.lang.Deprecated
      |  public val accent_color: kotlin.String?,
      |  @java.lang.Deprecated
      |  public val other_thing: kotlin.String,
      |)
      |""".trimMargin()
    )
  }

  @Test fun `query does not return type of unrelated view`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE VIEW first_song_in_album AS
      |SELECT * FROM song WHERE track_number = 1;
      |
      |CREATE TABLE song(
      |    title TEXT,
      |    track_number INTEGER,
      |    album_id INTEGER
      |);
      |
      |selectSongsByAlbumId:
      |SELECT * FROM song WHERE album_id = ?;
    """.trimMargin(),
      temporaryFolder, fileName = "song.sq"
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/SongQueries.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.db.SqlDriver
      |import kotlin.Any
      |import kotlin.Long
      |import kotlin.String
      |import kotlin.Unit
      |
      |public class SongQueries(
      |  private val driver: SqlDriver,
      |) : TransacterImpl(driver) {
      |  public fun <T : Any> selectSongsByAlbumId(album_id: Long?, mapper: (
      |    title: String?,
      |    track_number: Long?,
      |    album_id: Long?,
      |  ) -> T): Query<T> = SelectSongsByAlbumIdQuery(album_id) { cursor ->
      |    mapper(
      |      cursor.getString(0),
      |      cursor.getLong(1),
      |      cursor.getLong(2)
      |    )
      |  }
      |
      |  public fun selectSongsByAlbumId(album_id: Long?): Query<Song> = selectSongsByAlbumId(album_id) {
      |      title, track_number, album_id_ ->
      |    Song(
      |      title,
      |      track_number,
      |      album_id_
      |    )
      |  }
      |
      |  private inner class SelectSongsByAlbumIdQuery<out T : Any>(
      |    public val album_id: Long?,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    public override fun addListener(listener: Query.Listener): Unit {
      |      driver.addListener(listener, arrayOf("song"))
      |    }
      |
      |    public override fun removeListener(listener: Query.Listener): Unit {
      |      driver.removeListener(listener, arrayOf("song"))
      |    }
      |
      |    public override fun <R> execute(mapper: (SqlCursor) -> R): R = driver.executeQuery(null,
      |        ""${'"'}SELECT * FROM song WHERE album_id ${'$'}{ if (album_id == null) "IS" else "=" } ?""${'"'}, mapper,
      |        1) {
      |      bindLong(1, album_id)
      |    }
      |
      |    public override fun toString(): String = "song.sq:selectSongsByAlbumId"
      |  }
      |}
      |""".trimMargin()
    )
  }

  @Test fun `returning statement in select works fine`() {
    val result = FixtureCompiler.compileSql(
      """
      |CREATE TABLE IF NOT EXISTS userEntity (
      |    user_id SERIAL PRIMARY KEY,
      |    slack_user_id VARCHAR NOT NULL
      |);
      |
      |CREATE TABLE IF NOT EXISTS subscriptionEntity (
      |    user_id2 SERIAL NOT NULL,
      |    FOREIGN KEY (user_id2) REFERENCES userEntity(user_id)
      |);
      |
      |insertSubscription:
      |INSERT INTO subscriptionEntity(user_id2)
      |VALUES (?);
      |
      |insertUser:
      |WITH inserted_ids AS (
      |  INSERT INTO userEntity(slack_user_id)
      |  VALUES (?)
      |  RETURNING user_id AS insert_id
      |) SELECT insert_id FROM inserted_ids;
    """.trimMargin(),
      temporaryFolder, fileName = "Subscription.sq", overrideDialect = PostgreSqlDialect()
    )

    assertThat(result.errors).isEmpty()
    val generatedInterface = result.compilerOutput.get(
      File(result.outputDirectory, "com/example/SubscriptionQueries.kt")
    )
    assertThat(generatedInterface).isNotNull()
    assertThat(generatedInterface.toString()).isEqualTo(
      """
      |package com.example
      |
      |import app.cash.sqldelight.Query
      |import app.cash.sqldelight.TransacterImpl
      |import app.cash.sqldelight.db.SqlCursor
      |import app.cash.sqldelight.driver.jdbc.JdbcCursor
      |import app.cash.sqldelight.driver.jdbc.JdbcDriver
      |import app.cash.sqldelight.driver.jdbc.JdbcPreparedStatement
      |import kotlin.Any
      |import kotlin.Int
      |import kotlin.String
      |import kotlin.Unit
      |
      |public class SubscriptionQueries(
      |  private val driver: JdbcDriver,
      |) : TransacterImpl(driver) {
      |  public fun insertUser(slack_user_id: String): Query<Int> = InsertUserQuery(slack_user_id) {
      |      cursor ->
      |    check(cursor is JdbcCursor)
      |    cursor.getLong(0)!!.toInt()
      |  }
      |
      |  public fun insertSubscription(user_id2: Int): Unit {
      |    driver.execute(${result.compiledFile.namedMutators[0].id}, ""${'"'}
      |        |INSERT INTO subscriptionEntity(user_id2)
      |        |VALUES (?)
      |        ""${'"'}.trimMargin(), 1) {
      |          check(this is JdbcPreparedStatement)
      |          bindLong(1, user_id2.toLong())
      |        }
      |    notifyQueries(${result.compiledFile.namedMutators[0].id}) { emit ->
      |      emit("subscriptionEntity")
      |    }
      |  }
      |
      |  private inner class InsertUserQuery<out T : Any>(
      |    public val slack_user_id: String,
      |    mapper: (SqlCursor) -> T,
      |  ) : Query<T>(mapper) {
      |    public override fun addListener(listener: Query.Listener): Unit {
      |      driver.addListener(listener, arrayOf("userEntity"))
      |    }
      |
      |    public override fun removeListener(listener: Query.Listener): Unit {
      |      driver.removeListener(listener, arrayOf("userEntity"))
      |    }
      |
      |    public override fun <R> execute(mapper: (SqlCursor) -> R): R = driver.executeQuery(${result.compiledFile.namedQueries[0].id},
      |        ""${'"'}
      |    |WITH inserted_ids AS (
      |    |  INSERT INTO userEntity(slack_user_id)
      |    |  VALUES (?)
      |    |  RETURNING user_id AS insert_id
      |    |) SELECT insert_id FROM inserted_ids
      |    ""${'"'}.trimMargin(), mapper, 1) {
      |      check(this is JdbcPreparedStatement)
      |      bindString(1, slack_user_id)
      |    }
      |
      |    public override fun toString(): String = "Subscription.sq:insertUser"
      |  }
      |}
      |""".trimMargin()
    )
  }

  private fun checkFixtureCompiles(fixtureRoot: String) {
    val result = FixtureCompiler.compileFixture(
      fixtureRoot = "src/test/query-interface-fixtures/$fixtureRoot",
      compilationMethod = { _, _, file, output ->
        SqlDelightCompiler.writeQueryInterfaces(file, output)
      },
      generateDb = false
    )
    for ((expectedFile, actualOutput) in result.compilerOutput) {
      assertWithMessage("No file with name $expectedFile").that(expectedFile.exists()).isTrue()
      assertWithMessage(expectedFile.name).that(actualOutput.toString())
        .isEqualTo(expectedFile.readText().withInvariantLineSeparators())
    }
  }
}
