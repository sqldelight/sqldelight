package com.sample;

import android.arch.persistence.db.SupportSQLiteProgram;
import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "test";

  @Deprecated
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

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightQuery select_by_id(long id) {
      return new Select_by_idQuery(id);
    }

    public Mapper<T> select_by_idMapper() {
      return new Mapper<T>(this);
    }

    private final class Select_by_idQuery extends SqlDelightQuery {
      private final long id;

      Select_by_idQuery(long id) {
        super("SELECT *\n"
            + "FROM test\n"
            + "WHERE id = ?1\n"
            + "LIMIT 1",
            new TableSet("test"));

        this.id = id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, id);
      }
    }
  }
}
