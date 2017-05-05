package com.sample;

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

/**
 * This is a table.
 */
public interface TestModel {
  String TABLE_NAME = "test";

  /**
   * This is a column.
   */
  String _ID = "_id";

  /**
   * Another
   */
  String NAME = "name";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "\n"
      + "  name TEXT NULL\n"
      + ")";

  /**
   * This is a column.
   */
  long _id();

  /**
   * Another
   */
  @Nullable
  String name();

  interface Creator<T extends TestModel> {
    T create(long _id, @Nullable String name);
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
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    /**
     * Javadoc comment yo.
     */
    public SqlDelightStatement some_select() {
      return new SqlDelightStatement(""
          + "SELECT *\n"
          + "FROM test",
          new String[0], Collections.<String>singleton("test"));
    }

    /**
     * Dis too
     */
    public SqlDelightStatement some_select_2() {
      return new SqlDelightStatement(""
          + "SELECT *\n"
          + "FROM test",
          new String[0], Collections.<String>singleton("test"));
    }

    /**
     * This also works
     */
    public SqlDelightStatement some_select_3() {
      return new SqlDelightStatement(""
          + "SELECT *\n"
          + "FROM test",
          new String[0], Collections.<String>singleton("test"));
    }

    /**
     * @param name The name to search for
     */
    public SqlDelightStatement someSelect4(@Nullable String name) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM test\n"
              + "WHERE name = ");
      if (name == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add(name);
      }
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }

    /**
     * Javadoc comment yo.
     */
    public Mapper<T> some_selectMapper() {
      return new Mapper<T>(this);
    }

    /**
     * Dis too
     */
    public Mapper<T> some_select_2Mapper() {
      return new Mapper<T>(this);
    }

    /**
     * This also works
     */
    public Mapper<T> some_select_3Mapper() {
      return new Mapper<T>(this);
    }

    /**
     * @param name The name to search for
     */
    public Mapper<T> someSelect4Mapper() {
      return new Mapper<T>(this);
    }
  }
}
