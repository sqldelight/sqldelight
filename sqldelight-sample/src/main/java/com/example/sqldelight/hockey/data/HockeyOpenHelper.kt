package com.example.sqldelight.hockey.data

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.db.SupportSQLiteOpenHelper
import android.content.Context
import com.example.sqldelight.hockey.QueryWrapper
import com.example.sqldelight.hockey.data.PlayerVals.Position
import com.example.sqldelight.hockey.data.PlayerVals.Shoots
import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.android.create
import com.squareup.sqldelight.db.SqlDatabase
import java.util.GregorianCalendar

object HockeyOpenHelper : SupportSQLiteOpenHelper.Callback(QueryWrapper.version) {
  private var instance: QueryWrapper? = null

  private fun createQueryWrapper(database: SqlDatabase): QueryWrapper {
    return QueryWrapper(
        database = database,
        teamAdapter = Team.Adapter(DateAdapter()),
        playerAdapter = Player.Adapter(
            birth_dateAdapter = DateAdapter(),
            shootsAdapter = EnumColumnAdapter(),
            positionAdapter = EnumColumnAdapter()
        )
    )
  }

  fun getInstance(context: Context): QueryWrapper {
    if (instance == null) {
      instance = createQueryWrapper(QueryWrapper.create(
          context = context,
          callback = this
      ))
    }
    return instance!!
  }

  override fun onCreate(db: SupportSQLiteDatabase) {
    val sqlDatabase = QueryWrapper.create(db)
    val queryWrapper = createQueryWrapper(sqlDatabase)

    QueryWrapper.onCreate(sqlDatabase.getConnection())

    // Populate initial data.
    val ducks = queryWrapper.teamQueries.insertTeam(
        "Anaheim Ducks", GregorianCalendar(1993, 3, 1), "Randy Carlyle", true)

    queryWrapper.playerQueries.insertPlayer(
        "Corey", "Perry", 10, ducks, 30, 210F, GregorianCalendar(1985, 5, 16),
        Shoots.RIGHT, Position.RIGHT_WING
    )

    val getzlaf = queryWrapper.playerQueries.insertPlayer(
        "Ryan", "Getzlaf", 15, ducks, 30, 221F, GregorianCalendar(1985, 5, 10),
        Shoots.RIGHT, Position.CENTER
    )
    queryWrapper.teamQueries.updateCaptain(getzlaf, ducks)

    val pens = queryWrapper.teamQueries.insertTeam(
        "Pittsburgh Penguins", GregorianCalendar(1966, 2, 8), "Mike Sullivan", true)

    val crosby = queryWrapper.playerQueries.insertPlayer(
        "Sidney", "Crosby", 87, pens, 28, 200F, GregorianCalendar(1987, 8, 7),
        Shoots.LEFT, Position.CENTER
    )
    queryWrapper.teamQueries.updateCaptain(crosby, pens)

    val sharks = queryWrapper.teamQueries.insertTeam(
        "San Jose Sharks", GregorianCalendar(1990, 5, 5), "Peter DeBoer", false)

    queryWrapper.playerQueries.insertPlayer(
        "Patrick", "Marleau", 12, sharks, 36, 220F, GregorianCalendar(1979, 9, 15),
        Shoots.LEFT, Position.LEFT_WING
    )

    val pavelski = queryWrapper.playerQueries.insertPlayer(
        "Joe", "Pavelski", 8, sharks, 31, 194F, GregorianCalendar(1984, 7, 18),
        Shoots.RIGHT, Position.CENTER
    )
    queryWrapper.teamQueries.updateCaptain(pavelski, sharks)
  }

  override fun onOpen(db: SupportSQLiteDatabase) {
    super.onOpen(db)
    db.execSQL("PRAGMA foreign_keys=ON")
  }

  override fun onUpgrade(
    db: SupportSQLiteDatabase,
    oldVersion: Int,
    newVersion: Int
  ) {
    QueryWrapper.onMigrate(QueryWrapper.create(db).getConnection(), oldVersion, newVersion)
  }

}
