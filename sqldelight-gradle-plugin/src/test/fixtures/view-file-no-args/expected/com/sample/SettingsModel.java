package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.RowMapper;
import java.lang.Override;
import java.lang.String;

public interface SettingsModel {
  String TABLE_NAME = "settings";

  String ROW_ID = "row_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE settings (\n"
      + "  row_id INTEGER NOT NULL PRIMARY KEY\n"
      + ")";

  long row_id();

  interface Creator<T extends SettingsModel> {
    T create(long row_id);
  }

  final class Mapper<T extends SettingsModel> implements RowMapper<T> {
    private final Factory<T> settingsModelFactory;

    public Mapper(Factory<T> settingsModelFactory) {
      this.settingsModelFactory = settingsModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return settingsModelFactory.creator.create(
          cursor.getLong(0)
      );
    }
  }

  final class Factory<T extends SettingsModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }
  }
}
