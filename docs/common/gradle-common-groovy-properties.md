    // Package name used for the generated MyDatabase.kt
    packageName = "com.example.db"

    // An array of folders where the plugin will read your '.sq' and '.sqm' 
    // files. The folders are relative to the existing source set so if you
    // specify ["db"], the plugin will look into 'src/main/db' or 'src/commonMain/db' for KMM. 
    // Defaults to ["sqldelight"].
    sourceFolders = ["db"]

    // The directory where to store '.db' schema files relative to the root 
    // of the project. These files are used to verify that migrations yield 
    // a database with the latest schema. Defaults to null so the verification 
    // tasks will not be created.
    schemaOutputDirectory = file("src/main/sqldelight/databases")

    // Optionally specify schema dependencies on other gradle projects
    dependency project(':OtherProject')

    // The dialect version you would explicitly like to target. This can be skipped
    // on Android where a SQLite version is automatically picked based on your minSdk.
    // Defaults to "sqlite:3.18".
    dialect = "sqlite:3.24"
    
    // If set to true, migration files will fail during compilation if there are errors in them.
    // Defaults to false.
    verifyMigrations = true
