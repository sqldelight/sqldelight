package com.test;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface UserModel {
  @Deprecated
  String TABLE_NAME = "users";

  @Deprecated
  String ID = "id";

  @Deprecated
  String FIRST_NAME = "first_name";

  @Deprecated
  String MIDDLE_INITIAL = "middle_initial";

  @Deprecated
  String LAST_NAME = "last_name";

  @Deprecated
  String AGE = "age";

  @Deprecated
  String GENDER = "gender";

  String CREATE_TABLE = ""
      + "CREATE TABLE users (\n"
      + "  id INTEGER PRIMARY KEY NOT NULL,\n"
      + "  first_name TEXT NOT NULL,\n"
      + "  middle_initial TEXT,\n"
      + "  last_name TEXT NOT NULL,\n"
      + "  age INTEGER NOT NULL DEFAULT 0,\n"
      + "  gender TEXT NOT NULL\n"
      + ")";

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

  interface Creator<T extends UserModel> {
    T create(long id, @NonNull String first_name, @Nullable String middle_initial,
        @NonNull String last_name, int age, @NonNull User.Gender gender);
  }

  final class Mapper<T extends UserModel> implements RowMapper<T> {
    private final Factory<T> userModelFactory;

    public Mapper(@NonNull Factory<T> userModelFactory) {
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
          userModelFactory.genderAdapter.decode(cursor.getString(5))
      );
    }
  }

  final class Factory<T extends UserModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<User.Gender, String> genderAdapter;

    public Factory(@NonNull Creator<T> creator,
        @NonNull ColumnAdapter<User.Gender, String> genderAdapter) {
      this.creator = creator;
      this.genderAdapter = genderAdapter;
    }

    @NonNull
    public SqlDelightQuery females() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM users\n"
          + "WHERE gender = 'FEMALE'",
          new TableSet("users"));
    }

    @NonNull
    public Mapper<T> femalesMapper() {
      return new Mapper<T>(this);
    }
  }
}
