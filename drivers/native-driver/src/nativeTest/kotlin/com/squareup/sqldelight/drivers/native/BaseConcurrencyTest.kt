package com.squareup.sqldelight.drivers.native

import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.DatabaseFileContext
import co.touchlab.sqliter.JournalMode
import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.internal.copyOnWriteList
import kotlin.test.AfterTest

abstract class BaseConcurrencyTest {
    fun countRows(): Long {
        val cur = driver.executeQuery(0, "SELECT count(*) FROM test", 0)
        try {
            cur.next()
            val count = cur.getLong(0)
            return count!!
        } finally {
            cur.close()
        }
    }

    /*@Test
    fun executeAsOne() {
        val data1 = TestData(1, "val1")
        insertTestData(data1)

        assertNotEquals(data1, testDataQuery().executeAsOne())
    }*/

    internal val mapper = { cursor: SqlCursor ->
        TestData(
            cursor.getLong(0)!!, cursor.getString(1)!!
        )
    }

    internal var _driver: SqlDriver? = null
    internal val driver: SqlDriver
        get() = _driver!!

    fun setupDatabase(schema: SqlDriver.Schema, dbType: DbType): SqlDriver {
        val name = "testdb"
        DatabaseFileContext.deleteDatabase(name)

        return when(dbType){
            DbType.RegularWal -> {
                NativeSqliteDriver(schema, name, maxConcurrentConnections = 4)
            }
            DbType.RegularDelete -> {
                val config = DatabaseConfiguration(
                    name = name,
                    version = 1,
                    journalMode = JournalMode.DELETE,
                    create = { conn ->
                        wrapConnection(conn) { driver ->
                            schema.create(driver)
                        }
                    }
                )
                NativeSqliteDriver(config, maxConcurrentConnections = 4)
            }
            DbType.InMemoryShared -> {
                val config = DatabaseConfiguration(
                    name = name,
                    version = 1,
                    create = { conn ->
                        wrapConnection(conn) { driver ->
                            schema.create(driver)
                        }
                    },
                    inMemory = true
                )
                NativeSqliteDriver(config, maxConcurrentConnections = 4)
            }
            DbType.InMemorySingle -> {
                val config = DatabaseConfiguration(
                    name = null,
                    version = 1,
                    create = { conn ->
                        wrapConnection(conn) { driver ->
                            schema.create(driver)
                        }
                    },
                    inMemory = true
                )
                NativeSqliteDriver(config, maxConcurrentConnections = 4)
            }
        }
    }

    enum class DbType{
        RegularWal, RegularDelete, InMemoryShared, InMemorySingle
    }

    fun createDriver(dbType: DbType): SqlDriver {
        return setupDatabase(
            schema = object : SqlDriver.Schema {
                override val version: Int = 1

                override fun create(driver: SqlDriver) {
                    driver.execute(
                        null,
                        """
              CREATE TABLE test (
                id INTEGER NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
               );
            """.trimIndent(),
                        0
                    )
                }

                override fun migrate(
                    driver: SqlDriver,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                    // No-op.
                }
            },
            dbType
        )
    }

    fun initDriver(dbType: DbType) {
        _driver = createDriver(dbType)
    }

    @AfterTest
    fun tearDown() {
        _driver?.close()
    }

    internal fun insertTestData(testData: TestData) {
        driver.execute(1, "INSERT INTO test VALUES (?, ?)", 2) {
            bindLong(1, testData.id)
            bindString(2, testData.value)
        }
    }

    internal fun testDataQuery(): Query<TestData> {
        return object : Query<TestData>(copyOnWriteList(), mapper) {
            override fun execute(): SqlCursor {
                return driver.executeQuery(0, "SELECT * FROM test", 0)
            }
        }
    }

    internal data class TestData(val id: Long, val value: String)
}