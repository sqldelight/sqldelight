# Multiplatform

To use SQLDelight in Kotlin multiplatform configure the Gradle plugin with a package to generate code into.

```groovy
apply plugin: "org.jetbrains.kotlin.multiplatform"
apply plugin: "com.squareup.sqldelight"

sqldelight {
  MyDatabase {
    packageName = "com.example.hockey"
  }
}
```

Put `.sq` files in the `src/commonMain/sqldelight` directory, and then `expect` a `SqlDriver` to be provided by individual platforms when creating the `Database`. Migration files should also be in the same `src/commonMain/sqldelight` directory.
