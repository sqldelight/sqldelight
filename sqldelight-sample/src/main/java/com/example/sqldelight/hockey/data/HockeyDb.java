package com.example.sqldelight.hockey.data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.SupportSQLiteOpenHelper.Configuration;
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory;
import android.content.Context;
import java.util.GregorianCalendar;

public final class HockeyDb extends SupportSQLiteOpenHelper.Callback {
  private static final int DATABASE_VERSION = 2;

  private static SupportSQLiteOpenHelper instance;

  public static SupportSQLiteOpenHelper getInstance(Context context) {
    if (instance == null) {
      instance = new FrameworkSQLiteOpenHelperFactory().create(
          Configuration.builder(context)
              .callback(new HockeyDb())
              .build());
    }
    return instance;
  }

  public HockeyDb() {
    super(DATABASE_VERSION);
  }

  @Override public void onCreate(SupportSQLiteDatabase db) {
    db.execSQL(Team.CREATE_TABLE);
    db.execSQL(Player.CREATE_TABLE);

    Player.Insert_player insertPlayer = new PlayerModel.Insert_player(db, Player.FACTORY);
    Team.Insert_team insertTeam = new Team.Insert_team(db, Team.FACTORY);
    Team.Update_captain updateCaptain = new TeamModel.Update_captain(db);

    // Populate initial data.
    insertTeam.bind("Anaheim Ducks", new GregorianCalendar(1993, 3, 1), "Randy Carlyle", true);
    long ducks = insertTeam.executeInsert();

    insertPlayer.bind("Corey", "Perry", 10, ducks, 30, 210, new GregorianCalendar(1985, 5, 16),
        Player.Shoots.RIGHT, Player.Position.RIGHT_WING);
    insertPlayer.executeInsert();

    insertPlayer.bind("Ryan", "Getzlaf", 15, ducks, 30, 221, new GregorianCalendar(1985, 5, 10),
        Player.Shoots.RIGHT, Player.Position.CENTER);
    long getzlaf = insertPlayer.executeInsert();

    updateCaptain.bind(getzlaf, ducks);
    updateCaptain.execute();

    insertTeam.bind("Pittsburgh Penguins", new GregorianCalendar(1966, 2, 8), "Mike Sullivan", true);
    long pens = insertTeam.executeInsert();

    insertPlayer.bind("Sidney", "Crosby", 87, pens, 28, 200, new GregorianCalendar(1987, 8, 7),
        Player.Shoots.LEFT, Player.Position.CENTER);
    long crosby = insertPlayer.executeInsert();

    updateCaptain.bind(crosby, pens);
    updateCaptain.execute();

    insertTeam.bind("San Jose Sharks", new GregorianCalendar(1990, 5, 5), "Peter DeBoer", false);
    long sharks = insertTeam.executeInsert();

    insertPlayer.bind("Patrick", "Marleau", 12, sharks, 36, 220, new GregorianCalendar(1979, 9, 15),
        Player.Shoots.LEFT, Player.Position.LEFT_WING);
    insertPlayer.executeInsert();

    insertPlayer.bind("Joe", "Pavelski", 8, sharks, 31, 194, new GregorianCalendar(1984, 7, 18),
        Player.Shoots.RIGHT, Player.Position.CENTER);
    long pavelski = insertPlayer.executeInsert();

    updateCaptain.bind(pavelski, sharks);
    updateCaptain.execute();
  }

  @Override public void onOpen(SupportSQLiteDatabase db) {
    super.onOpen(db);
    db.execSQL("PRAGMA foreign_keys=ON");
  }

  @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
    switch (oldVersion) {
      case 1:
        Team.Update_coach_for_team updateCoachForTeam = new TeamModel.Update_coach_for_team(db);
        updateCoachForTeam.bind("Randy Carlyle", "Anaheim Ducks");
        updateCoachForTeam.execute();
    }
  }
}
