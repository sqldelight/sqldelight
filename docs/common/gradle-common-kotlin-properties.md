    packageName = "com.example.db"
    sourceFolders = listOf("db")
    schemaOutputDirectory = file("build/dbs")
    dependency(project(":OtherProject"))
    dialect = "sqlite:3.24"
    verifyMigrations = true
    treatNullAsUnknownForEquality = true
