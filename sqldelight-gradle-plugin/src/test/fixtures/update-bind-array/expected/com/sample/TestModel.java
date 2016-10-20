package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String SOME_TEXT = "some_text";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY,\n"
      + "  some_text TEXT NOT NULL\n"
      + ")";

  String SOME_UPDATE = ""
      + "UPDATE test\n"
      + "SET some_text = 'test'\n"
      + "WHERE _id IN ?";

  long _id();

  @NonNull
  String some_text();

  interface Creator<T extends TestModel> {
    T create(long _id, @NonNull String some_text);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getString(1)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this._id(copy._id());
        this.some_text(copy.some_text());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal some_text(String some_text) {
      contentValues.put("some_text", some_text);
      return this;
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public Marshal marshal() {
      return new Marshal(null);
    }

    public Marshal marshal(TestModel copy) {
      return new Marshal(copy);
    }

    public SqlDelightStatement some_update(long[] _id) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("UPDATE test\n"
              + "SET some_text = 'test'\n"
              + "WHERE _id IN ");
      query.append('(');
      for (int i = 0; i < _id.length; i++) {
        if (i != 0) query.append(", ");
        query.append(_id[i]);
      }
      query.append(')');
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }
  }
}
