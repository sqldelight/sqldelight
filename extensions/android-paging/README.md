Android Paging
==============

To use SQLDelight with [Android's Paging Library](https://developer.android.com/topic/libraries/architecture/paging/) add a dependency on the paging extension artifact.

```groovy
dependencies {
  implementation "com.squareup.sqldelight:android-paging-extensions:1.2.0"
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

