# SQLDelight

See the [project website](https://cashapp.github.io/sqldelight/) for documentation and APIs

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

![intro.gif](docs/images/intro.gif)

---

SQLDelight supports a variety of dialects and platforms:

SQLite

* [Android](https://cashapp.github.io/sqldelight/android_sqlite)
* [Native (iOS, macOS, or Windows)](https://cashapp.github.io/sqldelight/native_sqlite)
* [JVM](https://cashapp.github.io/sqldelight/jvm_sqlite)
* [Javascript](https://cashapp.github.io/sqldelight/js_sqlite)
* [Multiplatform](https://cashapp.github.io/sqldelight/multiplatform_sqlite)

[MySQL (JVM)](https://cashapp.github.io/sqldelight/jvm_mysql/)

[PostgreSQL (JVM)](https://cashapp.github.io/sqldelight/jvm_postgresql) (Experimental)

[HSQL/H2 (JVM)](https://cashapp.github.io/sqldelight/jvm_h2) (Experimental)

## Snapshots

Snapshots of the development version (including the IDE plugin zip) are available in
[Sonatype's `snapshots` repository](https://oss.sonatype.org/content/repositories/snapshots/). Note that the coordinates are all app.cash.sqldelight instead of com.squareup.cash for the 2.0.0+ SNAPSHOTs.

License
=======

    Copyright 2016 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
