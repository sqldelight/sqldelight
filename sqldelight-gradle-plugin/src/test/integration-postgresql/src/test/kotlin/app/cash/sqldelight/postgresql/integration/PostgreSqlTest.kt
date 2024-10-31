package app.cash.sqldelight.postgresql.integration

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.OptimisticLockException
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import com.google.common.truth.Truth.assertThat
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

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
    data_Adapter = Data_.Adapter(
      object : ColumnAdapter<Instant, LocalDateTime> {
        override fun encode(value: Instant): LocalDateTime {
          return LocalDateTime.ofInstant(value, ZoneOffset.UTC)
        }

        override fun decode(databaseValue: LocalDateTime): Instant {
          return databaseValue.toInstant(ZoneOffset.UTC)
        }
      },
      object : ColumnAdapter<Instant, LocalDateTime> {
        override fun encode(value: Instant): LocalDateTime {
          return LocalDateTime.ofInstant(value, ZoneOffset.UTC)
        }

        override fun decode(databaseValue: LocalDateTime): Instant {
          return databaseValue.toInstant(ZoneOffset.UTC)
        }
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

  @Test fun testMinMaxDates() {
    database.datesQueries.insertDate(
      date = LocalDate.of(2023, 1, 1),
      time = LocalTime.of(11, 30, 59, 10000),
      timestamp = LocalDateTime.of(2029, 1, 1, 21, 30, 59, 10000),
      timestamp_with_timezone = OffsetDateTime.of(1970, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
    ).executeAsOne()

    database.datesQueries.insertDate(
      date = LocalDate.of(2022, 10, 17),
      time = LocalTime.of(11, 30, 59, 10000),
      timestamp = LocalDateTime.of(2029, 1, 1, 21, 30, 59, 10000),
      timestamp_with_timezone = OffsetDateTime.of(1970, 4, 9, 20, 15, 45, 0, ZoneOffset.ofHours(0)),
    ).executeAsOne()

    database.datesQueries.selectMaxDate().executeAsOne().let {
      assertThat(it.max).isEqualTo(LocalDate.of(2023, 1, 1))
    }

    database.datesQueries.selectMinDate().executeAsOne().let {
      assertThat(it.min).isEqualTo(LocalDate.of(2022, 10, 17))
    }
  }

  @Test fun testStringAgg() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    database.dogQueries.insertDog("Bruno", "Pomeranian")
    database.dogQueries.insertDog("Mads", "Broholmer")

    database.dogQueries.selectDogsStringAggName().executeAsList().let {
      assertThat(it).containsExactly(
        SelectDogsStringAggName("Broholmer", "Mads"),
        SelectDogsStringAggName("Pomeranian", "Tilda,Bruno"),
      )
    }
  }

  @Test fun testStringAggOrderBy() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    database.dogQueries.insertDog("Bruno", "Pomeranian")
    database.dogQueries.insertDog("Mads", "Broholmer")

    database.dogQueries.selectDogsStringAggNameOrderBy().executeAsList().let {
      assertThat(it).containsExactly(
        SelectDogsStringAggNameOrderBy("Broholmer", "Mads"),
        SelectDogsStringAggNameOrderBy("Pomeranian", "Bruno,Tilda"),
      )
    }
  }

  @Test fun testArrayAgg() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    database.dogQueries.insertDog("Bruno", "Pomeranian")
    database.dogQueries.insertDog("Mads", "Broholmer")

    database.dogQueries.selectDogsArrayAggName().executeAsList().zip(
      listOf(
        SelectDogsArrayAggName("Broholmer", arrayOf("Mads")),
        SelectDogsArrayAggName("Pomeranian", arrayOf("Tilda", "Bruno")),
      ),
    ).forEach { dog ->
      assertThat(dog.first.breed).isEqualTo(dog.second.breed)
      assertThat(dog.first.expr).isEqualTo(dog.second.expr) // isEqualTo works with Array equality
    }
  }

  @Test fun testCoalesceArrayAgg() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    database.dogQueries.insertDog("Bruno", "Pomeranian")
    database.dogQueries.insertDog("Mads", "Broholmer")

    database.dogQueries.selectDogsCoalesceArrayAggName().executeAsList().zip(
      listOf(
        SelectDogsCoalesceArrayAggName("Broholmer", arrayOf("Mads")),
        SelectDogsCoalesceArrayAggName("Pomeranian", arrayOf("Tilda", "Bruno")),
      ),
    ).forEach { dog ->
      assertThat(dog.first.breed).isEqualTo(dog.second.breed)
      assertThat(dog.first.coalesce).isEqualTo(dog.second.coalesce) // isEqualTo works with Array equality
    }
  }

  @Test fun testArrayAggOrderBy() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    database.dogQueries.insertDog("Bruno", "Pomeranian")
    database.dogQueries.insertDog("Mads", "Broholmer")

    database.dogQueries.selectDogsArrayAggNameOrderBy().executeAsList().zip(
      listOf(
        SelectDogsArrayAggNameOrderBy("Broholmer", arrayOf("Mads")),
        SelectDogsArrayAggNameOrderBy("Pomeranian", arrayOf("Bruno", "Tilda")),
      ),
    ).forEach { dog ->
      assertThat(dog.first.breed).isEqualTo(dog.second.breed)
      assertThat(dog.first.expr).isEqualTo(dog.second.expr) // isEqualTo works with Array equality
    }
  }

  @Test fun testArrayAggOrderByWhereFilter() {
    database.dogQueries.insertDog("Tilda", "Pomeranian")
    database.dogQueries.insertDog("Bruno", "Pomeranian")
    database.dogQueries.insertDog("Mads", "Broholmer")

    database.dogQueries.selectDogsArrayAggNameOrderByWhereFilter().executeAsList().zip(
      listOf(
        SelectDogsArrayAggNameOrderByWhereFilter("Broholmer", arrayOf("Mads")),
        SelectDogsArrayAggNameOrderByWhereFilter("Pomeranian", arrayOf("Tilda", "Bruno")),
      ),
    ).forEach { dog ->
      assertThat(dog.first.breed).isEqualTo(dog.second.breed)
      assertThat(dog.first.expr).isEqualTo(dog.second.expr) // isEqualTo works with Array equality
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
    with(database.arraysQueries.insertAndReturn(arrayOf(1u, 2u), arrayOf("one", "two"), arrayOf("a", "b")).executeAsOne()) {
      assertThat(intArray!!.asList()).containsExactly(1u, 2u).inOrder()
      assertThat(textArray!!.asList()).containsExactly("one", "two").inOrder()
      assertThat(vcharArray!!.asList()).containsExactly("a", "b").inOrder()
    }
  }

  @Test fun testArrayContains() {
    database.arraysQueries.insertAndReturn(arrayOf(1u, 2u), arrayOf("one", "two"), arrayOf("a", "b")).executeAsOne()
    with(database.arraysQueries.contains(arrayOf(1u, 2u), arrayOf("a", "b")).executeAsList()) {
      assertThat(first().expr).isTrue()
      assertThat(first().expr_).isTrue()
    }
  }

  @Test fun testArrayContainsFirst() {
    database.arraysQueries.insertAndReturn(arrayOf(1u, 2u), arrayOf("one", "two"), arrayOf("a", "b")).executeAsOne()
    with(database.arraysQueries.containsFirst(arrayOf(1u, 2u)).executeAsList()) {
      assertThat(first().intArray!!.asList()).containsExactly(1u, 2u).inOrder()
      assertThat(first().textArray!!.asList()).containsExactly("one", "two").inOrder()
      assertThat(first().vcharArray!!.asList()).containsExactly("a", "b").inOrder()
    }
  }

  @Test fun testArrayContainsSecond() {
    database.arraysQueries.insertAndReturn(arrayOf(1u, 2u), arrayOf("one", "two"), arrayOf("a", "b")).executeAsOne()
    with(database.arraysQueries.containsSecond(arrayOf("a", "b")).executeAsList()) {
      assertThat(first().intArray!!.asList()).containsExactly(1u, 2u).inOrder()
      assertThat(first().textArray!!.asList()).containsExactly("one", "two").inOrder()
      assertThat(first().vcharArray!!.asList()).containsExactly("a", "b").inOrder()
    }
  }

  @Test fun testArrayOverlaps() {
    database.arraysQueries.insertAndReturn(arrayOf(1u, 2u), arrayOf("one", "two"), arrayOf("a", "b")).executeAsOne()
    with(database.arraysQueries.overlaps(arrayOf(1u, 2u)).executeAsList()) {
      assertThat(first().intArray!!.asList()).containsExactly(1u, 2u).inOrder()
      assertThat(first().textArray!!.asList()).containsExactly("one", "two").inOrder()
      assertThat(first().vcharArray!!.asList()).containsExactly("a", "b").inOrder()
    }
  }

  @Test fun now() {
    val now = database.datesQueries.selectNow().executeAsOne()
    assertThat(now).isNotNull()
    assertThat(now).isGreaterThan(OffsetDateTime.MIN)
  }

  @Test fun testDateLiteral() {
    val dateLiteral = database.datesQueries.selectDateLiteral().executeAsOne()
    assertThat(dateLiteral).isEqualTo(LocalDate.of(2023, 5, 15))
  }

  @Test fun testTimeLiteral() {
    val timeLiteral = database.datesQueries.selectTimeLiteral().executeAsOne()
    assertThat(timeLiteral).isEqualTo(LocalTime.of(10, 30, 45, 0))
  }

  @Test fun nowPlusInterval() {
    val selectNowInterval = database.datesQueries.selectNowInterval().executeAsOne()
    assertThat(selectNowInterval.now).isNotNull()
    assertThat(selectNowInterval.nowPlusOneDay).isGreaterThan(selectNowInterval.now)
  }

  @Test fun interval() {
    val interval = database.datesQueries.selectInterval().executeAsOne()
    assertThat(interval).isNotNull()
    assertThat(interval.getDays()).isEqualTo(1)
  }

  @Test fun intervalBinaryMultiplyExpression() {
    val interval = database.datesQueries.selectMultiplyInterval().executeAsOne()
    assertThat(interval).isNotNull()
    assertThat(interval.getDays()).isEqualTo(31)
  }

  @Test fun intervalBinaryAddExpression() {
    val interval = database.datesQueries.selectAddInterval().executeAsOne()
    assertThat(interval).isNotNull()
    assertThat(interval.getDays()).isEqualTo(1)
    assertThat(interval.getHours()).isEqualTo(3)
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

  @Test fun sequenceFunctions() {
    val nextVal = database.sequencesQueries.insertNextVal().executeAsOne()
    val currVal = database.sequencesQueries.selectCurrentVal().executeAsOne()
    assertThat(nextVal).isEqualTo(currVal)

    val selectNextVal = database.sequencesQueries.selectNextVal().executeAsOne()
    val lastVal = database.sequencesQueries.selectLastVal().executeAsOne()
    assertThat(selectNextVal).isEqualTo(lastVal)

    val selectSetVal = database.sequencesQueries.selectSetVal().executeAsOne()
    assertThat(selectSetVal).isEqualTo(nextVal)
  }

  @Test
  fun testGenerateSeries() {
    val start = OffsetDateTime.of(2023, 9, 1, 0, 0, 0, 0, ZoneOffset.ofHours(0))
    val finish = OffsetDateTime.of(2023, 9, 1, 5, 0, 0, 0, ZoneOffset.ofHours(0))
    val series = database.functionsQueries.selectGenerateSeries(start, finish).executeAsList()
    assertThat(series.size).isEqualTo(6)
    assertThat(series.first()).isEqualTo(start)
    assertThat(series.last()).isEqualTo(finish)
  }

  @Test
  fun testSelectDataBinaryComparison() {
    val created = Instant.parse("2017-12-03T10:00:00.00Z")
    val updated = Instant.parse("2022-05-01T10:00:00.00Z")
    database.binaryArgumentsQueries.insertData(10, 5, created, updated)
    val result = database.binaryArgumentsQueries.selectDataBinaryComparison(10, 10).executeAsList()
    assertThat(result.first().datum).isEqualTo(10)
  }

  @Test
  fun testSelectDataBinaryCast1() {
    val created = Instant.parse("2017-12-03T10:00:00.00Z")
    val updated = Instant.parse("2022-05-01T10:00:00.00Z")
    database.binaryArgumentsQueries.insertData(10, 5, created, updated)
    val result = database.binaryArgumentsQueries.selectDataBinaryCast1(10.0).executeAsOne()
    assertThat(result.expected_datum).isEqualTo(60.toDouble())
  }

  @Test
  fun testSelectDataBinaryCast2() {
    val created = Instant.parse("2017-12-03T10:00:00.00Z")
    val updated = Instant.parse("2022-05-01T10:00:00.00Z")
    database.binaryArgumentsQueries.insertData(10, 5, created, updated)
    val result = database.binaryArgumentsQueries.selectDataBinaryCast2(10.0, 10).executeAsOne()
    assertThat(result.expected_datum).isEqualTo(9.5)
  }

  @Test
  fun testSelectDataBinaryIntervalComparison1() {
    val created = Instant.parse("2017-12-03T10:00:00.00Z")
    val updated = Instant.parse("2022-05-01T10:00:00.00Z")
    val createdAt = Instant.parse("2017-12-05T10:00:00.00Z")
    val updatedAt = Instant.parse("2022-05-01T10:00:00.00Z")
    database.binaryArgumentsQueries.insertData(10, 5, created, updated)
    val result = database.binaryArgumentsQueries.selectDataBinaryIntervalComparison1(createdAt, updatedAt).executeAsList()
    assertThat(result.first().datum).isEqualTo(10)
  }

  @Test
  fun testSelectDataBinaryIntervalComparison2() {
    val created = Instant.parse("2017-12-03T10:00:00.00Z")
    val updated = Instant.parse("2022-05-01T10:00:00.00Z")
    database.binaryArgumentsQueries.insertData(10, 5, created, updated)
    val result = database.binaryArgumentsQueries.selectDataBinaryIntervalComparison2(created).executeAsList()
    assertThat(result.first().datum).isEqualTo(10)
  }

  @Test
  fun testInsertJson() {
    database.jsonQueries.insert("another key", "another value")
    with(database.jsonQueries.select().executeAsList()) {
      assertThat(first().data_).isEqualTo("""{"another key" : "another value"}""")
      assertThat(first().datab).isEqualTo("""{"key": "value"}""")
    }
  }

  @Test
  fun testInsertJsonLiteral() {
    database.jsonQueries.insertLiteral("""{"key a" : "value a"}""", """{"key b" : "value b"}""", """{}""", emptyArray<String>())
    with(database.jsonQueries.select().executeAsList()) {
      assertThat(first().data_).isEqualTo("""{"key a" : "value a"}""")
      assertThat(first().datab).isEqualTo("""{"key b": "value b"}""")
    }
  }

  @Test
  fun testJsonObjectOperators() {
    database.jsonQueries.insertLiteral("""{"a" : 11,"aa":[1,2,3]}""", """{"b" : 12,"bb":[1,2,3]}""", """{}""", emptyArray<String>())
    with(database.jsonQueries.selectJsonObjectOperators().executeAsList()) {
      assertThat(first().expr).isEqualTo("11")
      assertThat(first().expr_).isEqualTo("12")
      assertThat(first().expr__).isEqualTo("[1,2,3]")
      assertThat(first().expr___).isEqualTo("[1, 2, 3]")
      assertThat(first().expr____).isEqualTo("""{"bb": [1, 2, 3]}""")
    }
  }

  @Test
  fun testJsonArrayIndexOperators() {
    database.jsonQueries.insertLiteral("""[1,2,3]""", """[1,2,3]""", """{}""", emptyArray<String>())
    with(database.jsonQueries.selectJsonArrayIndexOperators().executeAsList()) {
      assertThat(first().expr).isEqualTo("1")
      assertThat(first().expr_).isEqualTo("2")
      assertThat(first().expr__).isEqualTo("3")
      assertThat(first().expr___).isEqualTo("[1, 3]")
    }
  }

  @Test
  fun testJsonBooleanOperators() {
    database.jsonQueries.insertLiteral("""{}""", """{"a":1, "b":2}""", """{"b":2}""", arrayOf("a", "b"))
    with(database.jsonQueries.selectJsonBooleanOperators().executeAsList()) {
      assertThat(first().expr).isEqualTo(true)
      assertThat(first().expr_).isEqualTo(true)
      assertThat(first().expr__).isEqualTo(true)
      assertThat(first().expr___).isEqualTo(true)
      assertThat(first().expr____).isEqualTo(true)
      assertThat(first().expr_____).isEqualTo(true)
    }
  }

  @Test
  fun testJsonConcatOperators() {
    database.jsonQueries.insertLiteral("""{}""", """{"a":1}""", """{"b":2}""", emptyArray<String>())
    with(database.jsonQueries.selectJsonConcatOperators().executeAsList()) {
      assertThat(first().expr).isEqualTo("""{"a": 1, "b": 2}""")
    }
  }

  @Test
  fun testJsonbPretty() {
    database.jsonQueries.insertLiteral("""{}""", """{"a":1,"b":2}""", """{}""", emptyArray<String>())
    with(database.jsonQueries.selectJsonPretty().executeAsList()) {
      assertThat(first()).isEqualTo(
        """{
      |    "a": 1,
      |    "b": 2
      |}
        """.trimMargin(),
      )
    }
  }

  @Test
  fun testJsonbSet() {
    database.jsonQueries.insertLiteral("""{}""", """[{"a":1},{"b":2}]""", """{}""", emptyArray<String>())
    with(database.jsonQueries.setJsonb("""{0, "a"}""", """123""").executeAsList()) {
      assertThat(first()).isEqualTo("""[{"a": 123}, {"b": 2}]""")
    }
  }

  @Test
  fun testSelectJsonbPath() {
    database.jsonQueries.insertLiteral("""{}""", """[{"a":1},{"b":2}]""", """{}""", emptyArray<String>())
    with(database.jsonQueries.selectJsonbPath("""[{"a":1}]""").executeAsList()) {
      assertThat(first().datab).isEqualTo("""[{"a": 1}, {"b": 2}]""")
    }
  }

  @Test
  fun testSelectJsonPathEquality() {
    database.jsonQueries.insertLiteral("""{"a": 1}""", """{"b":2}""", """{}""", emptyArray<String>())
    with(database.jsonQueries.selectJsonPathEquals("1", "2").executeAsList()) {
      assertThat(first().data_).isEqualTo("""{"a": 1}""")
      assertThat(first().datab).isEqualTo("""{"b": 2}""")
    }
  }

  @Test
  fun testSelectJsonbContains() {
    database.jsonQueries.insertLiteral("""{}""", """{"b":2}""", """{}""", emptyArray<String>())
    with(database.jsonQueries.selectJsonbContains("b").executeAsList()) {
      assertThat(first().datab).isEqualTo("""{"b": 2}""")
    }
  }

  @Test
  fun testUpdateSetFromId() {
    database.updatesQueries.insertTest(31)
    database.updatesQueries.insertTest2("X")
    with(database.updatesQueries.updateTestId().executeAsOne()) {
      assertThat(this).isEqualTo(1)
    }
  }

  @Test
  fun testUpdateSetFromId2() {
    database.updatesQueries.insertTest(31)
    database.updatesQueries.insertTest2("X")
    with(database.updatesQueries.updateTestId2("X").executeAsOne()) {
      assertThat(id2).isEqualTo(1)
    }
  }

  @Test
  fun testSelectTsVectorSearch() {
    database.textSearchQueries.insertLiteral("the rain in spain")
    with(database.textSearchQueries.search("rain").executeAsList()) {
      assertThat(first()).isEqualTo("'in' 'rain' 'spain' 'the'")
    }
  }

  @Test
  fun testSelectTsVectorContains() {
    database.textSearchQueries.insertLiteral("the rain in spain")
    with(database.textSearchQueries.contains("rain").executeAsList()) {
      assertThat(first()).isEqualTo(true)
    }
  }

  @Test
  fun testSelectTsQuery() {
    with(database.textSearchQueries.tsQuery("the & rain & spain'").executeAsList()) {
      assertThat(first()).isEqualTo("'rain' & 'spain'")
    }
  }

  @Test
  fun testSelectTsVector() {
    with(database.textSearchQueries.tsVector("the rain in spain").executeAsList()) {
      assertThat(first()).isEqualTo("'rain':2 'spain':4")
    }
  }

  @Test
  fun testContactTsVector() {
    database.textSearchQueries.insertLiteral("the rain in spain")
    with(database.textSearchQueries.concat("falls mainly on the plains").executeAsList()) {
      assertThat(first()).isEqualTo("'fall':1 'in' 'main':2 'plain':5 'rain' 'spain' 'the'")
    }
  }

  @Test
  fun testContactTsVectorRank() {
    database.textSearchQueries.insertLiteral("the rain in spain")
    with(database.textSearchQueries.rank("rain | plain").executeAsList()) {
      assertThat(first()).isEqualTo("0.030396355")
    }
  }

  @Test
  fun testMatchRegExOps() {
    database.regExOpsQueries.insert("thomas")
    with(database.regExOpsQueries.matchRegExOps("t.*ma", "T.*ma", "t.*max", "T.*ma").executeAsList()) {
      assertThat(first().expr).isTrue()
      assertThat(first().expr_).isTrue()
      assertThat(first().expr__).isTrue()
      assertThat(first().expr___).isFalse()
    }
  }

  @Test
  fun testMatchRegExWhere() {
    database.regExOpsQueries.insert("thomas")
    with(database.regExOpsQueries.matchRegExWhere("t.*ma").executeAsList()) {
      assertThat(first()).isEqualTo("thomas")
    }
  }

  @Test
  fun testLike() {
    database.likeQueries.insert("testing")

    with(database.likeQueries.selectWhereLike("test%").executeAsList()) {
      assertThat(first()).isEqualTo("testing")
    }

    with(database.likeQueries.selectWhereLikeRegex().executeAsList()) {
      assertThat(first()).isEqualTo("testing")
    }

    with(database.likeQueries.selectLikeRegex().executeAsList()) {
      assertThat(first().expr).isTrue()
      assertThat(first().expr_).isFalse()
    }
  }

  @Test
  fun testILike() {
    database.likeQueries.insert("TESTING")

    with(database.likeQueries.selectWhereILike("test%").executeAsList()) {
      assertThat(first()).isEqualTo("TESTING")
    }

    with(database.likeQueries.selectWhereILikeRegex().executeAsList()) {
      assertThat(first()).isEqualTo("TESTING")
    }

    with(database.likeQueries.selectILikeRegex().executeAsList()) {
      assertThat(first().expr).isTrue()
      assertThat(first().expr_).isFalse()
    }
  }

  @Test
  fun testRankOver() {
    database.windowFunctionsQueries.insert("t", 2)
    database.windowFunctionsQueries.insert("q", 3)
    database.windowFunctionsQueries.insert("p", 1)

    with(database.windowFunctionsQueries.selectRank().executeAsList()) {
      assertThat(first().name).isEqualTo("q")
      assertThat(first().rank).isEqualTo(1)
    }
  }

  @Test
  fun testOver() {
    database.windowFunctionsQueries.insert("a", 10)
    database.windowFunctionsQueries.insert("b", 11)
    database.windowFunctionsQueries.insert("c", 12)

    with(database.windowFunctionsQueries.selectOver().executeAsList()) {
      assertThat(first().name).isEqualTo("c")
      assertThat(first().dense_rank).isEqualTo(1)
      assertThat(first().row_num).isEqualTo(1)
      assertThat(first().lag).isNull()
      assertThat(first().lead).isEqualTo(11)
      assertThat(first().ntile).isEqualTo(1)
      assertThat(first().cume_dist).isEqualTo(0.3333333333333333)
      assertThat(first().percent_rank).isEqualTo(0)
    }
  }

  @Test
  fun testBooleans() {
    database.booleansQueries.insert(true)

    with(database.booleansQueries.select().executeAsOne()) {
      assertThat(expr).isTrue()
      assertThat(expr_).isFalse()
      assertThat(expr__).isFalse()
      assertThat(expr___).isTrue()
      assertThat(b).isTrue()
    }
  }

  @Test
  fun testAtTimeZone() {
    val ts = LocalDateTime.of(2001, 2, 16, 20, 38, 40)
    val tstz = OffsetDateTime.of(2001, 2, 16, 20, 38, 40, 0, ZoneOffset.ofHours(0))
    database.timeZoneQueries.insert(ts, tstz)

    with(database.timeZoneQueries.select(tstz, ts, "America/Chicago").executeAsOne()) {
      assertThat(expr).isEqualTo(LocalDateTime.of(2024, 5, 9, 15, 28, 36))
      assertThat(expr_).isEqualTo(OffsetDateTime.of(2001, 2, 17, 2, 38, 40, 0, ZoneOffset.ofHours(0)))
      assertThat(expr__).isEqualTo(LocalDateTime.of(2001, 2, 16, 18, 38, 40))
      assertThat(expr___).isEqualTo(OffsetDateTime.of(2001, 2, 17, 2, 38, 40, 0, ZoneOffset.ofHours(0)))
      assertThat(expr____).isEqualTo(OffsetDateTime.of(2001, 2, 17, 2, 38, 40, 0, ZoneOffset.ofHours(0)))
      assertThat(expr_____).isEqualTo(LocalDateTime.of(2001, 2, 16, 13, 38, 40))
      assertThat(expr______).isGreaterThan(LocalDateTime.MIN)
      assertThat(expr_______).isGreaterThan(LocalDateTime.MIN)
      assertThat(expr________).isEqualTo(LocalDateTime.of(2001, 2, 16, 14, 38, 40))
      assertThat(expr_________).isEqualTo(OffsetDateTime.of(2001, 2, 17, 2, 38, 40, 0, ZoneOffset.ofHours(0)))
    }
  }

  @Test
  fun testDataTypeCasts() {
    database.dataTypeCastsQueries.insert("42", null)

    with(database.dataTypeCastsQueries.select(42).executeAsOne()) {
      assertThat(expr).isEqualTo("1")
      assertThat(expr_).isEqualTo("3.14")
      assertThat(expr__).isEqualTo(42)
      assertThat(expr___).isFalse()
      assertThat(expr____).isTrue()
      assertThat(expr_____).isEqualTo(LocalDateTime.of(2023, 5, 1, 12, 34, 56))
      assertThat(expr______).isEqualTo(java.util.UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"))
      assertThat(expr_______).isEqualTo("""{"a":42}""")
      assertThat(expr________).isEqualTo(arrayOf(1, 2, 3))
      assertThat(expr_________).isEqualTo(42L)
      assertThat(expr__________).isEqualTo(3.14)
      assertThat(expr___________).isFalse()
      assertThat(expr____________).isEqualTo("hello")
      assertThat(expr_____________).isEqualTo(OffsetDateTime.of(2023, 4, 25, 8, 30, 0, 0, ZoneOffset.ofHours(0)))
      assertThat(expr______________).isEqualTo(LocalDate.of(2023, 4, 25))
      assertThat(expr_______________).isEqualTo(42)
      assertThat(expr________________).isNull()
      assertThat(expr_________________).isEqualTo(42)
    }
  }

  @Test
  fun testXml() {
    val a = "<?xml version=\"1.0\"?><book><title>Manual</title><chapter>test</chapter></book>"
    val b = "<book><title>Manual</title><chapter>test</chapter></book>"
    database.xmlQueries.insert(a, b)
    with(database.xmlQueries.select().executeAsOne()) {
      assertThat(x1).isEqualTo(b) // results are returned without <?xml...?>
      assertThat(x2).isEqualTo(b)
    }
  }

  @Test
  fun testExtract() {
    val sa = OffsetDateTime.of(2001, 2, 16, 19, 30, 0, 0, ZoneOffset.ofHours(0))
    val ea = OffsetDateTime.of(2001, 2, 16, 20, 30, 0, 0, ZoneOffset.ofHours(0))
    val cd = LocalDate.of(2001, 2, 16)

    database.extractQueries.insert(sa, ea, cd)

    with(database.extractQueries.select().executeAsOne()) {
      assertThat(expr).isEqualTo(5)
      assertThat(expr_).isEqualTo(2023)
      assertThat(expr__).isEqualTo(93600)
      assertThat(expr___).isEqualTo(20)
      assertThat(expr____).isEqualTo(38)
      assertThat(expr_____).isEqualTo(16)
      assertThat(expr______).isEqualTo(5)
      assertThat(expr_______).isEqualTo(10)
      assertThat(expr________).isEqualTo(3)
    }
  }

  @Test
  fun testSelectDistinctOn() {
    val studentExpected = Student(1000, "Test Student")
    val gradeExpected = Grade(4000, studentExpected.student_id, 5, LocalDateTime.of(1980, 1, 1, 1, 0, 0))
    database.distinctOnQueries.insertStudent(studentExpected)
    database.distinctOnQueries.insertGrade(gradeExpected)

    with(database.distinctOnQueries.selectDistinctOnStudent().executeAsOne()) {
      assertThat(student_id).isEqualTo(studentExpected.student_id)
      assertThat(name).isEqualTo(studentExpected.name)
      assertThat(grade_id).isEqualTo(gradeExpected.grade_id)
      assertThat(grade).isEqualTo(gradeExpected.grade)
      assertThat(grade_date).isEqualTo(gradeExpected.grade_date)
    }

    with(database.distinctOnQueries.selectDistinctOnStudentGradeDate().executeAsOne()) {
      assertThat(student_id).isEqualTo(studentExpected.student_id)
      assertThat(name).isEqualTo(studentExpected.name)
      assertThat(grade_id).isEqualTo(gradeExpected.grade_id)
      assertThat(grade).isEqualTo(gradeExpected.grade)
      assertThat(grade_date).isEqualTo(gradeExpected.grade_date)
    }
  }

  @Test
  fun testLateralJoin() {
    database.lateralQueries.insertSales()
    with(database.lateralQueries.selectSales().executeAsList()) {
      assertThat(first().salesperson_id).isEqualTo(1)
      assertThat(first().full_name).isEqualTo("A D")
      assertThat(first().home_region_id).isEqualTo(1)
      assertThat(first().home_region_name).isEqualTo("North America")
      assertThat(first().total_sales).isEqualTo(BigDecimal("1000.50"))
    }
  }

  @Test
  fun testSelectAppointments() {
    val slotBegin = LocalDateTime.of(2009, 1, 1, 9, 0).atOffset(ZoneOffset.UTC)
    val slotEnd = slotBegin.plusMinutes(30)

    database.temporalRangesQueries.insert("[$slotBegin, $slotEnd)")
    database.temporalRangesQueries.insert("[$slotEnd, ${slotEnd.plusMinutes(30)})")

    with(database.temporalRangesQueries.appointments().executeAsList()) {
      assertThat(first().begin).isEqualTo(slotBegin)
      assertThat(first().end).isEqualTo(slotEnd)
    }

    val multiRangeSlots = "{[$slotBegin, $slotEnd), [$slotEnd, ${slotEnd.plusMinutes(30)})}"
    with(database.temporalRangesQueries.selectAvailableAppointments(multiRangeSlots, "[$slotBegin, $slotEnd)").executeAsList()) {
      assertThat(first()).isEqualTo("""{["2009-01-01 09:30:00+00","2009-01-01 10:00:00+00")}""")
    }

    with(database.temporalRangesQueries.selectAppointmentContainsRange().executeAsList()) {
      assertThat(first()).isFalse()
    }
  }

  @Test
  fun testSelectContainsTemporalRange() {
    with(database.temporalRangesQueries.selectMultiRangeContainsTimestamp().executeAsList()) {
      assertThat(first()).isTrue()
    }
  }
}
