SQLDelight
==========

SQLDelight generates Java models from your SQL `CREATE TABLE` statements. These models give you a
typesafe API to read & write the rows of your tables. It helps you to keep your SQL statements
together, organized, and easy to access from Java.

Example
-------

To use SQLDelight, put your SQL statements in a `.sq` file, like
`src/main/sqldelight/com/example/HockeyPlayer.sq`. Typically the first statement creates a table.

```sql
CREATE TABLE hockey_player (
  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  player_number INTEGER NOT NULL,
  name TEXT NOT NULL
);

-- Further SQL statements are proceeded by an identifier.
select_all:
SELECT *
FROM hockey_player;

insert_row:
INSERT INTO hockey_player(player_number, name)
VALUES (?, ?);
```

From this SQLDelight will generate a `HockeyPlayerModel` Java interface with nested classes for reading and writing the database.

```java
package com.example;

public interface HockeyPlayerModel {
  String TABLE_NAME = "hockey_player";

  String _ID = "_id";

  String PLAYER_NUMBER = "player_number";

  String NAME = "name";

  String CREATE_TABLE = ""
      + "CREATE TABLE hockey_player (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  player_number INTEGER NOT NULL,\n"
      + "  name TEXT NOT NULL\n"
      + ")";

  String SELECT_ALL = ""
      + "SELECT *\n"
      + "FROM hockey_player;";

  long _id();

  long player_number();

  @NonNull
  String name();

  interface Creator<T extends HockeyPlayerModel> {
    T create(long _id, long player_number, String name);
  }

  final class Factory<T extends HockeyPlayerModel> {
    public Factory(Creator<T> creator);

    public RowMapper<T> select_allMapper();
  }

  final class Insert_row {
    public static final String table = "hockey_player";

    public final SQLiteStatement program;

    public Insert_row(SQLiteDatabase db);

    public void bind(long player_number, String name);
  }
}
```

AutoValue
---------

Using Google's [AutoValue](https://github.com/google/auto/tree/master/value) you can minimally
make implementations of the model:

```java
@AutoValue
public abstract class HockeyPlayer implements HockeyPlayerModel {
  public static final Factory<HockeyPlayer> FACTORY = new Factory<>(new Creator<HockeyPlayer>() {
    @Override public HockeyPlayer create(long _id, long player_number, String name) {
      return new AutoValue_HockeyPlayer(_id, player_number, name);
    }
  });

  public static final RowMapper<HockeyPlayer> SELECT_ALL_MAPPER = FACTORY.select_allMapper();
}
```

If you are also using [Retrolambda](https://github.com/orfjackal/retrolambda/) the anonymous class
can be replaced by a method reference:

```java
@AutoValue
public abstract class HockeyPlayer implements HockeyPlayerModel {
  public static final Factory<HockeyPlayer> FACTORY = new Factory<>(AutoValue_HockeyPlayer::new);

  public static final RowMapper<HockeyPlayer> SELECT_ALL_MAPPER = FACTORY.select_allMapper();
}
```


Consuming Code
--------------

Queries will have string constants generated for them as well as a function on the factory for
mapping your `Cursor` set to java objects.

```java
public List<HockeyPlayer> allPlayers(SQLiteDatabase db) {
  List<HockeyPlayer> result = new ArrayList<>();
  try (Cursor cursor = db.rawQuery(HockeyPlayer.SELECT_ALL, new String[0]})) {
    while (cursor.moveToNext()) {
      result.add(HockeyPlayer.SELECT_ALL_MAPPER.map(cursor));
    }
  }
  return result;
}
```


SQL Statement Arguments/Bind Args
---------------------------------

.sq files use the exact same syntax as SQLite, including [SQLite Bind Args](https://www.sqlite.org/c3ref/bind_blob.html).
If a statement contains bind args, a type safe method will be generated on the `Factory` which
returns a `SqlDelightStatement` containing fields for the query string, query args, and tables being
queried.

```sql
select_by_number:
SELECT *
FROM hockey_player
WHERE player_number = ?;
```

```java
SqlDelightStatement query = HockeyPlayer.FACTORY.select_by_number(10);
Cursor coreyPerry = db.rawQuery(query.statement, query.args);
```

Sets of values can also be passed as an argument.

```sql
select_by_names:
SELECT *
FROM hockey_player
WHERE name IN ?;
```

```java
SqlDelightStatement query = HockeyPlayer.FACTORY.select_by_names(new String[] { "Alec", "Jake", "Matt" });
Cursor players = db.rawQuery(query.statement, query.args);
```

Named parameters or indexed parameters can be used.

```sql
first_or_last_name:
SELECT *
FROM hockey_player
WHERE name LIKE '%' || ?1
OR name LIKE ?1 || '%';
```

```java
SqlDelightStatement query = HockeyPlayer.FACTORY.first_or_last_name("Perry");
Cursor players = db.rawQuery(query.statement, query.args);
```

Compiled Statements
-------------------

Inserts, Updates, and Deletes that are executed multiple times during your application's runtime
should be compiled once beforehand and have arguments bound to them for each independent call.

SQLDelight generates a typesafe class for any statements which should be compiled.

```sql
update_number:
SET player_number = ?
WHERE name = ?;
```

```java
interface PlayerModel {
  class Update_number {
    public static final table = "hockey_player";
    public final SQLiteStatement program;

    public Update_number(SQLiteDatabase db);

    public void bind(int player_number, String name);
  }
}
```

Compiling the statement requires passing a writable copy of your database which can be
retrieved from your `OpenHelper`

```java
class PlayerManager {
  private final Player.Update_number updateNumber;

  public PlayerManager(SQLiteOpenHelper helper) {
    SQLiteDatabase db = helper.getWritableDatabase();
    updateNumber = new Player.Update_number(db);
  }
}
```

Executing the statement can be done using the `SQLiteStatement` field of the generated class.

```java
updateNumber.bind(9, "Bobby Ryan");
int updated = updateNumber.program.executeUpdateDelete();
```

Projections
-----------

Each select statement will have an interface and mapper generated for it, as well as a method
on the factory to create a new instance of the mapper.

```sql
player_names:
SELECT name
FROM hockey_player;
```

Selects that only return a single value do not require a custom type to be mapped. The generated
file will only contain a new method on the factory.

```java
interface HockeyPlayerModel {

  ...

  final class Factory<T extends HockeyPlayerModel> {

    ...

    public RowMapper<String> player_namesMapper();
  }
}
```

Referencing the mapper is done the same as when you select an entire table.

```java
@AutoValue
public abstract class HockeyPlayer implements HockeyPlayerModel {
  public static final Factory<HockeyPlayer> FACTORY = new Factory<>(AutoValue_HockeyPlayer::new);

  public static final RowMapper<String> PLAYER_NAMES_MAPPER = FACTORY.player_namesMapper();

  public List<String> playerNames(SQLiteDatabase db) {
    List<String> names = new ArrayList<>();
    try (Cursor cursor = db.rawQuery(PLAYER_NAMES)) {
      while (cursor.moveToNext()) {
        names.add(PLAYER_NAMES_MAPPER.map(cursor));
      }
    }
    return names;
  }
}
```

Selects that return multiple result columns generate a custom model, mapper, and factory method
for the query.

```sql
names_for_number:
SELECT player_number, group_concat(name)
FROM hockey_player
GROUP BY player_number;
```

generates:
```java
interface HockeyPlayerModel {

  ...

  interface Names_for_numberModel {
    long player_number();

    String group_concat_name();
  }

  interface Names_for_numberCreator<T extends Names_for_numberModel> {
    T create(long player_number, String group_concat_name);
  }

  final class Names_for_numberMapper<T extends Names_for_numberModel> implements RowMapper<T> {
    ...
  }

  final class Factory<T extends HockeyPlayerModel> {

    ...

    public <R extends Names_for_numberModel> Names_for_numberMapper<R> names_for_numberMapper(
      Names_for_numberCreator<R> creator
    ) {
      return new Names_for_numberMapper<R>(creator);
    }
  }
}
```

Referencing the mapper requires an implementation of the result set type.

```java
@AutoValue
public abstract class HockeyPlayer implements HockeyPlayerModel {
  public static final Factory<HockeyPlayer> FACTORY = new Factory<>(AutoValue_HockeyPlayer::new);

  public static final RowMapper<NamesForNumber> NAMES_FOR_NUMBER_MAPPER =
      FACTORY.names_for_numberMapper(AutoValue_HockeyPlayer_NamesForNumber::new);

  public Map<Integer, String[]> namesForNumber(SQLiteDatabase db) {
    Map<Integer, String[]> namesForNumberMap = new LinkedHashMap<>();
    try (Cursor cursor = db.rawQuery(NAMES_FOR_NUMBER)) {
      while (cursor.moveToNext()) {
        NamesForNumber namesForNumber = NAMES_FOR_NUMBER_MAPPER.map(cursor);
        namesForNumberMap.put(namesForNumber.player_number(), namesForNumber.names());
      }
    }
    return namesForNumberMap;
  }

  @AutoValue
  public abstract class NamesForNumber implements Names_for_numberModel<NamesForNumber> {
    public String[] names() {
      return group_concat_names().split(",");
    }
  }
}
```

Types
-----

SQLDelight column definition are identical to regular SQLite column definitions but support an extra column constraint
which specifies the java type of the column in the generated interface. SQLDelight natively supports the same types that
`Cursor` and `ContentValues` expect:

```sql
CREATE TABLE some_types (
  some_long INTEGER,           -- Stored as INTEGER in db, retrieved as Long
  some_double REAL,            -- Stored as REAL in db, retrieved as Double
  some_string TEXT,            -- Stored as TEXT in db, retrieved as String
  some_blob BLOB,              -- Stored as BLOB in db, retrieved as byte[]
  some_int INTEGER AS Integer, -- Stored as INTEGER in db, retrieved as Integer
  some_short INTEGER AS Short, -- Stored as INTEGER in db, retrieved as Short
  some_float REAL AS Float     -- Stored as REAL in db, retrieved as Float
);
```

Booleans
--------

SQLDelight supports boolean columns and stores them in the db as ints. Since they are implemented
as ints they can be given int column constraints:

```sql
CREATE TABLE hockey_player (
  injured INTEGER AS Boolean DEFAULT 0
)
```

Custom Classes
--------------

If you'd like to retrieve columns as custom types you can specify the java type as a sqlite string:

```sql
import java.util.Calendar;

CREATE TABLE hockey_player (
  birth_date INTEGER AS Calendar NOT NULL
)
```

However, creating a Marshal or Factory will require you to provide a `ColumnAdapter` which knows how
to map between the database type and your custom type:

```java
public class HockeyPlayer implements HockeyPlayerModel {
  private static final ColumnAdapter<Calendar, Long> CALENDAR_ADAPTER = new ColumnAdapter<>() {
    @Override public Calendar decode(Long databaseValue) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(databaseValue);
      return calendar;
    }

    @Override public Long encode(Calendar value) {
      return value.getTimeInMillis();
    }
  };

  public static final Factory<HockeyPlayer> FACTORY = new Factory<>(new Creator<>() { },
      CALENDAR_ADAPTER);
}
```

Enums
-----

As a convenience the SQLDelight runtime includes a `ColumnAdapter` for storing an enum as TEXT.

```sql
import com.example.hockey.HockeyPlayer;

CREATE TABLE hockey_player (
  position TEXT AS HockeyPlayer.Position
)
```

```java
public class HockeyPlayer implements HockeyPlayerModel {
  public enum Position {
    CENTER, LEFT_WING, RIGHT_WING, DEFENSE, GOALIE
  }

  private static final ColumnAdapter<Position, String> POSITION_ADAPTER = EnumColumnAdapter.create(Position.class);

  public static final Factory<HockeyPlayer> FACTORY = new Factory<>(new Creator<>() { },
      POSITION_ADAPTER);
}
```

Views
-----

Views receive the same treatment in generated code as tables with their own model interface.

```sql
names_view:
CREATE VIEW names AS
SELECT substr(name, 0, instr(name, ' ')) AS first_name,
       substr(name, instr(name, ' ') + 1) AS last_name,
       _id
FROM hockey_player;

select_names:
SELECT *
FROM names;
```

generates:
```java
interface HockeyPlayerModel {

  ...

  interface NamesModel {
    String first_name();

    String last_name();

    long _id();
  }

  interface NamesCreator<T extends NamesModel> {
    T create(String first_name, String last_name, long _id);
  }

  final class NamesMapper<T extends NamesModel> implements RowMapper<T> {
    ...
  }

  final class Factory<T extends HockeyPlayerModel> {

    ...

    public <R extends NamesModel> NamesMapper<R> select_namesMapper(NamesCreator<R> creator) {
      return new NamesMapper<R>(creator);
    }
  }
}
```

Referencing the mapper requires an implementation of the view model.

```java
@AutoValue
public abstract class HockeyPlayer implements HockeyPlayerModel {
  public static final Factory<HockeyPlayer> FACTORY = new Factory<>(AutoValue_HockeyPlayer::new);

  public static final RowMapper<NamesForNumber> SELECT_NAMES_MAPPER =
      FACTORY.select_namesMapper(AutoValue_HockeyPlayer_Names::new);

  public List<Names> names(SQLiteDatabase) {
    List<Names> names = new ArrayList<>();
    try (Cursor cursor = db.rawQuery(SELECT_NAMES)) {
      while (cursor.moveToNext()) {
        names.add(SELECT_NAMES_MAPPER.map(cursor));
      }
    }
    return names;
  }

  @AutoValue
  public abstract class Names implements NamesModel { }
}
```

Join Projections
----------------

Selecting from multiple tables via joins also requires an implementation class.

```sql
select_all_info:
SELECT *
FROM hockey_player
JOIN names USING (_id);
```

generates:
```java
interface HockeyPlayerModel {

  ...

  interface Select_all_infoModel<T1 extends HockeyPlayerModel, V4 extends NamesModel> {
    T1 hockey_player();

    V4 names();
  }

  interface Select_all_infoCreator<T1 extends HockeyPlayerModel, V4 extends NamesModel, T extends Select_all_infoModel<T1, V4>> {
    T create(T1 hockey_player, V4 names);
  }

  final class Select_all_infoMapper<T1 extends HockeyPlayerModel, V4 extends NamesModel, T extends Select_all_infoModel<T1, V4>> implements RowMapper<T> {
    ...
  }

  final class Factory<T extends HockeyPlayerModel> {
    public <V4 extends NamesModel, R extends Select_all_infoModel<T, V4>> Select_all_infoMapper<T, V4, R> select_all_infoMapper(Select_all_infoCreator<T, V4, R> creator, NamesCreator<V4> namesCreator) {
      return new Select_all_infoMapper<T, V4, R>(creator, this, namesCreator);
    }
  }
}
```

implementation:
```java
@AutoValue
public abstract class HockeyPlayer implements HockeyPlayerModel {
  public static final Factory<HockeyPlayer> FACTORY = new Factory<>(AutoValue_HockeyPlayer::new);

  public static final RowMapper<AllInfo> SELECT_ALL_INFO_MAPPER =
      FACTORY.select_all_infoMapper(AutoValue_HockeyPlayer_AllInfo::new,
        AutoValue_HockeyPlayer_Names::new);

  public List<AllInfo> allInfo(SQLiteDatabase db) {
    List<AllInfo> allInfoList = new ArrayList<>();
    try (Cursor cursor = db.rawQuery(SELECT_ALL_INFO)) {
      while (cursor.moveToNext()) {
        allInfoList.add(SELECT_ALL_INFO_MAPPER.map(cursor));
      }
    }
    return allInfoList;
  }

  @AutoValue
  public abstract class Names implements NamesModel { }

  @AutoValue
  public abstract class AllInfo implements Select_all_infoModel<HockeyPlayer, Names> { }
}
```


IntelliJ Plugin
---------------

The Intellij plugin provides language-level features for `.sq` files, including:
 - Syntax highlighting
 - Refactoring/Find usages
 - Code autocompletion
 - Generate `Model` files after edits
 - Right click to copy as valid SQLite
 - Compiler errors in IDE click through to file

Download
--------

For the Gradle plugin:

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:0.5.1'
  }
}

apply plugin: 'com.squareup.sqldelight'
```

The Intellij plugin can be installed from Android Studio by navigating<br>
Android Studio -> Preferences -> Plugins -> Browse repositories -> Search for SQLDelight

Snapshots of the development version (including the IDE plugin zip) are available in
[Sonatype's `snapshots` repository](https://oss.sonatype.org/content/repositories/snapshots/).


License
=======

    Copyright 2016 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
