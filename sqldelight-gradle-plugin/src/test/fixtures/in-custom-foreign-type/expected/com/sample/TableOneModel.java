package com.sample;

import android.arch.persistence.db.SupportSQLiteProgram;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.QuestionMarks;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public interface TableOneModel {
  @Deprecated
  String TABLE_NAME = "table_one";

  @Deprecated
  String _ID = "_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE table_one (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT\n"
      + ")";

  long _id();

  interface Select_with_typesModel<T1 extends TableOneModel, T2 extends TableTwoModel> {
    @NonNull
    T1 table_one();

    @NonNull
    T2 table_two();
  }

  interface Select_with_typesCreator<T1 extends TableOneModel, T2 extends TableTwoModel, T extends Select_with_typesModel<T1, T2>> {
    T create(@NonNull T1 table_one, @NonNull T2 table_two);
  }

  final class Select_with_typesMapper<T1 extends TableOneModel, T2 extends TableTwoModel, T extends Select_with_typesModel<T1, T2>> implements RowMapper<T> {
    private final Select_with_typesCreator<T1, T2, T> creator;

    private final Factory<T1> tableOneModelFactory;

    private final TableTwoModel.Factory<T2> tableTwoModelFactory;

    public Select_with_typesMapper(Select_with_typesCreator<T1, T2, T> creator,
        Factory<T1> tableOneModelFactory, TableTwoModel.Factory<T2> tableTwoModelFactory) {
      this.creator = creator;
      this.tableOneModelFactory = tableOneModelFactory;
      this.tableTwoModelFactory = tableTwoModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          tableOneModelFactory.creator.create(
              cursor.getLong(0)
          ),
          tableTwoModelFactory.creator.create(
              cursor.getLong(1),
              cursor.isNull(2) ? null : tableTwoModelFactory.typeAdapter.decode(cursor.getString(2))
          )
      );
    }
  }

  interface Creator<T extends TableOneModel> {
    T create(long _id);
  }

  final class Mapper<T extends TableOneModel> implements RowMapper<T> {
    private final Factory<T> tableOneModelFactory;

    public Mapper(Factory<T> tableOneModelFactory) {
      this.tableOneModelFactory = tableOneModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return tableOneModelFactory.creator.create(
          cursor.getLong(0)
      );
    }
  }

  final class Factory<T extends TableOneModel> {
    public final Creator<T> creator;

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightQuery select_with_types(
        @NonNull TableTwoModel.Factory<? extends TableTwoModel> tableTwoModelFactory,
        @Nullable List[] types) {
      return new Select_with_typesQuery(tableTwoModelFactory, types);
    }

    public <T2 extends TableTwoModel, R extends Select_with_typesModel<T, T2>> Select_with_typesMapper<T, T2, R> select_with_typesMapper(
        Select_with_typesCreator<T, T2, R> creator,
        TableTwoModel.Factory<T2> tableTwoModelFactory) {
      return new Select_with_typesMapper<T, T2, R>(creator, this, tableTwoModelFactory);
    }

    private final class Select_with_typesQuery extends SqlDelightQuery {
      @NonNull
      private final TableTwoModel.Factory<? extends TableTwoModel> tableTwoModelFactory;

      @Nullable
      private final List[] types;

      Select_with_typesQuery(
          @NonNull TableTwoModel.Factory<? extends TableTwoModel> tableTwoModelFactory,
          @Nullable List[] types) {
        super("SELECT * FROM table_one, table_two\n"
            + "    WHERE type IN " + QuestionMarks.ofSize(types.length),
            new TableSet("table_one", "table_two"));

        this.tableTwoModelFactory = tableTwoModelFactory;
        this.types = types;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        int nextIndex = 1;

        List[] types = this.types;
        if (types != null) {
          for (List item : types) {
            program.bindString(nextIndex++, tableTwoModelFactory.typeAdapter.encode(item));
          }
        } else {
          program.bindNull(nextIndex++);
        }
      }
    }
  }
}
