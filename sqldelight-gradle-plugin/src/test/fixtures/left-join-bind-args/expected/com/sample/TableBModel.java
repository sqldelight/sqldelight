package com.sample;

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

  final class Factory<T extends TableBModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }
  }
}
