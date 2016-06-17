package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  String TABLE_NAME = "employee";

  String ID = "id";

  String DEPARTMENT = "department";

  String NAME = "name";

  String TITLE = "title";

  String BIO = "bio";

  String CREATE_TABLE = ""
      + "CREATE TABLE employee (\n"
      + "  id INTEGER NOT NULL PRIMARY KEY,\n"
      + "  department TEXT NOT NULL,\n"
      + "  name TEXT NOT NULL,\n"
      + "  title TEXT NOT NULL,\n"
      + "  bio TEXT NOT NULL\n"
      + ")";

  String SOME_SELECT = ""
      + "SELECT *\n"
      + "FROM employee\n"
      + "WHERE department = ?\n"
      + "AND (\n"
      + "  name LIKE '%' || ? || '%'\n"
      + "  OR title LIKE '%' || ? || '%'\n"
      + "  OR bio LIKE '%' || ? || '%'\n"
      + ")\n"
      + "ORDER BY department";

  long id();

  @NonNull
  String department();

  @NonNull
  String name();

  @NonNull
  String title();

  @NonNull
  String bio();

  interface Creator<T extends TestModel> {
    T create(long id, String department, String name, String title, String bio);
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
          cursor.getString(3),
          cursor.getString(4)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    Marshal(@Nullable TestModel copy) {
      if (copy != null) {
        this.id(copy.id());
        this.department(copy.department());
        this.name(copy.name());
        this.title(copy.title());
        this.bio(copy.bio());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal id(long id) {
      contentValues.put(ID, id);
      return this;
    }

    public Marshal department(String department) {
      contentValues.put(DEPARTMENT, department);
      return this;
    }

    public Marshal name(String name) {
      contentValues.put(NAME, name);
      return this;
    }

    public Marshal title(String title) {
      contentValues.put(TITLE, title);
      return this;
    }

    public Marshal bio(String bio) {
      contentValues.put(BIO, bio);
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

    public Mapper<T> some_selectMapper() {
      return new Mapper<T>(this);
    }
  }
}
