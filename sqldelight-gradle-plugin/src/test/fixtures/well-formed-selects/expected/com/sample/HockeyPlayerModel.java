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

  interface Select_allModel {
    HockeyPlayerModel hockey_player();

    TeamModel team();
  }

  interface Select_allCreator<T extends Select_allModel> {
    T create(HockeyPlayerModel hockey_player, TeamModel team);
  }

  final class Select_allMapper<T extends Select_allModel, R1 extends HockeyPlayerModel, R2 extends TeamModel> implements RowMapper<T> {
    private final Select_allCreator<T> creator;

    private final Factory<R1> hockeyPlayerModelFactory;

    private final TeamModel.Factory<R2> teamModelFactory;

    private Select_allMapper(Select_allCreator<T> creator, Factory<R1> hockeyPlayerModelFactory, TeamModel.Factory<R2> teamModelFactory) {
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

  interface For_teamModel {
    HockeyPlayerModel hockey_player();

    TeamModel team();
  }

  interface For_teamCreator<T extends For_teamModel> {
    T create(HockeyPlayerModel hockey_player, TeamModel team);
  }

  final class For_teamMapper<T extends For_teamModel, R1 extends HockeyPlayerModel, R2 extends TeamModel> implements RowMapper<T> {
    private final For_teamCreator<T> creator;

    private final Factory<R1> hockeyPlayerModelFactory;

    private final TeamModel.Factory<R2> teamModelFactory;

    private For_teamMapper(For_teamCreator<T> creator, Factory<R1> hockeyPlayerModelFactory, TeamModel.Factory<R2> teamModelFactory) {
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

    private Subquery_joinMapper(Subquery_joinCreator<T> creator) {
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
    String first_name();

    long count();
  }

  interface Select_expressionCreator<T extends Select_expressionModel> {
    T create(String first_name, long count);
  }

  final class Select_expressionMapper<T extends Select_expressionModel> implements RowMapper<T> {
    private final Select_expressionCreator<T> creator;

    private Select_expressionMapper(Select_expressionCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.getString(0),
          cursor.isNull(1) ? null : cursor.getLong(1)
      );
    }
  }

  interface Expression_subqueryModel {
    HockeyPlayerModel hockey_player();

    long size();
  }

  interface Expression_subqueryCreator<T extends Expression_subqueryModel> {
    T create(HockeyPlayerModel hockey_player, long size);
  }

  final class Expression_subqueryMapper<T extends Expression_subqueryModel, R1 extends HockeyPlayerModel> implements RowMapper<T> {
    private final Expression_subqueryCreator<T> creator;

    private final Factory<R1> hockeyPlayerModelFactory;

    private Expression_subqueryMapper(Expression_subqueryCreator<T> creator, Factory<R1> hockeyPlayerModelFactory) {
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
          cursor.isNull(10) ? null : cursor.getLong(10)
      );
    }
  }

  interface Some_joinModel {
    HockeyPlayerModel hockey_player();

    TeamModel team();
  }

  interface Some_joinCreator<T extends Some_joinModel> {
    T create(HockeyPlayerModel hockey_player, TeamModel team);
  }

  final class Some_joinMapper<T extends Some_joinModel, R1 extends HockeyPlayerModel, R2 extends TeamModel> implements RowMapper<T> {
    private final Some_joinCreator<T> creator;

    private final Factory<R1> hockeyPlayerModelFactory;

    private final TeamModel.Factory<R2> teamModelFactory;

    private Some_joinMapper(Some_joinCreator<T> creator, Factory<R1> hockeyPlayerModelFactory, TeamModel.Factory<R2> teamModelFactory) {
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

    private With_queryMapper(With_queryCreator<T> creator) {
      this.creator = creator;
    }

    @Override
    @NonNull
    public T map(@NonNull Cursor cursor) {
      return creator.create(
          cursor.isNull(0) ? null : cursor.getLong(0),
          cursor.isNull(1) ? null : cursor.getLong(1),
          cursor.isNull(2) ? null : cursor.getLong(2)
      );
    }
  }

  interface Creator<T extends HockeyPlayerModel> {
    T create(long _id, String first_name, String last_name, int number, Long team, int age, float weight, Calendar birth_date, HockeyPlayer.Shoots shoots, HockeyPlayer.Position position);
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

  class Marshal<T extends Marshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Calendar> birth_dateAdapter;

    private final ColumnAdapter<HockeyPlayer.Shoots> shootsAdapter;

    private final ColumnAdapter<HockeyPlayer.Position> positionAdapter;

    public Marshal(ColumnAdapter<Calendar> birth_dateAdapter, ColumnAdapter<HockeyPlayer.Shoots> shootsAdapter, ColumnAdapter<HockeyPlayer.Position> positionAdapter) {
      this.birth_dateAdapter = birth_dateAdapter;
      this.shootsAdapter = shootsAdapter;
      this.positionAdapter = positionAdapter;
    }

    public Marshal(HockeyPlayerModel copy, ColumnAdapter<Calendar> birth_dateAdapter, ColumnAdapter<HockeyPlayer.Shoots> shootsAdapter, ColumnAdapter<HockeyPlayer.Position> positionAdapter) {
      this._id(copy._id());
      this.first_name(copy.first_name());
      this.last_name(copy.last_name());
      this.number(copy.number());
      this.team(copy.team());
      this.age(copy.age());
      this.weight(copy.weight());
      this.birth_dateAdapter = birth_dateAdapter;
      this.birth_date(copy.birth_date());
      this.shootsAdapter = shootsAdapter;
      this.shoots(copy.shoots());
      this.positionAdapter = positionAdapter;
      this.position(copy.position());
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T _id(long _id) {
      contentValues.put(_ID, _id);
      return (T) this;
    }

    public T first_name(String first_name) {
      contentValues.put(FIRST_NAME, first_name);
      return (T) this;
    }

    public T last_name(String last_name) {
      contentValues.put(LAST_NAME, last_name);
      return (T) this;
    }

    public T number(int number) {
      contentValues.put(NUMBER, number);
      return (T) this;
    }

    public T team(Long team) {
      contentValues.put(TEAM, team);
      return (T) this;
    }

    public T age(int age) {
      contentValues.put(AGE, age);
      return (T) this;
    }

    public T weight(float weight) {
      contentValues.put(WEIGHT, weight);
      return (T) this;
    }

    public T birth_date(Calendar birth_date) {
      birth_dateAdapter.marshal(contentValues, BIRTH_DATE, birth_date);
      return (T) this;
    }

    public T shoots(HockeyPlayer.Shoots shoots) {
      shootsAdapter.marshal(contentValues, SHOOTS, shoots);
      return (T) this;
    }

    public T position(HockeyPlayer.Position position) {
      positionAdapter.marshal(contentValues, POSITION, position);
      return (T) this;
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

    public <T extends Select_allModel, R2 extends TeamModel> Select_allMapper select_allMapper(Select_allCreator<T> creator, TeamModel.Factory<R2> teamModelFactory) {
      return new Select_allMapper<>(creator, this, teamModelFactory);
    }

    public <T extends For_teamModel, R2 extends TeamModel> For_teamMapper for_teamMapper(For_teamCreator<T> creator, TeamModel.Factory<R2> teamModelFactory) {
      return new For_teamMapper<>(creator, this, teamModelFactory);
    }

    public Mapper join_friendsMapper() {
      return new Mapper<>(this);
    }

    public RowMapper<Long> subqueryMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.getLong(0);
        }
      };
    }

    public <T extends Subquery_joinModel> Subquery_joinMapper subquery_joinMapper(Subquery_joinCreator<T> creator) {
      return new Subquery_joinMapper<>(creator);
    }

    public <T extends Select_expressionModel> Select_expressionMapper select_expressionMapper(Select_expressionCreator<T> creator) {
      return new Select_expressionMapper<>(creator);
    }

    public <T extends Expression_subqueryModel> Expression_subqueryMapper expression_subqueryMapper(Expression_subqueryCreator<T> creator) {
      return new Expression_subqueryMapper<>(creator, this);
    }

    public Mapper order_by_ageMapper() {
      return new Mapper<>(this);
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
          return cursor.isNull(0) ? null : cursor.getLong(0);
        }
      };
    }

    public RowMapper<Long> kidsMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.isNull(0) ? null : cursor.getLong(0);
        }
      };
    }

    public <T extends Some_joinModel, R2 extends TeamModel> Some_joinMapper some_joinMapper(Some_joinCreator<T> creator, TeamModel.Factory<R2> teamModelFactory) {
      return new Some_joinMapper<>(creator, this, teamModelFactory);
    }

    public RowMapper<Long> multiple_values_for_queryMapper() {
      return new RowMapper<Long>() {
        @Override
        public Long map(Cursor cursor) {
          return cursor.isNull(0) ? null : cursor.getLong(0);
        }
      };
    }

    public <T extends With_queryModel> With_queryMapper with_queryMapper(With_queryCreator<T> creator) {
      return new With_queryMapper<>(creator);
    }

    public Mapper is_not_exprMapper() {
      return new Mapper<>(this);
    }

    public <T extends HockeyPlayerModel> RowMapper<Calendar> order_by_exprMapper(final Factory<T> hockeyPlayerModelFactory) {
      return new RowMapper<Calendar>() {
        @Override
        public Calendar map(Cursor cursor) {
          return hockeyPlayerModelFactory.birth_dateAdapter.map(cursor, 0);
        }
      };
    }

    public Mapper inner_joinMapper() {
      return new Mapper<>(this);
    }
  }
}
