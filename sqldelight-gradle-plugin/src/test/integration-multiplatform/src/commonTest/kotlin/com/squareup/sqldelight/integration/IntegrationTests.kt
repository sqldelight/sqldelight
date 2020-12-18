package com.squareup.sqldelight.integration

import co.touchlab.stately.concurrency.AtomicInt
import co.touchlab.stately.concurrency.value
import co.touchlab.stately.freeze
import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.Query
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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

  @BeforeTest fun before() {
    val database = createSqlDatabase()

    queryWrapper = QueryWrapper(database, NullableTypes.Adapter(listAdapter))
    queryWrapper.freeze()
    personQueries = queryWrapper.personQueries
    keywordsQueries = queryWrapper.sqliteKeywordsQueries
    nullableTypesQueries = queryWrapper.nullableTypesQueries
    bigTableQueries = queryWrapper.bigTableQueries
  }

  @Test fun indexedArgs() {
    // ?1 is the only arg
    val person = personQueries.equivalentNames("Bob").executeAsOne()
    assertEquals(Person(4, "Bob", "Bob"), person)
  }

  @Test fun startIndexAtTwo() {
    // ?2 is the only arg
    val person = personQueries.equivalentNames2("Bob").executeAsOne()
    assertEquals(Person(4, "Bob", "Bob"), person)
  }

  @Test fun namedIndexArgs() {
    // :name is the only arg
    val person = personQueries.equivalentNamesNamed("Bob").executeAsOne()
    assertEquals(Person(4, "Bob", "Bob"), person)
  }

  @Test fun indexedArgLast() {
    // First arg declared is ?, second arg declared is ?1.
    val person = personQueries.indexedArgLast("Bob").executeAsOne()
    assertEquals(Person(4, "Bob", "Bob"), person)
  }

  @Test fun indexedArgLastTwo() {
    // First arg declared is ?, second arg declared is ?2.
    val person = personQueries.indexedArgLast2("Alec", "Strong").executeAsOne()
    assertEquals(Person(1, "Alec", "Strong"), person)
  }

  @Test fun nameIn() {
    val people = personQueries.nameIn(listOf("Alec", "Matt", "Jake")).executeAsList()
    assertEquals(3, people.size)
  }

  @Test fun selectingWithNullParams() {
    nullableTypesQueries.insertNullableType(NullableTypes(listOf("Yo"), "Yo"))
    nullableTypesQueries.insertNullableType(NullableTypes(null, null))

    assertEquals(null, nullableTypesQueries.exprOnNullableColumn(null).executeAsOne().val1)
  }

  @Test fun sqliteKeywordQuery() {
    val keywords = keywordsQueries.selectAll().executeAsOne()
    assertEquals(Group(1, 10, 20), keywords)
  }

  @Test fun compiledStatement() {
    keywordsQueries.insertStmt(11, 21)
    keywordsQueries.insertStmt(12, 22)

    var current: Long = 10
    for (group in keywordsQueries.selectAll().executeAsList()) {
      assertEquals(current++, group.where_)
    }
    assertEquals(13, current)
  }

  @Test
  fun compiledStatementAcrossThread() {
    keywordsQueries.insertStmt(11, 21)

    val result: MPFuture<Int> = MPWorker().runBackground {
      keywordsQueries.insertStmt(12, 22)
      return@runBackground 1
    }

    assertEquals(1, result.consume())

    var current: Long = 10
    for (group in keywordsQueries.selectAll().executeAsList()) {
      assertEquals(current++, group.where_)
    }
    assertEquals(13, current)
  }

  @Test
  fun nullableColumnsUseAdapterProperly() {
    val cool = NullableTypes(listOf("Alec", "Matt", "Jake"), "Cool")
    val notCool = NullableTypes(null, "Not Cool")
    val nulled = NullableTypes(null, null)
    nullableTypesQueries.insertNullableType(cool)
    nullableTypesQueries.insertNullableType(notCool)
    nullableTypesQueries.insertNullableType(nulled)

    assertEquals(listOf(cool, notCool, nulled), nullableTypesQueries.selectAll().executeAsList())
  }

  @Test fun multipleNameIn() {
    val people =
      personQueries.multipleNameIn(listOf("Alec", "Jesse"), listOf("Wharton", "Precious"))
        .executeAsList()
    assertEquals(3, people.size)
  }

  @Test fun selectFromLowercaseFile() {
    assertEquals("sup", queryWrapper.lowerCaseQueries.selectAll().executeAsOne())
  }

  @Test fun bigTable() {
    val bigTable = BigTable(
      1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
      20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30
    )

    bigTableQueries.insert(bigTable)

    assertEquals(bigTable, bigTableQueries.select().executeAsOne())
  }

  @Test fun multipleQueriesAreNotified() {
    // Single query subscribed to twice.
    var equivalentNames1Notified = AtomicInt(0)
    val equivalentNames1 = personQueries.equivalentNames("Bob")
    equivalentNames1.addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        equivalentNames1Notified.incrementAndGet()
      }
    })
    equivalentNames1.addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        equivalentNames1Notified.incrementAndGet()
      }
    })

    // New instance of existing query subscribed to.
    var equivalentNames2Notified = AtomicInt(0)
    val equivalentNames2 = personQueries.equivalentNames("Bob")
    equivalentNames2.addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        equivalentNames2Notified.incrementAndGet()
      }
    })

    // Separate query on the same table.
    var peopleNotified = AtomicInt(0)
    val people = personQueries.nameIn(listOf("Alec", "Matt", "Jake"))
    people.addListener(object : Query.Listener {
      override fun queryResultsChanged() {
        peopleNotified.incrementAndGet()
      }
    })

    // Mutation which affects all of the above.
    personQueries.deleteAll()

    assertEquals(2, equivalentNames1Notified.value)
    assertEquals(1, equivalentNames2Notified.value)
    assertEquals(1, peopleNotified.value)
  }
}
