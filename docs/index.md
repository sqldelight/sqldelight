# SQLDelight

SQLDelight generates typesafe APIs from your SQL statements. It compile-time verifies your schema, statements, and migrations and provides IDE features like autocomplete and refactoring which make writing and maintaining SQL simple. SQLDelight currently supports the SQLite dialect and there are supported SQLite drivers on Android, JVM, iOS, and Windows.

## Example

To use SQLDelight, apply the [gradle plugin](/gradle) and put your SQL statements in a `.sq` file in `src/main/sqldelight`.  Typically the first statement in the SQL file creates a table.

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
  implementation "com.squareup.sqldelight:android-driver:1.2.1"
}
```
```kotlin
val driver: SqlDriver = AndroidSqliteDriver(Database.Schema, context, "test.db")
```

#### iOS, or Windows (Using Kotlin/Native)
```groovy
dependencies {
  implementation "com.squareup.sqldelight:native-driver:1.2.1"
}

// You'll also need to have SQLite linked via -lsqlite3 during compilation.
```
```kotlin
val driver: SqlDriver = NativeSqliteDriver(Database.Schema, "test.db")
```

#### JVM
```groovy
dependencies {
  implementation "com.squareup.sqldelight:sqlite-driver:1.2.1"
}
```
```kotlin
val driver: SqlDriver = JdbcSqliteDriver(IN_MEMORY)
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

insertFullPlayerObject:
INSERT INTO hockeyPlayer(player_number, full_name)
VALUES ?;
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

val player = HockeyPlayer(10, "Ronald McDonald")
playerQueries.insertFullPlayerObject(player)
```
