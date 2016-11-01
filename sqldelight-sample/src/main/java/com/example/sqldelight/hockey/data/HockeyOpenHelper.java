package com.example.sqldelight.hockey.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.GregorianCalendar;

public final class HockeyOpenHelper extends SQLiteOpenHelper {
  private static final int DATABASE_VERSION = 2;

  private static HockeyOpenHelper instance;

  public static HockeyOpenHelper getInstance(Context context) {
    if (instance == null) {
      instance = new HockeyOpenHelper(context);
    }
    return instance;
  }

  public HockeyOpenHelper(Context context) {
    super(context, null, null, DATABASE_VERSION);
  }

  @Override public void onCreate(SQLiteDatabase db) {
    db.execSQL(Team.CREATE_TABLE);
    db.execSQL(Player.CREATE_TABLE);

    Player.Insert_player insertPlayer = new PlayerModel.Insert_player(db, Player.FACTORY);
    Team.Insert_team insertTeam = new Team.Insert_team(db, Team.FACTORY);
    Team.Update_captain updateCaptain = new TeamModel.Update_captain(db);

    // Populate initial data.
    insertTeam.bind("Anaheim Ducks", new GregorianCalendar(1993, 3, 1), "Randy Carlyle", true);
    long ducks = insertTeam.program.executeInsert();

    insertPlayer.bind("Corey", "Perry", 10, ducks, 30, 210, new GregorianCalendar(1985, 5, 16),
        Player.Shoots.RIGHT, Player.Position.RIGHT_WING);
    insertPlayer.program.executeInsert();

    insertPlayer.bind("Ryan", "Getzlaf", 15, ducks, 30, 221, new GregorianCalendar(1985, 5, 10),
        Player.Shoots.RIGHT, Player.Position.CENTER);
    long getzlaf = insertPlayer.program.executeInsert();

    updateCaptain.bind(getzlaf, ducks);
    updateCaptain.program.execute();

    insertTeam.bind("Pittsburgh Penguins", new GregorianCalendar(1966, 2, 8), "Mike Sullivan", true);
    long pens = insertTeam.program.executeInsert();

    insertPlayer.bind("Sidney", "Crosby", 87, pens, 28, 200, new GregorianCalendar(1987, 8, 7),
        Player.Shoots.LEFT, Player.Position.CENTER);
    long crosby = insertPlayer.program.executeInsert();

    updateCaptain.bind(crosby, pens);
    updateCaptain.program.execute();

    insertTeam.bind("San Jose Sharks", new GregorianCalendar(1990, 5, 5), "Peter DeBoer", false);
    long sharks = insertTeam.program.executeInsert();

    insertPlayer.bind("Patrick", "Marleau", 12, sharks, 36, 220, new GregorianCalendar(1979, 9, 15),
        Player.Shoots.LEFT, Player.Position.LEFT_WING);
    insertPlayer.program.executeInsert();

    insertPlayer.bind("Joe", "Pavelski", 8, sharks, 31, 194, new GregorianCalendar(1984, 7, 18),
        Player.Shoots.RIGHT, Player.Position.CENTER);
    long pavelski = insertPlayer.program.executeInsert();

    updateCaptain.bind(pavelski, sharks);
    updateCaptain.program.execute();
  }

  @Override public void onOpen(SQLiteDatabase db) {
    super.onOpen(db);
    db.execSQL("PRAGMA foreign_keys=ON");
  }

  @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    switch (oldVersion) {
      case 1:
        Team.Update_coach_for_team updateCoachForTeam = new TeamModel.Update_coach_for_team(db);
        updateCoachForTeam.bind("Randy Carlyle", "Anaheim Ducks");
        updateCoachForTeam.program.execute();
    }
  }
}
