## Bind Args

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

## Named Arguments

Named parameters or indexed parameters can be used.

```sql
firstOrLastName:
SELECT *
FROM hockeyPlayer
WHERE full_name LIKE ('% ' || :name)
OR full_name LIKE (:name || ' %');
```

```kotlin
playerQueries.firstOrLastName(name = "Ryan")
```

## Variable Arguments

Sets of values can also be passed as an argument.

```sql
selectByNames:
SELECT *
FROM hockeyPlayer
WHERE full_name IN ?;
```

```kotlin
playerQueries.selectByNames(listOf("Alec", "Jake", "Matt"))
```

## Inserts

`INSERT VALUES` arguments can be bound to with the table's data class.

```sql
insertPlayer:
INSERT INTO hockeyPlayer
VALUES ?;
```

```kotlin
val rickardRakell = HockeyPlayer(
  full_name = "Rickard Rakell",
  number = 67
)
playerQueries.insertPlayer(rickardRakell)
```