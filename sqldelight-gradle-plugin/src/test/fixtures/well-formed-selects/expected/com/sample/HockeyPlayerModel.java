package com.sample;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;
import com.squareup.sqldelight.ColumnAdapter;
import java.lang.Long;
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
      + "  birth_date BLOB NOT NULL,\n"
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
      + "SELECT ?\n"
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

  String SCOPED_EXPRESSIONS = ""
      + "SELECT *\n"
      + "FROM hockey_player\n"
      + "JOIN (\n"
      + "  SELECT *\n"
      + "  FROM team\n"
      + "  WHERE age = team.captain\n"
      + ")";

  String SUBQUERIES_FOR_DAYS = ""
      + "SELECT *\n"
      + "FROM hockey_player AS one\n"
      + "JOIN (\n"
      + "  SELECT *\n"
      + "  FROM (\n"
      + "    SELECT *\n"
      + "    FROM (\n"
      + "      SELECT *\n"
      + "      FROM hockey_player\n"
      + "      WHERE hockey_player._id = one._id\n"
      + "    )\n"
      + "  )\n"
      + ")\n"
      + "GROUP BY hockey_player.team";

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

  long _id();

  String first_name();

  String last_name();

  int number();

  @Nullable
  Long team();

  int age();

  float weight();

  Calendar birth_date();

  HockeyPlayer.Shoots shoots();

  HockeyPlayer.Position position();

  final class Mapper<T extends HockeyPlayerModel> {
    private final Creator<T> creator;

    private final ColumnAdapter<Calendar> birth_dateAdapter;

    protected Mapper(Creator<T> creator, ColumnAdapter<Calendar> birth_dateAdapter) {
      this.creator = creator;
      this.birth_dateAdapter = birth_dateAdapter;
    }

    public T map(Cursor cursor) {
      return creator.create(
          cursor.getLong(cursor.getColumnIndex(_ID)),
          cursor.getString(cursor.getColumnIndex(FIRST_NAME)),
          cursor.getString(cursor.getColumnIndex(LAST_NAME)),
          cursor.getInt(cursor.getColumnIndex(NUMBER)),
          cursor.isNull(cursor.getColumnIndex(TEAM)) ? null : cursor.getLong(cursor.getColumnIndex(TEAM)),
          cursor.getInt(cursor.getColumnIndex(AGE)),
          cursor.getFloat(cursor.getColumnIndex(WEIGHT)),
          birth_dateAdapter.map(cursor, cursor.getColumnIndex(BIRTH_DATE)),
          HockeyPlayer.Shoots.valueOf(cursor.getString(cursor.getColumnIndex(SHOOTS))),
          HockeyPlayer.Position.valueOf(cursor.getString(cursor.getColumnIndex(POSITION)))
      );
    }

    public interface Creator<R extends HockeyPlayerModel> {
      R create(long _id, String first_name, String last_name, int number, Long team, int age, float weight, Calendar birth_date, HockeyPlayer.Shoots shoots, HockeyPlayer.Position position);
    }
  }

  class HockeyPlayerMarshal<T extends HockeyPlayerMarshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    private final ColumnAdapter<Calendar> birth_dateAdapter;

    public HockeyPlayerMarshal(ColumnAdapter<Calendar> birth_dateAdapter) {
      this.birth_dateAdapter = birth_dateAdapter;
    }

    public HockeyPlayerMarshal(HockeyPlayerModel copy, ColumnAdapter<Calendar> birth_dateAdapter) {
      this._id(copy._id());
      this.first_name(copy.first_name());
      this.last_name(copy.last_name());
      this.number(copy.number());
      this.team(copy.team());
      this.age(copy.age());
      this.weight(copy.weight());
      this.birth_dateAdapter = birth_dateAdapter;
      this.birth_date(copy.birth_date());
      this.shoots(copy.shoots());
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
      contentValues.put(SHOOTS, shoots.name());
      return (T) this;
    }

    public T position(HockeyPlayer.Position position) {
      contentValues.put(POSITION, position.name());
      return (T) this;
    }
  }
}
