package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public interface TableTwoModel {
  @Deprecated
  String TABLE_NAME = "table_two";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String TYPE = "type";

  String CREATE_TABLE = ""
      + "CREATE TABLE table_two(\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  type TEXT\n"
      + ")";

  long _id();

  @Nullable
  List type();

  interface Creator<T extends TableTwoModel> {
    T create(long _id, @Nullable List type);
  }

  final class Mapper<T extends TableTwoModel> implements RowMapper<T> {
    private final Factory<T> tableTwoModelFactory;

    public Mapper(Factory<T> tableTwoModelFactory) {
      this.tableTwoModelFactory = tableTwoModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return tableTwoModelFactory.creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : tableTwoModelFactory.typeAdapter.decode(cursor.getString(1))
      );
    }
  }

  final class Factory<T extends TableTwoModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<List, String> typeAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<List, String> typeAdapter) {
      this.creator = creator;
      this.typeAdapter = typeAdapter;
    }
  }
}
