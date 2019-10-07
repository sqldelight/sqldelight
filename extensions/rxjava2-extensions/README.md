RxJava
======

To observe a query, depend on the RxJava extensions artifact and use the extension method it provides:

```groovy
dependencies {
  implementation "com.squareup.sqldelight:rxjava2-extensions:1.2.0"
}
```

```kotlin
val players: Observable<List<HockeyPlayer>> = 
  playerQueries.selectAll()
    .asObservable()
    .mapToList()
```

