# Android Paging

## AndroidX Paging

To use SQLDelight with [Android's Paging 3 Library](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) add a dependency on the paging extension artifact.

```groovy
dependencies {
  implementation "com.squareup.sqldelight:android-paging3-extensions:{{ versions.sqldelight }}"
}
```

SQLDelight offers two methods for paging data -- offset based paging and keyset paging.

### Offset Based Paging

Offset paging achieves paged results using `OFFSET` and `LIMIT` clauses. Creating a `PagingSource` that performs offset based paging requires a count query as well as the paged query.

```sql
countPlayers:
SELECT count(*) FROM hockeyPlayer;

players:
SELECT *
FROM hockeyPlayer
LIMIT :limit OFFSET :offset;
```

```kotlin
import com.squareup.sqldelight.android.paging3.QueryPagingSource

val pagingSource: PagingSource = QueryPagingSource(
  countQuery = playerQueries.countPlayers(),
  transacter = playerQueries,
  dispatcher = Dispatchers.IO,
  queryProvider = playerQueries::players,
)
```

By default, queries are performed on `Dispatchers.IO` if no dispatcher is specified. Consumers expecting to use RxJava's `Scheduler` to perform queries should use the [`Scheduler.asCoroutineDispatcher`](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-rx2/kotlinx.coroutines.rx2/io.reactivex.-scheduler/as-coroutine-dispatcher.html) extension function.

### Keyset Paging

Offset paging is simple and easy to maintain. Unfortunately it performs poorly on large datasets. The `OFFSET` clause of a SQL statement really just drops already executed rows in a SQL query. Therefore, as the number to `OFFSET` grows, so does the amount of time it takes to execute your query. To overcome this, SQLDelight offers a "keyset paging" implementation of `PagingSource`. Rather than querying an entire dataset and inefficiently dropping the first `OFFSET` elements, keyset paging operates using a unique column to restrict the bounds of your queries. This performs better at the expense of higher developer maintenance. 

The `queryProvider` callback that this paging source accepts has two parameters -- a `beginInclusive` non-null unique `Key` as well as an `endExclusive` nullable unique `Key?`. An example of the core paging query is shown below. 

```sql
keyedQuery:
SELECT * FROM hockeyPlayer
WHERE id >= :beginInclusive AND (id < :endExclusive OR :endExclusive IS NULL)
ORDER BY id ASC;
```

Queries used in keyset paging must have a unique ordering like shown above. 

Both `beginInclusive` and `endExclusive` are _pre-calculated_ keys that act as page boundaries. Page sizes are established when pre-calculating page boundaries. The `pageBoundariesProvider` callback takes an `anchor: Key?` parameter as well as a `limit: Long?` parameter. An example query that pre-calculates page boundaries is shown below. 

```sql
pageBoundaries:
SELECT id 
FROM (
  SELECT
    id,
    CASE
      WHEN ((row_number() OVER(ORDER BY id ASC) - 0) % :limit) = 0 THEN 1
      WHEN id = :anchor THEN 1
      ELSE 0
    END page_boundary;
  FROM hockeyPlayer
  ORDER BY id ASC
)
WHERE page_boundary = 1;
```

Pre-calculating page boundaries of a SQL query will likely require [SQLite Window Functions](https://www.sqlite.org/windowfunctions.html). Window functions were introduced in SQLite version 3.25.0, and therefore are not available by default until Android API 30. To use keyset paging SQLDelight recommends either setting `minApi 30` _or_ bundling your own SQLite version. The Requery organization [offers an up-to-date distribution](https://github.com/requery/sqlite-android) of SQLite as a standalone library. 

The AndroidX paging library allows for the first page fetch to differ in size from the subsequent page fetches with `PagingConfig.initialLoadSize`. **This functionality should be avoided**, as the `pageBoundariesProvider` callback is invoked a single time on the first page fetch. Failing to have matching `PagingConifg.initialLoadSize` and `PagingConfig.pageSize` will result in unexpected page boundary generation. 

This paging source _does not_ support jumping. 

To create this paging source, use the `QueryPagingSource` factory function. 

```kotlin
import com.squareup.sqldelight.android.paging3.QueryPagingSource

val keyedSource = QueryPagingSource(
  transacter = playerQueries,
  dispatcher = Dispatchers.IO,
  pageBoundariesProvider = playerQueries::pageBoundaries,
  queryProvider = playerQueries::keyedQuery,
)
```

By default, queries are performed on `Dispatchers.IO` if no dispatcher is specified. Consumers expecting to use RxJava's `Scheduler` to perform queries should use the [`Scheduler.asCoroutineDispatcher`](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-rx2/kotlinx.coroutines.rx2/io.reactivex.-scheduler/as-coroutine-dispatcher.html) extension function.
