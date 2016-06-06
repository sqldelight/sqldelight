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
    T create(long _id, String name, Calendar founded, String coach, Long captain, boolean won_cup);
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
          teamModelFactory.foundedAdapter.map(cursor, 2),
          cursor.getString(3),
          cursor.isNull(4) ? null : cursor.getLong(4),
          cursor.getInt(5) == 1
      );
    }
  }

  class Marshal<T extends Marshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Calendar> foundedAdapter;

    public Marshal(ColumnAdapter<Calendar> foundedAdapter) {
      this.foundedAdapter = foundedAdapter;
    }

    public Marshal(TeamModel copy, ColumnAdapter<Calendar> foundedAdapter) {
      this._id(copy._id());
      this.name(copy.name());
      this.foundedAdapter = foundedAdapter;
      this.founded(copy.founded());
      this.coach(copy.coach());
      this.captain(copy.captain());
      this.won_cup(copy.won_cup());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T _id(long _id) {
      contentValues.put(_ID, _id);
      return (T) this;
    }

    public T name(String name) {
      contentValues.put(NAME, name);
      return (T) this;
    }

    public T founded(Calendar founded) {
      foundedAdapter.marshal(contentValues, FOUNDED, founded);
      return (T) this;
    }

    public T coach(String coach) {
      contentValues.put(COACH, coach);
      return (T) this;
    }

    public T captain(Long captain) {
      contentValues.put(CAPTAIN, captain);
      return (T) this;
    }

    public T won_cup(boolean won_cup) {
      contentValues.put(WON_CUP, won_cup ? 1 : 0);
      return (T) this;
    }
  }

  final class Factory<T extends TeamModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Calendar> foundedAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Calendar> foundedAdapter) {
      this.creator = creator;
      this.foundedAdapter = foundedAdapter;
    }

    public Mapper select_allMapper() {
      return new Mapper<>(this);
    }
  }
}
