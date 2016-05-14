package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.Map;

public interface UserModel {
  String TABLE_NAME = "users";

  String ID = "id";

  String FIRST_NAME = "first_name";

  String MIDDLE_INITIAL = "middle_initial";

  String LAST_NAME = "last_name";

  String AGE = "age";

  String GENDER = "gender";

  String SOME_GENERIC = "some_generic";

  String CREATE_TABLE = ""
      + "CREATE TABLE users (\n"
      + "  id INTEGER PRIMARY KEY NOT NULL,\n"
      + "  first_name TEXT NOT NULL,\n"
      + "  middle_initial TEXT,\n"
      + "  last_name TEXT NOT NULL,\n"
      + "  age INTEGER NOT NULL DEFAULT 0,\n"
      + "  gender TEXT NOT NULL,\n"
      + "  some_generic BLOB\n"
      + ")";

  String FEMALES = ""
      + "SELECT *\n"
      + "FROM users\n"
      + "WHERE gender = 'FEMALE'";

  long id();

  @NonNull
  String first_name();

  @Nullable
  String middle_initial();

  @NonNull
  String last_name();

  int age();

  @NonNull
  User.Gender gender();

  @Nullable
  Map<List<Integer>, Float> some_generic();

  final class Mapper<T extends UserModel> implements RowMapper<T> {
    private final Creator<T> creator;

    private final ColumnAdapter<User.Gender> genderAdapter;

    private final ColumnAdapter<Map<List<Integer>, Float>> some_genericAdapter;

    protected Mapper(Creator<T> creator, ColumnAdapter<User.Gender> genderAdapter, ColumnAdapter<Map<List<Integer>, Float>> some_genericAdapter) {
      this.creator = creator;
      this.genderAdapter = genderAdapter;
      this.some_genericAdapter = some_genericAdapter;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(cursor.getColumnIndex(ID)),
          cursor.getString(cursor.getColumnIndex(FIRST_NAME)),
          cursor.isNull(cursor.getColumnIndex(MIDDLE_INITIAL)) ? null : cursor.getString(cursor.getColumnIndex(MIDDLE_INITIAL)),
          cursor.getString(cursor.getColumnIndex(LAST_NAME)),
          cursor.getInt(cursor.getColumnIndex(AGE)),
          genderAdapter.map(cursor, cursor.getColumnIndex(GENDER)),
          cursor.isNull(cursor.getColumnIndex(SOME_GENERIC)) ? null : some_genericAdapter.map(cursor, cursor.getColumnIndex(SOME_GENERIC))
      );
    }

    public interface Creator<R extends UserModel> {
      R create(long id, String first_name, String middle_initial, String last_name, int age, User.Gender gender, Map<List<Integer>, Float> some_generic);
    }
  }

  class UserMarshal<T extends UserMarshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<User.Gender> genderAdapter;

    private final ColumnAdapter<Map<List<Integer>, Float>> some_genericAdapter;

    public UserMarshal(ColumnAdapter<User.Gender> genderAdapter, ColumnAdapter<Map<List<Integer>, Float>> some_genericAdapter) {
      this.genderAdapter = genderAdapter;
      this.some_genericAdapter = some_genericAdapter;
    }

    public UserMarshal(UserModel copy, ColumnAdapter<User.Gender> genderAdapter, ColumnAdapter<Map<List<Integer>, Float>> some_genericAdapter) {
      this.id(copy.id());
      this.first_name(copy.first_name());
      this.middle_initial(copy.middle_initial());
      this.last_name(copy.last_name());
      this.age(copy.age());
      this.genderAdapter = genderAdapter;
      this.gender(copy.gender());
      this.some_genericAdapter = some_genericAdapter;
      this.some_generic(copy.some_generic());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T id(long id) {
      contentValues.put(ID, id);
      return (T) this;
    }

    public T first_name(String first_name) {
      contentValues.put(FIRST_NAME, first_name);
      return (T) this;
    }

    public T middle_initial(String middle_initial) {
      contentValues.put(MIDDLE_INITIAL, middle_initial);
      return (T) this;
    }

    public T last_name(String last_name) {
      contentValues.put(LAST_NAME, last_name);
      return (T) this;
    }

    public T age(int age) {
      contentValues.put(AGE, age);
      return (T) this;
    }

    public T gender(User.Gender gender) {
      genderAdapter.marshal(contentValues, GENDER, gender);
      return (T) this;
    }

    public T some_generic(Map<List<Integer>, Float> some_generic) {
      some_genericAdapter.marshal(contentValues, SOME_GENERIC, some_generic);
      return (T) this;
    }
  }
}
