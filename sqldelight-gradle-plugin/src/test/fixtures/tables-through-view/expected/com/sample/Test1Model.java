package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface Test1Model {
  String TABLE_NAME = "test1";

  String _ID = "_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE test1 (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT\n"
      + ")";

  long _id();

  interface Creator<T extends Test1Model> {
    T create(long _id);
  }

  final class Mapper<T extends Test1Model> implements RowMapper<T> {
    private final Factory<T> test1ModelFactory;

    public Mapper(Factory<T> test1ModelFactory) {
      this.test1ModelFactory = test1ModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return test1ModelFactory.creator.create(
          cursor.getLong(0)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable Test1Model copy) {
      if (copy != null) {
        this._id(copy._id());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
      return this;
    }
  }

  final class Factory<T extends Test1Model> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(Test1Model copy) {
      return new Marshal(copy);
    }
  }
}
