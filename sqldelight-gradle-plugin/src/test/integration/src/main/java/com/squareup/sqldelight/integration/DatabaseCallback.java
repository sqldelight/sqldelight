package com.squareup.sqldelight.integration;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

public final class DatabaseCallback extends SupportSQLiteOpenHelper.Callback {
  public DatabaseCallback() {
    super(1);
  }

  @Override public void onCreate(SupportSQLiteDatabase db) {
    db.execSQL(Person.CREATE_TABLE);
    db.execSQL(SqliteKeywords.CREATE_TABLE);
  }

  @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
    // No-op.
  }
}
