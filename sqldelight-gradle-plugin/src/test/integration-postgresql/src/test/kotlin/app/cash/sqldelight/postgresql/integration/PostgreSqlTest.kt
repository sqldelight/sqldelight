package app.cash.sqldelight.postgresql.integration

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.OptimisticLockException
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PostgreSqlTest {
  val conn = DriverManager.getConnection("jdbc:tc:postgresql:latest:///my_db")
  val driver = object : JdbcDriver() {
    override fun getConnection() = conn
    override fun closeConnection(connection: Connection) = Unit
    override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun notifyListeners(vararg queryKeys: String) = Unit
  }
  val database = MyDatabase(
    driver,
    arraysAdapter = Arrays.Adapter(
      object : ColumnAdapter<Array<UInt>, Array<Int>> {
        override fun decode(databaseValue: Array<Int>): Array<UInt> =
          databaseValue.map { it.toUInt() }.toTypedArray()

        override fun encode(value: Array<UInt>): Array<Int> =
          value.map { it.toInt() }.toTypedArray()
      },
    ),
  )

  @Before fun before() {
    driver.execute(null, "SET timezone TO UTC", 0)
    MyDatabase.Schema.create(driver)
  }

  @After fun after() {
    conn.close()
  }

  @Test fun simpleSelect() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    assertThat(database.dogQueries.selectDogs().executeAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = 1,
        ),
      )
  }

  @Test fun booleanSelect() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    assertThat(database.dogQueries.selectGoodDogs(true).executeAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = 1,
        ),
      )
  }

  @Test fun insertReturning1() {
    assertThat(database.dogQueries.insertAndReturn1("Tilda", "Pomeranian").executeAsOne())
      .isEqualTo(
        "Tilda",
      )
  }

  @Test fun insertReturningMany() {
    assertThat(database.dogQueries.insertAndReturnMany("Tilda", "Pomeranian").executeAsOne())
      .isEqualTo(
        InsertAndReturnMany(
          name = "Tilda",
          breed = "Pomeranian",
        ),
      )
  }

  @Test fun insertReturningAll() {
    assertThat(database.dogQueries.insertAndReturnAll("Tilda", "Pomeranian").executeAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = 1,
        ),
      )
  }

  @Test fun updateReturning1() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    assertThat(database.dogQueries.updateAndReturn1(1, "Tilda").executeAsOne())
      .isEqualTo(
        "Tilda",
      )
  }

  @Test fun updateReturningMany() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    assertThat(database.dogQueries.updateAndReturnMany(1, "Tilda").executeAsOne())
      .isEqualTo(
        UpdateAndReturnMany(
          name = "Tilda",
          breed = "Pomeranian",
        ),
      )
  }

  @Test fun updateReturningAll() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    assertThat(database.dogQueries.updateAndReturnAll(1, "Tilda").executeAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = 1,
        ),
      )
  }

  @Test fun deleteReturning1() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    assertThat(database.dogQueries.deleteAndReturn1("Tilda").executeAsOne())
      .isEqualTo(
        "Tilda",
      )
  }

  @Test fun deleteReturningMany() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    assertThat(database.dogQueries.deleteAndReturnMany("Tilda").executeAsOne())
      .isEqualTo(
        DeleteAndReturnMany(
          name = "Tilda",
          breed = "Pomeranian",
        ),
      )
  }

  @Test fun deleteReturningAll() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    assertThat(database.dogQueries.deleteAndReturnAll("Tilda").executeAsOne())
      .isEqualTo(
        Dog(
          name = "Tilda",
          breed = "Pomeranian",
          is_good = 1,
        ),
      )
  }

  @Test fun testDates() {
    assertThat(
      database.datesQueries.insertDate(
        date = LocalDate.of(2020, 1, 1),
        time = LocalTime.of(21, 30, 59, 10000),
        timestamp = LocalDateTime.of(2020, 1, 1, 21, 30, 59, 10000),
        timestamp_with_timezone = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
      ).executeAsOne(),
    )
      .isEqualTo(
        Dates(
          date = LocalDate.of(2020, 1, 1),
          time = LocalTime.of(21, 30, 59, 10000),
          timestamp = LocalDateTime.of(2020, 1, 1, 21, 30, 59, 10000),
          timestamp_with_timezone = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
        ),
      )
  }

  @Test fun testDateFunctions() {
    database.datesQueries.insertDate(
      date = LocalDate.of(2020, 1, 1),
      time = LocalTime.of(21, 30, 59, 10000),
      timestamp = LocalDateTime.of(2020, 1, 1, 21, 30, 59, 10000),
      timestamp_with_timezone = OffsetDateTime.of(1980, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
    ).executeAsOne()

    assertThat(
      database.datesQueries.selectDateTrunc().executeAsOne(),
    )
      .isEqualTo(
        SelectDateTrunc(
          date_trunc = LocalDateTime.of(2020, 1, 1, 21, 0, 0, 0),
          date_trunc_ = OffsetDateTime.of(1980, 4, 9, 20, 0, 0, 0, ZoneOffset.ofHours(0)),
        ),
      )

    assertThat(
      database.datesQueries.selectDatePart().executeAsOne(),
    )
      .isEqualTo(
        SelectDatePart(
          date_part = 21.0,
          date_part_ = 20.0,
        ),
      )
  }

  @Test fun testMinMaxTimeStamps() {
    database.datesQueries.insertDate(
      date = LocalDate.of(2022, 1, 1),
      time = LocalTime.of(11, 30, 59, 10000),
      timestamp = LocalDateTime.of(2029, 1, 1, 21, 30, 59, 10000),
      timestamp_with_timezone = OffsetDateTime.of(1970, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
    ).executeAsOne()

    database.datesQueries.selectMax().executeAsOne().let {
      assertThat(it.max).isEqualTo(LocalDateTime.of(2029, 1, 1, 21, 30, 59, 10000))
    }

    database.datesQueries.selectMin().executeAsOne().let {
      assertThat(it.min).isEqualTo(OffsetDateTime.of(1970, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)))
    }
  }

  @Test fun testSerial() {
    database.run {
      oneEntityQueries.transaction {
        oneEntityQueries.insert("name1")
        oneEntityQueries.insert("name2")
        oneEntityQueries.insert("name3")
      }
      assertThat(oneEntityQueries.selectAll().executeAsList().map { it.id }).containsExactly(1, 2, 3)
    }
  }

  @Test fun testArrays() {
    with(database.arraysQueries.insertAndReturn(arrayOf(1u, 2u), arrayOf("one", "two")).executeAsOne()) {
      assertThat(intArray!!.asList()).containsExactly(1u, 2u).inOrder()
      assertThat(textArray!!.asList()).containsExactly("one", "two").inOrder()
    }
  }

  @Test fun now() {
    val now = database.datesQueries.selectNow().executeAsOne()
    assertThat(now).isNotNull()
    assertThat(now).isGreaterThan(OffsetDateTime.MIN)
  }

  @Test fun interval() {
    val interval = database.datesQueries.selectInterval().executeAsOne()
    assertThat(interval).isNotNull()
    assertThat(interval.getDays()).isEqualTo(1)
  }

  @Test fun successfulOptimisticLock() {
    with(database.withLockQueries) {
      val row = insertText("sup").executeAsOne()

      updateText(
        id = row.id,
        version = row.version,
        text = "sup2",
      )

      assertThat(selectForId(row.id).executeAsOne().text).isEqualTo("sup2")
    }
  }

  @Test fun unsuccessfulOptimisticLock() {
    with(database.withLockQueries) {
      val row = insertText("sup").executeAsOne()

      updateText(
        id = row.id,
        version = row.version,
        text = "sup2",
      )

      try {
        updateText(
          id = row.id,
          version = row.version,
          text = "sup3",
        )
        Assert.fail()
      } catch (e: OptimisticLockException) { }
    }
  }

  @Test fun values() {
    with(database.valueQueries) {
      val id: ValueTable.Id = insertValue(ValueTable(ValueTable.Id(42), "")).executeAsOne()
      assertThat(id.id).isEqualTo(42)
      insertRef(RefTable(RefTable.Id(10), id))
    }
  }

  @Test fun genRandomUuid() {
    val uuid: UUID = database.uuidsQueries.randomUuid().executeAsOne()
    assertThat(uuid).isNotNull()
  }

  @Test fun lengthFunction() {
    database.charactersQueries.insertCharacter("abcdef", null)
    val name = database.charactersQueries.selectNameLength().executeAsOne()
    assertThat(name).isEqualTo(6)
    val desc = database.charactersQueries.selectDescriptionLength().executeAsOne()
    assertThat(desc.length).isNull()
  }

  @Test fun statFunctions() {
    val percentile: SelectPercentile = database.functionsQueries.selectPercentile().executeAsOne()
    val result: Double? = 2.0
    assertThat(percentile).isEqualTo(SelectPercentile(result))
    val stats: List<SelectStats> = database.functionsQueries.selectStats().executeAsList()
    assertThat(stats).isEqualTo(
      listOf(
        SelectStats(null, null, 1),
        SelectStats(null, null, 1),
        SelectStats(null, null, 1),
      ),
    )
  }

  @Test fun testIntsMinMaxSum() {
    with(
      database.numbersQueries.sumInts().executeAsOne(),
    ) {
      assertThat(sumSmall).isNull()
      assertThat(sumInt).isNull()
      assertThat(sumBig).isNull()
    }

    with(
      database.numbersQueries.minInts().executeAsOne(),
    ) {
      assertThat(minSmall).isNull()
      assertThat(minInt).isNull()
      assertThat(minBig).isNull()
    }

    with(
      database.numbersQueries.maxInts().executeAsOne(),
    ) {
      assertThat(maxSmall).isNull()
      assertThat(maxInt).isNull()
      assertThat(maxBig).isNull()
    }

    database.numbersQueries.insertInts(
      small = 1,
      reg = 1,
      big = 1,
    )
    database.numbersQueries.insertInts(
      small = 2,
      big = 2,
      reg = 2,
    )

    with(
      database.numbersQueries.sumInts().executeAsOne(),
    ) {
      assertThat(sumSmall).isInstanceOf(Long::class.javaObjectType)
      assertThat(sumInt).isInstanceOf(Long::class.javaObjectType)
      assertThat(sumBig).isInstanceOf(Long::class.javaObjectType)
      assertThat(sumSmall).isEqualTo(3)
      assertThat(sumInt).isEqualTo(3)
      assertThat(sumBig).isEqualTo(3)
    }

    with(
      database.numbersQueries.minInts().executeAsOne(),
    ) {
      assertThat(minSmall).isEqualTo(1)
      assertThat(minInt).isEqualTo(1)
      assertThat(minBig).isEqualTo(1)
    }

    with(
      database.numbersQueries.maxInts().executeAsOne(),
    ) {
      assertThat(maxSmall).isEqualTo(2)
      assertThat(maxInt).isEqualTo(2)
      assertThat(maxBig).isEqualTo(2)
    }
  }

  @Test
  fun testMultiplySmallerIntsBecomeLongs() {
    database.numbersQueries.insertInts(
      small = 1,
      reg = 1,
      big = Long.MAX_VALUE,
    )

    with(
      database.numbersQueries.multiplyWithBigInts().executeAsOne(),
    ) {
      assertThat(small).isEqualTo(Long.MAX_VALUE)
      assertThat(reg).isEqualTo(Long.MAX_VALUE)
    }
  }

  @Test
  fun testFloat() {
    with(
      database.numbersQueries.sumMinMaxFloat().executeAsOne(),
    ) {
      assertThat(sumFloat).isNull()
      assertThat(minFloat).isNull()
      assertThat(maxFloat).isNull()
    }

    database.numbersQueries.insertFloats(
      flo = 1.5,
    )

    with(
      database.numbersQueries.sumMinMaxFloat().executeAsOne(),
    ) {
      assertThat(sumFloat).isEqualTo(1.5)
      assertThat(minFloat).isEqualTo(1.5)
      assertThat(maxFloat).isEqualTo(1.5)
    }
  }

  @Test
  fun testMultiplyFloatInt() {
    database.numbersQueries.insertInts(
      small = 3,
      reg = 3,
      big = 3,
    )

    database.numbersQueries.insertFloats(
      flo = 1.5,
    )

    with(
      database.numbersQueries.multiplyFloatInt().executeAsOne(),
    ) {
      assertThat(mul).isEqualTo(4.5)
    }
  }
}
