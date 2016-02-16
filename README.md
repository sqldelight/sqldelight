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
  _id LONG NOT NULL PRIMARY KEY AUTOINCREMENT,
  number INT NOT NULL,
  name STRING NOT NULL
);

-- Further SQL statements are proceeded by an identifier. This will be used to name the constant
-- in the generated Java code.
select_by_name:
SELECT *
FROM hockey_player
WHERE name = ?;
```

From this SQLDelight will generate a `HockeyPlayerModel` Java class with nested classes for reading
(the Mapper) and writing (the Marshal) the table.

```java
package com.example;

import android.content.ContentValues;
import android.database.Cursor;
import java.lang.String;

public interface HockeyPlayerModel {
  String TABLE_NAME = "hockey_player";

  String _ID = "_id";

  String NUMBER = "number";

  String NAME = "name";

  String CREATE_TABLE = ""
      + "CREATE TABLE hockey_player (\n"
      + "  _id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n"
      + "  number INTEGER NOT NULL,\n"
      + "  name TEXT NOT NULL\n"
      + ")";

  String SELECT_BY_NAME = ""
      + "SELECT *\n"
      + "FROM hockey_player\n"
      + "WHERE name = ?";

  long _id();

  int number();

  String name();

  final class Mapper<T extends HockeyPlayerModel> {
    private final Creator<T> creator;

    protected Mapper(Creator<T> creator) {
      this.creator = creator;
    }

    public T map(Cursor cursor) {
      return creator.create(
          cursor.getLong(cursor.getColumnIndex(_ID)),
          cursor.getInt(cursor.getColumnIndex(NUMBER)),
          cursor.getString(cursor.getColumnIndex(NAME))
      );
    }

    public interface Creator<R extends HockeyPlayerModel> {
      R create(long _id, int number, String name);
    }
  }

  class HockeyPlayerMarshal<T extends HockeyPlayerMarshal<T>> {
    protected ContentValues contentValues = new ContentValues();

    public HockeyPlayerMarshal() {
    }

    public final ContentValues asContentValues() {
      return contentValues;
    }

    public T _id(long _id) {
      contentValues.put(_ID, _id);
      return (T) this;
    }

    public T number(int number) {
      contentValues.put(NUMBER, number);
      return (T) this;
    }

    public T name(String name) {
      contentValues.put(NAME, name);
      return (T) this;
    }
  }
}
```

AutoValue
---------

Using Google's [AutoValue](https://github.com/google/auto/tree/master/value) you can minimally
make implementations of the model/marshal/mapper:

```java
@AutoValue
public abstract class HockeyPlayer implements HockeyPlayerModel {
  public static final Mapper<HockeyPlayer> MAPPER = new Mapper<>(new Mapper.Creator() {
    @Override public HockeyPlayer create(long _id, int number, String name) {
      return new AutoValue_HockeyPlayer(_id, age, number, gender);
    }
  }

  public static final class Marshal extends HockeyPlayerMarshal<Marshal>() { }
}
```

If you are also using [Retrolambda](https://github.com/orfjackal/retrolambda/) the anonymous class
can be replaced by a method reference:

```java
@AutoValue
public abstract class HockeyPlayer implements HockeyPlayerModel {
  public static final Mapper<HockeyPlayer> MAPPER = new Mapper<>(AutoValue_HockeyPlayer::new);

  public static final class Marshal extends HockeyPlayerMarshal<Marshal>() { }
}
```

Consuming Code
--------------

Use the generated constants to reference table names and SQL statements.

```java
public void insert(SqliteDatabase db, long _id, int number, String name) {
  db.insert(HockeyPlayer.TABLE_NAME, null, new HockeyPlayer.Marshal()
    ._id(_id)
    .number(number)
    .name(name)
    .asContentValues());
}

public List<HockeyPlayer> alecs(SqliteDatabase db) {
  List<HockeyPlayer> result = new ArrayList<>();
  try (Cursor cursor = db.rawQuery(HockeyPlayer.SELECT_BY_NAME, new String[] { "Alec" })) {
    while (cursor.moveToNext()) {
      result.add(HockeyPlayer.MAPPER.map(cursor));
    }
  }
  return result;
}
```

Types
-----

SQLDelight supports the same types that `Cursor` and `ContentValues` expect:

```sql
CREATE TABLE some_types {
  some_int INT,       -- Stored as INTEGER
  some_short SHORT,   -- Stored as INTEGER
  some_long LONG,     -- Stored as INTEGER
  some_float FLOAT,   -- Stored as REAL
  some_double DOUBLE, -- Stored as REAL
  some_string STRING, -- Stored as TEXT
  some_blob BLOB      -- Stored as BLOB
}
```

Enums
-----

SQLDelight supports Enums, but requires the fully qualified Enum type in your SQL:

```sql
CREATE TABLE hockey_player (
  position ENUM('com.example.hockey.Player.Position')
)
```

Booleans
--------

SQLDelight supports boolean columns and stores them in the db as ints. Since they are implemented
as ints they can be given int column constraints:

```sql
CREATE TABLE hockey_player (
  injured BOOLEAN DEFAULT 0
)
```

Custom Classes
--------------

If you'd like to store columns as custom types it looks similar to the definition for Enums:

```sql
CREATE TABLE hockey_player (
  birth_date CLASS('java.util.Calendar') NOT NULL
)
```

However, creating a Marshal or Mapper will require you to provide a `ColumnAdapter` which knows how
to map a `Cursor` to your type and marshal your type into a `ContentValues`:

```java
public class HockeyPlayer implements HockeyPlayerModel {
  private static final ColumnAdapter<Calendar> CALENDAR_ADAPTER = new ColumnAdapter<>() {
    @Override public Calendar map(Cursor cursor, int columnIndex) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(cursor.getLong(columnIndex));
      return calendar;
    }

    @Override public void marshal(ContentValues contentValues, String key, Calendar value) {
      contentValues.put(key, value.getTimeInMillis());
    }
  }

  public static final Mapper<HockeyPlayer> MAPPER = new Mapper<>(new Mapper.Creator<>() { },
    CALENDAR_ADAPTER);

  public static final class Marshal extends HockeyPlayerMarshal<Marshal> {
    public Marshal() {
      super(CALENDAR_ADAPTER);
    }
  }
}
```

SQL Statement Arguments
-----------------------

SQL queries can also contain arguments the same way `SqliteDatabase` does:

```sql
select_by_position:
SELECT *
FROM hockey_player
WHERE position = ?;
```

```java
Cursor centers = db.rawQuery(HockeyPlayer.SELECT_BY_POSITION, new String[] { Center.name() });
```

Intellij Plugin
---------------

The Intellij plugin provides language-level features for `.sq` files, including:
 - Syntax highlighting
 - Refactoring/Find usages
 - Code autocompletion
 - Generate `Model` files after edits

Download
--------

For the Gradle plugin:

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:0.1.2'
  }
}

apply plugin: 'com.squareup.sqldelight'
```

The Intellij plugin can be installed from Android Studio by navigating<br>
Android Studio -> Preferences -> Plugins -> Install Jetbrains Plugin -> Search for SQLDelight

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
