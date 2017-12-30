package com.sample;

import android.arch.persistence.db.SupportSQLiteProgram;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import com.squareup.sqldelight.SqlDelightQuery;
import com.squareup.sqldelight.internal.TableSet;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Float;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.Short;
import java.lang.String;
import java.util.Calendar;
import java.util.Collections;

public interface HockeyPlayerModel {
  String TABLE_NAME = "hockey_player";

  String _ID = "_id";

  String FIRST_NAME = "first_name";

  String LAST_NAME = "last_name";

  String NUMBER = "number";

  String TEAM = "team";

  String AGE = "age";

  String WEIGHT = "weight";

  String BIRTH_DATE = "birth_date";

  String SHOOTS = "shoots";

  String POSITION = "position";

  String CREATE_TABLE = ""
      + "CREATE TABLE hockey_player (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  first_name TEXT NOT NULL,\n"
      + "  last_name TEXT NOT NULL,\n"
      + "  number INTEGER NOT NULL,\n"
      + "  team INTEGER,\n"
      + "  age INTEGER NOT NULL,\n"
      + "  weight REAL NOT NULL,\n"
      + "  birth_date TEXT NOT NULL,\n"
      + "  shoots TEXT NOT NULL,\n"
      + "  position TEXT NOT NULL,\n"
      + "  FOREIGN KEY (team) REFERENCES team(_id)\n"
      + ")";

  long _id();

  @NonNull
  String first_name();

  @NonNull
  String last_name();

  int number();

  @Nullable
  Long team();

  int age();

  float weight();

  @NonNull
  Calendar birth_date();

  @NonNull
  HockeyPlayer.Shoots shoots();

  @NonNull
  HockeyPlayer.Position position();

  interface Select_allModel<T1 extends HockeyPlayerModel, T2 extends TeamModel> {
    @NonNull
    T1 hockey_player();

    @NonNull
    T2 team();
  }

  interface Select_allCreator<T1 extends HockeyPlayerModel, T2 extends TeamModel, T extends Select_allModel<T1, T2>> {
    T create(@NonNull T1 hockey_player, @NonNull T2 team);
  }

  final class Select_allMapper<T1 extends HockeyPlayerModel, T2 extends TeamModel, T extends Select_allModel<T1, T2>> implements RowMapper<T> {
    private final Select_allCreator<T1, T2, T> creator;

    private final Factory<T1> hockeyPlayerModelFactory;

    private final TeamModel.Factory<T2> teamModelFactory;

    public Select_allMapper(Select_allCreator<T1, T2, T> creator,
        Factory<T1> hockeyPlayerModelFactory, TeamModel.Factory<T2> teamModelFactory) {
      this.creator = creator;
      this.hockeyPlayerModelFactory = hockeyPlayerModelFactory;
      this.teamModelFactory = teamModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          hockeyPlayerModelFactory.creator.create(
              cursor.getLong(0),
              cursor.getString(1),
              cursor.getString(2),
              cursor.getInt(3),
              cursor.isNull(4) ? null : cursor.getLong(4),
              cursor.getInt(5),
              cursor.getFloat(6),
              hockeyPlayerModelFactory.birth_dateAdapter.decode(cursor.getString(7)),
              hockeyPlayerModelFactory.shootsAdapter.decode(cursor.getString(8)),
              hockeyPlayerModelFactory.positionAdapter.decode(cursor.getString(9))
          ),
          teamModelFactory.creator.create(
              cursor.getLong(10),
              cursor.getString(11),
              teamModelFactory.foundedAdapter.decode(cursor.getString(12)),
              cursor.getString(13),
              cursor.isNull(14) ? null : cursor.getLong(14),
              cursor.getInt(15) == 1
          )
      );
    }
  }

  interface For_teamModel<T1 extends HockeyPlayerModel, T2 extends TeamModel> {
    @NonNull
    T1 hockey_player();

    @NonNull
    T2 team();
  }

  interface For_teamCreator<T1 extends HockeyPlayerModel, T2 extends TeamModel, T extends For_teamModel<T1, T2>> {
    T create(@NonNull T1 hockey_player, @NonNull T2 team);
  }

  final class For_teamMapper<T1 extends HockeyPlayerModel, T2 extends TeamModel, T extends For_teamModel<T1, T2>> implements RowMapper<T> {
    private final For_teamCreator<T1, T2, T> creator;

    private final Factory<T1> hockeyPlayerModelFactory;

    private final TeamModel.Factory<T2> teamModelFactory;

    public For_teamMapper(For_teamCreator<T1, T2, T> creator, Factory<T1> hockeyPlayerModelFactory,
        TeamModel.Factory<T2> teamModelFactory) {
      this.creator = creator;
      this.hockeyPlayerModelFactory = hockeyPlayerModelFactory;
      this.teamModelFactory = teamModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          hockeyPlayerModelFactory.creator.create(
              cursor.getLong(0),
              cursor.getString(1),
              cursor.getString(2),
              cursor.getInt(3),
              cursor.isNull(4) ? null : cursor.getLong(4),
              cursor.getInt(5),
              cursor.getFloat(6),
              hockeyPlayerModelFactory.birth_dateAdapter.decode(cursor.getString(7)),
              hockeyPlayerModelFactory.shootsAdapter.decode(cursor.getString(8)),
              hockeyPlayerModelFactory.positionAdapter.decode(cursor.getString(9))
          ),
          teamModelFactory.creator.create(
              cursor.getLong(10),
              cursor.getString(11),
              teamModelFactory.foundedAdapter.decode(cursor.getString(12)),
              cursor.getString(13),
              cursor.isNull(14) ? null : cursor.getLong(14),
              cursor.getInt(15) == 1
          )
      );
    }
  }

  interface Subquery_joinModel {
    long _id();

    int age();
  }

  interface Subquery_joinCreator<T extends Subquery_joinModel> {
    T create(long _id, int age);
  }

  final class Subquery_joinMapper<T extends Subquery_joinModel> implements RowMapper<T> {
    private final Subquery_joinCreator<T> creator;

    public Subquery_joinMapper(Subquery_joinCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.getInt(1)
      );
    }
  }

  interface Select_expressionModel {
    @NonNull
    String first_name();

    long count();
  }

  interface Select_expressionCreator<T extends Select_expressionModel> {
    T create(@NonNull String first_name, long count);
  }

  final class Select_expressionMapper<T extends Select_expressionModel> implements RowMapper<T> {
    private final Select_expressionCreator<T> creator;

    public Select_expressionMapper(Select_expressionCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getString(0),
          cursor.getLong(1)
      );
    }
  }

  interface Expression_subqueryModel<T1 extends HockeyPlayerModel> {
    @NonNull
    T1 hockey_player();

    long size();
  }

  interface Expression_subqueryCreator<T1 extends HockeyPlayerModel, T extends Expression_subqueryModel<T1>> {
    T create(@NonNull T1 hockey_player, long size);
  }

  final class Expression_subqueryMapper<T1 extends HockeyPlayerModel, T extends Expression_subqueryModel<T1>> implements RowMapper<T> {
    private final Expression_subqueryCreator<T1, T> creator;

    private final Factory<T1> hockeyPlayerModelFactory;

    public Expression_subqueryMapper(Expression_subqueryCreator<T1, T> creator,
        Factory<T1> hockeyPlayerModelFactory) {
      this.creator = creator;
      this.hockeyPlayerModelFactory = hockeyPlayerModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          hockeyPlayerModelFactory.creator.create(
              cursor.getLong(0),
              cursor.getString(1),
              cursor.getString(2),
              cursor.getInt(3),
              cursor.isNull(4) ? null : cursor.getLong(4),
              cursor.getInt(5),
              cursor.getFloat(6),
              hockeyPlayerModelFactory.birth_dateAdapter.decode(cursor.getString(7)),
              hockeyPlayerModelFactory.shootsAdapter.decode(cursor.getString(8)),
              hockeyPlayerModelFactory.positionAdapter.decode(cursor.getString(9))
          ),
          cursor.getLong(10)
      );
    }
  }

  interface Some_joinModel<T1 extends HockeyPlayerModel, T2 extends TeamModel> {
    @NonNull
    T1 hockey_player();

    @NonNull
    T2 team();
  }

  interface Some_joinCreator<T1 extends HockeyPlayerModel, T2 extends TeamModel, T extends Some_joinModel<T1, T2>> {
    T create(@NonNull T1 hockey_player, @NonNull T2 team);
  }

  final class Some_joinMapper<T1 extends HockeyPlayerModel, T2 extends TeamModel, T extends Some_joinModel<T1, T2>> implements RowMapper<T> {
    private final Some_joinCreator<T1, T2, T> creator;

    private final Factory<T1> hockeyPlayerModelFactory;

    private final TeamModel.Factory<T2> teamModelFactory;

    public Some_joinMapper(Some_joinCreator<T1, T2, T> creator,
        Factory<T1> hockeyPlayerModelFactory, TeamModel.Factory<T2> teamModelFactory) {
      this.creator = creator;
      this.hockeyPlayerModelFactory = hockeyPlayerModelFactory;
      this.teamModelFactory = teamModelFactory;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          hockeyPlayerModelFactory.creator.create(
              cursor.getLong(0),
              cursor.getString(1),
              cursor.getString(2),
              cursor.getInt(3),
              cursor.isNull(4) ? null : cursor.getLong(4),
              cursor.getInt(5),
              cursor.getFloat(6),
              hockeyPlayerModelFactory.birth_dateAdapter.decode(cursor.getString(7)),
              hockeyPlayerModelFactory.shootsAdapter.decode(cursor.getString(8)),
              hockeyPlayerModelFactory.positionAdapter.decode(cursor.getString(9))
          ),
          teamModelFactory.creator.create(
              cursor.getLong(10),
              cursor.getString(11),
              teamModelFactory.foundedAdapter.decode(cursor.getString(12)),
              cursor.getString(13),
              cursor.isNull(14) ? null : cursor.getLong(14),
              cursor.getInt(15) == 1
          )
      );
    }
  }

  interface With_queryModel {
    long int_literal();

    long int_literal_2();

    long int_literal_3();
  }

  interface With_queryCreator<T extends With_queryModel> {
    T create(long int_literal, long int_literal_2, long int_literal_3);
  }

  final class With_queryMapper<T extends With_queryModel> implements RowMapper<T> {
    private final With_queryCreator<T> creator;

    public With_queryMapper(With_queryCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getLong(0),
          cursor.getLong(1),
          cursor.getLong(2)
      );
    }
  }

  interface Creator<T extends HockeyPlayerModel> {
    T create(long _id, @NonNull String first_name, @NonNull String last_name, int number,
        @Nullable Long team, int age, float weight, @NonNull Calendar birth_date,
        @NonNull HockeyPlayer.Shoots shoots, @NonNull HockeyPlayer.Position position);
  }

  final class Mapper<T extends HockeyPlayerModel> implements RowMapper<T> {
    private final Factory<T> hockeyPlayerModelFactory;

    public Mapper(Factory<T> hockeyPlayerModelFactory) {
      this.hockeyPlayerModelFactory = hockeyPlayerModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return hockeyPlayerModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getString(1),
          cursor.getString(2),
          cursor.getInt(3),
          cursor.isNull(4) ? null : cursor.getLong(4),
          cursor.getInt(5),
          cursor.getFloat(6),
          hockeyPlayerModelFactory.birth_dateAdapter.decode(cursor.getString(7)),
          hockeyPlayerModelFactory.shootsAdapter.decode(cursor.getString(8)),
          hockeyPlayerModelFactory.positionAdapter.decode(cursor.getString(9))
      );
    }
  }

  final class Factory<T extends HockeyPlayerModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Calendar, String> birth_dateAdapter;

    public final ColumnAdapter<HockeyPlayer.Shoots, String> shootsAdapter;

    public final ColumnAdapter<HockeyPlayer.Position, String> positionAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Calendar, String> birth_dateAdapter,
        ColumnAdapter<HockeyPlayer.Shoots, String> shootsAdapter,
        ColumnAdapter<HockeyPlayer.Position, String> positionAdapter) {
      this.creator = creator;
      this.birth_dateAdapter = birth_dateAdapter;
      this.shootsAdapter = shootsAdapter;
      this.positionAdapter = positionAdapter;
    }

    public SqlDelightQuery select_all() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM hockey_player\n"
          + "JOIN team ON hockey_player.team = team._id",
          new TableSet("hockey_player", "team"));
    }

    public SqlDelightQuery for_team(long _id) {
      return new For_teamQuery(_id);
    }

    public SqlDelightQuery join_friends() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM hockey_player\n"
          + "WHERE first_name = 'Alec'\n"
          + "UNION\n"
          + "SELECT cheetos.*\n"
          + "FROM hockey_player cheetos\n"
          + "WHERE first_name = 'Jake'\n"
          + "UNION SELECT hockey_player.*\n"
          + "FROM hockey_player\n"
          + "WHERE first_name = 'Matt'",
          new TableSet("hockey_player"));
    }

    public SqlDelightQuery subquery() {
      return new SqlDelightQuery(""
          + "SELECT _id\n"
          + "FROM (\n"
          + "  SELECT *\n"
          + "  FROM hockey_player\n"
          + ")",
          new TableSet("hockey_player"));
    }

    public SqlDelightQuery subquery_join() {
      return new SqlDelightQuery(""
          + "SELECT stuff._id, other_stuff.age\n"
          + "FROM (\n"
          + "  SELECT *\n"
          + "  FROM hockey_player AS stuff\n"
          + ")\n"
          + "JOIN hockey_player AS other_stuff",
          new TableSet("hockey_player"));
    }

    public SqlDelightQuery select_expression() {
      return new SqlDelightQuery(""
          + "SELECT first_name, count(*)\n"
          + "FROM hockey_player\n"
          + "GROUP BY first_name",
          new TableSet("hockey_player"));
    }

    public SqlDelightQuery expression_subquery() {
      return new SqlDelightQuery(""
          + "SELECT hockey_player.*, size\n"
          + "FROM hockey_player\n"
          + "JOIN (SELECT count(*) AS size FROM hockey_player)",
          new TableSet("hockey_player"));
    }

    public SqlDelightQuery order_by_age() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM hockey_player\n"
          + "ORDER BY age",
          new TableSet("hockey_player"));
    }

    public SqlDelightQuery question_marks_everywhere(@Nullable Object arg1, @Nullable Object arg2,
        @Nullable Object arg3, long arg4, @Nullable Object arg5, long arg6) {
      return new Question_marks_everywhereQuery(arg1, arg2, arg3, arg4, arg5, arg6);
    }

    public SqlDelightQuery subquery_uses_ignored_column() {
      return new SqlDelightQuery(""
          + "SELECT count(*)\n"
          + "FROM (\n"
          + "  SELECT count(*) as cheese\n"
          + "  FROM hockey_player\n"
          + "  WHERE age = 19\n"
          + ") as cheesy\n"
          + "WHERE cheesy.cheese = 10",
          new TableSet("hockey_player"));
    }

    public SqlDelightQuery kids() {
      return new SqlDelightQuery(""
          + "SELECT count(*)\n"
          + "FROM hockey_player\n"
          + "WHERE age=19",
          new TableSet("hockey_player"));
    }

    public SqlDelightQuery some_join() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM hockey_player\n"
          + "INNER JOIN team ON hockey_player._id = team._id",
          new TableSet("hockey_player", "team"));
    }

    public SqlDelightQuery multiple_values_for_query() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM ( VALUES (1), (2), (3), (4) )",
          Collections.<String>emptySet());
    }

    public SqlDelightQuery with_query() {
      return new SqlDelightQuery(""
          + "WITH temp_table AS (\n"
          + "  VALUES (1)\n"
          + "), temp_table2 AS (\n"
          + "  VALUES (1, 2)\n"
          + ")\n"
          + "SELECT *\n"
          + "FROM temp_table2\n"
          + "JOIN temp_table",
          Collections.<String>emptySet());
    }

    public SqlDelightQuery is_not_expr() {
      return new SqlDelightQuery(""
          + "SELECT *\n"
          + "FROM hockey_player\n"
          + "WHERE _id IS NOT 2",
          new TableSet("hockey_player"));
    }

    public SqlDelightQuery order_by_expr() {
      return new SqlDelightQuery(""
          + "SELECT birth_date\n"
          + "FROM hockey_player\n"
          + "ORDER BY age\n"
          + "LIMIT 1",
          new TableSet("hockey_player"));
    }

    public SqlDelightQuery inner_join() {
      return new SqlDelightQuery(""
          + "SELECT hockey_player.*\n"
          + "FROM hockey_player\n"
          + "INNER JOIN team ON hockey_player.team = team._id",
          new TableSet("hockey_player", "team"));
    }

    public <T2 extends TeamModel, R extends Select_allModel<T, T2>> Select_allMapper<T, T2, R> select_allMapper(Select_allCreator<T, T2, R> creator,
        TeamModel.Factory<T2> teamModelFactory) {
      return new Select_allMapper<T, T2, R>(creator, this, teamModelFactory);
    }

    public <T2 extends TeamModel, R extends For_teamModel<T, T2>> For_teamMapper<T, T2, R> for_teamMapper(For_teamCreator<T, T2, R> creator,
        TeamModel.Factory<T2> teamModelFactory) {
      return new For_teamMapper<T, T2, R>(creator, this, teamModelFactory);
    }

    public Mapper<T> join_friendsMapper() {
      return new Mapper<T>(this);
    }

    public RowMapper<Long> subqueryMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    public <R extends Subquery_joinModel> Subquery_joinMapper<R> subquery_joinMapper(Subquery_joinCreator<R> creator) {
      return new Subquery_joinMapper<R>(creator);
    }

    public <R extends Select_expressionModel> Select_expressionMapper<R> select_expressionMapper(Select_expressionCreator<R> creator) {
      return new Select_expressionMapper<R>(creator);
    }

    public <R extends Expression_subqueryModel<T>> Expression_subqueryMapper<T, R> expression_subqueryMapper(Expression_subqueryCreator<T, R> creator) {
      return new Expression_subqueryMapper<T, R>(creator, this);
    }

    public Mapper<T> order_by_ageMapper() {
      return new Mapper<T>(this);
    }

    public RowMapper<Long> question_marks_everywhereMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    public RowMapper<Long> subquery_uses_ignored_columnMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    public RowMapper<Long> kidsMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    public <T2 extends TeamModel, R extends Some_joinModel<T, T2>> Some_joinMapper<T, T2, R> some_joinMapper(Some_joinCreator<T, T2, R> creator,
        TeamModel.Factory<T2> teamModelFactory) {
      return new Some_joinMapper<T, T2, R>(creator, this, teamModelFactory);
    }

    public RowMapper<Long> multiple_values_for_queryMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    public <R extends With_queryModel> With_queryMapper<R> with_queryMapper(With_queryCreator<R> creator) {
      return new With_queryMapper<R>(creator);
    }

    public Mapper<T> is_not_exprMapper() {
      return new Mapper<T>(this);
    }

    public RowMapper<Calendar> order_by_exprMapper() {
      return new RowMapper<Calendar>() {
        @Override
        public Calendar map(Cursor cursor) {
          return birth_dateAdapter.decode(cursor.getString(0));
        }
      };
    }

    public Mapper<T> inner_joinMapper() {
      return new Mapper<T>(this);
    }

    private final class For_teamQuery extends SqlDelightQuery {
      private final long _id;

      For_teamQuery(long _id) {
        super("SELECT *\n"
            + "FROM hockey_player\n"
            + "JOIN team ON hockey_player.team = team._id\n"
            + "WHERE team._id = ?1",
            new TableSet("hockey_player", "team"));

        this._id = _id;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        program.bindLong(1, _id);
      }
    }

    private final class Question_marks_everywhereQuery extends SqlDelightQuery {
      @Nullable
      private final Object arg1;

      @Nullable
      private final Object arg2;

      @Nullable
      private final Object arg3;

      private final long arg4;

      @Nullable
      private final Object arg5;

      private final long arg6;

      Question_marks_everywhereQuery(@Nullable Object arg1, @Nullable Object arg2,
          @Nullable Object arg3, long arg4, @Nullable Object arg5, long arg6) {
        super("SELECT _id\n"
            + "FROM hockey_player\n"
            + "WHERE ?1 = ?2\n"
            + "GROUP BY ?3 HAVING ?4\n"
            + "ORDER BY ?5 ASC\n"
            + "LIMIT ?6",
            new TableSet("hockey_player"));

        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
        this.arg4 = arg4;
        this.arg5 = arg5;
        this.arg6 = arg6;
      }

      @Override
      public void bindTo(SupportSQLiteProgram program) {
        Object arg1 = this.arg1;
        if (arg1 != null) {
          if (arg1 instanceof String) {
            program.bindString(1, (String) arg1);
          } else if (arg1 instanceof Long || arg1 instanceof Integer || arg1 instanceof Short) {
            program.bindLong(1, (long) arg1);
          } else if (arg1 instanceof Boolean) {
            program.bindLong(1, (boolean) arg1 ? 0 : 1);
          } else if (arg1 instanceof byte[]) {
            program.bindBlob(1, (byte[]) arg1);
          } else if (arg1 instanceof Float || arg1 instanceof Double) {
            program.bindDouble(1, (double) arg1);
          } else {
            throw new IllegalArgumentException("Attempting to bind an object that is not one of String, Integer, Short, Long, Float, Double, Boolean, or byte[] to argument arg1");
          }
        } else {
          program.bindNull(1);
        }

        Object arg2 = this.arg2;
        if (arg2 != null) {
          if (arg2 instanceof String) {
            program.bindString(2, (String) arg2);
          } else if (arg2 instanceof Long || arg2 instanceof Integer || arg2 instanceof Short) {
            program.bindLong(2, (long) arg2);
          } else if (arg2 instanceof Boolean) {
            program.bindLong(2, (boolean) arg2 ? 0 : 1);
          } else if (arg2 instanceof byte[]) {
            program.bindBlob(2, (byte[]) arg2);
          } else if (arg2 instanceof Float || arg2 instanceof Double) {
            program.bindDouble(2, (double) arg2);
          } else {
            throw new IllegalArgumentException("Attempting to bind an object that is not one of String, Integer, Short, Long, Float, Double, Boolean, or byte[] to argument arg2");
          }
        } else {
          program.bindNull(2);
        }

        Object arg3 = this.arg3;
        if (arg3 != null) {
          if (arg3 instanceof String) {
            program.bindString(3, (String) arg3);
          } else if (arg3 instanceof Long || arg3 instanceof Integer || arg3 instanceof Short) {
            program.bindLong(3, (long) arg3);
          } else if (arg3 instanceof Boolean) {
            program.bindLong(3, (boolean) arg3 ? 0 : 1);
          } else if (arg3 instanceof byte[]) {
            program.bindBlob(3, (byte[]) arg3);
          } else if (arg3 instanceof Float || arg3 instanceof Double) {
            program.bindDouble(3, (double) arg3);
          } else {
            throw new IllegalArgumentException("Attempting to bind an object that is not one of String, Integer, Short, Long, Float, Double, Boolean, or byte[] to argument arg3");
          }
        } else {
          program.bindNull(3);
        }

        program.bindLong(4, arg4);

        Object arg5 = this.arg5;
        if (arg5 != null) {
          if (arg5 instanceof String) {
            program.bindString(5, (String) arg5);
          } else if (arg5 instanceof Long || arg5 instanceof Integer || arg5 instanceof Short) {
            program.bindLong(5, (long) arg5);
          } else if (arg5 instanceof Boolean) {
            program.bindLong(5, (boolean) arg5 ? 0 : 1);
          } else if (arg5 instanceof byte[]) {
            program.bindBlob(5, (byte[]) arg5);
          } else if (arg5 instanceof Float || arg5 instanceof Double) {
            program.bindDouble(5, (double) arg5);
          } else {
            throw new IllegalArgumentException("Attempting to bind an object that is not one of String, Integer, Short, Long, Float, Double, Boolean, or byte[] to argument arg5");
          }
        } else {
          program.bindNull(5);
        }

        program.bindLong(6, arg6);
      }
    }
  }
}
