package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.List;

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

    public SqlDelightStatement some_query(@Nullable SomeEnum some_enum, @NonNull String[] token) {
      List<Object> args = new ArrayList<Object>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM test\n"
              + "WHERE some_enum = ");
      if (some_enum == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) some_enumAdapter.encode(some_enum));
      }
      query.append("\n"
              + "AND token IN ");
      query.append('(');
      for (int i = 0; i < token.length; i++) {
        if (i != 0) query.append(", ");
        query.append('?').append(currentIndex++);
        args.add(token[i]);
      }
      query.append(')');
      return new SqlDelightStatement(query.toString(), args.toArray(new Object[args.size()]), new TableSet("test"));
    }

    public Mapper<T> some_queryMapper() {
      return new Mapper<T>(this);
    }
  }
}
