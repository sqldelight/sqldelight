package com.squareup.sqldelight.integration;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
  public DatabaseHelper(Context context) {
    super(context, null, null, 1);
  }

  @Override public void onCreate(SQLiteDatabase db) {
    db.execSQL(Person.CREATE_TABLE);
    db.execSQL(SqliteKeywords.CREATE_TABLE);
  }

  @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // No-op.
  }
}
