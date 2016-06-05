package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
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

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Creator<T> creator;

    protected Mapper(Creator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(cursor.getColumnIndex(ID)),
          cursor.getString(cursor.getColumnIndex(DEPARTMENT)),
          cursor.getString(cursor.getColumnIndex(NAME)),
          cursor.getString(cursor.getColumnIndex(TITLE)),
          cursor.getString(cursor.getColumnIndex(BIO))
      );
    }

    public interface Creator<R extends TestModel> {
      R create(long id, String department, String name, String title, String bio);
    }
  }

  class Marshal<T extends Marshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    public Marshal() {
    }

    public Marshal(TestModel copy) {
      this.id(copy.id());
      this.department(copy.department());
      this.name(copy.name());
      this.title(copy.title());
      this.bio(copy.bio());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T id(long id) {
      contentValues.put(ID, id);
      return (T) this;
    }

    public T department(String department) {
      contentValues.put(DEPARTMENT, department);
      return (T) this;
    }

    public T name(String name) {
      contentValues.put(NAME, name);
      return (T) this;
    }

    public T title(String title) {
      contentValues.put(TITLE, title);
      return (T) this;
    }

    public T bio(String bio) {
      contentValues.put(BIO, bio);
      return (T) this;
    }
  }
}
