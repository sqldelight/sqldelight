import app.cash.sqldelight.gradle.SqlDelightExtension

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.sqldelight)
}

sqldelight {
  databases {
    create("Database") {
      packageName.set("com.example")
      schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
    }
  }
}

tasks.register("checkSources") {
  val kotlinSourceDirectorySet = (project.extensions["kotlin"] as org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension)
    .sourceSets["main"]
    .kotlin

  val output = file("build/sourcesChecked")
  inputs.files(kotlinSourceDirectorySet)
  outputs.file(output)
  doLast {
    check(kotlinSourceDirectorySet.files.any { it.name == "Database.kt" }) {
      "Database.kt was not generated"
    }
    output.writeText("Sources were generated as a dependency of checkSources ✅")
  }
}
