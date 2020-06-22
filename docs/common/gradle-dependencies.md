## Dependencies

You can specify schema dependencies on another module:

```groovy
sqldelight {
  MyDatabase {
    package = "com.example.projecta"
    dependency project(":ProjectB")
  }
}
```

This looks for `MyDatabase` in `ProjectB` and includes it's schema when compiling. For this to work,
ProjectB must have a database with the same name (`MyDatabase` in this case) but generate in a
different package, so here is what `ProjectB`'s gradle might look like:

```groovy
sqldelight {
  MyDatabase {
    package = "com.example.projectb"
  }
}
```