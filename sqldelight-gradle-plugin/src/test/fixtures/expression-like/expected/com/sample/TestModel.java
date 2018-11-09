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

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "employee";

  @Deprecated
  String ID = "id";

  @Deprecated
  String DEPARTMENT = "department";

  @Deprecated
  String NAME = "name";

  @Deprecated
  String TITLE = "title";

  @Deprecated
  String BIO = "bio";

  String CREATE_TABLE = ""
      + "CREATE TABLE employee (\n"
      + "  id INTEGER NOT NULL PRIMARY KEY,\n"
      + "  department TEXT NOT NULL,\n"
      + "  name TEXT NOT NULL,\n"
      + "  title TEXT NOT NULL,\n"
      + "  bio TEXT NOT NULL\n"
      + ")";

  long id();

  @NonNull
  String department();

  @NonNull
  String name();

  @NonNull
  String title();

  @NonNull
  String bio();

  interface Creator<T extends TestModel> {
    T create(long id, @NonNull String department, @NonNull String name, @NonNull String title,
        @NonNull String bio);
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
          cursor.getString(1),
          cursor.getString(2),
          cursor.getString(3),
          cursor.getString(4)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery some_select(@NonNull String department, @Nullable String arg2,
        @Nullable String arg3, @Nullable String arg4) {
      return new Some_selectQuery(department, arg2, arg3, arg4);
    }

    @NonNull
    public Mapper<T> some_selectMapper() {
      return new Mapper<T>(this);
    }

    private final class Some_selectQuery extends SqlDelightQuery {
      @NonNull
      private final String department;

      @Nullable
      private final String arg2;

      @Nullable
      private final String arg3;

      @Nullable
      private final String arg4;

      Some_selectQuery(@NonNull String department, @Nullable String arg2, @Nullable String arg3,
          @Nullable String arg4) {
        super("SELECT *\n"
            + "FROM employee\n"
            + "WHERE department = ?1\n"
            + "AND (\n"
            + "  name LIKE '%' || ?2 || '%'\n"
            + "  OR title LIKE '%' || ?3 || '%'\n"
            + "  OR bio LIKE '%' || ?4 || '%'\n"
            + ")\n"
            + "ORDER BY department",
            new TableSet("employee"));

        this.department = department;
        this.arg2 = arg2;
        this.arg3 = arg3;
        this.arg4 = arg4;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindString(1, department);

        String arg2 = this.arg2;
        if (arg2 != null) {
          program.bindString(2, arg2);
        } else {
          program.bindNull(2);
        }

        String arg3 = this.arg3;
        if (arg3 != null) {
          program.bindString(3, arg3);
        } else {
          program.bindNull(3);
        }

        String arg4 = this.arg4;
        if (arg4 != null) {
          program.bindString(4, arg4);
        } else {
          program.bindNull(4);
        }
      }
    }
  }
}
