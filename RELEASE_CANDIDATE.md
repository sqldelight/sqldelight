SqlDelight 1.0.0 RC
===================

This doc is a WIP towards a README as well. If you have feedback or questions about the release candidate/README please file an issue.

### Kotlin/Gradle

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:1.0.0-rc2'
  }
}

apply plugin: 'org.jetbrains.kotlin.jvm'
apply plugin: 'com.squareup.sqldelight'

sqldelight {
  packageName = "com.example"
  sourceSet = files("src/main/sqldelight")
  schemaOutputDirectory = file("src/main/sqldelight/databases")
}
```

This will create the task `generateSqlDelightInterfaces` and add it as a dependency of your Kotlin
compilation task. Specifying `schemaOutputDirectory` will add the `generateSqlDelightSchema` task to
generate a SQLite `.db` file to test migrations against, and `verifySqlDelightMigrations` task to
verify any migration files you have.

### Android/Gradle

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.squareup.sqldelight:gradle-plugin:1.0.0-rc2'
  }
}

apply plugin: 'com.android.application'
apply plugin: 'org.jetbrains.kotlin.android'
apply plugin: 'com.squareup.sqldelight.android'

sqldelight {
  schemaOutputDirectory = file("src/main/sqldelight/databases")
}
```

NOTE: You must be on AGP 3.2 or greater.

This will create a `generate[Variant]SqlDelightInterface` task for each variant you have and add it
as a dependency for that variant's assembly task. The `schemaOutputDirectory` optional property
works the same as for the Kotlin plugin.

### Multiplatform

See [SdkSearch](https://github.com/JakeWharton/SdkSearch/pull/98) as an example of setting up
SQL Delight in a Kotlin multiplatform project.

### Usage

Write `.sq` files same as SqlDelight pre-1.0. Any unlabeled statements in a `.sq` file are run during
creation - so you can also have multiple `CREATE TABLE` statements per `.sq` file.

Run the `generateSqlDelightInterface` task to create the `QueryWrapper` object for the first time
needed to execute any of your labeled queries. Download the [IntelliJ plugin rc2](https://plugins.jetbrains.com/plugin/8191-sqldelight)
to have generation happen automatically while editing.

Create a `QueryWrapper` by providing a driver to it's constructor. In Android, use the Android driver
artifact provided:

```groovy
dependencies {
  implementation 'com.squareup.sqldelight:android-driver:1.0.0-rc2'
}
```

```kotlin
val driver = AndroidSqlDatabase(context, "sample.db")
val queryWrapper = QueryWrapper(driver) // Adapters are also passed to this constructor if you use custom types
```

Each `.sq` file gets a generated class accessible through the `QueryWrapper` you can use to execute
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

You can use a custom mapper for projections to use your own type instead of the one SQL Delight generates:

```kotlin
// Makes a List<MyType>
val results = queryWrapper.testQueries.selectAll({ column1, column2 -> MyType(column1, column2) }).executeAsList()
```

### RxJava

To make queries observable, include the RxJava artifact.

```groovy
dependencies {
  implementation 'com.squareup.sqldelight:rxjava2-extensions:1.0.0-rc2'
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

Place your SQLite migrations in `[version_upgrading_from].sqm`

So if you want to add a column to the `test` table when you upgrade from version 1 to 2, create `1.sqm`
with:

```sql
ALTER TABLE test ADD COLUMN new_column TEXT;
```

The current version of your schema is the maximum of your `.sqm` files plus one. 

If you also have the file `1.db` in your source set, run the `verifySqlDelightMigration` task to
have your migrations verified. In our case it will apply `1.sqm` to `1.db` and verify the schema
is equivalent to a database created fresh from your `CREATE` statements. 

This doesn't do any data migration verification, only schema. The IDE plugin is very broken for 
`.sqm` files at the moment (rc2).

### Upgrading from SQLDelight 0.7 / SQLBrite

_If you're still on SQLDelight 0.6 doing the upgrade to 0.7 first so you stay on the SupportSQLite artifact will likely be easiest_

Upgrade the gradle plugin from 0.7 to 0.7.1. This will upgrade the arch.persistence.db dependency to 1.1.1, but should have no effect on your usage of sqldelight.

Upgrade the gradle plugin from 0.7.1 to 0.7.2. This changes the runtime package from `com.squareup.sqldelight` to `com.squareup.sqldelight.prerelease`, so you will need to change references in your own code.

Upgrade the gradle plugin from 0.7.2 to 0.9.0. This upgrades the transitive dependencies and generated code to instead use AndroidX, which is a requirement of SQLDelight. This should be done at the same time as you upgrading your own project to AndroidX, and cannot be done separately since SQLDelight generates code which references android support/AndroidX.

*ALTERNATIVELY* Upgrade the gradle plugin from 0.7 to 0.8.0 before then upgrading to 0.9.0

Suppose on SQLDelight 0.9 you have this `User.sq` file:

```sql
CREATE TABLE user (
  id INTEGER NOT NULL PRIMARY KEY,
  name TEXT NOT NULL
);

insertDefaultData:
INSERT INTO user
VALUES (1, 'Alec');

users:
SELECT *
FROM user;

names:
SELECT name
FROM user;

insertUser:
INSERT INTO user
VALUES (?, ?);
```

This will generate the `UserModel` class with methods for your queries. 

Copy and paste all `*Model.java` files out of the build directory and into your `src/main/java` folder. 

Upgrade the gradle plugin from 0.9 to 1.0.0-rc2. Note your build will fail at this point because of
the model code having undefined references to the old SQL Delight runtime (like `SqlDelightStatement`).
To add these back in add an `implementation` dependency on `com.squareup.sqldelight:runtime:0.9.0`.

At this point your build should still be working, but changes to `.sq` files will not be reflected
in your `*Model.java` files. If things aren't working at this point, please file an issue!

Begin by modifying your `SupportSQLiteOpenHelper.Callback` to call into the now generated `QueryWrapper`
which holds generated code for SQL Delight 1.0:

```java
//Before
@Override void onCreate(SupportSQLiteDatabase db) {
  db.execSql(UserModel.CREATE_TABLE);
  db.execSql(UserModel.INSERTDEFAULTDATA);
  // Other create table/initialization
}
```

In SQL Delight 1.0 all unlabeled statements in `.sq` files (including `CREATE` statements) will be run
during `onCreate`, so we can remove the `insertDefaultData` identifier from above:

`User.sq`
```sql
...

--insertDefaultData:
INSERT INTO user
VALUES (1, 'Alec');

...
```

and now your `SupportSQLiteOpenHelper.Callback` should call into the `QueryWrapper` for `onCreate`

```java
@Override void onCreate(SupportSQLiteDatabase db) {
  SqlDatabase database = SqlDelight.create(QueryWrapper.Companion, db)
  QueryWrapper.onCreate(database.getConnection())
}
```

You can do the same for your migrations if you place them in `.sqm` files, but thats not necessary part
of the upgrade.

At this point things should still work normally.

Next add in the code to create your `QueryWrapper` as part of an object graph/singleton pattern/whevs:

```java
@Provides @Singleton static SupportSQLiteOpenHelper provideDatabaseHelper(
    @App Context context) {
  SupportSQLiteOpenHelper.Configuration config =  SupportSQLiteOpenHelper.Configuration.builder(context)
      .name(DATABASE_NAME)
      .callback(new MyDatabaseCallback())
      .build();
  return new FrameworkSQLiteOpenHelperFactory().create(config);
}

@Provides @Singleton static QueryWrapper provideQueryWrapper(
    SupportSQLiteOpenHelper helper) {
  return QueryWrapper(new SqlDelightDatabaseHelper(helper));
}
```

If you're also using SQL Brite make sure you create a `BriteDatabase` with the same `SupportSQLiteOpenHelper`
that's being used to create the `QueryWrapper`.

Things should still be working.

The following assume you're using SQL Brite to get reactive callbacks from the database, but upgrades
using only SQL Delight will be similar.

Mutating queries can be converted individually by using the `QueryWrapper`:

before:
```kotlin
private val insertUser: UserModel.InsertUser by lazy {
  UserModel.InsertUser(datbaseOpenHelper.writableDatabase)
}

insertUser.bind(2, "Jake")
insertUser.executeInsert()
```

after:
```kotlin
queryWrapper.insertUser(2, "Jake")
```

You no longer need a "Factory" type to perform queries, the query wrapper is all that is needed.

before:
```kotlin
val query = User.FACTORY.users()
val usersObservable = briteDatabase.createQuery(query.tables, query.statement, query.args)
  .mapToList(User.FACTORY.usersMapper()::map)
```

after:
```kotlin
val usersObservable = queryWrapper.userQueries.users()
  .asObservable(Schedulers.io()) // The scheduler to run the query on.
  .mapToList()
```

If you still want to use a custom type, pass it as a parameter to the query.

```kotlin
val myUsersObservable = queryWrapper.userQueries.users(::MyUser)
  .asObservable(Schedulers.io())
  .mapToList()
```

Once you no longer have references to `UserModel.java`, delete the whole class. Repeat for each of
your `*Model.java` files until upgrading is complete!
