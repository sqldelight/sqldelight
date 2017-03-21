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
import java.util.ArrayList;
import java.util.Collections;
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

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this.TestId(copy.TestId());
        this.TestText(copy.TestText());
        this.SecondId(copy.SecondId());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal TestId(long TestId) {
      contentValues.put("TestId", TestId);
      return this;
    }

    public Marshal TestText(String TestText) {
      contentValues.put("TestText", TestText);
      return this;
    }

    public Marshal SecondId(long SecondId) {
      contentValues.put("SecondId", SecondId);
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

    public SqlDelightStatement TEST_QUERY(long SecondId, @NonNull String TestText) {
      List<String> args = new ArrayList<String>();
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
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("TEST"));
    }

    public Mapper<T> tEST_QUERYMapper() {
      return new Mapper<T>(this);
    }
  }
}
