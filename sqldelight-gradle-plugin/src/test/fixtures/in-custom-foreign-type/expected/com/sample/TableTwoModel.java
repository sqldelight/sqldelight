package com.sample;

import android.content.ContentValues;
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
  String TABLE_NAME = "table_two";

  String _ID = "_id";

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

  final class Marshal {
    final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<List, String> typeAdapter;

    Marshal(@Nullable TableTwoModel copy, ColumnAdapter<List, String> typeAdapter) {
      this.typeAdapter = typeAdapter;
      if (copy != null) {
        this._id(copy._id());
        this.type(copy.type());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal type(@Nullable List type) {
      if (type != null) {
        contentValues.put("type", typeAdapter.encode(type));
      } else {
        contentValues.putNull("type");
      }
      return this;
    }
  }

  final class Factory<T extends TableTwoModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<List, String> typeAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<List, String> typeAdapter) {
      this.creator = creator;
      this.typeAdapter = typeAdapter;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null, typeAdapter);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(TableTwoModel copy) {
      return new Marshal(copy, typeAdapter);
    }
  }
}
