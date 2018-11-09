package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteProgram;
import com.squareup.sqldelight.prerelease.RowMapper;
import com.squareup.sqldelight.prerelease.SqlDelightQuery;
import com.squareup.sqldelight.prerelease.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public interface TableAModel {
  @Deprecated
  String TABLE_NAME = "tablea";

  @Deprecated
  String _ID = "_id";

  @Deprecated
  String TABLEB_ID = "tableb_id";

  String CREATE_TABLE = ""
      + "CREATE TABLE tablea(\n"
      + "  _id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
      + "  tableb_id INTEGER NOT NULL\n"
      + ")";

  @Nullable
  Long _id();

  long tableb_id();

  interface Select_customModel<T1 extends TableAModel, T2 extends TableBModel> {
    @NonNull
    T1 tablea();

    @Nullable
    T2 tableb();

    @Nullable
    T2 tableb_2();
  }

  interface Select_customCreator<T1 extends TableAModel, T2 extends TableBModel, T extends Select_customModel<T1, T2>> {
    T create(@NonNull T1 tablea, @Nullable T2 tableb, @Nullable T2 tableb_2);
  }

  final class Select_customMapper<T1 extends TableAModel, T2 extends TableBModel, T extends Select_customModel<T1, T2>> implements RowMapper<T> {
    private final Select_customCreator<T1, T2, T> creator;

    private final Factory<T1> tableAModelFactory;

    private final TableBModel.Factory<T2> tableBModelFactory;

    public Select_customMapper(Select_customCreator<T1, T2, T> creator,
        @NonNull Factory<T1> tableAModelFactory,
        @NonNull TableBModel.Factory<T2> tableBModelFactory) {
      this.creator = creator;
      this.tableAModelFactory = tableAModelFactory;
      this.tableBModelFactory = tableBModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          tableAModelFactory.creator.create(
              cursor.isNull(0) ? null : cursor.getLong(0),
              cursor.getLong(1)
          ),
          cursor.isNull(3)
              ? null
              : tableBModelFactory.creator.create(
                  cursor.isNull(2) ? null : cursor.getLong(2),
                  cursor.getInt(3),
                  cursor.getInt(4)
              ),
          cursor.isNull(6)
              ? null
              : tableBModelFactory.creator.create(
                  cursor.isNull(5) ? null : cursor.getLong(5),
                  cursor.getInt(6),
                  cursor.getInt(7)
              )
      );
    }
  }

  interface Creator<T extends TableAModel> {
    T create(@Nullable Long _id, long tableb_id);
  }

  final class Mapper<T extends TableAModel> implements RowMapper<T> {
    private final Factory<T> tableAModelFactory;

    public Mapper(@NonNull Factory<T> tableAModelFactory) {
      this.tableAModelFactory = tableAModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return tableAModelFactory.creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.getLong(1)
      );
    }
  }

  final class Factory<T extends TableAModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }

    @NonNull
    public SqlDelightQuery select_custom(@Nullable Integer col1, @Nullable Integer col2) {
      return new Select_customQuery(col1, col2);
    }

    @NonNull
    public <T2 extends TableBModel, R extends Select_customModel<T, T2>> Select_customMapper<T, T2, R> select_customMapper(
        Select_customCreator<T, T2, R> creator, TableBModel.Factory<T2> tableBModelFactory) {
      return new Select_customMapper<T, T2, R>(creator, this, tableBModelFactory);
    }

    private final class Select_customQuery extends SqlDelightQuery {
      @Nullable
      private final Integer col1;

      @Nullable
      private final Integer col2;

      Select_customQuery(@Nullable Integer col1, @Nullable Integer col2) {
        super("SELECT *, tableb.*\n"
            + "FROM tablea\n"
            + "LEFT JOIN tableb\n"
            + "ON tablea.tableb_id=tableb._id\n"
            + "WHERE tableb.col1=?1 OR tableb.col2=?2",
            new TableSet("tablea", "tableb"));

        this.col1 = col1;
        this.col2 = col2;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Integer col1 = this.col1;
        if (col1 != null) {
          program.bindLong(1, col1);
        } else {
          program.bindNull(1);
        }

        Integer col2 = this.col2;
        if (col2 != null) {
          program.bindLong(2, col2);
        } else {
          program.bindNull(2);
        }
      }
    }
  }
}
