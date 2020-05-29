## Flow

To consume a query as a Flow, depend on the Coroutines extensions artifact and use the extension method it provides:

```groovy
dependencies {
  implementation "com.squareup.sqldelight:coroutines-extensions:1.2.1"
}
```

```kotlin
val players: Flow<List<HockeyPlayer>> = 
  playerQueries.selectAll()
    .asFlow()
    .mapToList()
```

This flow emits the query result, and emits a new result every time the database changes for that query.
