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
import java.util.Collections;
import java.util.List;

public interface TestViewModel {
  String TEST_VIEW_VIEW_NAME = "test_view";

  String CREATE_VIEW = ""
      + "CREATE VIEW test_view AS SELECT id, date FROM test";

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

  final class Factory<T1 extends TestModel> {
    TestModel.Factory<T1> testModelFactory;

    public Factory(TestModel.Factory<T1> testModelFactory) {
      this.testModelFactory = testModelFactory;
    }

    public SqlDelightStatement queryTest1(@NonNull List date) {
      StringBuilder query = new StringBuilder();
      query.append("SELECT * FROM test_view WHERE date > ");
      query.append(testModelFactory.dateAdapter.encode(date));
      return new SqlDelightStatement(query.toString(), new String[0], Collections.<String>singleton("test"));
    }

    public SqlDelightStatement queryTest2(@Nullable Long id, @NonNull List date) {
      StringBuilder query = new StringBuilder();
      query.append("SELECT * FROM test_view WHERE id = ");
      if (id == null) {
        int start = query.lastIndexOf("= ");
        int end = query.length();
        query.replace(start, end, "is null");
      } else {
        query.append(id);
      }
      query.append(" AND date > ");
      query.append(testModelFactory.dateAdapter.encode(date));
      return new SqlDelightStatement(query.toString(), new String[0], Collections.<String>singleton("test"));
    }

    public <R extends Test_viewModel> Test_viewMapper<R, T1> queryTest1Mapper(Test_viewCreator<R> creator) {
      return new Test_viewMapper<R, T1>(creator, testModelFactory);
    }

    public <R extends Test_viewModel> Test_viewMapper<R, T1> queryTest2Mapper(Test_viewCreator<R> creator) {
      return new Test_viewMapper<R, T1>(creator, testModelFactory);
    }
  }
}
