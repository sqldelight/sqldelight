# Getting Started on Android

First apply the gradle plugin in your project.

```groovy
buildscript {
  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:{{ versions.sqldelight }}'
  }
}

apply plugin: 'com.squareup.sqldelight'
```

Put your SQL statements in a `.sq` file under `src/main/sqldelight`. Typically the first statement in the SQL file creates a table.

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

```groovy
dependencies {
  implementation "com.squareup.sqldelight:android-driver:{{ versions.sqldelight }}"
}
```
```kotlin
val driver: SqlDriver = AndroidSqliteDriver(Database.Schema, context, "test.db")
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

And that's it! Check out the other pages on the sidebar for other functionality.