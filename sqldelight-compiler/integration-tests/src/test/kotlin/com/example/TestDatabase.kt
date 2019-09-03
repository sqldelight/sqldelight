package com.example

import com.example.testmodule.newInstance
import com.example.testmodule.schema
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDriver

interface TestDatabase : Transacter {
  val groupQueries: GroupQueries

  val playerQueries: PlayerQueries

  val teamQueries: TeamQueries

  companion object {
    val Schema: SqlDriver.Schema
      get() = TestDatabase::class.schema

    operator fun invoke(
      driver: SqlDriver,
      playerAdapter: Player.Adapter,
      teamAdapter: Team.Adapter
    ): TestDatabase = TestDatabase::class.newInstance(driver, playerAdapter, teamAdapter)}
}
