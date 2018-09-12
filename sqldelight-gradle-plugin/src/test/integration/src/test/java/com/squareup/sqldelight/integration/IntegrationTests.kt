package com.squareup.sqldelight.integration

import com.squareup.sqldelight.sqlite.driver.SqliteJdbcOpenHelper
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.ColumnAdapter
import java.util.Arrays
import java.util.concurrent.CountDownLatch
import org.junit.After
import org.junit.Before
import org.junit.Test

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.Assert.assertTrue

class IntegrationTests {
  private lateinit var queryWrapper: QueryWrapper
  private lateinit var personQueries: PersonQueries
  private lateinit var keywordsQueries: SqliteKeywordsQueries
  private lateinit var nullableTypesQueries: NullableTypesQueries
  private lateinit var bigTableQueries: BigTableQueries

  private object listAdapter : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> = databaseValue.split(",")
    override fun encode(value: List<String>): String = value.joinToString(",")
  }

  @Before fun before() {
    val database = SqliteJdbcOpenHelper()
    QueryWrapper.onCreate(database.getConnection())

    queryWrapper = QueryWrapper(database, NullableTypes.Adapter(listAdapter))
    personQueries = queryWrapper.personQueries
    keywordsQueries = queryWrapper.sqliteKeywordsQueries
    nullableTypesQueries = queryWrapper.nullableTypesQueries
    bigTableQueries = queryWrapper.bigTableQueries
  }

  @Test fun indexedArgs() {
    // ?1 is the only arg
    val person = personQueries.equivalentNames("Bob").executeAsOne()
    assertThat(person).isEqualTo(Person.Impl(4, "Bob", "Bob"))
  }

  @Test fun startIndexAtTwo() {
    // ?2 is the only arg
    val person = personQueries.equivalentNames2("Bob").executeAsOne()
    assertThat(person).isEqualTo(Person.Impl(4, "Bob", "Bob"))
  }

  @Test fun namedIndexArgs() {
    // :name is the only arg
    val person = personQueries.equivalentNamesNamed("Bob").executeAsOne()
    assertThat(person).isEqualTo(Person.Impl(4, "Bob", "Bob"))
  }

  @Test fun indexedArgLast() {
    // First arg declared is ?, second arg declared is ?1.
    val person = personQueries.indexedArgLast("Bob").executeAsOne()
    assertThat(person).isEqualTo(Person.Impl(4, "Bob", "Bob"))
  }

  @Test fun indexedArgLastTwo() {
    // First arg declared is ?, second arg declared is ?2.
    val person = personQueries.indexedArgLast2("Alec", "Strong").executeAsOne()
    assertThat(person).isEqualTo(Person.Impl(1, "Alec", "Strong"))
  }

  @Test fun nameIn() {
    val people = personQueries.nameIn(Arrays.asList("Alec", "Matt", "Jake")).executeAsList()
    assertThat(people).hasSize(3)
  }

  @Test fun sqliteKeywordQuery() {
    val keywords = keywordsQueries.selectAll().executeAsOne()
    assertThat(keywords).isEqualTo(_group_.Impl(1, 10, 20))
  }

  @Test fun compiledStatement() {
    keywordsQueries.insertStmt(11, 21)
    keywordsQueries.insertStmt(12, 22)

    var current: Long = 10
    for (group in keywordsQueries.selectAll().executeAsList()) {
      assertThat(group._where_).isEqualTo(current++)
    }
    assertThat(current).isEqualTo(13)
  }

  @Test @Throws(InterruptedException::class)
  fun compiledStatementAcrossThread() {
    keywordsQueries.insertStmt(11, 21)

    val latch = CountDownLatch(1)
    Thread(object : Runnable {
      override fun run() {
        keywordsQueries.insertStmt(12, 22)
        latch.countDown()
      }
    }).start()

    assertTrue(latch.await(10, SECONDS))

    var current: Long = 10
    for (group in keywordsQueries.selectAll().executeAsList()) {
      assertThat(group._where_).isEqualTo(current++)
    }
    assertThat(current).isEqualTo(13)
  }

  @Test
  fun nullableColumnsUseAdapterProperly() {
    val cool = NullableTypes.Impl(listOf("Alec", "Matt", "Jake"), "Cool")
    val notCool = NullableTypes.Impl(null, "Not Cool")
    val nulled = NullableTypes.Impl(null, null)
    nullableTypesQueries.insertNullableType(cool)
    nullableTypesQueries.insertNullableType(notCool)
    nullableTypesQueries.insertNullableType(nulled)

    assertThat(nullableTypesQueries.selectAll().executeAsList())
        .containsExactly(cool, notCool, nulled)
  }

  @Test fun multipleNameIn() {
    val people = personQueries.multipleNameIn(listOf("Alec", "Jesse"), listOf("Wharton", "Precious")).executeAsList()
    assertThat(people).hasSize(3)
  }

  @Test fun bigTable() {
    val bigTable = BigTable.Impl(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
        20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30)

    bigTableQueries.insert(bigTable)

    assertThat(bigTableQueries.select().executeAsOne()).isEqualTo(bigTable)
  }
}
