package com.sample;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteProgram;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.SqlDelightStatement;
import com.squareup.sqldelight.internal.QuestionMarks;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface TestModel {
  @Deprecated
  String TABLE_NAME = "test";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String ENUM_VALUE = "enum_value";

  @Deprecated
  String ENUM_VALUE_INT = "enum_value_int";

  @Deprecated
  String FOREIGN_KEY = "foreign_key";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  enum_value TEXT,\n"
      + "  enum_value_int INTEGER,\n"
      + "  foreign_key INTEGER REFERENCES foreign_table\n"
      + ")";

  long _id();

  @Nullable
  Test.TestEnum enum_value();

  @Nullable
  Test.TestEnum enum_value_int();

  @Nullable
  Long foreign_key();

  interface Multiple_foreign_enumsModel<T1 extends TestModel, T2 extends ForeignTableModel> {
    @NonNull
    T1 test();

    @NonNull
    T2 foreign_table();
  }

  interface Multiple_foreign_enumsCreator<T1 extends TestModel, T2 extends ForeignTableModel, T extends Multiple_foreign_enumsModel<T1, T2>> {
    T create(@NonNull T1 test, @NonNull T2 foreign_table);
  }

  final class Multiple_foreign_enumsMapper<T1 extends TestModel, T2 extends ForeignTableModel, T extends Multiple_foreign_enumsModel<T1, T2>> implements RowMapper<T> {
    private final Multiple_foreign_enumsCreator<T1, T2, T> creator;

    private final Factory<T1> testModelFactory;

    private final ForeignTableModel.Factory<T2> foreignTableModelFactory;

    public Multiple_foreign_enumsMapper(Multiple_foreign_enumsCreator<T1, T2, T> creator,
        Factory<T1> testModelFactory, ForeignTableModel.Factory<T2> foreignTableModelFactory) {
      this.creator = creator;
      this.testModelFactory = testModelFactory;
      this.foreignTableModelFactory = foreignTableModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          testModelFactory.creator.create(
              cursor.getLong(0),
              cursor.isNull(1) ? null : testModelFactory.enum_valueAdapter.decode(cursor.getString(1)),
              cursor.isNull(2) ? null : testModelFactory.enum_value_intAdapter.decode(cursor.getLong(2)),
              cursor.isNull(3) ? null : cursor.getLong(3)
          ),
          foreignTableModelFactory.creator.create(
              cursor.getLong(4),
              cursor.isNull(5) ? null : foreignTableModelFactory.test_enumAdapter.decode(cursor.getString(5))
          )
      );
    }
  }

  interface Creator<T extends TestModel> {
    T create(long _id, @Nullable Test.TestEnum enum_value, @Nullable Test.TestEnum enum_value_int,
        @Nullable Long foreign_key);
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
          cursor.isNull(1) ? null : testModelFactory.enum_valueAdapter.decode(cursor.getString(1)),
          cursor.isNull(2) ? null : testModelFactory.enum_value_intAdapter.decode(cursor.getLong(2)),
          cursor.isNull(3) ? null : cursor.getLong(3)
      );
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Test.TestEnum, String> enum_valueAdapter;

    public final ColumnAdapter<Test.TestEnum, Long> enum_value_intAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Test.TestEnum, String> enum_valueAdapter,
        ColumnAdapter<Test.TestEnum, Long> enum_value_intAdapter) {
      this.creator = creator;
      this.enum_valueAdapter = enum_valueAdapter;
      this.enum_value_intAdapter = enum_value_intAdapter;
    }

    public SqlDelightQuery local_enum(@Nullable Test.TestEnum enum_value) {
      return new Local_enumQuery(enum_value);
    }

    public SqlDelightQuery local_enum_int(@Nullable Test.TestEnum enum_value_int) {
      return new Local_enum_intQuery(enum_value_int);
    }

    public SqlDelightQuery enum_array(@Nullable Test.TestEnum[] enum_value) {
      return new Enum_arrayQuery(enum_value);
    }

    public SqlDelightQuery foreign_enum(
        @NonNull ForeignTableModel.Factory<? extends ForeignTableModel> foreignTableModelFactory,
        @Nullable Test.TestEnum test_enum) {
      return new Foreign_enumQuery(foreignTableModelFactory, test_enum);
    }

    public SqlDelightQuery multiple_foreign_enums(
        @NonNull ForeignTableModel.Factory<? extends ForeignTableModel> foreignTableModelFactory,
        @Nullable Test.TestEnum test_enum, @Nullable Test.TestEnum test_enum_,
        @Nullable Test.TestEnum test_enum__, @Nullable Test.TestEnum test_enum___) {
      return new Multiple_foreign_enumsQuery(foreignTableModelFactory, test_enum, test_enum_,
          test_enum__, test_enum___);
    }

    public SqlDelightQuery named_arg(@Nullable Test.TestEnum stuff) {
      return new Named_argQuery(stuff);
    }

    public Mapper<T> local_enumMapper() {
      return new Mapper<T>(this);
    }

    public Mapper<T> local_enum_intMapper() {
      return new Mapper<T>(this);
    }

    public Mapper<T> enum_arrayMapper() {
      return new Mapper<T>(this);
    }

    public Mapper<T> foreign_enumMapper() {
      return new Mapper<T>(this);
    }

    public <T2 extends ForeignTableModel, R extends Multiple_foreign_enumsModel<T, T2>> Multiple_foreign_enumsMapper<T, T2, R> multiple_foreign_enumsMapper(
        Multiple_foreign_enumsCreator<T, T2, R> creator,
        ForeignTableModel.Factory<T2> foreignTableModelFactory) {
      return new Multiple_foreign_enumsMapper<T, T2, R>(creator, this, foreignTableModelFactory);
    }

    public Mapper<T> named_argMapper() {
      return new Mapper<T>(this);
    }

    private final class Local_enumQuery extends SqlDelightQuery {
      @Nullable
      private final Test.TestEnum enum_value;

      Local_enumQuery(@Nullable Test.TestEnum enum_value) {
        super("SELECT *\n"
            + "FROM test\n"
            + "WHERE enum_value = ?1",
            new TableSet("test"));

        this.enum_value = enum_value;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Test.TestEnum enum_value = this.enum_value;
        if (enum_value != null) {
          program.bindString(1, enum_valueAdapter.encode(enum_value));
        } else {
          program.bindNull(1);
        }
      }
    }

    private final class Local_enum_intQuery extends SqlDelightQuery {
      @Nullable
      private final Test.TestEnum enum_value_int;

      Local_enum_intQuery(@Nullable Test.TestEnum enum_value_int) {
        super("SELECT *\n"
            + "FROM test\n"
            + "WHERE enum_value_int = ?1",
            new TableSet("test"));

        this.enum_value_int = enum_value_int;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Test.TestEnum enum_value_int = this.enum_value_int;
        if (enum_value_int != null) {
          program.bindLong(1, enum_value_intAdapter.encode(enum_value_int));
        } else {
          program.bindNull(1);
        }
      }
    }

    private final class Enum_arrayQuery extends SqlDelightQuery {
      @Nullable
      private final Test.TestEnum[] enum_value;

      Enum_arrayQuery(@Nullable Test.TestEnum[] enum_value) {
        super("SELECT *\n"
            + "FROM test\n"
            + "WHERE enum_value IN " + QuestionMarks.ofSize(enum_value.length),
            new TableSet("test"));

        this.enum_value = enum_value;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        int nextIndex = 1;

        Test.TestEnum[] enum_value = this.enum_value;
        if (enum_value != null) {
          for (Test.TestEnum item : enum_value) {
            program.bindString(nextIndex++, enum_valueAdapter.encode(item));
          }
        } else {
          program.bindNull(nextIndex++);
        }
      }
    }

    private final class Foreign_enumQuery extends SqlDelightQuery {
      @NonNull
      private final ForeignTableModel.Factory<? extends ForeignTableModel> foreignTableModelFactory;

      @Nullable
      private final Test.TestEnum test_enum;

      Foreign_enumQuery(
          @NonNull ForeignTableModel.Factory<? extends ForeignTableModel> foreignTableModelFactory,
          @Nullable Test.TestEnum test_enum) {
        super("SELECT test.*\n"
            + "FROM test\n"
            + "JOIN foreign_table ON foreign_key=foreign_table._id\n"
            + "WHERE foreign_table.test_enum = ?1",
            new TableSet("test", "foreign_table"));

        this.foreignTableModelFactory = foreignTableModelFactory;
        this.test_enum = test_enum;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Test.TestEnum test_enum = this.test_enum;
        if (test_enum != null) {
          program.bindString(1, foreignTableModelFactory.test_enumAdapter.encode(test_enum));
        } else {
          program.bindNull(1);
        }
      }
    }

    private final class Multiple_foreign_enumsQuery extends SqlDelightQuery {
      @NonNull
      private final ForeignTableModel.Factory<? extends ForeignTableModel> foreignTableModelFactory;

      @Nullable
      private final Test.TestEnum test_enum;

      @Nullable
      private final Test.TestEnum test_enum_;

      @Nullable
      private final Test.TestEnum test_enum__;

      @Nullable
      private final Test.TestEnum test_enum___;

      Multiple_foreign_enumsQuery(
          @NonNull ForeignTableModel.Factory<? extends ForeignTableModel> foreignTableModelFactory,
          @Nullable Test.TestEnum test_enum, @Nullable Test.TestEnum test_enum_,
          @Nullable Test.TestEnum test_enum__, @Nullable Test.TestEnum test_enum___) {
        super("SELECT *\n"
            + "FROM test\n"
            + "JOIN foreign_table ON foreign_key=foreign_table._id\n"
            + "WHERE foreign_table.test_enum IN (?1, ?2, ?3, ?4)",
            new TableSet("test", "foreign_table"));

        this.foreignTableModelFactory = foreignTableModelFactory;
        this.test_enum = test_enum;
        this.test_enum_ = test_enum_;
        this.test_enum__ = test_enum__;
        this.test_enum___ = test_enum___;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Test.TestEnum test_enum = this.test_enum;
        if (test_enum != null) {
          program.bindString(1, foreignTableModelFactory.test_enumAdapter.encode(test_enum));
        } else {
          program.bindNull(1);
        }

        Test.TestEnum test_enum_ = this.test_enum_;
        if (test_enum_ != null) {
          program.bindString(2, foreignTableModelFactory.test_enumAdapter.encode(test_enum_));
        } else {
          program.bindNull(2);
        }

        Test.TestEnum test_enum__ = this.test_enum__;
        if (test_enum__ != null) {
          program.bindString(3, foreignTableModelFactory.test_enumAdapter.encode(test_enum__));
        } else {
          program.bindNull(3);
        }

        Test.TestEnum test_enum___ = this.test_enum___;
        if (test_enum___ != null) {
          program.bindString(4, foreignTableModelFactory.test_enumAdapter.encode(test_enum___));
        } else {
          program.bindNull(4);
        }
      }
    }

    private final class Named_argQuery extends SqlDelightQuery {
      @Nullable
      private final Test.TestEnum stuff;

      Named_argQuery(@Nullable Test.TestEnum stuff) {
        super("SELECT *\n"
            + "FROM test\n"
            + "WHERE enum_value = ?1\n"
            + "OR enum_value = ?1 || '2'",
            new TableSet("test"));

        this.stuff = stuff;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Test.TestEnum stuff = this.stuff;
        if (stuff != null) {
          program.bindString(1, enum_valueAdapter.encode(stuff));
        } else {
          program.bindNull(1);
        }
      }
    }
  }

  final class Insert_statement extends SqlDelightStatement {
    private final Factory<? extends TestModel> testModelFactory;

    public Insert_statement(SupportSQLiteDatabase database,
        Factory<? extends TestModel> testModelFactory) {
      super("test", database.compileStatement(""
              + "INSERT INTO test (enum_value, enum_value_int, foreign_key)\n"
              + "VALUES (?, ?, ?)"));
      this.testModelFactory = testModelFactory;
    }

    public void bind(@Nullable Test.TestEnum enum_value, @Nullable Test.TestEnum enum_value_int,
        @Nullable Long foreign_key) {
      if (enum_value == null) {
        bindNull(1);
      } else {
        bindString(1, testModelFactory.enum_valueAdapter.encode(enum_value));
      }
      if (enum_value_int == null) {
        bindNull(2);
      } else {
        bindLong(2, testModelFactory.enum_value_intAdapter.encode(enum_value_int));
      }
      if (foreign_key == null) {
        bindNull(3);
      } else {
        bindLong(3, foreign_key);
      }
    }
  }

  final class Update_with_foreign extends SqlDelightStatement {
    private final Factory<? extends TestModel> testModelFactory;

    private final ForeignTableModel.Factory<? extends ForeignTableModel> foreignTableModelFactory;

    public Update_with_foreign(SupportSQLiteDatabase database,
        Factory<? extends TestModel> testModelFactory,
        ForeignTableModel.Factory<? extends ForeignTableModel> foreignTableModelFactory) {
      super("test", database.compileStatement(""
              + "UPDATE test\n"
              + "SET enum_value_int = ?\n"
              + "WHERE foreign_key IN (\n"
              + "  SELECT _id\n"
              + "  FROM foreign_table\n"
              + "  WHERE test_enum = ?\n"
              + ")"));
      this.testModelFactory = testModelFactory;
      this.foreignTableModelFactory = foreignTableModelFactory;
    }

    public void bind(@Nullable Test.TestEnum enum_value_int, @Nullable Test.TestEnum test_enum) {
      if (enum_value_int == null) {
        bindNull(1);
      } else {
        bindLong(1, testModelFactory.enum_value_intAdapter.encode(enum_value_int));
      }
      if (test_enum == null) {
        bindNull(2);
      } else {
        bindString(2, foreignTableModelFactory.test_enumAdapter.encode(test_enum));
      }
    }
  }
}
