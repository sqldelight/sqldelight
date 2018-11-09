package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface SimpleTableModel {
  @Deprecated
  String TABLE_NAME = "SimpleTable";

  @Deprecated
  String ID = "id";

  @Deprecated
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

    public Mapper(@NonNull Factory<T> simpleTableModelFactory) {
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

  final class Factory<T extends SimpleTableModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }
  }
}
