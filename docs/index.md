SQLDelight generates typesafe kotlin APIs from your SQL statements. It verifies your schema, statements, and migrations at compile-time and provides IDE features like autocomplete and refactoring which make writing and maintaining SQL simple.

SQLDelight understands your existing SQL schema.

```sql
CREATE TABLE hockey_player (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  number INTEGER NOT NULL
);
```

It generates typesafe code for any labeled SQL statements.

![intro.gif](images/intro.gif)

---

SQLDelight supports a variety of dialects and platforms:

SQLite

* [Android](android_sqlite)
* [Native (iOS, macOS, or Windows)](native_sqlite)
* [JVM](jvm_sqlite)
* [Javascript](js_sqlite)
* [Multiplatform](multiplatform_sqlite)

[MySQL (JVM)](jvm_mysql)

[PostgreSQL (JVM)](jvm_postgresql) (Experimental)

[HSQL/H2 (JVM)](jvm_h2) (Experimental)

## Snapshots

Snapshots of the development version (including the IDE plugin zip) are available in
[Sonatype's `snapshots` repository](https://oss.sonatype.org/content/repositories/snapshots/).
