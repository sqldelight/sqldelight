package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightStatement;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface TestViewModel {
  String CREATE_VIEW = ""
      + "CREATE VIEW test_view AS SELECT id, date FROM test";

  String QUERYTEST1 = ""
      + "SELECT * FROM test_view WHERE date > ?";

  String QUERYTEST2 = ""
      + "SELECT * FROM test_view WHERE id = ? AND date > ?";

  interface Test_viewModel {
    @Nullable
    Long id();

    @NonNull
    List date();
  }

  interface Test_viewCreator<T extends Test_viewModel> {
    T create(@Nullable Long id, @NonNull List date);
  }

  final class Test_viewMapper<T extends Test_viewModel, T1 extends TestModel> implements RowMapper<T> {
    private final Test_viewCreator<T> creator;

    private final TestModel.Factory<T1> testModelFactory;

    public Test_viewMapper(Test_viewCreator<T> creator, TestModel.Factory<T1> testModelFactory) {
      this.creator = creator;
      this.testModelFactory = testModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          testModelFactory.dateAdapter.decode(cursor.getLong(1))
      );
    }
  }

  final class Factory {
    public Factory() {
    }

    public SqlDelightStatement queryTest1(TestModel.Factory testModelFactory, @NonNull List date) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT * FROM test_view WHERE date > ");
      query.append(testModelFactory.dateAdapter.encode(date));
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }

    public SqlDelightStatement queryTest2(TestModel.Factory testModelFactory, @Nullable Long id, @NonNull List date) {
      List<String> args = new ArrayList<String>();
      int currentIndex = 1;
      StringBuilder query = new StringBuilder();
      query.append("SELECT * FROM test_view WHERE id = ");
      if (id == null) {
        query.append("null");
      } else {
        query.append(id);
      }
      query.append(" AND date > ");
      query.append(testModelFactory.dateAdapter.encode(date));
      return new SqlDelightStatement(query.toString(), args.toArray(new String[args.size()]), Collections.<String>singleton("test"));
    }

    public <R extends Test_viewModel, T1 extends TestModel> Test_viewMapper<R, T1> queryTest1Mapper(Test_viewCreator<R> creator, TestModel.Factory<T1> testModelFactory) {
      return new Test_viewMapper<R, T1>(creator, testModelFactory);
    }

    public <R extends Test_viewModel, T1 extends TestModel> Test_viewMapper<R, T1> queryTest2Mapper(Test_viewCreator<R> creator, TestModel.Factory<T1> testModelFactory) {
      return new Test_viewMapper<R, T1>(creator, testModelFactory);
    }
  }
}
