## Valid SQL migrations

Using custom kotlin types in migration files means those files are no longer valid SQL.
You can optionally configure a gradle task to output your migration files as valid SQL for other
services to read from:

```groovy
sqldelight {
  Database {
    migrationOutputDirectory = file("$buildDir/resources/main/migrations")
    migrationOutputFileFormat = ".sql" // Defaults to .sql
  }
}
```

This creates a new task `generateMainDatabaseMigrations` which will output your `.sqm` files as
valid SQL in the output directory, with the output format. Create a dependency from your
compileKotlin task so that services such as flyway will have the files available on their
classpath:

```groovy
compileKotlin.configure {
  dependsOn "generateMainDatabaseMigrations"
}
```