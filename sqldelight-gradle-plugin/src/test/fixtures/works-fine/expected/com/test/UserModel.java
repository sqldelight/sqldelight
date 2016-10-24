package com.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Deprecated;
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

  String SOME_LIST = "some_list";

  String GENDER2 = "gender2";

  String FULL_USER = "full_user";

  String SUCH_LIST = "such_list";

  String CREATE_TABLE = ""
      + "CREATE TABLE users (\n"
      + "  id INTEGER PRIMARY KEY NOT NULL,\n"
      + "  first_name TEXT NOT NULL,\n"
      + "  middle_initial TEXT,\n"
      + "  last_name TEXT NOT NULL,\n"
      + "  age INTEGER NOT NULL DEFAULT 0,\n"
      + "  gender TEXT NOT NULL,\n"
      + "  some_generic BLOB,\n"
      + "  some_list BLOB,\n"
      + "  gender2 TEXT,\n"
      + "  full_user BLOB,\n"
      + "  such_list BLOB\n"
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

  @Nullable
  List<Map<List<List<Integer>>, List<Integer>>> some_list();

  @Nullable
  User.Gender gender2();

  @Nullable
  User full_user();

  @Nullable
  List<List<List<List<String>>>> such_list();

  interface Creator<T extends UserModel> {
    T create(long id, @NonNull String first_name, @Nullable String middle_initial, @NonNull String last_name, int age, @NonNull User.Gender gender, @Nullable Map<List<Integer>, Float> some_generic, @Nullable List<Map<List<List<Integer>>, List<Integer>>> some_list, @Nullable User.Gender gender2, @Nullable User full_user, @Nullable List<List<List<List<String>>>> such_list);
  }

  final class Mapper<T extends UserModel> implements RowMapper<T> {
    private final Factory<T> userModelFactory;

    public Mapper(Factory<T> userModelFactory) {
      this.userModelFactory = userModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return userModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getString(1),
          cursor.isNull(2) ? null : cursor.getString(2),
          cursor.getString(3),
          cursor.getInt(4),
          userModelFactory.genderAdapter.decode(cursor.getString(5)),
          cursor.isNull(6) ? null : userModelFactory.some_genericAdapter.decode(cursor.getBlob(6)),
          cursor.isNull(7) ? null : userModelFactory.some_listAdapter.decode(cursor.getBlob(7)),
          cursor.isNull(8) ? null : userModelFactory.gender2Adapter.decode(cursor.getString(8)),
          cursor.isNull(9) ? null : userModelFactory.full_userAdapter.decode(cursor.getBlob(9)),
          cursor.isNull(10) ? null : userModelFactory.such_listAdapter.decode(cursor.getBlob(10))
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<User.Gender, String> genderAdapter;

    private final ColumnAdapter<Map<List<Integer>, Float>, byte[]> some_genericAdapter;

    private final ColumnAdapter<List<Map<List<List<Integer>>, List<Integer>>>, byte[]> some_listAdapter;

    private final ColumnAdapter<User.Gender, String> gender2Adapter;

    private final ColumnAdapter<User, byte[]> full_userAdapter;

    private final ColumnAdapter<List<List<List<List<String>>>>, byte[]> such_listAdapter;

    Marshal(@Nullable UserModel copy, ColumnAdapter<User.Gender, String> genderAdapter, ColumnAdapter<Map<List<Integer>, Float>, byte[]> some_genericAdapter, ColumnAdapter<List<Map<List<List<Integer>>, List<Integer>>>, byte[]> some_listAdapter, ColumnAdapter<User.Gender, String> gender2Adapter, ColumnAdapter<User, byte[]> full_userAdapter, ColumnAdapter<List<List<List<List<String>>>>, byte[]> such_listAdapter) {
      this.genderAdapter = genderAdapter;
      this.some_genericAdapter = some_genericAdapter;
      this.some_listAdapter = some_listAdapter;
      this.gender2Adapter = gender2Adapter;
      this.full_userAdapter = full_userAdapter;
      this.such_listAdapter = such_listAdapter;
      if (copy != null) {
        this.id(copy.id());
        this.first_name(copy.first_name());
        this.middle_initial(copy.middle_initial());
        this.last_name(copy.last_name());
        this.age(copy.age());
        this.gender(copy.gender());
        this.some_generic(copy.some_generic());
        this.some_list(copy.some_list());
        this.gender2(copy.gender2());
        this.full_user(copy.full_user());
        this.such_list(copy.such_list());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal id(long id) {
      contentValues.put("id", id);
      return this;
    }

    public Marshal first_name(String first_name) {
      contentValues.put("first_name", first_name);
      return this;
    }

    public Marshal middle_initial(String middle_initial) {
      contentValues.put("middle_initial", middle_initial);
      return this;
    }

    public Marshal last_name(String last_name) {
      contentValues.put("last_name", last_name);
      return this;
    }

    public Marshal age(int age) {
      contentValues.put("age", age);
      return this;
    }

    public Marshal gender(@NonNull User.Gender gender) {
      contentValues.put("gender", genderAdapter.encode(gender));
      return this;
    }

    public Marshal some_generic(@Nullable Map<List<Integer>, Float> some_generic) {
      if (some_generic != null) {
        contentValues.put("some_generic", some_genericAdapter.encode(some_generic));
      } else {
        contentValues.putNull("some_generic");
      }
      return this;
    }

    public Marshal some_list(@Nullable List<Map<List<List<Integer>>, List<Integer>>> some_list) {
      if (some_list != null) {
        contentValues.put("some_list", some_listAdapter.encode(some_list));
      } else {
        contentValues.putNull("some_list");
      }
      return this;
    }

    public Marshal gender2(@Nullable User.Gender gender2) {
      if (gender2 != null) {
        contentValues.put("gender2", gender2Adapter.encode(gender2));
      } else {
        contentValues.putNull("gender2");
      }
      return this;
    }

    public Marshal full_user(@Nullable User full_user) {
      if (full_user != null) {
        contentValues.put("full_user", full_userAdapter.encode(full_user));
      } else {
        contentValues.putNull("full_user");
      }
      return this;
    }

    public Marshal such_list(@Nullable List<List<List<List<String>>>> such_list) {
      if (such_list != null) {
        contentValues.put("such_list", such_listAdapter.encode(such_list));
      } else {
        contentValues.putNull("such_list");
      }
      return this;
    }
  }

  final class Factory<T extends UserModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<User.Gender, String> genderAdapter;

    public final ColumnAdapter<Map<List<Integer>, Float>, byte[]> some_genericAdapter;

    public final ColumnAdapter<List<Map<List<List<Integer>>, List<Integer>>>, byte[]> some_listAdapter;

    public final ColumnAdapter<User.Gender, String> gender2Adapter;

    public final ColumnAdapter<User, byte[]> full_userAdapter;

    public final ColumnAdapter<List<List<List<List<String>>>>, byte[]> such_listAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<User.Gender, String> genderAdapter, ColumnAdapter<Map<List<Integer>, Float>, byte[]> some_genericAdapter, ColumnAdapter<List<Map<List<List<Integer>>, List<Integer>>>, byte[]> some_listAdapter, ColumnAdapter<User.Gender, String> gender2Adapter, ColumnAdapter<User, byte[]> full_userAdapter, ColumnAdapter<List<List<List<List<String>>>>, byte[]> such_listAdapter) {
      this.creator = creator;
      this.genderAdapter = genderAdapter;
      this.some_genericAdapter = some_genericAdapter;
      this.some_listAdapter = some_listAdapter;
      this.gender2Adapter = gender2Adapter;
      this.full_userAdapter = full_userAdapter;
      this.such_listAdapter = such_listAdapter;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null, genderAdapter, some_genericAdapter, some_listAdapter, gender2Adapter, full_userAdapter, such_listAdapter);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(UserModel copy) {
      return new Marshal(copy, genderAdapter, some_genericAdapter, some_listAdapter, gender2Adapter, full_userAdapter, such_listAdapter);
    }

    public Mapper<T> femalesMapper() {
      return new Mapper<T>(this);
    }
  }
}
