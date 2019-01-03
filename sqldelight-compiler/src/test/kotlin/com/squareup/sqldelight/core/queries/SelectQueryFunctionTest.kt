package com.squareup.sqldelight.core.queries

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.compiler.SelectQueryGenerator
import com.squareup.sqldelight.test.util.FixtureCompiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SelectQueryFunctionTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `query function with default result type generates properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo("""
      |fun selectForId(id: kotlin.Long): com.squareup.sqldelight.Query<com.example.Data> = selectForId(id, com.example.Data::Impl)
      |""".trimMargin())
  }

  @Test fun `query bind args appear in the correct order`() {
    val file = FixtureCompiler.parseSql("""
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
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo("""
      |fun select(value: kotlin.String, id: kotlin.Long): com.squareup.sqldelight.Query<com.example.Data> = select(value, id, com.example.Data::Impl)
      |""".trimMargin())
  }

  @Test fun `query function with custom result type generates properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
    |fun <T : kotlin.Any> selectForId(id: kotlin.Long, mapper: (id: kotlin.Long, value: kotlin.String) -> T): com.squareup.sqldelight.Query<T> = SelectForId(id) { cursor ->
    |    mapper(
    |        cursor.getLong(0)!!,
    |        cursor.getString(1)!!
    |    )
    |}
    |
      """.trimMargin())
  }

  @Test fun `custom result type query function uses adapter`() {
    val file = FixtureCompiler.parseSql("""
      |import kotlin.collections.List;
      |
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT AS List NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id = ?;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T : kotlin.Any> selectForId(id: kotlin.Long, mapper: (id: kotlin.Long, value: kotlin.collections.List) -> T): com.squareup.sqldelight.Query<T> = SelectForId(id) { cursor ->
      |    mapper(
      |        cursor.getLong(0)!!,
      |        queryWrapper.dataAdapter.valueAdapter.decode(cursor.getString(1)!!)
      |    )
      |}
      |
      """.trimMargin())
  }

  @Test fun `multiple values types are folded into proper result type`() {
    val file = FixtureCompiler.parseSql("""
      |selectValues:
      |VALUES (1), ('sup');
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).contains("""
      |fun selectValues(): com.squareup.sqldelight.Query<kotlin.String>
      """.trimMargin())
  }

  @Test fun `query with no parameters doesn't subclass Query`() {
    val file = FixtureCompiler.parseSql("""
      |import kotlin.collections.List;
      |
      |CREATE TABLE data (
      |  id INTEGER NOT NULL,
      |  value TEXT AS List NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T : kotlin.Any> selectForId(mapper: (id: kotlin.Long, value: kotlin.collections.List) -> T): com.squareup.sqldelight.Query<T> = com.squareup.sqldelight.Query(${query.id}, selectForId, database, ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |    mapper(
      |        cursor.getLong(0)!!,
      |        queryWrapper.dataAdapter.valueAdapter.decode(cursor.getString(1)!!)
      |    )
      |}
      |""".trimMargin())
  }

  @Test fun `integer primary key is always exposed as non-null`() {
    val file = FixtureCompiler.parseSql(
        """
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun selectData(): com.squareup.sqldelight.Query<kotlin.Long> = com.squareup.sqldelight.Query(${query.id}, selectData, database, ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |    cursor.getLong(0)!!
      |}
      |
      """.trimMargin())
  }

  @Test fun `bind parameter used in IN expression explodes into multiple query args`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER NOT NULL
      |);
      |
      |selectForId:
      |SELECT *
      |FROM data
      |WHERE id IN :good AND id NOT IN :bad;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class SelectForId<out T : kotlin.Any>(
      |    private val good: kotlin.collections.Collection<kotlin.Long>,
      |    private val bad: kotlin.collections.Collection<kotlin.Long>,
      |    mapper: (com.squareup.sqldelight.db.SqlCursor) -> T
      |) : com.squareup.sqldelight.Query<T>(selectForId, mapper) {
      |    override fun execute(): com.squareup.sqldelight.db.SqlCursor {
      |        val goodIndexes = createArguments(count = good.size, offset = 3)
      |        val badIndexes = createArguments(count = bad.size, offset = good.size + 3)
      |        return database.executeQuery(null, ""${'"'}
      |        |SELECT *
      |        |FROM data
      |        |WHERE id IN ${"$"}goodIndexes AND id NOT IN ${"$"}badIndexes
      |        ""${'"'}.trimMargin(), good.size + bad.size) {
      |            good.forEachIndexed { index, good ->
      |                    bindLong(index + 3, good)
      |                    }
      |            bad.forEachIndexed { index, bad ->
      |                    bindLong(index + good.size + 3, bad)
      |                    }
      |        }
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `limit and offset bind expressions gets proper types`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  some_column INTEGER NOT NULL,
      |  some_column2 INTEGER NOT NULL
      |);
      |
      |someSelect:
      |SELECT *
      |FROM data
      |WHERE EXISTS (SELECT * FROM data LIMIT :minimum OFFSET :offset)
      |LIMIT :minimum;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo("""
      |fun someSelect(minimum: kotlin.Long, offset: kotlin.Long): com.squareup.sqldelight.Query<com.example.Data> = someSelect(minimum, offset, com.example.Data::Impl)
      |""".trimMargin())
  }

  @Test fun `boolean column mapper from result set properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  id INTEGER PRIMARY KEY,
      |  value INTEGER AS Boolean
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T : kotlin.Any> selectData(mapper: (id: kotlin.Long, value: kotlin.Boolean?) -> T): com.squareup.sqldelight.Query<T> = com.squareup.sqldelight.Query(${query.id}, selectData, database, ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |    mapper(
      |        cursor.getLong(0)!!,
      |        cursor.getLong(1)?.let { it == 1L }
      |    )
      |}
      |
      """.trimMargin())
  }

  @Test fun `named bind arg can be reused`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE person (
      |  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  first_name TEXT NOT NULL,
      |  last_name TEXT NOT NULL
      |);
      |
      |equivalentNamesNamed:
      |SELECT *
      |FROM person
      |WHERE first_name = :name AND last_name = :name;
      """.trimMargin(), tempFolder
    )

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.querySubtype().toString()).isEqualTo("""
      |private inner class EquivalentNamesNamed<out T : kotlin.Any>(private val name: kotlin.String, mapper: (com.squareup.sqldelight.db.SqlCursor) -> T) : com.squareup.sqldelight.Query<T>(equivalentNamesNamed, mapper) {
      |    override fun execute(): com.squareup.sqldelight.db.SqlCursor = database.executeQuery(${query.id}, ""${'"'}
      |    |SELECT *
      |    |FROM person
      |    |WHERE first_name = ?1 AND last_name = ?1
      |    ""${'"'}.trimMargin(), 1) {
      |        bindString(1, name)
      |    }
      |}
      |
      """.trimMargin())
  }

  @Test fun `real is exposed properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  value REAL NOT NULL
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun selectData(): com.squareup.sqldelight.Query<kotlin.Double> = com.squareup.sqldelight.Query(${query.id}, selectData, database, ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |    cursor.getDouble(0)!!
      |}
      |
      """.trimMargin())
  }

  @Test fun `blob is exposed properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  value BLOB NOT NULL
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun selectData(): com.squareup.sqldelight.Query<kotlin.ByteArray> = com.squareup.sqldelight.Query(${query.id}, selectData, database, ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |    cursor.getBytes(0)!!
      |}
      |
      """.trimMargin())
  }

  @Test fun `null is exposed properly`() {
    val file = FixtureCompiler.parseSql("""
      |selectData:
      |SELECT NULL;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T : kotlin.Any> selectData(mapper: (expr: java.lang.Void?) -> T): com.squareup.sqldelight.Query<T> = com.squareup.sqldelight.Query(${query.id}, selectData, database, "SELECT NULL") { cursor ->
      |    mapper(
      |        null
      |    )
      |}
      |
      """.trimMargin())
  }

  @Test fun `non null boolean is exposed properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  value INTEGER AS Boolean NOT NULL
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun selectData(): com.squareup.sqldelight.Query<kotlin.Boolean> = com.squareup.sqldelight.Query(${query.id}, selectData, database, ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |    cursor.getLong(0)!! == 1L
      |}
      |
      """.trimMargin())
  }

  @Test fun `nonnull int is computed properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  value INTEGER AS Int NOT NULL
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun selectData(): com.squareup.sqldelight.Query<kotlin.Int> = com.squareup.sqldelight.Query(${query.id}, selectData, database, ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |    cursor.getLong(0)!!.toInt()
      |}
      |
      """.trimMargin())
  }

  @Test fun `nullable int is computed properly`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  value INTEGER AS Int
      |);
      |
      |selectData:
      |SELECT *
      |FROM data;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T : kotlin.Any> selectData(mapper: (value: kotlin.Int?) -> T): com.squareup.sqldelight.Query<T> = com.squareup.sqldelight.Query(${query.id}, selectData, database, ""${'"'}
      ||SELECT *
      ||FROM data
      |""${'"'}.trimMargin()) { cursor ->
      |    mapper(
      |        cursor.getLong(0)?.toInt()
      |    )
      |}
      |
      """.trimMargin())
  }

  @Test fun `query returns custom query type`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE data (
      |  value INTEGER,
      |  value2 INTEGER
      |);
      |
      |selectData:
      |SELECT coalesce(value, value2), value, value2
      |FROM data;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.defaultResultTypeFunction().toString()).isEqualTo("""
      |fun selectData(): com.squareup.sqldelight.Query<com.example.SelectData> = selectData(com.example.SelectData::Impl)
      |""".trimMargin())
  }

  @Test fun `optional parameter with type inferred from case expression`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE a (
      |  name TEXT NOT NULL
      |);
      |
      |broken:
      |SELECT a.name
      |FROM a
      |WHERE
      |  :input IS NULL
      |  OR :input =
      |    CASE name
      |      WHEN 'a' THEN 'b'
      |      ELSE name
      |    END;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun broken(input: kotlin.String?): com.squareup.sqldelight.Query<kotlin.String> = Broken(input) { cursor ->
      |    cursor.getString(0)!!
      |}
      |
      """.trimMargin())
  }

  @Test fun `projection with more columns than there are runtime Function types`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE bigTable (
      |  val1 INTEGER,
      |  val2 INTEGER,
      |  val3 INTEGER,
      |  val4 INTEGER,
      |  val5 INTEGER,
      |  val6 INTEGER,
      |  val7 INTEGER,
      |  val8 INTEGER,
      |  val9 INTEGER,
      |  val10 INTEGER,
      |  val11 INTEGER,
      |  val12 INTEGER,
      |  val13 INTEGER,
      |  val14 INTEGER,
      |  val15 INTEGER,
      |  val16 INTEGER,
      |  val17 INTEGER,
      |  val18 INTEGER,
      |  val19 INTEGER,
      |  val20 INTEGER,
      |  val21 INTEGER,
      |  val22 INTEGER,
      |  val23 INTEGER,
      |  val24 INTEGER,
      |  val25 INTEGER,
      |  val26 INTEGER,
      |  val27 INTEGER,
      |  val28 INTEGER,
      |  val29 INTEGER,
      |  val30 INTEGER
      |);
      |
      |select:
      |SELECT *
      |FROM bigTable;
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun select(): com.squareup.sqldelight.Query<com.example.BigTable> = com.squareup.sqldelight.Query(${query.id}, select, database, ""${'"'}
      ||SELECT *
      ||FROM bigTable
      |""${'"'}.trimMargin()) { cursor ->
      |    com.example.BigTable.Impl(
      |        cursor.getLong(0),
      |        cursor.getLong(1),
      |        cursor.getLong(2),
      |        cursor.getLong(3),
      |        cursor.getLong(4),
      |        cursor.getLong(5),
      |        cursor.getLong(6),
      |        cursor.getLong(7),
      |        cursor.getLong(8),
      |        cursor.getLong(9),
      |        cursor.getLong(10),
      |        cursor.getLong(11),
      |        cursor.getLong(12),
      |        cursor.getLong(13),
      |        cursor.getLong(14),
      |        cursor.getLong(15),
      |        cursor.getLong(16),
      |        cursor.getLong(17),
      |        cursor.getLong(18),
      |        cursor.getLong(19),
      |        cursor.getLong(20),
      |        cursor.getLong(21),
      |        cursor.getLong(22),
      |        cursor.getLong(23),
      |        cursor.getLong(24),
      |        cursor.getLong(25),
      |        cursor.getLong(26),
      |        cursor.getLong(27),
      |        cursor.getLong(28),
      |        cursor.getLong(29)
      |    )
      |}
      |""".trimMargin())
  }

  @Test fun `match expression`() {
    val file = FixtureCompiler.parseSql("""
      |CREATE TABLE item(
      |  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      |  packageName TEXT NOT NULL,
      |  className TEXT NOT NULL,
      |  deprecated INTEGER AS Boolean NOT NULL DEFAULT 0,
      |  link TEXT NOT NULL,
      |
      |  UNIQUE (packageName, className)
      |);
      |
      |CREATE VIRTUAL TABLE item_index USING fts4(content TEXT);
      |
      |queryTerm:
      |SELECT item.*
      |FROM item_index
      |JOIN item ON (docid = item.id)
      |WHERE content MATCH ?1
      |ORDER BY
      |  -- deprecated classes are always last
      |  deprecated ASC,
      |  CASE
      |    -- exact match
      |    WHEN className LIKE ?1 ESCAPE '\' THEN 1
      |    -- prefix match with no nested type
      |    WHEN className LIKE ?1 || '%' ESCAPE '\' AND instr(className, '.') = 0 THEN 2
      |    -- exact match on nested type
      |    WHEN className LIKE '%.' || ?1 ESCAPE '\' THEN 3
      |    -- prefix match (allowing nested types)
      |    WHEN className LIKE ?1 || '%' ESCAPE '\' THEN 4
      |    -- prefix match on nested type
      |    WHEN className LIKE '%.' || ?1 || '%' ESCAPE '\' THEN 5
      |    -- infix match
      |    ELSE 6
      |  END ASC,
      |  -- prefer "closer" matches based on length
      |  length(className) ASC,
      |  -- alphabetize to eliminate any remaining non-determinism
      |  packageName ASC,
      |  className ASC
      |LIMIT 50
      |;
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T : kotlin.Any> queryTerm(content: kotlin.String, mapper: (
      |    id: kotlin.Long,
      |    packageName: kotlin.String,
      |    className: kotlin.String,
      |    deprecated: kotlin.Boolean,
      |    link: kotlin.String
      |) -> T): com.squareup.sqldelight.Query<T> = QueryTerm(content) { cursor ->
      |    mapper(
      |        cursor.getLong(0)!!,
      |        cursor.getString(1)!!,
      |        cursor.getString(2)!!,
      |        cursor.getLong(3)!! == 1L,
      |        cursor.getString(4)!!
      |    )
      |}
      |
      """.trimMargin())
  }

  @Test fun `adapted column in inner query exposed in projection`() {
    val file = FixtureCompiler.parseSql("""
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
      """.trimMargin(), tempFolder)

    val query = file.namedQueries.first()
    val generator = SelectQueryGenerator(query)

    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T : kotlin.Any> someSelect(mapper: (
      |    id: kotlin.String,
      |    status: Test.Status?,
      |    attr: kotlin.String?,
      |    ordering: kotlin.Long
      |) -> T): com.squareup.sqldelight.Query<T> = com.squareup.sqldelight.Query(${query.id}, someSelect, database, ""${'"'}
      ||SELECT *
      ||FROM (
      ||  SELECT *, 1 AS ordering
      ||  FROM testA
      ||  WHERE testA.attr IS NOT NULL
      ||
      ||  UNION
      ||
      ||  SELECT *, 2 AS ordering
      ||         FROM testA
      ||  WHERE testA.attr IS NULL
      ||)
      |""${'"'}.trimMargin()) { cursor ->
      |    mapper(
      |        cursor.getString(0)!!,
      |        cursor.getString(1)?.let(queryWrapper.testAAdapter.statusAdapter::decode),
      |        cursor.getString(2),
      |        cursor.getLong(3)!!
      |    )
      |}
      |""".trimMargin())
  }

  @Test fun `adapted column in foreign table exposed properly`() {
    val file = FixtureCompiler.parseSql("""
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
      """.trimMargin(), tempFolder)

    val generator = SelectQueryGenerator(file.namedQueries.first())
    assertThat(generator.customResultTypeFunction().toString()).isEqualTo("""
      |fun <T : kotlin.Any> exact_match(
      |    parent_id: kotlin.Long,
      |    child_id: kotlin.Long,
      |    mapper: (
      |        _id: kotlin.Long,
      |        parent_id: kotlin.Long,
      |        child_id: kotlin.Long,
      |        _id_: kotlin.Long,
      |        category: java.util.List,
      |        type: java.util.List,
      |        name: kotlin.String,
      |        _id__: kotlin.Long,
      |        category_: java.util.List,
      |        type_: java.util.List,
      |        name_: kotlin.String
      |    ) -> T
      |): com.squareup.sqldelight.Query<T> = Exact_match(parent_id, child_id) { cursor ->
      |    mapper(
      |        cursor.getLong(0)!!,
      |        cursor.getLong(1)!!,
      |        cursor.getLong(2)!!,
      |        cursor.getLong(3)!!,
      |        queryWrapper.testBAdapter.categoryAdapter.decode(cursor.getString(4)!!),
      |        queryWrapper.testBAdapter.typeAdapter.decode(cursor.getString(5)!!),
      |        cursor.getString(6)!!,
      |        cursor.getLong(7)!!,
      |        queryWrapper.testBAdapter.categoryAdapter.decode(cursor.getString(8)!!),
      |        queryWrapper.testBAdapter.typeAdapter.decode(cursor.getString(9)!!),
      |        cursor.getString(10)!!
      |    )
      |}
      |
      """.trimMargin())
  }
}
