package app.cash.sqldelight.gradle

import java.sql.Driver
import java.sql.DriverManager
import java.util.ServiceLoader

/**
 * Interface used to mark a custom subclass of [java.sql.Driver] to be loaded by the isolated
 * ClassLoader of the [VerifyMigrationTask]'s work queue prior to running migration verifications.
 *
 * On the consumer side, ensure that:
 *   1. The driver is registered with java's [ServiceLoader] mechanism by placing a file
 *   app.cash.sqldelight.gradle.VerifyMigrationDriver into resources/META-INF.services folder of
 *   your custom driver library. The file should contain a binary name of the concrete provider
 *   class. See [ServiceLoader] documentation for more details.
 *   2. Your driver gets registered with the [DriverManager] in the
 *   static (java) or init (kotlin) blocks so that it gets picked up when running
 *   [VerifyMigrationTask].
 *
 * For example,
 *
 *   class CustomVerifyMigrationDriver : VerifyMigrationDriver {
 *     init {
 *       // Optionally, unregister other drivers (JDBC driver self-registers itself)
 *       DriverManager.registerDriver(this)
 *     }
 *   }
 */
interface VerifyMigrationDriver: Driver