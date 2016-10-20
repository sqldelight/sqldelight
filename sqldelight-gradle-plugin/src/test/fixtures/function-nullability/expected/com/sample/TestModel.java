package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String NAME = "name";

  String GENDER = "gender";

  String MIDDLE_NAME = "middle_name";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  name TEXT NOT NULL,\n"
      + "  gender TEXT NOT NULL,\n"
      + "  middle_name TEXT\n"
      + ")";

  String NAMES_FOR_GENDER = ""
      + "SELECT gender, group_concat(DISTINCT name)\n"
      + "FROM test\n"
      + "GROUP BY gender";

  String MIDDLE_NAMES_FOR_GENDER = ""
      + "SELECT gender, group_concat(DISTINCT middle_name)\n"
      + "FROM test\n"
      + "GROUP BY gender";

  String UPPER_NAMES = ""
      + "SELECT upper(name), upper(middle_name)\n"
      + "FROM test";

  String LOWER_NAMES = ""
      + "SELECT lower(name), lower(middle_name)\n"
      + "FROM test";

  String NULLIF_NAMES = ""
      + "SELECT _id, nullif(name, middle_name)\n"
      + "FROM test";

  long _id();

  @NonNull
  String name();

  @NonNull
  String gender();

  @Nullable
  String middle_name();

  interface Names_for_genderModel {
    @NonNull
    String gender();

    @NonNull
    String group_concat_name();
  }

  interface Names_for_genderCreator<T extends Names_for_genderModel> {
    T create(@NonNull String gender, @NonNull String group_concat_name);
  }

  final class Names_for_genderMapper<T extends Names_for_genderModel> implements RowMapper<T> {
    private final Names_for_genderCreator<T> creator;

    public Names_for_genderMapper(Names_for_genderCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getString(0),
          cursor.getString(1)
      );
    }
  }

  interface Middle_names_for_genderModel {
    @NonNull
    String gender();

    @Nullable
    String group_concat_middle_name();
  }

  interface Middle_names_for_genderCreator<T extends Middle_names_for_genderModel> {
    T create(@NonNull String gender, @Nullable String group_concat_middle_name);
  }

  final class Middle_names_for_genderMapper<T extends Middle_names_for_genderModel> implements RowMapper<T> {
    private final Middle_names_for_genderCreator<T> creator;

    public Middle_names_for_genderMapper(Middle_names_for_genderCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getString(0),
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  interface Upper_namesModel {
    @NonNull
    String upper_name();

    @Nullable
    String upper_middle_name();
  }

  interface Upper_namesCreator<T extends Upper_namesModel> {
    T create(@NonNull String upper_name, @Nullable String upper_middle_name);
  }

  final class Upper_namesMapper<T extends Upper_namesModel> implements RowMapper<T> {
    private final Upper_namesCreator<T> creator;

    public Upper_namesMapper(Upper_namesCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getString(0),
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  interface Lower_namesModel {
    @NonNull
    String lower_name();

    @Nullable
    String lower_middle_name();
  }

  interface Lower_namesCreator<T extends Lower_namesModel> {
    T create(@NonNull String lower_name, @Nullable String lower_middle_name);
  }

  final class Lower_namesMapper<T extends Lower_namesModel> implements RowMapper<T> {
    private final Lower_namesCreator<T> creator;

    public Lower_namesMapper(Lower_namesCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getString(0),
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  interface Nullif_namesModel {
    long _id();

    @Nullable
    String nullif_name();
  }

  interface Nullif_namesCreator<T extends Nullif_namesModel> {
    T create(long _id, @Nullable String nullif_name);
  }

  final class Nullif_namesMapper<T extends Nullif_namesModel> implements RowMapper<T> {
    private final Nullif_namesCreator<T> creator;

    public Nullif_namesMapper(Nullif_namesCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getString(1)
      );
    }
  }

  interface Creator<T extends TestModel> {
    T create(long _id, @NonNull String name, @NonNull String gender, @Nullable String middle_name);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getString(1),
          cursor.getString(2),
          cursor.isNull(3) ? null : cursor.getString(3)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this._id(copy._id());
        this.name(copy.name());
        this.gender(copy.gender());
        this.middle_name(copy.middle_name());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal name(String name) {
      contentValues.put("name", name);
      return this;
    }

    public Marshal gender(String gender) {
      contentValues.put("gender", gender);
      return this;
    }

    public Marshal middle_name(String middle_name) {
      contentValues.put("middle_name", middle_name);
      return this;
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public Marshal marshal() {
      return new Marshal(null);
    }

    public Marshal marshal(TestModel copy) {
      return new Marshal(copy);
    }

    public <R extends Names_for_genderModel> Names_for_genderMapper<R> names_for_genderMapper(Names_for_genderCreator<R> creator) {
      return new Names_for_genderMapper<R>(creator);
    }

    public <R extends Middle_names_for_genderModel> Middle_names_for_genderMapper<R> middle_names_for_genderMapper(Middle_names_for_genderCreator<R> creator) {
      return new Middle_names_for_genderMapper<R>(creator);
    }

    public <R extends Upper_namesModel> Upper_namesMapper<R> upper_namesMapper(Upper_namesCreator<R> creator) {
      return new Upper_namesMapper<R>(creator);
    }

    public <R extends Lower_namesModel> Lower_namesMapper<R> lower_namesMapper(Lower_namesCreator<R> creator) {
      return new Lower_namesMapper<R>(creator);
    }

    public <R extends Nullif_namesModel> Nullif_namesMapper<R> nullif_namesMapper(Nullif_namesCreator<R> creator) {
      return new Nullif_namesMapper<R>(creator);
    }
  }
}
