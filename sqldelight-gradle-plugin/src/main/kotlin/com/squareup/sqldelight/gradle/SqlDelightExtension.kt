package com.squareup.sqldelight.gradle

import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.util.ConfigureUtil
import java.io.File

open class SqlDelightExtension {
  internal val databases = mutableListOf<SqlDelightDatabase>()
  internal var configuringDatabase: SqlDelightDatabase? = null
  internal lateinit var project: Project

  // TODO: Remove these after 1.1.0
  var packageName: String? = null
    set(value) = newDsl()
  var className: String? = null
    set(value) = newDsl()
  var sourceSet: FileCollection? = null
    set(value) = newDsl()
  var schemaOutputDirectory: File? = null
    set(value) = newDsl()

  fun methodMissing(name: String, args: Any): Any {
    configuringDatabase?.methodMissing(name, args)?.let { return it }

    val closure = (args as? Array<*>)?.getOrNull(0) as? Closure<*>
        ?: throw IllegalStateException("""
        |Expected a closure for database names:
        |
        |sqldelight {
        |  $name {
        |    packageName = "com.sample"
        |    sourceSet = files("src/main/sqldelight")
        |  }
        |}
      """.trimMargin())

    val database = SqlDelightDatabase(project, name = name)
    configuringDatabase = database
    ConfigureUtil.configure(closure, database)
    configuringDatabase = null

    if (databases.any { it.name == database.name }) {
      throw IllegalStateException("There is already a database defined for ${database.name}")
    }

    databases.add(database)
    return Unit
  }

  companion object {
    private fun newDsl(): Nothing = throw GradleException("""
      |Format of specifying databases has changed from:
      |
      |sqldelight {
      |  className = "MyDatabase"
      |  packageName = "com.example"
      |  sourceSet = files("src/main/sqldelight")
      |  schemaOutputDirectory = file("src/main/sqldelight/migrations")
      |}
      |
      |to
      |
      |sqldelight {
      |  MyDatabase {
      |    packageName = "com.example"
      |    sourceFolders = ["sqldelight"]
      |    schemaOutputDirectory = file("src/main/sqldelight/migrations")
      |  }
      |}
    """.trimMargin())
  }
}
