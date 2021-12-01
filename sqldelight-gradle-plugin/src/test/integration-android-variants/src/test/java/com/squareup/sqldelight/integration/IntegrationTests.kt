package com.squareup.sqldelight.integration

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Arrays
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

class IntegrationTests {
  private lateinit var queryWrapper: QueryWrapper
  private lateinit var personQueries: PersonQueries
  private lateinit var keywordsQueries: SqliteKeywordsQueries

  @Before fun before() {
    val database = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY + "test.db")
    QueryWrapper.Schema.create(database)

    queryWrapper = QueryWrapper(database)
    personQueries = queryWrapper.personQueries
    keywordsQueries = queryWrapper.sqliteKeywordsQueries
  }

  @After fun after() {
    File("test.db").delete()
  }

  @Test fun indexedArgs() {
    // ?1 is the only arg
    val person: Person = personQueries.equivalentNames("Bob").executeAsOne()
    assertThat(person).isEqualTo(Person(4, "Bob", "Bob"))
  }

  @Test fun startIndexAtTwo() {
    // ?2 is the only arg
    val person: Person = personQueries.equivalentNames2("Bob").executeAsOne()
    assertThat(person).isEqualTo(Person(4, "Bob", "Bob"))
  }

  @Test fun namedIndexArgs() {
    // :name is the only arg
    val person: Person = personQueries.equivalentNamesNamed("Bob").executeAsOne()
    assertThat(person).isEqualTo(Person(4, "Bob", "Bob"))
  }

  @Test fun indexedArgLast() {
    // First arg declared is ?, second arg declared is ?1.
    val person: Person = personQueries.indexedArgLast("Bob").executeAsOne()
    assertThat(person).isEqualTo(Person(4, "Bob", "Bob"))
  }

  @Test fun indexedArgLastTwo() {
    // First arg declared is ?, second arg declared is ?2.
    val person: Person = personQueries.indexedArgLast2("Alec", "Strong").executeAsOne()
    assertThat(person).isEqualTo(Person(1, "Alec", "Strong"))
  }

  @Test fun nameIn() {
    val people = personQueries.nameIn(Arrays.asList("Alec", "Matt", "Jake")).executeAsList()
    assertThat(people).hasSize(3)
  }

  @Test fun sqliteKeywordQuery() {
    val keywords: Group = keywordsQueries.selectAll().executeAsOne()
    assertThat(keywords).isEqualTo(Group(1, 10, 20))
  }

  @Test fun compiledStatement() {
    keywordsQueries.insertStmt(11, 21)
    keywordsQueries.insertStmt(12, 22)
    var current: Long = 10
    for (group in keywordsQueries.selectAll().executeAsList()) {
      assertThat(group.where_).isEqualTo(current++)
    }
    assertThat(current).isEqualTo(13)
  }

  @Test @kotlin.Throws(InterruptedException::class) fun compiledStatementAcrossThread() {
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
      assertThat(group.where_).isEqualTo(current++)
    }
    assertThat(current).isEqualTo(13)
  }
}
