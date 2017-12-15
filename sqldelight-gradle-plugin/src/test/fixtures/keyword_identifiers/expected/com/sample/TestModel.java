package com.sample;

import android.database.Cursor;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightCompiledStatement;
import com.squareup.sqldelight.SqlDelightStatement;
import com.squareup.sqldelight.internal.TableSet;
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
    T create(@Nullable String ASC, @Nullable String DESC, @Nullable List TEXT,
        @Nullable Boolean Boolean, @Nullable String new_);
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

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<List, String> TEXTAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<List, String> TEXTAdapter) {
      this.creator = creator;
      this.TEXTAdapter = TEXTAdapter;
    }

    public SqlDelightStatement some_select() {
      return new SqlDelightStatement(""
          + "SELECT *\n"
          + "FROM test",
          new String[0], new TableSet("test"));
    }

    public SqlDelightStatement get_desc() {
      return new SqlDelightStatement(""
          + "SELECT \"DESC\", [Boolean]\n"
          + "FROM test",
          new String[0], new TableSet("test"));
    }

    public Mapper<T> some_selectMapper() {
      return new Mapper<T>(this);
    }

    public <R extends Get_descModel> Get_descMapper<R> get_descMapper(Get_descCreator<R> creator) {
      return new Get_descMapper<R>(creator);
    }
  }

  final class Insert_stmt extends SqlDelightCompiledStatement {
    private final Factory<? extends TestModel> testModelFactory;

    public Insert_stmt(SupportSQLiteDatabase database, Factory<? extends TestModel> testModelFactory) {
      super("test", database.compileStatement(""
              + "INSERT INTO test('ASC', \"DESC\", `TEXT`, [Boolean], new)\n"
              + "VALUES (?, ?, ?, ?, ?)"));
      this.testModelFactory = testModelFactory;
    }

    public void bind(@Nullable String ASC, @Nullable String DESC, @Nullable List TEXT,
        @Nullable Boolean Boolean, @Nullable String new_) {
      if (ASC == null) {
        program.bindNull(1);
      } else {
        program.bindString(1, ASC);
      }
      if (DESC == null) {
        program.bindNull(2);
      } else {
        program.bindString(2, DESC);
      }
      if (TEXT == null) {
        program.bindNull(3);
      } else {
        program.bindString(3, testModelFactory.TEXTAdapter.encode(TEXT));
      }
      if (Boolean == null) {
        program.bindNull(4);
      } else {
        program.bindLong(4, Boolean ? 1 : 0);
      }
      if (new_ == null) {
        program.bindNull(5);
      } else {
        program.bindString(5, new_);
      }
    }
  }
}
