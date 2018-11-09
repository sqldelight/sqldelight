package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import com.squareup.sqldelight.prerelease.RowMapper;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface SettingsModel {
  @Deprecated
  String TABLE_NAME = "settings";

  @Deprecated
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

    public Mapper(@NonNull Factory<T> settingsModelFactory) {
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

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }
  }
}
