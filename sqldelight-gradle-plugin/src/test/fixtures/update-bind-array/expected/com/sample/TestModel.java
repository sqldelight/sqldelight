package com.sample;

import android.arch.persistence.db.SupportSQLiteProgram;
import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.QuestionMarks;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "test";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String SOME_TEXT = "some_text";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY,\n"
      + "  some_text TEXT NOT NULL\n"
      + ")";

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

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightQuery some_update(long[] _id) {
      return new Some_updateQuery(_id);
    }

    private final class Some_updateQuery extends SqlDelightQuery {
      private final long[] _id;

      Some_updateQuery(long[] _id) {
        super("UPDATE test\n"
            + "SET some_text = 'test'\n"
            + "WHERE _id IN " + QuestionMarks.ofSize(_id.length),
            new TableSet("test"));

        this._id = _id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        int nextIndex = 1;

        for (long item : _id) {
          program.bindLong(nextIndex++, item);
        }
      }
    }
  }
}
