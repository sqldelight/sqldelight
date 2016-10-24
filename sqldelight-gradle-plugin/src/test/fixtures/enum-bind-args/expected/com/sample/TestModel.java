package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightCompiledStatement;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public interface TestModel {
  String TABLE_NAME = "test";

  String _ID = "_id";

  String ENUM_VALUE = "enum_value";

  String ENUM_VALUE_INT = "enum_value_int";

  String FOREIGN_KEY = "foreign_key";

  String CREATE_TABLE = ""
      + "CREATE TABLE test (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  enum_value TEXT,\n"
      + "  enum_value_int INTEGER,\n"
      + "  foreign_key INTEGER REFERENCES foreign_table\n"
      + ")";

  String LOCAL_ENUM = ""
      + "SELECT *\n"
      + "FROM test\n"
      + "WHERE enum_value = ?";

  String LOCAL_ENUM_INT = ""
      + "SELECT *\n"
      + "FROM test\n"
      + "WHERE enum_value_int = ?";

  String ENUM_ARRAY = ""
      + "SELECT *\n"
      + "FROM test\n"
      + "WHERE enum_value IN ?";

  String FOREIGN_ENUM = ""
      + "SELECT test.*\n"
      + "FROM test\n"
      + "JOIN foreign_table ON foreign_key=foreign_table._id\n"
      + "WHERE foreign_table.test_enum = ?";

  String MULTIPLE_FOREIGN_ENUMS = ""
      + "SELECT *\n"
      + "FROM test\n"
      + "JOIN foreign_table ON foreign_key=foreign_table._id\n"
      + "WHERE foreign_table.test_enum IN (?1, ?2, ?3, ?5)";

  String NAMED_ARG = ""
      + "SELECT *\n"
      + "FROM test\n"
      + "WHERE enum_value = :stuff\n"
      + "OR enum_value = :stuff || '2'";

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

    public Multiple_foreign_enumsMapper(Multiple_foreign_enumsCreator<T1, T2, T> creator, Factory<T1> testModelFactory, ForeignTableModel.Factory<T2> foreignTableModelFactory) {
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
    T create(long _id, @Nullable Test.TestEnum enum_value, @Nullable Test.TestEnum enum_value_int, @Nullable Long foreign_key);
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

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Test.TestEnum, String> enum_valueAdapter;

    private final ColumnAdapter<Test.TestEnum, Long> enum_value_intAdapter;

    Marshal(@Nullable TestModel copy, ColumnAdapter<Test.TestEnum, String> enum_valueAdapter, ColumnAdapter<Test.TestEnum, Long> enum_value_intAdapter) {
      this.enum_valueAdapter = enum_valueAdapter;
      this.enum_value_intAdapter = enum_value_intAdapter;
      if (copy != null) {
        this._id(copy._id());
        this.enum_value(copy.enum_value());
        this.enum_value_int(copy.enum_value_int());
        this.foreign_key(copy.foreign_key());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put("_id", _id);
      return this;
    }

    public Marshal enum_value(@Nullable Test.TestEnum enum_value) {
      if (enum_value != null) {
        contentValues.put("enum_value", enum_valueAdapter.encode(enum_value));
      } else {
        contentValues.putNull("enum_value");
      }
      return this;
    }

    public Marshal enum_value_int(@Nullable Test.TestEnum enum_value_int) {
      if (enum_value_int != null) {
        contentValues.put("enum_value_int", enum_value_intAdapter.encode(enum_value_int));
      } else {
        contentValues.putNull("enum_value_int");
      }
      return this;
    }

    public Marshal foreign_key(Long foreign_key) {
      contentValues.put("foreign_key", foreign_key);
      return this;
    }
  }

  final class Factory<T extends TestModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Test.TestEnum, String> enum_valueAdapter;

    public final ColumnAdapter<Test.TestEnum, Long> enum_value_intAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Test.TestEnum, String> enum_valueAdapter, ColumnAdapter<Test.TestEnum, Long> enum_value_intAdapter) {
      this.creator = creator;
      this.enum_valueAdapter = enum_valueAdapter;
      this.enum_value_intAdapter = enum_value_intAdapter;
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal() {
      return new Marshal(null, enum_valueAdapter, enum_value_intAdapter);
    }

    /**
     * @deprecated Use compiled statements (https://github.com/square/sqldelight#compiled-statements)
     */
    @Deprecated
    public Marshal marshal(TestModel copy) {
      return new Marshal(copy, enum_valueAdapter, enum_value_intAdapter);
    }

    public SqlDelightStatement local_enum(@Nullable Test.TestEnum enum_value) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM test\n"
              + "WHERE enum_value = ");
      if (enum_value == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) enum_valueAdapter.encode(enum_value));
      }
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }

    public SqlDelightStatement local_enum_int(@Nullable Test.TestEnum enum_value_int) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM test\n"
              + "WHERE enum_value_int = ");
      if (enum_value_int == null) {
        query.append("null");
      } else {
        query.append(enum_value_intAdapter.encode(enum_value_int));
      }
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }

    public SqlDelightStatement enum_array(@Nullable Test.TestEnum[] enum_value) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM test\n"
              + "WHERE enum_value IN ");
      query.append('(');
      for (int i = 0; i < enum_value.length; i++) {
        if (i != 0) query.append(", ");
        query.append('?').append(currentIndex++);
        args.add(enum_valueAdapter.encode(enum_value[i]));
      }
      query.append(')');
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }

    public SqlDelightStatement foreign_enum(ForeignTableModel.Factory foreignTableModelFactory, @Nullable Test.TestEnum test_enum) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT test.*\n"
              + "FROM test\n"
              + "JOIN foreign_table ON foreign_key=foreign_table._id\n"
              + "WHERE foreign_table.test_enum = ");
      if (test_enum == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) foreignTableModelFactory.test_enumAdapter.encode(test_enum));
      }
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("test","foreign_table"))));
    }

    public SqlDelightStatement multiple_foreign_enums(ForeignTableModel.Factory foreignTableModelFactory, @Nullable Test.TestEnum test_enum, @Nullable Test.TestEnum test_enum_, @Nullable Test.TestEnum test_enum__, @Nullable Test.TestEnum test_enum___) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM test\n"
              + "JOIN foreign_table ON foreign_key=foreign_table._id\n"
              + "WHERE foreign_table.test_enum IN (");
      if (test_enum == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) foreignTableModelFactory.test_enumAdapter.encode(test_enum));
      }
      query.append(", ");
      if (test_enum_ == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) foreignTableModelFactory.test_enumAdapter.encode(test_enum_));
      }
      query.append(", ");
      if (test_enum__ == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) foreignTableModelFactory.test_enumAdapter.encode(test_enum__));
      }
      query.append(", ");
      if (test_enum___ == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) foreignTableModelFactory.test_enumAdapter.encode(test_enum___));
      }
      query.append(")");
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("test","foreign_table"))));
    }

    public SqlDelightStatement named_arg(@Nullable Test.TestEnum stuff) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT *\n"
              + "FROM test\n"
              + "WHERE enum_value = ");
      int arg1Index = currentIndex;
      if (stuff == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) enum_valueAdapter.encode(stuff));
      }
      query.append("\n"
              + "OR enum_value = ");
      if (stuff == null) {
        query.append("null");
      } else {
        query.append('?').append(arg1Index);
      }
      query.append(" || '2'");
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }

    /**
     * @deprecated Use {@link Insert_statement}
     */
    @Deprecated
    public SqlDelightStatement insert_statement(@Nullable Test.TestEnum enum_value, @Nullable Test.TestEnum enum_value_int, @Nullable Long foreign_key) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("INSERT INTO test (enum_value, enum_value_int, foreign_key)\n"
              + "VALUES (");
      if (enum_value == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) enum_valueAdapter.encode(enum_value));
      }
      query.append(", ");
      if (enum_value_int == null) {
        query.append("null");
      } else {
        query.append(enum_value_intAdapter.encode(enum_value_int));
      }
      query.append(", ");
      if (foreign_key == null) {
        query.append("null");
      } else {
        query.append(foreign_key);
      }
      query.append(")");
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }

    /**
     * @deprecated Use {@link Update_with_foreign}
     */
    @Deprecated
    public SqlDelightStatement update_with_foreign(ForeignTableModel.Factory foreignTableModelFactory, @Nullable Test.TestEnum enum_value_int, @Nullable Test.TestEnum test_enum) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("UPDATE test\n"
              + "SET enum_value_int = ");
      if (enum_value_int == null) {
        query.append("null");
      } else {
        query.append(enum_value_intAdapter.encode(enum_value_int));
      }
      query.append("\n"
              + "WHERE foreign_key IN (\n"
              + "  SELECT _id\n"
              + "  FROM foreign_table\n"
              + "  WHERE test_enum = ");
      if (test_enum == null) {
        query.append("null");
      } else {
        query.append('?').append(currentIndex++);
        args.add((String) foreignTableModelFactory.test_enumAdapter.encode(test_enum));
      }
      query.append("\n"
              + ")");
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
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

    public <T2 extends ForeignTableModel, R extends Multiple_foreign_enumsModel<T, T2>> Multiple_foreign_enumsMapper<T, T2, R> multiple_foreign_enumsMapper(Multiple_foreign_enumsCreator<T, T2, R> creator, ForeignTableModel.Factory<T2> foreignTableModelFactory) {
      return new Multiple_foreign_enumsMapper<T, T2, R>(creator, this, foreignTableModelFactory);
    }

    public Mapper<T> named_argMapper() {
      return new Mapper<T>(this);
    }
  }

  final class Insert_statement extends SqlDelightCompiledStatement.Insert {
    private final Factory<? extends TestModel> testModelFactory;

    public Insert_statement(SQLiteDatabase database, Factory<? extends TestModel> testModelFactory) {
      super("test", database.compileStatement(""
              + "INSERT INTO test (enum_value, enum_value_int, foreign_key)\n"
              + "VALUES (?, ?, ?)"));
      this.testModelFactory = testModelFactory;
    }

    public void bind(@Nullable Test.TestEnum enum_value, @Nullable Test.TestEnum enum_value_int, @Nullable Long foreign_key) {
      if (enum_value == null) {
        program.bindNull(1);
      } else {
        program.bindString(1, testModelFactory.enum_valueAdapter.encode(enum_value));
      }
      if (enum_value_int == null) {
        program.bindNull(2);
      } else {
        program.bindLong(2, testModelFactory.enum_value_intAdapter.encode(enum_value_int));
      }
      if (foreign_key == null) {
        program.bindNull(3);
      } else {
        program.bindLong(3, foreign_key);
      }
    }
  }

  final class Update_with_foreign extends SqlDelightCompiledStatement.Update {
    private final Factory<? extends TestModel> testModelFactory;

    private final ForeignTableModel.Factory<? extends ForeignTableModel> foreignTableModelFactory;

    public Update_with_foreign(SQLiteDatabase database, Factory<? extends TestModel> testModelFactory, ForeignTableModel.Factory<? extends ForeignTableModel> foreignTableModelFactory) {
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
        program.bindNull(1);
      } else {
        program.bindLong(1, testModelFactory.enum_value_intAdapter.encode(enum_value_int));
      }
      if (test_enum == null) {
        program.bindNull(2);
      } else {
        program.bindString(2, foreignTableModelFactory.test_enumAdapter.encode(test_enum));
      }
    }
  }
}
