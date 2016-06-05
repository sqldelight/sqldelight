package com.thing;

import com.sample.Test1Model;
import com.test.Test2Model;
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

  interface Join_tablesModel {
    Test1Model test1();

    Test2Model test2();
  }

  interface Join_tablesCreator<T extends Join_tablesModel> {
    T create(Test1Model test1, Test2Model test2);
  }

  interface Tables_and_valueModel {
    Test1Model test1();

    long count();

    Test2Model table_alias();
  }

  interface Tables_and_valueCreator<T extends Tables_and_valueModel> {
    T create(Test1Model test1, long count, Test2Model table_alias);
  }

  interface Custom_valueModel {
    Test2Model test2();

    Test1Model test1();

    Date date();
  }

  interface Custom_valueCreator<T extends Custom_valueModel> {
    T create(Test2Model test2, Test1Model test1, Date date);
  }

  interface Aliased_custom_valueModel {
    Test2Model test2();

    Test1Model test1();

    Date created_date();
  }

  interface Aliased_custom_valueCreator<T extends Aliased_custom_valueModel> {
    T create(Test2Model test2, Test1Model test1, Date created_date);
  }

  interface Aliased_tablesModel {
    Test1Model sender();

    Test1Model recipient();

    Test2Model test2();
  }

  interface Aliased_tablesCreator<T extends Aliased_tablesModel> {
    T create(Test1Model sender, Test1Model recipient, Test2Model test2);
  }
}
