SQLDelight
==========

SQLDelight generates typesafe APIs from your SQL statements. It compile-time verifies your schema, statements, and migrations and provides IDE features like autocomplete and refactoring which make writing and maintaining SQL simple. SQLDelight currently supports the SQLite dialect and there are supported SQLite drivers on Android, JVM and iOS.

Example
-------

To use SQLDelight, apply the [gradle plugin](https://github.com/square/sqldelight#gradle) and put your SQL statements in a `.sq` file in `src/main/sqldelight`.  Typically the first statement in the SQL file creates a table.

```sql
-- src/main/sqldelight/com/example/sqldelight/hockey/data/Player.sq

CREATE TABLE hockeyPlayer (
  player_number INTEGER NOT NULL,
  full_name TEXT NOT NULL
);

CREATE INDEX hockeyPlayer_full_name ON hockeyPlayer(full_name);

INSERT INTO hockeyPlayer (player_number, full_name)
VALUES (15, 'Ryan Getzlaf');
```

From this SQLDelight will generate a `Database` Kotlin class with an associated `Schema` object that can be used to create your database and run your statements on it. Doing this also requires a driver, which SQLDelight provides implementations of:

#### Android
```groovy
dependencies {
  implementation "com.squareup.sqldelight:android-driver:1.1.1"
}
```
```kotlin
val driver: SqlDriver = AndroidSqliteDriver(Database.Schema, context, "test.db")
```

#### iOS (Using Kotlin/Native)
```groovy
dependencies {
  implementation "com.squareup.sqldelight:ios-driver:1.1.1"
}

// You'll also need to have SQLite linked via -lsqlite3 during compilation.
```
```kotlin
val driver: SqlDriver = NativeSqliteDriver(Database.Schema, "test.db")
```

#### JVM
```groovy
dependencies {
  implementation "com.squareup.sqldelight:sqlite-driver:1.1.1"
}
```
```kotlin
val driver: SqlDriver = JdbcSqliteDriver()
Database.Schema.create(driver)
```

SQL statements inside a `.sq` file can be labeled to have a typesafe function generated for them available at runtime.

```sql
selectAll:
SELECT *
FROM hockeyPlayer;

insert:
INSERT INTO hockeyPlayer(player_number, full_name)
VALUES (?, ?);
```

Files with labeled statements in them will have a queries file generated from them that matches the `.sq` file name - putting the above sql into `Player.sq` generates `PlayerQueries.kt`. To get a reference to `PlayerQueries` you need to wrap the driver we made above:

```kotlin
// In reality the database and driver above should be created a single time
// and passed around using your favourite dependency injection/service locator/singleton pattern.
val database = Database(driver)

val playerQueries: PlayerQueries = database.playerQueries

println(playerQueries.selectAll().executeAsList())
// Prints [HockeyPlayer.Impl(15, "Ryan Getzlaf")]

playerQueries.insert(player_number = 10, full_name = "Corey Perry")
println(playerQueries.selectAll().executeAsList())
// Prints [HockeyPlayer.Impl(15, "Ryan Getzlaf"), HockeyPlayer.Impl(10, "Corey Perry")]
```

Custom Projections
------------------

By default queries will return a data class implementation of the table schema. To override this behavior pass a custom mapper to the query function. Your custom mapper will receive typesafe parameters which are the projection of your select statement.

```kotlin
val selectAllNames = playerQueries.selectAll(mapper = { player_number, full_name -> full_name.toUppercase() })
println(selectAllNames.executeAsList())
// Prints ["RYAN GETZLAF", "COREY PERRY"]
```

In general you should be leveraging SQL to do custom projections whenever possible.

```sql
selectNames:
SELECT upper(full_name)
FROM hockeyPlayer;
```

```kotlin
val selectAllNames = playerQueries.selectNames()
println(selectAllNames.executeAsList())
// Prints ["RYAN GETZLAF", "COREY PERRY"]
```

But the custom mapping is there when this isn't possible, for example getting a `Parcelable` type for query results on Android.

Query Arguments
---------------

`.sq` files use the exact same syntax as SQLite, including [SQLite Bind Args](https://www.sqlite.org/c3ref/bind_blob.html). If a statement contains bind args, the associated method will require corresponding arguments.

```sql
selectByNumber:
SELECT *
FROM hockeyPlayer
WHERE player_number = ?;
```

```kotlin
val selectNumber10 = playerQueries.selectByNumber(player_number = 10)
println(selectNumber10.executeAsOne())
// Prints "Corey Perry"
```

Sets of values can also be passed as an argument.

```sql
selectByNames:
SELECT *
FROM hockeyPlayer
WHERE full_name IN ?;
```

```kotlin
playerQueries.selectByNames(listOf("Alec Strong", "Jake Wharton", "Matt Precious"))
```

Named parameters or indexed parameters can be used.

```sql
firstOrLastName:
SELECT *
FROM hockeyPlayer
WHERE full_name LIKE ('% ' || :name)
OR full_name LIKE (:name || ' %');
```

```java
playerQueries.firstOrLastName(name = "Ryan")
```

Types
-----

SQLDelight column definitions are identical to regular SQLite column definitions but support an extra column constraint
which specifies the Kotlin type of the column in the generated interface. SQLDelight natively supports Long, Double, String, ByteArray, Int, Short, Float, and Booleans.

```sql
CREATE TABLE some_types (
  some_long INTEGER,           -- Stored as INTEGER in db, retrieved as Long
  some_double REAL,            -- Stored as REAL in db, retrieved as Double
  some_string TEXT,            -- Stored as TEXT in db, retrieved as String
  some_blob BLOB,              -- Stored as BLOB in db, retrieved as ByteArray
  some_int INTEGER AS Int,     -- Stored as INTEGER in db, retrieved as Int
  some_short INTEGER AS Short, -- Stored as INTEGER in db, retrieved as Short
  some_float REAL AS Float     -- Stored as REAL in db, retrieved as Float
);
```

Boolean columns are stored in the db as `INTEGER`, and so they can be given `INTEGER` column constraints. Use `DEFAULT 0` to default to false, for example.

```sql
CREATE TABLE hockey_player (
  injured INTEGER AS Boolean DEFAULT 0
)
```

Custom Column Types
-------------------

If you'd like to retrieve columns as custom types you can specify a Kotlin type:

```sql
import kotlin.collections.List;

CREATE TABLE hockeyPlayer (
  cup_wins TEXT AS List<String> NOT NULL
);
```

However, creating the `Database` will require you to provide a `ColumnAdapter` which knows how
to map between the database type and your custom type:

```kotlin
val listOfStringsAdapter = object : ColumnAdapter<List<String>, String> {
  override fun decode(databaseValue: String) = databaseValue.split(",")
  override fun encode(value: List<String>) = value.joinToString(separator = ",")
}

val queryWrapper: Database = Database(
  driver = driver,
  hockeyPlayerAdapter = hockeyPlayer.Adapter(
    cup_winsAdapter = listOfStringsAdapter
  )
)
```

Enums
-----

As a convenience the SQLDelight runtime includes a `ColumnAdapter` for storing an enum as TEXT.

```sql
import com.example.hockey.HockeyPlayer;

CREATE TABLE hockeyPlayer (
  position TEXT AS HockeyPlayer.Position
)
```

```kotlin
val queryWrapper: Database = Database(
  driver = driver,
  hockeyPlayerAdapter = HockeyPlayer.Adapter(
    positionAdapter = EnumColumnAdapter()
  )
)
```

Migrations
----------

The `.sq` file always describes how to create the latest schema in an empty database. If your database is currently on an earlier version, migration files bring those databases up-to-date. 

The first version of the schema is 1. Migration files are named `<version to upgrade from>.sqm`. To migrate to version 2, put migration statements in `1.sqm`:

```sql
ALTER TABLE hockeyPlayer ADD COLUMN draft_year INTEGER;
ALTER TABLE hockeyPlayer ADD COLUMN draft_order INTEGER;
```

Migration files go in the `src/main/sqldelight` folder. 

These SQL statements are run by `Database.Schema.migrate()`. This is automatic for the Android and iOS drivers. 

You can also place a `.db` file in the `src/main/sqldelight` folder of the same `<version number>.db` format. If there is a `.db` file present, a new `verifySqlDelightMigration` task will be added to the gradle project, and it will run as part of the `test` task, meaning your migrations will be verified against that `.db` file. It confirms that the migrations yield a database with the latest schema.

To generate a `.db` file from your latest schema, run the `generateSqlDelightSchema` task. You should probably do this before you create your first migration.

RxJava
------

To observe a query, depend on the RxJava extensions artifact and use the extension method it provides:

```groovy
dependencies {
  implementation "com.squareup.sqldelight:rxjava2-extensions:1.1.1"
}
```

```kotlin
val players: Observable<List<HockeyPlayer>> = 
  playerQueries.selectAll()
    .asObservable()
    .mapToList()
```

Multiplatform
-------------

To use SQLDelight in Kotlin multiplatform configure the Gradle plugin with a package to generate code into.

```groovy
apply plugin: "org.jetbrains.kotlin.multiplatform"
apply plugin: "com.squareup.sqldelight"

sqldelight {
  packageName = "com.example.hockey"
}
```

Continue to put `.sq` files in the `src/main/kotlin` directory, and then `expect` a `SqlDriver` to be provided by individual platforms when creating the `Database`.

Multiplatform requires the gradle metadata feature, which you can enable via the `settings.gradle` file in the project root;

```groovy
enableFeaturePreview('GRADLE_METADATA')
```

Android Paging
--------------

To use SQLDelight with [Android's Paging Library](https://developer.android.com/topic/libraries/architecture/paging/) add a dependency on the paging extension artifact.

```groovy
dependencies {
  implementation "com.squareup.sqldelight:android-paging-extensions:1.1.1"
}
```

To create a `DataSource` write a query to get the number of rows and a query that takes an offset and a limit.

```sql
countPlayers:
SELECT count(*) FROM hockeyPlayer;

players:
SELECT *
FROM hockeyPlayer
LIMIT :limit OFFSET :offset;
```

```kotlin
val dataSource = QueryDataSourceFactory(
  queryProvider = playerQueries::players,
  countQuery = playerQueries.countPlayers()
).create()
```

Supported Dialects
------------------

#### SQLite
Full support of dialect including views, triggers, indexes, FTS tables, etc. If features are missing please file an issue!

IntelliJ Plugin
---------------

The IntelliJ plugin provides language-level features for `.sq` files, including:
 - Syntax highlighting
 - Refactoring/Find usages
 - Code autocompletion
 - Generate `Queries` files after edits
 - Right click to copy as valid SQLite
 - Compiler errors in IDE click through to file

Gradle
------

```groovy
buildscript {
  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:1.1.1'
  }
}

apply plugin: 'com.squareup.sqldelight'
```

You can declare databases explicitely in gradle for tighter control of the generated schema:

```groovy
sqldelight {
  MyDatabase {
    packageName = "com.example.db"
    // By default this is ["sqldelight"], and means your sqldelight will be in
    // folders like 'src/main/db' instead of 'src/main/sqldelight'
    sourceFolders = ["db"]

    // Defaults to file("src/main/sqldelight")
    schemaOutputDirectory = file("build/dbs")

    // Optionally specify schema dependencies on other gradle projects
    dependency project(':OtherProject')
  }
}
```

The IntelliJ plugin can be installed from Android Studio by navigating<br>
Android Studio -> Preferences -> Plugins -> Browse repositories -> Search for SQLDelight

Snapshots of the development version (including the IDE plugin zip) are available in
[Sonatype's `snapshots` repository](https://oss.sonatype.org/content/repositories/snapshots/).

Upgrading From Previous Versions
--------------------------------

There's a separate guide for upgrading from 0.7 and other pre-1.0 versions [here](https://github.com/square/sqldelight/blob/master/UPGRADING.md)

# [KDoc](http://square.github.io/sqldelight/1.x/runtime/sqldelight-runtime/)

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
