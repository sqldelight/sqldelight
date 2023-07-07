# Getting Started on JVM with SQLite

{% include 'common/index_gradle_database.md' %}

{% include 'common/index_schema.md' %}

To use the generated database in your code, you must add the SQLDelight SQLite driver dependency to
your project.

=== "Kotlin"
    ```groovy
    dependencies {
      implementation("app.cash.sqldelight:sqlite-driver:{{ versions.sqldelight }}")
    }
    ```
=== "Groovy"
    ```groovy
    dependencies {
      implementation "app.cash.sqldelight:sqlite-driver:{{ versions.sqldelight }}"
    }
    ```

An instance of the driver can be constructed as shown below. The constructor accepts a JDBC 
connection string that specifies the location of the database file. The `IN_MEMORY`
constant can also be passed to the constructor to create an in-memory database.

=== "On-Disk"
    ```kotlin
    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:test.db")
    Database.Schema.create(driver)
    ```
=== "In-Memory"
    ```kotlin
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    Database.Schema.create(driver)
    ```

{% include 'common/index_queries.md' %}
