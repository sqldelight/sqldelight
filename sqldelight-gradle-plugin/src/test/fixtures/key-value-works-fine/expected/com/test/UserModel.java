package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;
import java.lang.String;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public interface UserModel {
  String TABLE_NAME = "users";

  String ID = "id";

  String FIRST_NAME = "first_name";

  String MIDDLE_INITIAL = "middle_initial";

  String LAST_NAME = "last_name";

  String AGE = "age";

  String GENDER = "gender";

  String QUERY = ""
      + "SELECT *\n"
      + "  FROM users\n"
      + " WHERE key IN ('id', 'first_name', 'middle_initial', 'last_name', 'age', 'gender')";

  String CREATE_TABLE = ""
      + "CREATE TABLE users (\n"
      + "  key TEXT NOT NULL PRIMARY KEY,\n"
      + "  value BLOB\n"
      + ");";

  long id();

  String first_name();

  @Nullable
  String middle_initial();

  String last_name();

  int age();

  User.Gender gender();

  final class Mapper<T extends UserModel> {
    private final Creator<T> creator;

    protected Mapper(Creator<T> creator) {
      this.creator = creator;
    }

    public T map(Cursor cursor, UserModel defaults) {
      long id = defaults == null ? 0 : defaults.id();
      String first_name = defaults == null ? null : defaults.first_name();
      String middle_initial = defaults == null ? null : defaults.middle_initial();
      String last_name = defaults == null ? null : defaults.last_name();
      int age = defaults == null ? 0 : defaults.age();
      User.Gender gender = defaults == null ? null : defaults.gender();
      try {
        while (cursor.moveToNext()) {
          String key = cursor.getString(cursor.getColumnIndexOrThrow("key"));
          switch (key) {
            case ID:
              id = cursor.getLong(cursor.getColumnIndex("value"));
              break;
            case FIRST_NAME:
              first_name = cursor.getString(cursor.getColumnIndex("value"));
              break;
            case MIDDLE_INITIAL:
              middle_initial = cursor.isNull(cursor.getColumnIndex("value")) ? null : cursor.getString(cursor.getColumnIndex("value"));
              break;
            case LAST_NAME:
              last_name = cursor.getString(cursor.getColumnIndex("value"));
              break;
            case AGE:
              age = cursor.getInt(cursor.getColumnIndex("value"));
              break;
            case GENDER:
              gender = User.Gender.valueOf(cursor.getString(cursor.getColumnIndex("value")));
              break;
          }
        }
        return creator.create(id,
            first_name,
            middle_initial,
            last_name,
            age,
            gender);
      } finally {
        cursor.close();
      }
    }

    public interface Creator<R extends UserModel> {
      R create(long id, String first_name, String middle_initial, String last_name, int age, User.Gender gender);
    }
  }

  class UserMarshal<T extends UserMarshal<T>> {
    protected final Map<String, ContentValues> contentValuesMap = new LinkedHashMap<>();

    public UserMarshal() {
    }

    public final Collection<ContentValues> asContentValues() {
      return contentValuesMap.values();
    }

    public T id(long id) {
      ContentValues contentValues = contentValuesMap.get(ID);
      if (contentValues == null) {
        contentValues = new ContentValues();
        contentValues.put("key", ID);
        contentValuesMap.put(ID, contentValues);
      }
      contentValues.put("value", id);
      return (T) this;
    }

    public T first_name(String first_name) {
      if (first_name == null) {
        throw new NullPointerException("Cannot insert NULL value for NOT NULL column first_name");
      }
      ContentValues contentValues = contentValuesMap.get(FIRST_NAME);
      if (contentValues == null) {
        contentValues = new ContentValues();
        contentValues.put("key", FIRST_NAME);
        contentValuesMap.put(FIRST_NAME, contentValues);
      }
      contentValues.put("value", first_name);
      return (T) this;
    }

    public T middle_initial(String middle_initial) {
      ContentValues contentValues = contentValuesMap.get(MIDDLE_INITIAL);
      if (contentValues == null) {
        contentValues = new ContentValues();
        contentValues.put("key", MIDDLE_INITIAL);
        contentValuesMap.put(MIDDLE_INITIAL, contentValues);
      }
      contentValues.put("value", middle_initial);
      return (T) this;
    }

    public T last_name(String last_name) {
      if (last_name == null) {
        throw new NullPointerException("Cannot insert NULL value for NOT NULL column last_name");
      }
      ContentValues contentValues = contentValuesMap.get(LAST_NAME);
      if (contentValues == null) {
        contentValues = new ContentValues();
        contentValues.put("key", LAST_NAME);
        contentValuesMap.put(LAST_NAME, contentValues);
      }
      contentValues.put("value", last_name);
      return (T) this;
    }

    public T age(int age) {
      ContentValues contentValues = contentValuesMap.get(AGE);
      if (contentValues == null) {
        contentValues = new ContentValues();
        contentValues.put("key", AGE);
        contentValuesMap.put(AGE, contentValues);
      }
      contentValues.put("value", age);
      return (T) this;
    }

    public T gender(User.Gender gender) {
      if (gender == null) {
        throw new NullPointerException("Cannot insert NULL value for NOT NULL column gender");
      }
      ContentValues contentValues = contentValuesMap.get(GENDER);
      if (contentValues == null) {
        contentValues = new ContentValues();
        contentValues.put("key", GENDER);
        contentValuesMap.put(GENDER, contentValues);
      }
      contentValues.put("value", gender.name());
      return (T) this;
    }
  }
}
