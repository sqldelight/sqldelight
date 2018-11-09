package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.squareup.sqldelight.prerelease.ColumnAdapter;
import com.squareup.sqldelight.prerelease.RowMapper;
import com.squareup.sqldelight.prerelease.SqlDelightQuery;
import com.squareup.sqldelight.prerelease.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface ForeignTableModel {
  @Deprecated
  String TABLE_NAME = "foreign_table";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String TEST_ENUM = "test_enum";

  String CREATE_TABLE = ""
      + "CREATE TABLE foreign_table (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  test_enum TEXT\n"
      + ")";

  long _id();

  @Nullable
  Test.TestEnum test_enum();

  interface Creator<T extends ForeignTableModel> {
    T create(long _id, @Nullable Test.TestEnum test_enum);
  }

  final class Mapper<T extends ForeignTableModel> implements RowMapper<T> {
    private final Factory<T> foreignTableModelFactory;

    public Mapper(@NonNull Factory<T> foreignTableModelFactory) {
      this.foreignTableModelFactory = foreignTableModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return foreignTableModelFactory.creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : foreignTableModelFactory.test_enumAdapter.decode(cursor.getString(1))
      );
    }
  }

  final class Factory<T extends ForeignTableModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Test.TestEnum, String> test_enumAdapter;

    public Factory(@NonNull Creator<T> creator,
        @NonNull ColumnAdapter<Test.TestEnum, String> test_enumAdapter) {
      this.creator = creator;
      this.test_enumAdapter = test_enumAdapter;
    }

    @NonNull
    public SqlDelightQuery external_table() {
      return new SqlDelightQuery(""
          + "SELECT enum_value\n"
          + "FROM test",
          new TableSet("test"));
    }

    public <T extends TestModel> RowMapper<Test.TestEnum> external_tableMapper(
        final TestModel.Factory<T> testModelFactory) {
      return new RowMapper<Test.TestEnum>() {
        @Override
        public Test.TestEnum map(Cursor cursor) {
          return cursor.isNull(0) ? null : testModelFactory.enum_valueAdapter.decode(cursor.getString(0));
        }
      };
    }
  }
}
