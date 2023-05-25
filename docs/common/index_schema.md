{% include 'common/index_schema_sq.md' %}

From this, SQLDelight will generate a `Database` Kotlin class with an associated `Schema` object that can be used to create your database and run your statements on it. Generating the `Database` file happens during the 'generateSqlDelightInterface' gradle task. This task runs during the 'make project'/'make module' build task, or automatically if you have the SQLDelight IDE plugin.

Accessing the generated database also requires a driver, which SQLDelight provides implementations of:
