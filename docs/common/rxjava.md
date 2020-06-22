# RxJava

To observe a query, depend on the RxJava extensions artifact and use the extension method it provides:

```groovy
dependencies {
  implementation "com.squareup.sqldelight:rxjava3-extensions:{{ versions.sqldelight }}"
}
```

```kotlin
val players: Observable<List<HockeyPlayer>> = 
  playerQueries.selectAll()
    .asObservable()
    .mapToList()
```

_Note: For RxJava 2, use `rxjava2-extensions` as the artifact name._
