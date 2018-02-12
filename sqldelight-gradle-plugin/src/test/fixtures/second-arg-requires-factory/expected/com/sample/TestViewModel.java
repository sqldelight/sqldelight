package com.sample;

import android.arch.persistence.db.SupportSQLiteProgram;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Deprecated;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public interface TestViewModel {
  @Deprecated
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

    public SqlDelightQuery queryTest1(
        @NonNull TestModel.Factory<? extends TestModel> testModelFactory, @NonNull List date) {
      return new QueryTest1Query(testModelFactory, date);
    }

    public SqlDelightQuery queryTest2(
        @NonNull TestModel.Factory<? extends TestModel> testModelFactory, @Nullable Long id,
        @NonNull List date) {
      return new QueryTest2Query(testModelFactory, id, date);
    }

    public <R extends Test_viewModel> Test_viewMapper<R, T1> queryTest1Mapper(
        Test_viewCreator<R> creator) {
      return new Test_viewMapper<R, T1>(creator, testModelFactory);
    }

    public <R extends Test_viewModel> Test_viewMapper<R, T1> queryTest2Mapper(
        Test_viewCreator<R> creator) {
      return new Test_viewMapper<R, T1>(creator, testModelFactory);
    }

    private final class QueryTest1Query extends SqlDelightQuery {
      @NonNull
      private final TestModel.Factory<? extends TestModel> testModelFactory;

      @NonNull
      private final List date;

      QueryTest1Query(@NonNull TestModel.Factory<? extends TestModel> testModelFactory,
          @NonNull List date) {
        super("SELECT * FROM test_view WHERE date > ?1",
            new TableSet("test"));

        this.testModelFactory = testModelFactory;
        this.date = date;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, testModelFactory.dateAdapter.encode(date));
      }
    }

    private final class QueryTest2Query extends SqlDelightQuery {
      @NonNull
      private final TestModel.Factory<? extends TestModel> testModelFactory;

      @Nullable
      private final Long id;

      @NonNull
      private final List date;

      QueryTest2Query(@NonNull TestModel.Factory<? extends TestModel> testModelFactory,
          @Nullable Long id, @NonNull List date) {
        super("SELECT * FROM test_view WHERE id = ?1 AND date > ?2",
            new TableSet("test"));

        this.testModelFactory = testModelFactory;
        this.id = id;
        this.date = date;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Long id = this.id;
        if (id != null) {
          program.bindLong(1, id);
        } else {
          program.bindNull(1);
        }

        program.bindLong(2, testModelFactory.dateAdapter.encode(date));
      }
    }
  }
}
