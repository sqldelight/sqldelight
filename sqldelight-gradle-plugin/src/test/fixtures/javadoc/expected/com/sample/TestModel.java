package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

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
   * Javadoc comment yo.
   */
  String SOME_SELECT = ""
      + "SELECT *\n"
      + "FROM test";

  /**
   * Dis too
   */
  String SOME_SELECT_2 = ""
      + "SELECT *\n"
      + "FROM test";

  /**
   * This also works
   */
  String SOME_SELECT_3 = ""
      + "SELECT *\n"
      + "FROM test";

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

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this._id(copy._id());
        this.name(copy.name());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    /**
     * This is a column.
     */
    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    /**
     * Another
     */
    public Marshal name(String name) {
      contentValues.put("name", name);
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
  }
}
