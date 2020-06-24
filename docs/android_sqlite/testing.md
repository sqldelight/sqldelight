In some tests (like verification of migrations) you might wish to swap out the Android driver with
the [JVM driver](https://github.com/square/sqldelight#JVM), enabling you to test code involving the
database without needing an Android emulator or physical device. To do that use the jvm SQLite
driver:

```groovy
dependencies {
  testImplementation 'com.squareup.sqldelight:sqlite-driver:{{ versions.sqldelight }}'
}
```

```kotlin
// When your test needs a driver
@Before fun before() {
  driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
  Database.Schema.create(driver)
}
```

If you are using the SQLite that comes bundled with Android (rather than shipping [your own](https://github.com/requery/sqlite-android/)), you can override the version of [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc) to one that [matches your Android minSdkVersion](https://stackoverflow.com/questions/2421189/version-of-sqlite-used-in-android#4377116), for example for API 23 use SQLite 3.8.10.2:

```groovy
dependencies {
  testImplementation('org.xerial:sqlite-jdbc:3.8.10.2') {
    // Override the version of sqlite used by sqlite-driver to match Android API 23
    force = true
  }
}
```
