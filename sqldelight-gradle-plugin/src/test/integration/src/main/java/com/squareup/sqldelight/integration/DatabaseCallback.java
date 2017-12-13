package com.squareup.sqldelight.integration;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;

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
