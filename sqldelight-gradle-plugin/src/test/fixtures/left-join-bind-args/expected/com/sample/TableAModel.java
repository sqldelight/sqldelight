package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;

public interface TableAModel {
  String TABLE_NAME = "tablea";

  String _ID = "_id";

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
        Factory<T1> tableAModelFactory, TableBModel.Factory<T2> tableBModelFactory) {
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

    public Mapper(Factory<T> tableAModelFactory) {
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

    public Factory(Creator<T> creator) {
      this.creator = creator;
    }

    public SqlDelightStatement select_custom(@Nullable Integer col1, @Nullable Integer col2) {
      StringBuilder query = new StringBuilder();
      query.append("SELECT *, tableb.*\n"
              + "FROM tablea\n"
              + "LEFT JOIN tableb\n"
              + "ON tablea.tableb_id=tableb._id\n"
              + "WHERE tableb.col1=");
      if (col1 == null) {
        query.append("null");
      } else {
        query.append(col1);
      }
      query.append(" OR tableb.col2=");
      if (col2 == null) {
        query.append("null");
      } else {
        query.append(col2);
      }
      return new SqlDelightStatement(query.toString(), new String[0], new TableSet("tablea", "tableb"));
    }

    public <T2 extends TableBModel, R extends Select_customModel<T, T2>> Select_customMapper<T, T2, R> select_customMapper(Select_customCreator<T, T2, R> creator,
        TableBModel.Factory<T2> tableBModelFactory) {
      return new Select_customMapper<T, T2, R>(creator, this, tableBModelFactory);
    }
  }
}
