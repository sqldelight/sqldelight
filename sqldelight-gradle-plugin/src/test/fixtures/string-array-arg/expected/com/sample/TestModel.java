package com.sample;

import android.arch.persistence.db.SupportSQLiteProgram;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.QuestionMarks;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "test";

  String TOKEN = "token";

  String SOME_ENUM = "some_enum";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  token TEXT NOT NULL,\n"
      + "  some_enum TEXT\n"
      + ")";

  @NonNull
  String token();

  @Nullable
  SomeEnum some_enum();

  interface Creator<T extends TestModel> {
    T create(@NonNull String token, @Nullable SomeEnum some_enum);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getString(0),
          cursor.isNull(1) ? null : testModelFactory.some_enumAdapter.decode(cursor.getString(1))
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<SomeEnum, String> some_enumAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<SomeEnum, String> some_enumAdapter) {
      this.creator = creator;
      this.some_enumAdapter = some_enumAdapter;
    }

    public SqlDelightQuery some_query(@Nullable SomeEnum some_enum, @NonNull String[] token) {
      return new Some_queryQuery(some_enum, token);
    }

    public Mapper<T> some_queryMapper() {
      return new Mapper<T>(this);
    }

    private final class Some_queryQuery extends SqlDelightQuery {
      @Nullable
      private final SomeEnum some_enum;

      @NonNull
      private final String[] token;

      Some_queryQuery(@Nullable SomeEnum some_enum, @NonNull String[] token) {
        super("SELECT *\n"
            + "FROM test\n"
            + "WHERE some_enum = ?1\n"
            + "AND token IN " + QuestionMarks.ofSize(token.length),
            new TableSet("test"));

        this.some_enum = some_enum;
        this.token = token;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        SomeEnum some_enum = this.some_enum;
        if (some_enum != null) {
          program.bindString(1, some_enumAdapter.encode(some_enum));
        } else {
          program.bindNull(1);
        }

        int nextIndex = 2;

        for (String item : token) {
          program.bindString(nextIndex++, item);
        }
      }
    }
  }
}
