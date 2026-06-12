@file:Suppress("REDUNDANT_VISIBILITY_MODIFIER", "ASSIGNED_VALUE_IS_NEVER_READ")

package com.example

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.example.testmodule.newInstance
import com.example.testmodule.schema
import kotlin.String
import kotlin.Unit
import kotlin.collections.List

public interface TestDatabase : Transacter {
  public val groupQueries: GroupQueries

  public val playerQueries: PlayerQueries

  public val teamQueries: TeamQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = TestDatabase::class.schema

    public operator fun invoke(
      driver: SqlDriver,
      playerAdapter: Player.Adapter,
      teamAdapter: Team.Adapter,
    ): TestDatabase = TestDatabase::class.newInstance(driver, playerAdapter, teamAdapter)

    public fun allTableNames(): List<String> = listOf("group", "myftstable", "myftstable2", "player", "team")
  }
}
