package com.thing;

import android.database.Cursor;
import android.support.annotation.NonNull;
import com.sample.Test1Model;
import com.squareup.sqldelight.RowMapper;
import com.test.Test2Model;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Date;

public interface Test3Model {
  String JOIN_TABLES = ""
      + "SELECT *\n"
      + "FROM test1\n"
      + "JOIN test2";

  String ONE_TABLE = ""
      + "SELECT *\n"
      + "FROM test1";

  String TABLES_AND_VALUE = ""
      + "SELECT test1.*, count(*), table_alias.*\n"
      + "FROM test2 AS table_alias\n"
      + "JOIN test1";

  String CUSTOM_VALUE = ""
      + "SELECT test2.*, test1.*, test1.date\n"
      + "FROM test1\n"
      + "JOIN test2";

  String ALIASED_CUSTOM_VALUE = ""
      + "SELECT test2.*, test1.*, test1.date AS created_date\n"
      + "FROM test1\n"
      + "JOIN test2";

  String ALIASED_TABLES = ""
      + "SELECT sender.*, recipient.*, test2.*\n"
      + "FROM test1 AS sender\n"
      + "JOIN test1 AS recipient\n"
      + "JOIN test2";

  String SINGLE_VALUE = ""
      + "SELECT count(_id)\n"
      + "FROM test1";

  interface Join_tablesModel {
    Test1Model test1();

    Test2Model test2();
  }

  interface Join_tablesCreator<T extends Join_tablesModel> {
    T create(Test1Model test1, Test2Model test2);
  }

  final class Join_tablesMapper<T extends Join_tablesModel, R1 extends Test1Model, R2 extends Test2Model> implements RowMapper<T> {
    private final Join_tablesCreator<T> creator;

    private final Test1Model.Factory<R1> test1ModelFactory;

    private final Test2Model.Factory<R2> test2ModelFactory;

    private Join_tablesMapper(Join_tablesCreator<T> creator, Test1Model.Factory<R1> test1ModelFactory, Test2Model.Factory<R2> test2ModelFactory) {
      this.creator = creator;
      this.test1ModelFactory = test1ModelFactory;
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test1ModelFactory.creator.create(
              cursor.isNull(0) ? null : cursor.getLong(0),
              cursor.isNull(1) ? null : test1ModelFactory.dateAdapter.map(cursor, 1)
          ),
          test2ModelFactory.creator.create(
              cursor.isNull(2) ? null : cursor.getLong(2)
          )
      );
    }
  }

  interface Tables_and_valueModel {
    Test1Model test1();

    long count();

    Test2Model table_alias();
  }

  interface Tables_and_valueCreator<T extends Tables_and_valueModel> {
    T create(Test1Model test1, long count, Test2Model table_alias);
  }

  final class Tables_and_valueMapper<T extends Tables_and_valueModel, R1 extends Test1Model, R2 extends Test2Model> implements RowMapper<T> {
    private final Tables_and_valueCreator<T> creator;

    private final Test1Model.Factory<R1> test1ModelFactory;

    private final Test2Model.Factory<R2> test2ModelFactory;

    private Tables_and_valueMapper(Tables_and_valueCreator<T> creator, Test1Model.Factory<R1> test1ModelFactory, Test2Model.Factory<R2> test2ModelFactory) {
      this.creator = creator;
      this.test1ModelFactory = test1ModelFactory;
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test1ModelFactory.creator.create(
              cursor.isNull(0) ? null : cursor.getLong(0),
              cursor.isNull(1) ? null : test1ModelFactory.dateAdapter.map(cursor, 1)
          ),
          cursor.isNull(2) ? null : cursor.getLong(2),
          test2ModelFactory.creator.create(
              cursor.isNull(3) ? null : cursor.getLong(3)
          )
      );
    }
  }

  interface Custom_valueModel {
    Test2Model test2();

    Test1Model test1();

    Date date();
  }

  interface Custom_valueCreator<T extends Custom_valueModel> {
    T create(Test2Model test2, Test1Model test1, Date date);
  }

  final class Custom_valueMapper<T extends Custom_valueModel, R1 extends Test2Model, R2 extends Test1Model> implements RowMapper<T> {
    private final Custom_valueCreator<T> creator;

    private final Test2Model.Factory<R1> test2ModelFactory;

    private final Test1Model.Factory<R2> test1ModelFactory;

    private Custom_valueMapper(Custom_valueCreator<T> creator, Test2Model.Factory<R1> test2ModelFactory, Test1Model.Factory<R2> test1ModelFactory) {
      this.creator = creator;
      this.test2ModelFactory = test2ModelFactory;
      this.test1ModelFactory = test1ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test2ModelFactory.creator.create(
              cursor.isNull(0) ? null : cursor.getLong(0)
          ),
          test1ModelFactory.creator.create(
              cursor.isNull(1) ? null : cursor.getLong(1),
              cursor.isNull(2) ? null : test1ModelFactory.dateAdapter.map(cursor, 2)
          ),
          cursor.isNull(3) ? null : test1ModelFactory.dateAdapter.map(cursor, 3)
      );
    }
  }

  interface Aliased_custom_valueModel {
    Test2Model test2();

    Test1Model test1();

    Date created_date();
  }

  interface Aliased_custom_valueCreator<T extends Aliased_custom_valueModel> {
    T create(Test2Model test2, Test1Model test1, Date created_date);
  }

  final class Aliased_custom_valueMapper<T extends Aliased_custom_valueModel, R1 extends Test2Model, R2 extends Test1Model> implements RowMapper<T> {
    private final Aliased_custom_valueCreator<T> creator;

    private final Test2Model.Factory<R1> test2ModelFactory;

    private final Test1Model.Factory<R2> test1ModelFactory;

    private Aliased_custom_valueMapper(Aliased_custom_valueCreator<T> creator, Test2Model.Factory<R1> test2ModelFactory, Test1Model.Factory<R2> test1ModelFactory) {
      this.creator = creator;
      this.test2ModelFactory = test2ModelFactory;
      this.test1ModelFactory = test1ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test2ModelFactory.creator.create(
              cursor.isNull(0) ? null : cursor.getLong(0)
          ),
          test1ModelFactory.creator.create(
              cursor.isNull(1) ? null : cursor.getLong(1),
              cursor.isNull(2) ? null : test1ModelFactory.dateAdapter.map(cursor, 2)
          ),
          cursor.isNull(3) ? null : test1ModelFactory.dateAdapter.map(cursor, 3)
      );
    }
  }

  interface Aliased_tablesModel {
    Test1Model sender();

    Test1Model recipient();

    Test2Model test2();
  }

  interface Aliased_tablesCreator<T extends Aliased_tablesModel> {
    T create(Test1Model sender, Test1Model recipient, Test2Model test2);
  }

  final class Aliased_tablesMapper<T extends Aliased_tablesModel, R1 extends Test1Model, R2 extends Test2Model> implements RowMapper<T> {
    private final Aliased_tablesCreator<T> creator;

    private final Test1Model.Factory<R1> test1ModelFactory;

    private final Test2Model.Factory<R2> test2ModelFactory;

    private Aliased_tablesMapper(Aliased_tablesCreator<T> creator, Test1Model.Factory<R1> test1ModelFactory, Test2Model.Factory<R2> test2ModelFactory) {
      this.creator = creator;
      this.test1ModelFactory = test1ModelFactory;
      this.test2ModelFactory = test2ModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          test1ModelFactory.creator.create(
              cursor.isNull(0) ? null : cursor.getLong(0),
              cursor.isNull(1) ? null : test1ModelFactory.dateAdapter.map(cursor, 1)
          ),
          test1ModelFactory.creator.create(
              cursor.isNull(2) ? null : cursor.getLong(2),
              cursor.isNull(3) ? null : test1ModelFactory.dateAdapter.map(cursor, 3)
          ),
          test2ModelFactory.creator.create(
              cursor.isNull(4) ? null : cursor.getLong(4)
          )
      );
    }
  }

  final class Factory {
    public Factory() {
    }

    public <R extends Join_tablesModel, R1 extends Test1Model, R2 extends Test2Model> Join_tablesMapper<R, R1, R2> join_tablesMapper(Join_tablesCreator<R> creator, Test1Model.Factory<R1> test1ModelFactory, Test2Model.Factory<R2> test2ModelFactory) {
      return new Join_tablesMapper<>(creator, test1ModelFactory, test2ModelFactory);
    }

    public <R1 extends Test1Model> Test1Model.Mapper<R1> one_tableMapper(Test1Model.Factory<R1> test1ModelFactory) {
      return new Test1Model.Mapper<>(test1ModelFactory);
    }

    public <R extends Tables_and_valueModel, R1 extends Test1Model, R2 extends Test2Model> Tables_and_valueMapper<R, R1, R2> tables_and_valueMapper(Tables_and_valueCreator<R> creator, Test1Model.Factory<R1> test1ModelFactory, Test2Model.Factory<R2> test2ModelFactory) {
      return new Tables_and_valueMapper<>(creator, test1ModelFactory, test2ModelFactory);
    }

    public <R extends Custom_valueModel, R1 extends Test2Model, R2 extends Test1Model> Custom_valueMapper<R, R1, R2> custom_valueMapper(Custom_valueCreator<R> creator, Test2Model.Factory<R1> test2ModelFactory, Test1Model.Factory<R2> test1ModelFactory) {
      return new Custom_valueMapper<>(creator, test2ModelFactory, test1ModelFactory);
    }

    public <R extends Aliased_custom_valueModel, R1 extends Test2Model, R2 extends Test1Model> Aliased_custom_valueMapper<R, R1, R2> aliased_custom_valueMapper(Aliased_custom_valueCreator<R> creator, Test2Model.Factory<R1> test2ModelFactory, Test1Model.Factory<R2> test1ModelFactory) {
      return new Aliased_custom_valueMapper<>(creator, test2ModelFactory, test1ModelFactory);
    }

    public <R extends Aliased_tablesModel, R1 extends Test1Model, R2 extends Test2Model> Aliased_tablesMapper<R, R1, R2> aliased_tablesMapper(Aliased_tablesCreator<R> creator, Test1Model.Factory<R1> test1ModelFactory, Test2Model.Factory<R2> test2ModelFactory) {
      return new Aliased_tablesMapper<>(creator, test1ModelFactory, test2ModelFactory);
    }

    public RowMapper<Long> single_valueMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.isNull(0) ? null : cursor.getLong(0);
        }
      };
    }
  }
}
