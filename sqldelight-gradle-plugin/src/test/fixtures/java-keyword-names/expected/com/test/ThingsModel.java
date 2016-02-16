package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import java.lang.String;

public interface ThingsModel {
  String TABLE_NAME = "things";

  String BYTE = "byte";

  String PACKAGE = "package";

  String CREATE_TABLE = ""
      + "CREATE TABLE things (\n"
      + "  byte TEXT NOT NUll,\n"
      + "  package TEXT NOT NULL\n"
      + ")";

  String byte_();

  String package_();

  final class Mapper<T extends ThingsModel> {
    private final Creator<T> creator;

    protected Mapper(Creator<T> creator) {
      this.creator = creator;
    }

    public T map(Cursor cursor) {
      return creator.create(
          cursor.getString(cursor.getColumnIndex(BYTE)),
          cursor.getString(cursor.getColumnIndex(PACKAGE))
      );
    }

    public interface Creator<R extends ThingsModel> {
      R create(String byte_, String package_);
    }
  }

  class ThingsMarshal<T extends ThingsMarshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    public ThingsMarshal() {
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T byte_(String byte_) {
      contentValues.put(BYTE, byte_);
      return (T) this;
    }

    public T package_(String package_) {
      contentValues.put(PACKAGE, package_);
      return (T) this;
    }
  }
}
