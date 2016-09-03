package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Boolean;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public interface TestModel {
  String TABLE_NAME = "test";

  String ASC = "'ASC'";

  String DESC = "\"DESC\"";

  String TEXT = "`TEXT`";

  String BOOLEAN = "[Boolean]";

  String NEW_ = "new";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  'ASC' TEXT,\n"
      + "  \"DESC\" TEXT,\n"
      + "  `TEXT` TEXT,\n"
      + "  [Boolean] INTEGER,\n"
      + "  new TEXT\n"
      + ")";

  @Nullable
  String ASC();

  @Nullable
  String DESC();

  @Nullable
  List TEXT();

  @Nullable
  Boolean Boolean();

  @Nullable
  String new_();

  interface Creator<T extends TestModel> {
    T create(@Nullable String ASC_, @Nullable String DESC_, @Nullable List TEXT_, @Nullable Boolean Boolean, @Nullable String new_);
  }

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Factory<T> testModelFactory;

    public Mapper(Factory<T> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return testModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getString(0),
          cursor.isNull(1) ? null : cursor.getString(1),
          cursor.isNull(2) ? null : testModelFactory.TEXTAdapter.map(cursor, 2),
          cursor.isNull(3) ? null : cursor.getInt(3) == 1,
          cursor.isNull(4) ? null : cursor.getString(4)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<List> TEXTAdapter;

    Marshal(@Nullable TestModel copy, ColumnAdapter<List> TEXTAdapter) {
      this.TEXTAdapter = TEXTAdapter;
      if (copy != null) {
        this.ASC(copy.ASC());
        this.DESC(copy.DESC());
        this.TEXT(copy.TEXT());
        this.Boolean(copy.Boolean());
        this.new_(copy.new_());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal ASC(String ASC_) {
      contentValues.put(ASC, ASC_);
      return this;
    }

    public Marshal DESC(String DESC_) {
      contentValues.put(DESC, DESC_);
      return this;
    }

    public Marshal TEXT(@Nullable List TEXT_) {
      if (TEXT_ != null) {
        TEXTAdapter.marshal(contentValues, TEXT, TEXT_);
      } else {
        contentValues.putNull(TEXT);
      }
      return this;
    }

    public Marshal Boolean(Boolean Boolean) {
      if (Boolean == null) {
        contentValues.putNull(BOOLEAN);
        return this;
      }
      contentValues.put(BOOLEAN, Boolean ? 1 : 0);
      return this;
    }

    public Marshal new_(String new_) {
      contentValues.put(NEW_, new_);
      return this;
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<List> TEXTAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<List> TEXTAdapter) {
      this.creator = creator;
      this.TEXTAdapter = TEXTAdapter;
    }

    public Marshal marshal() {
      return new Marshal(null, TEXTAdapter);
    }

    public Marshal marshal(TestModel copy) {
      return new Marshal(copy, TEXTAdapter);
    }
  }
}
