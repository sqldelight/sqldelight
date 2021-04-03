package com.example

import com.example.testmodule.newInstance
import com.example.testmodule.schema
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDriver

public interface TestDatabase : Transacter {
  public val groupQueries: GroupQueries

  public val playerQueries: PlayerQueries

  public val teamQueries: TeamQueries

  public companion object {
    public val Schema: SqlDriver.Schema
      get() = TestDatabase::class.schema

    public operator fun invoke(
      driver: SqlDriver,
      playerAdapter: Player.Adapter,
      teamAdapter: Team.Adapter
    ): TestDatabase = TestDatabase::class.newInstance(driver, playerAdapter, teamAdapter)
  }
}
