package com.example

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import com.example.testmodule.newInstance
import com.example.testmodule.schema

public interface TestDatabase : Transacter {
  public val groupQueries: GroupQueries

  public val playerQueries: PlayerQueries

  public val teamQueries: TeamQueries

  public companion object {
    public val Schema: SqlDriver.Schema<SqlPreparedStatement, SqlCursor>
      get() = TestDatabase::class.schema

    public operator fun invoke(
      driver: SqlDriver<SqlPreparedStatement, SqlCursor>,
      playerAdapter: Player.Adapter,
      teamAdapter: Team.Adapter
    ): TestDatabase = TestDatabase::class.newInstance(driver, playerAdapter, teamAdapter)
  }
}
