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

  String firstName();

  @Nullable
  String middleInitial();

  String lastName();

  int age();

  User.Gender gender();

  final class Mapper<T extends UserModel> {
    private final UserModel.Mapper.Creator<T> creator;

    protected Mapper(UserModel.Mapper.Creator<T> creator) {
      this.creator = creator;
    }

    public T map(Cursor cursor, UserModel defaults) {
      long id = defaults == null ? 0 : defaults.id();
      String firstName = defaults == null ? null : defaults.firstName();
      String middleInitial = defaults == null ? null : defaults.middleInitial();
      String lastName = defaults == null ? null : defaults.lastName();
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
              firstName = cursor.getString(cursor.getColumnIndex("value"));
              break;
            case MIDDLE_INITIAL:
              middleInitial = cursor.isNull(cursor.getColumnIndex("value")) ? null : cursor.getString(cursor.getColumnIndex("value"));
              break;
            case LAST_NAME:
              lastName = cursor.getString(cursor.getColumnIndex("value"));
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
            firstName,
            middleInitial,
            lastName,
            age,
            gender);
      } finally {
        cursor.close();
      }
    }

    public interface Creator<R extends UserModel> {
      R create(long id, String firstName, String middleInitial, String lastName, int age, User.Gender gender);
    }
  }

  class UserMarshal<T extends UserModel.UserMarshal> {
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

    public T firstName(String firstName) {
      if (firstName == null) {
        throw new NullPointerException("Cannot insert NULL value for NOT NULL column first_name");
      }
      ContentValues contentValues = contentValuesMap.get(FIRST_NAME);
      if (contentValues == null) {
        contentValues = new ContentValues();
        contentValues.put("key", FIRST_NAME);
        contentValuesMap.put(FIRST_NAME, contentValues);
      }
      contentValues.put("value", firstName);
      return (T) this;
    }

    public T middleInitial(String middleInitial) {
      ContentValues contentValues = contentValuesMap.get(MIDDLE_INITIAL);
      if (contentValues == null) {
        contentValues = new ContentValues();
        contentValues.put("key", MIDDLE_INITIAL);
        contentValuesMap.put(MIDDLE_INITIAL, contentValues);
      }
      contentValues.put("value", middleInitial);
      return (T) this;
    }

    public T lastName(String lastName) {
      if (lastName == null) {
        throw new NullPointerException("Cannot insert NULL value for NOT NULL column last_name");
      }
      ContentValues contentValues = contentValuesMap.get(LAST_NAME);
      if (contentValues == null) {
        contentValues = new ContentValues();
        contentValues.put("key", LAST_NAME);
        contentValuesMap.put(LAST_NAME, contentValues);
      }
      contentValues.put("value", lastName);
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
