# Custom Projections

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
