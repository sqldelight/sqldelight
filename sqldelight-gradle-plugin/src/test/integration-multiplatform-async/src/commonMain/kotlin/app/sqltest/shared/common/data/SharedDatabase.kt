package app.sqltest.shared.common.data

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.async.coroutines.awaitMigrate
import app.cash.sqldelight.async.coroutines.awaitQuery
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.sqltest.shared.SqlTestDb
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SharedDatabase(private val driverFactory: DriverFactory) {

  private val versionPragma = "user_version"
  private var database: SqlTestDb? = null

  private suspend fun initDatabase(): SqlTestDb {
    return Mutex().withLock {
      database ?: kotlin.run {
        println("Creating database\n")
        val driver = driverFactory.createDriver()

        // schema is not created automatically on async drivers and pragma is null
        // schema was created automatically on sync drivers and pragma is 1, but it is bugged
        // now schema is not created but pragma is 1... TODO
        try {
          migrateIfNeeded(driver)
        } catch (e: Throwable) {
          e.printStackTrace()
        }

        val newDb = SqlTestDb(driver)
        database = newDb
        newDb
      }
    }
  }

  suspend operator fun <R> invoke(block: suspend SqlTestDb.() -> R): R {
    val db = initDatabase()
    return block(db)
  }

  private suspend fun migrateIfNeeded(driver: SqlDriver) {
    println("Migrate db \n")
    val newVersion = SqlTestDb.Schema.version
    if (driverFactory.isAsync()) {
      println("DB Driver is async\n")
      val oldVersion = driver.awaitQuery(
        identifier = null,
        sql = "PRAGMA $versionPragma",
        mapper = { cursor ->
          if (cursor.next().await()) {
            cursor.getLong(0)
          } else {
            null
          }
        },
        parameters = 0,
      ) ?: 0L

      println("Old version: $oldVersion \n")

      if (oldVersion == 0L) {
        SqlTestDb.Schema.awaitCreate(driver)
        println("Created SCHEMA \n")
        driver.await(null, "PRAGMA $versionPragma = $newVersion", 0)
        println("Updated version to $newVersion")
      } else if (oldVersion < newVersion) {
        println("Updating SCHEMA")
        SqlTestDb.Schema.awaitMigrate(driver, oldVersion, newVersion)
        println("Updated SCHEMA")
      }
    } else {
      println("DB Driver is sync")

      val oldVersion = driver.executeQuery(
        identifier = null,
        sql = "PRAGMA $versionPragma",
        mapper = { cursor ->
          QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
        },
        parameters = 0,
      ).await() ?: 0L

      if (oldVersion == 0L) {
        SqlTestDb.Schema.create(driver).await()
        driver.await(null, "PRAGMA $versionPragma = $newVersion", 0)
      } else if (oldVersion < newVersion) {
        SqlTestDb.Schema.awaitMigrate(driver, oldVersion, newVersion)
      }
    }
  }
}
