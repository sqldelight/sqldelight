package com.example

import com.squareup.sqldelight.TransacterImpl
import com.squareup.sqldelight.db.SqlDriver

class TestDatabaseImpl(
    driver: SqlDriver,
    internal val playerAdapter: Player.Adapter,
    internal val teamAdapter: Team.Adapter
) : TransacterImpl(driver), TestDatabase {
    override val groupQueries: GroupQueries = GroupQueries(this, driver)

    override val playerQueries: PlayerQueries = PlayerQueries(this, driver)

    override val teamQueries: TeamQueries = TeamQueries(this, driver)
}
