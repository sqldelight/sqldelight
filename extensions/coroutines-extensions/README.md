Coroutines
==========

To observe a query, depend on the coroutines extensions artifact and use the extension method it provides:

```groovy
dependencies {
  implementation "com.squareup.sqldelight:coroutines-extensions:1.2.0"
}
```

```kotlin
val players: Flow<List<HockeyPlayer>> = 
  playerQueries.selectAll()
    .asFlow()
    .mapToList()
```

