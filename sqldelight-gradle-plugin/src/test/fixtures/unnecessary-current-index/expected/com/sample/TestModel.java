package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.Collections;

public interface TestModel {
  String TABLE_NAME = "test";

  String ID = "id";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT\n"
      + ")";

  long id();

  interface Creator<T extends TestModel> {
    T create(long id);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getLong(0)
      );
    }
  }

  final class Marshal {
    final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this.id(copy.id());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal id(long id) {
      contentValues.put("id", id);
      return this;
    }
  }

  final class Factory<T extends TestModel> {
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
    public Marshal marshal(TestModel copy) {
      return new Marshal(copy);
    }

    public SqlDelightStatement select_by_id(long id) {
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM test\n"
              + "WHERE id = ");
      query.append(id);
      query.append("\n"
              + "LIMIT 1");
      return new SqlDelightStatement(query.toString(), new String[0], Collections.<String>singleton("test"));
    }

    public Mapper<T> select_by_idMapper() {
      return new Mapper<T>(this);
    }
  }
}
