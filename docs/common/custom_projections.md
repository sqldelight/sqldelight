# Projections

By default queries will return a data class with your projection, but you can override the behavior with a typesafe mapper.

```kotlin
val selectAllNames = playerQueries.selectAll(
  mapper = { player_number, full_name -> full_name.toUppercase() }
)
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
