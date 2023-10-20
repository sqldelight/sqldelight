package app.cash.sqldelight.core.queries

import app.cash.sqldelight.core.TestDialect
import app.cash.sqldelight.core.TestDialect.SQLITE_3_18
import app.cash.sqldelight.core.compiler.MutatorQueryGenerator
import app.cash.sqldelight.core.compiler.SelectQueryGenerator
import app.cash.sqldelight.core.dialects.binderCheck
import app.cash.sqldelight.core.dialects.cursorCheck
import app.cash.sqldelight.core.dialects.textType
import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.withUnderscores
import com.google.common.truth.Truth.assertThat
import com.squareup.burst.BurstJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(BurstJUnit4::class)
class JavadocTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `select - properly formatted javadoc`(testDialect: TestDialect) {
    val file = FixtureCompiler.parseSql(
      createTable(testDialect) + """
      |/**
      | * Queries all values.
      | */
      |selectAll:
      |SELECT *
      |FROM test;
      |
      """.trimMargin(),
      tempFolder,
      dialect = testDialect.dialect,
    )

    val selectGenerator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |/**
      | * Queries all values.
      | */
      |public fun selectAll(): app.cash.sqldelight.Query<com.example.Test> = selectAll { _id, value_ ->
      |  com.example.Test(
      |    _id,
      |    value_
      |  )
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `select - properly formatted javadoc when there are two`(testDialect: TestDialect) {
    val file = FixtureCompiler.parseSql(
      createTable(testDialect) + """
      |/**
      | * Queries all values.
      | */
      |selectAll:
      |SELECT *
      |FROM test;
      |
      |/**
      | * Queries all values.
      | */
      |selectAll2:
      |SELECT *
      |FROM test;
      |
      """.trimMargin(),
      tempFolder,
      dialect = testDialect.dialect,
    )

    val selectGenerator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |/**
      | * Queries all values.
      | */
      |public fun selectAll(): app.cash.sqldelight.Query<com.example.Test> = selectAll { _id, value_ ->
      |  com.example.Test(
      |    _id,
      |    value_
      |  )
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `select - multiline javadoc`(testDialect: TestDialect) {
    val file = FixtureCompiler.parseSql(
      createTable(testDialect) + """
      |/**
      | * Queries all values.
      | * Returns values as a List.
      | *
      | * @deprecated Don't use it!
      | */
      |selectAll:
      |SELECT *
      |FROM test;
      |
      """.trimMargin(),
      tempFolder,
      dialect = testDialect.dialect,
    )

    val selectGenerator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |/**
      | * Queries all values.
      | * Returns values as a List.
      | *
      | * @deprecated Don't use it!
      | */
      |public fun selectAll(): app.cash.sqldelight.Query<com.example.Test> = selectAll { _id, value_ ->
      |  com.example.Test(
      |    _id,
      |    value_
      |  )
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `select - javadoc containing star symbols`(testDialect: TestDialect) {
    val file = FixtureCompiler.parseSql(
      createTable(testDialect) + """
      |/**
      | * Queries all values. **
      | * Returns values as a * List.
      | *
      | * ** @deprecated Don't use it!
      | */
      |selectAll:
      |SELECT *
      |FROM test;
      |
      """.trimMargin(),
      tempFolder,
      dialect = testDialect.dialect,
    )

    val selectGenerator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |/**
      | * Queries all values. **
      | * Returns values as a * List.
      | *
      | * ** @deprecated Don't use it!
      | */
      |public fun selectAll(): app.cash.sqldelight.Query<com.example.Test> = selectAll { _id, value_ ->
      |  com.example.Test(
      |    _id,
      |    value_
      |  )
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `select - single line javadoc`(testDialect: TestDialect) {
    val file = FixtureCompiler.parseSql(
      createTable(testDialect) + """
      |/** Queries all values. */
      |selectAll:
      |SELECT CAST(:input AS ${testDialect.textType});
      |
      """.trimMargin(),
      tempFolder,
      dialect = testDialect.dialect,
    )

    val selectGenerator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |/**
      | * Queries all values.
      | */
      |public fun selectAll(input: kotlin.String?): app.cash.sqldelight.ExecutableQuery<com.example.SelectAll> = selectAll(input) { expr ->
      |  com.example.SelectAll(
      |    expr
      |  )
      |}
      |
      """.trimMargin(),
    )

    assertThat(selectGenerator.customResultTypeFunction().toString()).isEqualTo(
      """
      |/**
      | * Queries all values.
      | */
      |public fun <T : kotlin.Any> selectAll(input: kotlin.String?, mapper: (expr: kotlin.String?) -> T): app.cash.sqldelight.ExecutableQuery<T> = SelectAllQuery(input) { cursor ->
      |  ${testDialect.cursorCheck(2)}mapper(
      |    cursor.getString(0)
      |  )
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `select - misformatted javadoc`(testDialect: TestDialect) {
    val file = FixtureCompiler.parseSql(
      createTable(testDialect) + """
      |/**
      |Queries all values.
      |*/
      |selectAll:
      |SELECT *
      |FROM test;
      |
      """.trimMargin(),
      tempFolder,
      dialect = testDialect.dialect,
    )

    val selectGenerator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(selectGenerator.defaultResultTypeFunction().toString()).isEqualTo(
      """
      |/**
      | * Queries all values.
      | */
      |public fun selectAll(): app.cash.sqldelight.Query<com.example.Test> = selectAll { _id, value_ ->
      |  com.example.Test(
      |    _id,
      |    value_
      |  )
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `insert`(testDialect: TestDialect) {
    val file = FixtureCompiler.parseSql(
      createTable(testDialect) + """
      |/**
      | * Insert new value.
      | */
      |insertValue:
      |INSERT INTO test(value)
      |VALUES (?);
      |
      """.trimMargin(),
      tempFolder,
      dialect = testDialect.dialect,
    )

    val insert = file.namedMutators.first()
    val insertGenerator = MutatorQueryGenerator(insert)

    assertThat(insertGenerator.function().toString()).isEqualTo(
      """
      |/**
      | * Insert new value.
      | */
      |public fun insertValue(value_: kotlin.String) {
      |  driver.execute(${insert.id.withUnderscores}, ""${'"'}
      |      |INSERT INTO test(value)
      |      |VALUES (?)
      |      ""${'"'}.trimMargin(), 1) {
      |        ${testDialect.binderCheck}bindString(0, value_)
      |      }
      |  notifyQueries(${insert.id.withUnderscores}) { emit ->
      |    emit("test")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `update`() {
    val file = FixtureCompiler.parseSql(
      createTable() + """
      |/**
      | * Update value by id.
      | */
      |updateById:
      |UPDATE test
      |SET value = ?
      |WHERE _id = ?;
      |
      """.trimMargin(),
      tempFolder,
    )

    val update = file.namedMutators.first()
    val updateGenerator = MutatorQueryGenerator(update)

    assertThat(updateGenerator.function().toString()).isEqualTo(
      """
      |/**
      | * Update value by id.
      | */
      |public fun updateById(value_: kotlin.String, _id: kotlin.Long) {
      |  driver.execute(${update.id.withUnderscores}, ""${'"'}
      |      |UPDATE test
      |      |SET value = ?
      |      |WHERE _id = ?
      |      ""${'"'}.trimMargin(), 2) {
      |        bindString(0, value_)
      |        bindLong(1, _id)
      |      }
      |  notifyQueries(${update.id.withUnderscores}) { emit ->
      |    emit("test")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  @Test fun `delete`(testDialect: TestDialect) {
    val file = FixtureCompiler.parseSql(
      createTable(testDialect) + """
      |/**
      | * Delete all.
      | */
      |deleteAll:
      |DELETE FROM test;
      |
      """.trimMargin(),
      tempFolder,
      dialect = testDialect.dialect,
    )

    val delete = file.namedMutators.first()
    val deleteGenerator = MutatorQueryGenerator(delete)

    assertThat(deleteGenerator.function().toString()).isEqualTo(
      """
      |/**
      | * Delete all.
      | */
      |public fun deleteAll() {
      |  driver.execute(${delete.id.withUnderscores}, ""${'"'}DELETE FROM test""${'"'}, 0)
      |  notifyQueries(${delete.id.withUnderscores}) { emit ->
      |    emit("test")
      |  }
      |}
      |
      """.trimMargin(),
    )
  }

  companion object {
    private fun createTable(testDialect: TestDialect = SQLITE_3_18) = """
      |CREATE TABLE test (
      |  _id INTEGER NOT NULL PRIMARY KEY DEFAULT 0,
      |  value ${testDialect.textType} NOT NULL
      |);
      |
    """.trimMargin()
  }
}
