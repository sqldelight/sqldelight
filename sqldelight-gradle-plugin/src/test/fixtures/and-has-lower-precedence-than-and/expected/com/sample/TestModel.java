package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
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
  String TABLE_NAME = "TEST";

  String TESTID = "TestId";

  String TESTTEXT = "TestText";

  String SECONDID = "SecondId";

  String CREATE_TABLE = ""
      + "CREATE TABLE TEST (\n"
      + "  TestId INTEGER NOT NULL,\n"
      + "  TestText TEXT NOT NULL COLLATE NOCASE,\n"
      + "  SecondId INTEGER NOT NULL,\n"
      + "  PRIMARY KEY (TestId)\n"
      + ")";

  long TestId();

  @NonNull
  String TestText();

  long SecondId();

  interface Creator<T extends TestModel> {
    T create(long TestId, @NonNull String TestText, long SecondId);
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
          cursor.getString(1),
          cursor.getLong(2)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightStatement TEST_QUERY(long SecondId, @NonNull String TestText) {
      List<Object> args = new ArrayList<Object>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM TEST\n"
              + "WHERE SecondId = ");
      query.append(SecondId);
      query.append(" AND TestText LIKE ");
      query.append('?').append(currentIndex++);
      args.add(TestText);
      query.append(" ESCAPE '\\' COLLATE NOCASE");
      return new SqlDelightStatement(query.toString(), args.toArray(new Object[args.size()]), new TableSet("TEST"));
    }

    public Mapper<T> tEST_QUERYMapper() {
      return new Mapper<T>(this);
    }
  }
}
