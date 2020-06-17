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
// and passed around using your favourite dependency injection/service
// locator/singleton pattern.
val database = Database(driver)

val playerQueries: PlayerQueries = database.playerQueries

println(playerQueries.selectAll().executeAsList())
// Prints [HockeyPlayer(15, "Ryan Getzlaf")]

playerQueries.insert(player_number = 10, full_name = "Corey Perry")
println(playerQueries.selectAll().executeAsList())
// Prints [HockeyPlayer(15, "Ryan Getzlaf"), HockeyPlayer(10, "Corey Perry")]

val player = HockeyPlayer(10, "Ronald McDonald")
playerQueries.insertFullPlayerObject(player)
```

And that's it! Check out the other pages on the sidebar for other functionality.