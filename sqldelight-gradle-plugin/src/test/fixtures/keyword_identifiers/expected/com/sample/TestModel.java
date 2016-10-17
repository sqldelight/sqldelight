package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
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

  String ASC = "ASC";

  String DESC = "DESC";

  String TEXT = "TEXT";

  String BOOLEAN = "Boolean";

  String NEW_ = "new";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  'ASC' TEXT,\n"
      + "  \"DESC\" TEXT,\n"
      + "  `TEXT` TEXT,\n"
      + "  [Boolean] INTEGER,\n"
      + "  new TEXT\n"
      + ")";

  String SOME_SELECT = ""
      + "SELECT *\n"
      + "FROM test";

  String GET_DESC = ""
      + "SELECT \"DESC\", [Boolean]\n"
      + "FROM test";

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

  interface Get_descModel {
    @Nullable
    String DESC();

    @Nullable
    Boolean Boolean();
  }

  interface Get_descCreator<T extends Get_descModel> {
    T create(@Nullable String DESC, @Nullable Boolean Boolean);
  }

  final class Get_descMapper<T extends Get_descModel> implements RowMapper<T> {
    private final Get_descCreator<T> creator;

    public Get_descMapper(Get_descCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(0) ? null : cursor.getString(0),
          cursor.isNull(1) ? null : cursor.getInt(1) == 1
      );
    }
  }

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
          cursor.isNull(2) ? null : testModelFactory.TEXTAdapter.decode(cursor.getString(2)),
          cursor.isNull(3) ? null : cursor.getInt(3) == 1,
          cursor.isNull(4) ? null : cursor.getString(4)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<List, String> TEXTAdapter;

    Marshal(@Nullable TestModel copy, ColumnAdapter<List, String> TEXTAdapter) {
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
        contentValues.put(TEXT, TEXTAdapter.encode(TEXT_));
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

    public final ColumnAdapter<List, String> TEXTAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<List, String> TEXTAdapter) {
      this.creator = creator;
      this.TEXTAdapter = TEXTAdapter;
    }

    public Marshal marshal() {
      return new Marshal(null, TEXTAdapter);
    }

    public Marshal marshal(TestModel copy) {
      return new Marshal(copy, TEXTAdapter);
    }

    public void insert_stmt(Insert_stmt statement, @Nullable String ASC_, @Nullable String DESC_, @Nullable List TEXT_, @Nullable Boolean Boolean, @Nullable String new_) {
      if (ASC_ == null) {
        statement.program.bindNull(1);
      } else {
        statement.program.bindString(1, ASC_);
      }
      if (DESC_ == null) {
        statement.program.bindNull(2);
      } else {
        statement.program.bindString(2, DESC_);
      }
      if (TEXT_ == null) {
        statement.program.bindNull(3);
      } else {
        statement.program.bindString(3, TEXTAdapter.encode(TEXT_));
      }
      if (Boolean == null) {
        statement.program.bindNull(4);
      } else {
        statement.program.bindLong(4, Boolean ? 1 : 0);
      }
      if (new_ == null) {
        statement.program.bindNull(5);
      } else {
        statement.program.bindString(5, new_);
      }
    }

    public Mapper<T> some_selectMapper() {
      return new Mapper<T>(this);
    }

    public <R extends Get_descModel> Get_descMapper<R> get_descMapper(Get_descCreator<R> creator) {
      return new Get_descMapper<R>(creator);
    }
  }

  final class Insert_stmt {
    public static final String table = "test";

    public final SQLiteStatement program;

    public Insert_stmt(SQLiteDatabase database) {
      program = database.compileStatement(""
              + "INSERT INTO test('ASC', \"DESC\", `TEXT`, [Boolean], new)\n"
              + "VALUES (?, ?, ?, ?, ?)");
    }
  }
}
