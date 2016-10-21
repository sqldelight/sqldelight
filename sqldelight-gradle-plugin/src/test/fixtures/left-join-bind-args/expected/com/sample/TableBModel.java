package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface TableBModel {
  String TABLE_NAME = "tableb";

  String _ID = "_id";

  String COL1 = "col1";

  String COL2 = "col2";

  String CREATE_TABLE = ""
      + "CREATE TABLE tableb(\n"
      + "  _id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
      + "  col1 INTEGER NOT NULL,\n"
      + "  col2 INTEGER NOT NULL\n"
      + ")";

  @Nullable
  Long _id();

  int col1();

  int col2();

  interface Creator<T extends TableBModel> {
    T create(@Nullable Long _id, int col1, int col2);
  }

  final class Mapper<T extends TableBModel> implements RowMapper<T> {
    private final Factory<T> tableBModelFactory;

    public Mapper(Factory<T> tableBModelFactory) {
      this.tableBModelFactory = tableBModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return tableBModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.getInt(1),
          cursor.getInt(2)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TableBModel copy) {
      if (copy != null) {
        this._id(copy._id());
        this.col1(copy.col1());
        this.col2(copy.col2());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(Long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal col1(int col1) {
      contentValues.put("col1", col1);
      return this;
    }

    public Marshal col2(int col2) {
      contentValues.put("col2", col2);
      return this;
    }
  }

  final class Factory<T extends TableBModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public Marshal marshal() {
      return new Marshal(null);
    }

    public Marshal marshal(TableBModel copy) {
      return new Marshal(copy);
    }
  }
}
