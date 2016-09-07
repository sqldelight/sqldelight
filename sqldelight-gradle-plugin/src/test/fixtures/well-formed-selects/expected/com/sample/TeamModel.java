package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Calendar;

public interface TeamModel {
  String TABLE_NAME = "team";

  String _ID = "_id";

  String NAME = "name";

  String FOUNDED = "founded";

  String COACH = "coach";

  String CAPTAIN = "captain";

  String WON_CUP = "won_cup";

  String CREATE_TABLE = ""
      + "CREATE TABLE team (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  name TEXT NOT NULL UNIQUE,\n"
      + "  founded TEXT NOT NULL,\n"
      + "  coach TEXT NOT NULL,\n"
      + "  captain INTEGER,\n"
      + "  won_cup INTEGER NOT NULL DEFAULT 0,\n"
      + "  FOREIGN KEY(captain) REFERENCES hockey_player(_id)\n"
      + ")";

  String SELECT_ALL = ""
      + "SELECT *\n"
      + "FROM team";

  long _id();

  @NonNull
  String name();

  @NonNull
  Calendar founded();

  @NonNull
  String coach();

  @Nullable
  Long captain();

  boolean won_cup();

  interface Creator<T extends TeamModel> {
    T create(long _id, @NonNull String name, @NonNull Calendar founded, @NonNull String coach, @Nullable Long captain, boolean won_cup);
  }

  final class Mapper<T extends TeamModel> implements RowMapper<T> {
    private final Factory<T> teamModelFactory;

    public Mapper(Factory<T> teamModelFactory) {
      this.teamModelFactory = teamModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return teamModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getString(1),
          teamModelFactory.foundedAdapter.decode(cursor.getString(2)),
          cursor.getString(3),
          cursor.isNull(4) ? null : cursor.getLong(4),
          cursor.getInt(5) == 1
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Calendar, String> foundedAdapter;

    Marshal(@Nullable TeamModel copy, ColumnAdapter<Calendar, String> foundedAdapter) {
      this.foundedAdapter = foundedAdapter;
      if (copy != null) {
        this._id(copy._id());
        this.name(copy.name());
        this.founded(copy.founded());
        this.coach(copy.coach());
        this.captain(copy.captain());
        this.won_cup(copy.won_cup());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put(_ID, _id);
      return this;
    }

    public Marshal name(String name) {
      contentValues.put(NAME, name);
      return this;
    }

    public Marshal founded(@NonNull Calendar founded) {
      contentValues.put(FOUNDED, foundedAdapter.encode(founded));
      return this;
    }

    public Marshal coach(String coach) {
      contentValues.put(COACH, coach);
      return this;
    }

    public Marshal captain(Long captain) {
      contentValues.put(CAPTAIN, captain);
      return this;
    }

    public Marshal won_cup(boolean won_cup) {
      contentValues.put(WON_CUP, won_cup ? 1 : 0);
      return this;
    }
  }

  final class Factory<T extends TeamModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Calendar, String> foundedAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Calendar, String> foundedAdapter) {
      this.creator = creator;
      this.foundedAdapter = foundedAdapter;
    }

    public Marshal marshal() {
      return new Marshal(null, foundedAdapter);
    }

    public Marshal marshal(TeamModel copy) {
      return new Marshal(copy, foundedAdapter);
    }

    public Mapper<T> select_allMapper() {
      return new Mapper<T>(this);
    }
  }
}
