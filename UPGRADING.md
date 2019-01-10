Upgrading from Versions before 1.0
==================================

_If you're still on SQLDelight 0.6 doing the upgrade to 0.7 first so you stay on the SupportSQLite artifact will likely be easiest_

Upgrade the gradle plugin from 0.7 to 0.7.1. This will upgrade the arch.persistence.db dependency to 1.1.1, but should have no effect on your usage of sqldelight.

Upgrade the gradle plugin from 0.7.1 to 0.7.2. This changes the runtime package from `com.squareup.sqldelight` to `com.squareup.sqldelight.prerelease`, so you will need to change references in your own code.

Upgrade the gradle plugin from 0.7.2 to 0.9.0. This upgrades the transitive dependencies and generated code to instead use AndroidX, which is a requirement of SQLDelight. This should be done at the same time as you upgrading your own project to AndroidX, and cannot be done separately since SQLDelight generates code which references android support/AndroidX.

*ALTERNATIVELY* Upgrade the gradle plugin from 0.7 to 0.8.0 before then upgrading to 0.9.0. This upgrades to AndroidX without changing the sqldelight package name to `com.squareup.sqldelight.prerelease`

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

Upgrade the gradle plugin from 0.9 to 1.0.0-rc4. Note your build will fail at this point because of
the model code having undefined references to the old SQL Delight runtime (like `SqlDelightStatement`).
To add these back in add an `implementation` dependency on `com.squareup.sqldelight:runtime:0.9.0`.

At this point your build should still be working, but changes to `.sq` files will not be reflected
in your `*Model.java` files. If things aren't working at this point, please file an issue!

Begin by modifying your `SupportSQLiteOpenHelper.Callback` to call into the now generated `Database`
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

and now your `SupportSQLiteOpenHelper.Callback` should call into the `Database` for `create`

```java
@Override void onCreate(SupportSQLiteDatabase db) {
  SqlDriver driver = AndroidSqliteDriver(db)
  Database.Schema.create(driver)
}
```

You can do the same for your migrations if you place them in `.sqm` files, but thats not necessary part
of the upgrade.

At this point things should still work normally.

Next add in the code to create your `Database` as part of an object graph/singleton pattern/whevs:

```java
@Provides @Singleton static SupportSQLiteOpenHelper provideDatabaseHelper(
    @App Context context) {
  SupportSQLiteOpenHelper.Configuration config =  SupportSQLiteOpenHelper.Configuration.builder(context)
      .name(DATABASE_NAME)
      .callback(new MyDatabaseCallback())
      .build();
  return new FrameworkSQLiteOpenHelperFactory().create(config);
}

@Provides @Singleton static Database provideDatabase(
    SupportSQLiteOpenHelper helper) {
  return new Database(new AndroidSqliteDriver(helper));
}
```

If you're also using SQL Brite make sure you create a `BriteDatabase` with the same `SupportSQLiteOpenHelper`
that's being used to create the `Database`.

Things should still be working.

The following assume you're using SQL Brite to get reactive callbacks from the database, but upgrades
using only SQL Delight will be similar.

Mutating queries can be converted individually by using the `Database`:

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
database.userQueries.insertUser(2, "Jake")
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
val usersObservable = database.userQueries.users()
  .asObservable(Schedulers.io()) // The scheduler to run the query on.
  .mapToList()
```

If you still want to use a custom type, pass it as a parameter to the query.

```kotlin
val myUsersObservable = database.userQueries.users(::MyUser)
  .asObservable(Schedulers.io())
  .mapToList()
```

Once you no longer have references to `UserModel.java`, delete the whole class. Repeat for each of
your `*Model.java` files until upgrading is complete!
