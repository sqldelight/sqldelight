package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteProgram;
import com.squareup.sqldelight.prerelease.RowMapper;
import com.squareup.sqldelight.prerelease.SqlDelightQuery;
import com.squareup.sqldelight.prerelease.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

/**
 * This is a table.
 */
public interface TestModel {
  @Deprecated
  String TABLE_NAME = "test";

  /**
   * This is a column.
   */
  @Deprecated
  String _ID = "_id";

  /**
   * Another
   */
  @Deprecated
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

    public Mapper(@NonNull Factory<T> testModelFactory) {
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

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    /**
     * Javadoc comment yo.
     */
    @NonNull
    public SqlDelightQuery some_select() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM test",
          new TableSet("test"));
    }

    /**
     * Dis too
     */
    @NonNull
    public SqlDelightQuery some_select_2() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM test",
          new TableSet("test"));
    }

    /**
     * This also works
     */
    @NonNull
    public SqlDelightQuery some_select_3() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM test",
          new TableSet("test"));
    }

    /**
     * @param name The name to search for
     */
    @NonNull
    public SqlDelightQuery someSelect4(@Nullable String name) {
      return new SomeSelect4Query(name);
    }

    /**
     * Javadoc comment yo.
     */
    @NonNull
    public Mapper<T> some_selectMapper() {
      return new Mapper<T>(this);
    }

    /**
     * Dis too
     */
    @NonNull
    public Mapper<T> some_select_2Mapper() {
      return new Mapper<T>(this);
    }

    /**
     * This also works
     */
    @NonNull
    public Mapper<T> some_select_3Mapper() {
      return new Mapper<T>(this);
    }

    /**
     * @param name The name to search for
     */
    @NonNull
    public Mapper<T> someSelect4Mapper() {
      return new Mapper<T>(this);
    }

    private final class SomeSelect4Query extends SqlDelightQuery {
      @Nullable
      private final String name;

      SomeSelect4Query(@Nullable String name) {
        super("SELECT *\n"
            + "FROM test\n"
            + "WHERE name = ?1",
            new TableSet("test"));

        this.name = name;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        String name = this.name;
        if (name != null) {
          program.bindString(1, name);
        } else {
          program.bindNull(1);
        }
      }
    }
  }
}
