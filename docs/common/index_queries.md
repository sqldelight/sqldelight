## Defining Typesafe Queries

SQLDelight will generate a typesafe function for any labeled SQL statement in a `.sq` file.

```sql title="src/main/sqldelight/com/example/sqldelight/hockey/data/Player.sq"
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

A "Queries" object will be generated for each `.sq` file containing labeled statements.
For example, a `PlayerQueries` object will be generated for the `Player.sq` file shown above.
This object can be used to call the generated typesafe functions which will execute the actual SQL
statements.

```kotlin
{% if async %}suspend {% endif %}fun doDatabaseThings(driver: SqlDriver) {
  val database = Database(driver)
  val playerQueries: PlayerQueries = database.playerQueries

  println(playerQueries.selectAll().{% if async %}await{% else %}execute{% endif %}AsList()) 
  // [HockeyPlayer(15, "Ryan Getzlaf")]

  playerQueries.insert(player_number = 10, full_name = "Corey Perry")
  println(playerQueries.selectAll().{% if async %}await{% else %}execute{% endif %}AsList()) 
  // [HockeyPlayer(15, "Ryan Getzlaf"), HockeyPlayer(10, "Corey Perry")]

  val player = HockeyPlayer(10, "Ronald McDonald")
  playerQueries.insertFullPlayerObject(player)
}
```

{% if async %}
!!! warning
    When using an asynchronous driver, use the suspending `awaitAs*()` extension functions when 
    running queries instead of the blocking `executeAs*()` functions.
{% endif %}

And that's it! Check out the other pages on the sidebar for other functionality.
