package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface SimpleTableModel {
  String TABLE_NAME = "SimpleTable";

  String ID = "id";

  String TEXT = "text";

  String CREATE_TABLE = ""
      + "CREATE TABLE SimpleTable (\n"
      + "  id INTEGER NOT NULL PRIMARY KEY,\n"
      + "  text TEXT NOT NULL\n"
      + ")";

  String TRIGGER1 = ""
      + "CREATE TRIGGER delete_before_insert\n"
      + "BEFORE INSERT ON SimpleTable\n"
      + "WHEN (SELECT count(*) FROM SimpleTable) > 1000\n"
      + "BEGIN\n"
      + "    DELETE FROM SimpleTable WHERE id = (SELECT min(id) FROM SimpleTable);\n"
      + "END";

  String TRIGGER2 = ""
      + "CREATE TRIGGER update_before_insert\n"
      + "BEFORE INSERT ON SimpleTable\n"
      + "WHEN (SELECT count(*) FROM SimpleTable) > 1000\n"
      + "BEGIN\n"
      + "    UPDATE SimpleTable SET text = 'stuff' WHERE id = (SELECT min(id) FROM SimpleTable);\n"
      + "END";

  long id();

  @NonNull
  String text();

  interface Creator<T extends SimpleTableModel> {
    T create(long id, @NonNull String text);
  }

  final class Mapper<T extends SimpleTableModel> implements RowMapper<T> {
    private final Factory<T> simpleTableModelFactory;

    public Mapper(Factory<T> simpleTableModelFactory) {
      this.simpleTableModelFactory = simpleTableModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return simpleTableModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getString(1)
      );
    }
  }

  final class Marshal {
    final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable SimpleTableModel copy) {
      if (copy != null) {
        this.id(copy.id());
        this.text(copy.text());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal id(long id) {
      contentValues.put("id", id);
      return this;
    }

    public Marshal text(String text) {
      contentValues.put("text", text);
      return this;
    }
  }

  final class Factory<T extends SimpleTableModel> {
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
    public Marshal marshal(SimpleTableModel copy) {
      return new Marshal(copy);
    }
  }
}
