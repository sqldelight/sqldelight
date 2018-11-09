package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteProgram;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "TEST";

  @Deprecated
  String TESTID = "TestId";

  @Deprecated
  String TESTTEXT = "TestText";

  @Deprecated
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

    public Mapper(@NonNull Factory<T> testModelFactory) {
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

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery TEST_QUERY(long SecondId, @NonNull String TestText) {
      return new TEST_QUERYQuery(SecondId, TestText);
    }

    @NonNull
    public Mapper<T> tEST_QUERYMapper() {
      return new Mapper<T>(this);
    }

    private final class TEST_QUERYQuery extends SqlDelightQuery {
      private final long SecondId;

      @NonNull
      private final String TestText;

      TEST_QUERYQuery(long SecondId, @NonNull String TestText) {
        super("SELECT *\n"
            + "FROM TEST\n"
            + "WHERE SecondId = ?1 AND TestText LIKE ?2 ESCAPE '\\' COLLATE NOCASE",
            new TableSet("TEST"));

        this.SecondId = SecondId;
        this.TestText = TestText;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, SecondId);

        program.bindString(2, TestText);
      }
    }
  }
}
