package com.example

import com.example.testmodule.TestDatabaseImplExposer
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDriver
import kotlin.Int

interface TestDatabase : Transacter {
    val groupQueries: GroupQueries

    val playerQueries: PlayerQueries

    val teamQueries: TeamQueries

    object Schema : SqlDriver.Schema {
        override val version: Int
            get() = TestDatabaseImplExposer.schema.version

        override fun create(driver: SqlDriver) = TestDatabaseImplExposer.schema.create(driver)

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Int,
            newVersion: Int
        ) = TestDatabaseImplExposer.schema.migrate(driver, oldVersion, newVersion)
    }

    companion object {
        operator fun invoke(
            driver: SqlDriver,
            playerAdapter: Player.Adapter,
            teamAdapter: Team.Adapter
        ): TestDatabase = TestDatabaseImplExposer.newInstance(driver, playerAdapter, teamAdapter)}
}
