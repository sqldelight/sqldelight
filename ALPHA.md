SqlDelight 1.0 Alpha
====================

### Kotlin/Gradle

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:1.0.0-alpha1'
  }
}

apply plugin: 'kotlin'
apply plugin: 'com.squareup.sqldelight'

sqldelight {
  packageName = "com.example"
  sourceSet = files("src/main/sqldelight")
  schemaOutputDirectory = file("src/main/sqldelight/databases")
}
```

This will create the task `generateSqlDelightInterfaces` and add it as a dependency of your kotlin
compilation task. Specifying `schemaOutputDirectory` will add the `generateSqlDelightSchema` task to
generate a sqlite `.db` file to test migrations against, and `verifySqlDelightMigrations` task to
verify any migration files you have.

### Android/Gradle

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.sqldelight:android-gradle-plugin:1.0.0-alpha1'
  }
}

apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'
apply plugin: 'com.squareup.sqldelight.android'

sqldelight {
  schemaOutputDirectory = file("src/main/sqldelight/databases")
}
```

NOTE: You must be on AGP 3.2 or greater.

This will create a `generate[Variant]SqlDelightInterface` task for each variant you have and add it
as a dependency for that variant's assembly task. The `schemaOutputDirectory` optional property
works the same as for the kotlin plugin.

### Usage

Write .sq files same as SqlDelight pre-1.0. Any unlabeled statements in a .sq file are run during
creation - so you can also have multiple CREATE TABLE statements per .sq file.

Run the `generateSqlDelightInterface` task to create the `QueryWrapper` object for the first time
needed to execute any of your labeled queries. Download the [intellij plugin](https://oss.sonatype.org/content/repositories/snapshots/com/squareup/sqldelight/idea-plugin/)
to have generation happen automatically while editing.

Create a QueryWrapper by providing a driver to it's constructor. In android, use the android driver
artifact provided:

```groovy
dependencies {
  implementation 'com.squareup.sqldelight:android-driver:1.0.0-alpha1'
}
```

```kotlin
val driver = QueryWrapper.create(context, "sample.db")
val queryWrapper = QueryWrapper(driver) // Adapters are also passed to this constructor if you use custom types
```

Each .sq file gets a generated class accessible through the QueryWrapper you can use to execute
queries.

```sql
-- In file Test.sq
selectAll:
SELECT *
FROM test;
```

```kotlin
val results = queryWrapper.testQueries.selectAll().executeAsList()
```

### RxJava

To make queries observable, include the rxjava artifact.

```groovy
dependencies {
  implementation 'com.squareup.sqldelight:rxjava2-extensions:1.0.0-alpha1'
}
```

```kotlin
val disposable = queryWrapper.testQueries.selectAll()
  .observe(Schedulers.io()) // The scheduler passed in is the scheduler the query will run on.
  .mapToList()
  .observeOn(AndroidSchedulers.mainThread())
  .subscribe { listOfResults -> doStuff() }
```

### Migrations

Place your sqlite migrations in [version_upgrading_from].sqm

So if you want to add a column to the `test` table when you upgrade from version 1 to 2, create 1.sqm
with:

```sql
ALTER TABLE test ADD COLUMN new_column TEXT;
```

The current version of your schema is the maximum of your .sqm files plus one. 

If you also have the file `1.db` in your source set, run the `verifySqlDelightMigration` task to
have your migrations verified. In our case it will apply `1.sqm` to `1.db` and verify the schema
is equivalent to a database created fresh from your `CREATE` statements. 

This doesn't do any data migration verification, only schema. The IDE plugin is very broken for 
.sqm files at the moment (alpha1).