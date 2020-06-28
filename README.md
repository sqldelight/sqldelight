# SQLDelight

See the [project website](https://cashapp.github.io/sqldelight/) for documentation and APIs

SQLDelight generates typesafe kotlin APIs from your SQL statements. It compile-time verifies your schema, statements, and migrations and provides IDE features like autocomplete and refactoring which make writing and maintaining SQL simple.

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

* [Android](android_sqlite)
* [Native (iOS, macOS, or Windows)](native_sqlite)
* [JVM](jvm_sqlite)
* Javascript (Work In Progress)
* [Multiplatform](multiplatform_sqlite)

[MySQL (JVM)](jvm_mysql)

[PostgreSQL (JVM)](jvm_postgresql) (Experimental)

[HSQL/H2 (JVM)](jvm_h2) (Experimental)

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
