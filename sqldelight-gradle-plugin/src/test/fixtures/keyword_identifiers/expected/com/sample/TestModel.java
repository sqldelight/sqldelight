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

  final class Mapper<T extends TestModel> implements RowMapper<T> {
    private final Creator<T> creator;

    private final ColumnAdapter<List> TEXTAdapter;

    protected Mapper(Creator<T> creator, ColumnAdapter<List> TEXTAdapter) {
      this.creator = creator;
      this.TEXTAdapter = TEXTAdapter;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(cursor.getColumnIndex(ASC)) ? null : cursor.getString(cursor.getColumnIndex(ASC)),
          cursor.isNull(cursor.getColumnIndex(DESC)) ? null : cursor.getString(cursor.getColumnIndex(DESC)),
          cursor.isNull(cursor.getColumnIndex(TEXT)) ? null : TEXTAdapter.map(cursor, cursor.getColumnIndex(TEXT)),
          cursor.isNull(cursor.getColumnIndex(BOOLEAN)) ? null : cursor.getInt(cursor.getColumnIndex(BOOLEAN)) == 1,
          cursor.isNull(cursor.getColumnIndex(NEW_)) ? null : cursor.getString(cursor.getColumnIndex(NEW_))
      );
    }

    public interface Creator<R extends TestModel> {
      R create(String ASC, String DESC, List TEXT, Boolean Boolean, String new_);
    }
  }

  class TestMarshal<T extends TestMarshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<List> TEXTAdapter;

    public TestMarshal(ColumnAdapter<List> TEXTAdapter) {
      this.TEXTAdapter = TEXTAdapter;
    }

    public TestMarshal(TestModel copy, ColumnAdapter<List> TEXTAdapter) {
      this.ASC(copy.ASC());
      this.DESC(copy.DESC());
      this.TEXTAdapter = TEXTAdapter;
      this.TEXT(copy.TEXT());
      this.Boolean(copy.Boolean());
      this.new_(copy.new_());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T ASC(String ASC_) {
      contentValues.put(ASC, ASC_);
      return (T) this;
    }

    public T DESC(String DESC_) {
      contentValues.put(DESC, DESC_);
      return (T) this;
    }

    public T TEXT(List TEXT_) {
      TEXTAdapter.marshal(contentValues, TEXT, TEXT_);
      return (T) this;
    }

    public T Boolean(Boolean Boolean) {
      if (Boolean == null) {
        contentValues.putNull(BOOLEAN);
        return (T) this;
      }
      contentValues.put(BOOLEAN, Boolean ? 1 : 0);
      return (T) this;
    }

    public T new_(String new_) {
      contentValues.put(NEW_, new_);
      return (T) this;
    }
  }
}
