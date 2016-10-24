package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface ForeignTableModel {
  String TABLE_NAME = "foreign_table";

  String _ID = "_id";

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

    public Mapper(Factory<T> foreignTableModelFactory) {
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

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Test.TestEnum, String> test_enumAdapter;

    Marshal(@Nullable ForeignTableModel copy, ColumnAdapter<Test.TestEnum, String> test_enumAdapter) {
      this.test_enumAdapter = test_enumAdapter;
      if (copy != null) {
        this._id(copy._id());
        this.test_enum(copy.test_enum());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal test_enum(@Nullable Test.TestEnum test_enum) {
      if (test_enum != null) {
        contentValues.put("test_enum", test_enumAdapter.encode(test_enum));
      } else {
        contentValues.putNull("test_enum");
      }
      return this;
    }
  }

  final class Factory<T extends ForeignTableModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Test.TestEnum, String> test_enumAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Test.TestEnum, String> test_enumAdapter) {
      this.creator = creator;
      this.test_enumAdapter = test_enumAdapter;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null, test_enumAdapter);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(ForeignTableModel copy) {
      return new Marshal(copy, test_enumAdapter);
    }
  }
}
