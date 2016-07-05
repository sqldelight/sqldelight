package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Calendar;

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

  String SELECT_ALL = ""
      + "SELECT *\n"
      + "FROM hockey_player\n"
      + "JOIN team ON hockey_player.team = team._id";

  String FOR_TEAM = ""
      + "SELECT *\n"
      + "FROM hockey_player\n"
      + "JOIN team ON hockey_player.team = team._id\n"
      + "WHERE team._id = ?";

  String JOIN_FRIENDS = ""
      + "SELECT *\n"
      + "FROM hockey_player\n"
      + "WHERE first_name = 'Alec'\n"
      + "UNION\n"
      + "SELECT cheetos.*\n"
      + "FROM hockey_player cheetos\n"
      + "WHERE first_name = 'Jake'\n"
      + "UNION SELECT hockey_player.*\n"
      + "FROM hockey_player\n"
      + "WHERE first_name = 'Matt'";

  String SUBQUERY = ""
      + "SELECT _id\n"
      + "FROM (\n"
      + "  SELECT *\n"
      + "  FROM hockey_player\n"
      + ")";

  String SUBQUERY_JOIN = ""
      + "SELECT stuff._id, other_stuff.age\n"
      + "FROM (\n"
      + "  SELECT *\n"
      + "  FROM hockey_player AS stuff\n"
      + ")\n"
      + "JOIN hockey_player AS other_stuff";

  String SELECT_EXPRESSION = ""
      + "SELECT first_name, count(*)\n"
      + "FROM hockey_player\n"
      + "GROUP BY first_name";

  String EXPRESSION_SUBQUERY = ""
      + "SELECT hockey_player.*, size\n"
      + "FROM hockey_player\n"
      + "JOIN (SELECT count(*) AS size FROM hockey_player)";

  String ORDER_BY_AGE = ""
      + "SELECT *\n"
      + "FROM hockey_player\n"
      + "ORDER BY age";

  String QUESTION_MARKS_EVERYWHERE = ""
      + "SELECT _id\n"
      + "FROM hockey_player\n"
      + "WHERE ? = ?\n"
      + "GROUP BY ? HAVING ?\n"
      + "ORDER BY ? ASC\n"
      + "LIMIT ?";

  String SUBQUERY_USES_IGNORED_COLUMN = ""
      + "SELECT count(*)\n"
      + "FROM (\n"
      + "  SELECT count(*) as cheese\n"
      + "  FROM hockey_player\n"
      + "  WHERE age = 19\n"
      + ") as cheesy\n"
      + "WHERE cheesy.cheese = 10";

  String KIDS = ""
      + "SELECT count(*)\n"
      + "FROM hockey_player\n"
      + "WHERE age=19";

  String SOME_JOIN = ""
      + "SELECT *\n"
      + "FROM hockey_player\n"
      + "INNER JOIN team ON hockey_player._id = team._id";

  String MULTIPLE_VALUES_FOR_QUERY = ""
      + "SELECT *\n"
      + "FROM ( VALUES (1), (2), (3), (4) )";

  String WITH_QUERY = ""
      + "WITH temp_table AS (\n"
      + "  VALUES (1)\n"
      + "), temp_table2 AS (\n"
      + "  VALUES (1, 2)\n"
      + ")\n"
      + "SELECT *\n"
      + "FROM temp_table2\n"
      + "JOIN temp_table";

  String IS_NOT_EXPR = ""
      + "SELECT *\n"
      + "FROM hockey_player\n"
      + "WHERE _id IS NOT 2";

  String ORDER_BY_EXPR = ""
      + "SELECT birth_date\n"
      + "FROM hockey_player\n"
      + "ORDER BY age\n"
      + "LIMIT 1";

  String INNER_JOIN = ""
      + "SELECT hockey_player.*\n"
      + "FROM hockey_player\n"
      + "INNER JOIN team ON hockey_player.team = team._id";

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

    public Select_allMapper(Select_allCreator<T1, T2, T> creator, Factory<T1> hockeyPlayerModelFactory, TeamModel.Factory<T2> teamModelFactory) {
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
              hockeyPlayerModelFactory.birth_dateAdapter.map(cursor, 7),
              hockeyPlayerModelFactory.shootsAdapter.map(cursor, 8),
              hockeyPlayerModelFactory.positionAdapter.map(cursor, 9)
          ),
          teamModelFactory.creator.create(
              cursor.getLong(10),
              cursor.getString(11),
              teamModelFactory.foundedAdapter.map(cursor, 12),
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

    public For_teamMapper(For_teamCreator<T1, T2, T> creator, Factory<T1> hockeyPlayerModelFactory, TeamModel.Factory<T2> teamModelFactory) {
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
              hockeyPlayerModelFactory.birth_dateAdapter.map(cursor, 7),
              hockeyPlayerModelFactory.shootsAdapter.map(cursor, 8),
              hockeyPlayerModelFactory.positionAdapter.map(cursor, 9)
          ),
          teamModelFactory.creator.create(
              cursor.getLong(10),
              cursor.getString(11),
              teamModelFactory.foundedAdapter.map(cursor, 12),
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

    public Expression_subqueryMapper(Expression_subqueryCreator<T1, T> creator, Factory<T1> hockeyPlayerModelFactory) {
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
              hockeyPlayerModelFactory.birth_dateAdapter.map(cursor, 7),
              hockeyPlayerModelFactory.shootsAdapter.map(cursor, 8),
              hockeyPlayerModelFactory.positionAdapter.map(cursor, 9)
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

    public Some_joinMapper(Some_joinCreator<T1, T2, T> creator, Factory<T1> hockeyPlayerModelFactory, TeamModel.Factory<T2> teamModelFactory) {
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
              hockeyPlayerModelFactory.birth_dateAdapter.map(cursor, 7),
              hockeyPlayerModelFactory.shootsAdapter.map(cursor, 8),
              hockeyPlayerModelFactory.positionAdapter.map(cursor, 9)
          ),
          teamModelFactory.creator.create(
              cursor.getLong(10),
              cursor.getString(11),
              teamModelFactory.foundedAdapter.map(cursor, 12),
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
    T create(long _id, @NonNull String first_name, @NonNull String last_name, int number, @Nullable Long team, int age, float weight, @NonNull Calendar birth_date, @NonNull HockeyPlayer.Shoots shoots, @NonNull HockeyPlayer.Position position);
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
          hockeyPlayerModelFactory.birth_dateAdapter.map(cursor, 7),
          hockeyPlayerModelFactory.shootsAdapter.map(cursor, 8),
          hockeyPlayerModelFactory.positionAdapter.map(cursor, 9)
      );
    }
  }

  final class Marshal {
    protected final ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Calendar> birth_dateAdapter;

    private final ColumnAdapter<HockeyPlayer.Shoots> shootsAdapter;

    private final ColumnAdapter<HockeyPlayer.Position> positionAdapter;

    Marshal(@Nullable HockeyPlayerModel copy, ColumnAdapter<Calendar> birth_dateAdapter, ColumnAdapter<HockeyPlayer.Shoots> shootsAdapter, ColumnAdapter<HockeyPlayer.Position> positionAdapter) {
      this.birth_dateAdapter = birth_dateAdapter;
      this.shootsAdapter = shootsAdapter;
      this.positionAdapter = positionAdapter;
      if (copy != null) {
        this._id(copy._id());
        this.first_name(copy.first_name());
        this.last_name(copy.last_name());
        this.number(copy.number());
        this.team(copy.team());
        this.age(copy.age());
        this.weight(copy.weight());
        this.birth_date(copy.birth_date());
        this.shoots(copy.shoots());
        this.position(copy.position());
      }
    }

    public ContentValues asContentValues() {
      return contentValues;
    }

    public Marshal _id(long _id) {
      contentValues.put(_ID, _id);
      return this;
    }

    public Marshal first_name(String first_name) {
      contentValues.put(FIRST_NAME, first_name);
      return this;
    }

    public Marshal last_name(String last_name) {
      contentValues.put(LAST_NAME, last_name);
      return this;
    }

    public Marshal number(int number) {
      contentValues.put(NUMBER, number);
      return this;
    }

    public Marshal team(Long team) {
      contentValues.put(TEAM, team);
      return this;
    }

    public Marshal age(int age) {
      contentValues.put(AGE, age);
      return this;
    }

    public Marshal weight(float weight) {
      contentValues.put(WEIGHT, weight);
      return this;
    }

    public Marshal birth_date(Calendar birth_date) {
      birth_dateAdapter.marshal(contentValues, BIRTH_DATE, birth_date);
      return this;
    }

    public Marshal shoots(HockeyPlayer.Shoots shoots) {
      shootsAdapter.marshal(contentValues, SHOOTS, shoots);
      return this;
    }

    public Marshal position(HockeyPlayer.Position position) {
      positionAdapter.marshal(contentValues, POSITION, position);
      return this;
    }
  }

  final class Factory<T extends HockeyPlayerModel> {
    public final Creator<T> creator;

    public final ColumnAdapter<Calendar> birth_dateAdapter;

    public final ColumnAdapter<HockeyPlayer.Shoots> shootsAdapter;

    public final ColumnAdapter<HockeyPlayer.Position> positionAdapter;

    public Factory(Creator<T> creator, ColumnAdapter<Calendar> birth_dateAdapter, ColumnAdapter<HockeyPlayer.Shoots> shootsAdapter, ColumnAdapter<HockeyPlayer.Position> positionAdapter) {
      this.creator = creator;
      this.birth_dateAdapter = birth_dateAdapter;
      this.shootsAdapter = shootsAdapter;
      this.positionAdapter = positionAdapter;
    }

    public Marshal marshal() {
      return new Marshal(null, birth_dateAdapter, shootsAdapter, positionAdapter);
    }

    public Marshal marshal(HockeyPlayerModel copy) {
      return new Marshal(copy, birth_dateAdapter, shootsAdapter, positionAdapter);
    }

    public <T2 extends TeamModel, R extends Select_allModel<T, T2>> Select_allMapper<T, T2, R> select_allMapper(Select_allCreator<T, T2, R> creator, TeamModel.Factory<T2> teamModelFactory) {
      return new Select_allMapper<T, T2, R>(creator, this, teamModelFactory);
    }

    public <T2 extends TeamModel, R extends For_teamModel<T, T2>> For_teamMapper<T, T2, R> for_teamMapper(For_teamCreator<T, T2, R> creator, TeamModel.Factory<T2> teamModelFactory) {
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

    public <T2 extends TeamModel, R extends Some_joinModel<T, T2>> Some_joinMapper<T, T2, R> some_joinMapper(Some_joinCreator<T, T2, R> creator, TeamModel.Factory<T2> teamModelFactory) {
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
          return birth_dateAdapter.map(cursor, 0);
        }
      };
    }

    public Mapper<T> inner_joinMapper() {
      return new Mapper<T>(this);
    }
  }
}
