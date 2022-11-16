package app.cash.sqldelight.gradle

import app.cash.sqldelight.VERSION

fun sqldelightDependency(artifact: String) = "$artifact:$VERSION"

object SqlDelightDependencies {
  val runtime get() = sqldelightDependency("app.cash.sqldelight:runtime")
  val asyncRuntime get() = sqldelightDependency("app.cash.sqldelight:runtime-async")
  val drivers = DriverDependencies
  val extensions = ExtensionDependencies
  val dialects = DialectDependencies
}

object DriverDependencies {
  val jdbc get() = sqldelightDependency("app.cash.sqldelight:jdbc-driver")
  val sqlite = object {
    val jvm get() = sqldelightDependency("app.cash.sqldelight:sqlite-driver")
    val android get() = sqldelightDependency("app.cash.sqldelight:android-driver")
    val native get() = sqldelightDependency("app.cash.sqldelight:native-driver")
    val js get() = sqldelightDependency("app.cash.sqldelight:sqljs-driver")
  }
  val r2dbc get() = sqldelightDependency("app.cash.sqldelight:r2dbc-driver")
}

object ExtensionDependencies {
  val androidPaging3 get() = sqldelightDependency("app.cash.sqldelight:android-paging3-extensions")
  val async get() = sqldelightDependency("app.cash.sqldelight:async-extensions")
  val coroutines get() = sqldelightDependency("app.cash.sqldelight:coroutines-extensions")
  val rxjava2 get() = sqldelightDependency("app.cash.sqldelight:rxjava2-extensions")
  val rxjava3 get() = sqldelightDependency("app.cash.sqldelight:rxjava3-extensions")
}

object DialectDependencies {
  val hsql get() = sqldelightDependency("app.cash.sqldelight:hsql-dialect")
  val mysql get() = sqldelightDependency("app.cash.sqldelight:mysql-dialect")
  val postgresql get() = sqldelightDependency("app.cash.sqldelight:postgresql-dialect")
  val sqlite = object {
    val jsonModule get() = sqldelightDependency("app.cash.sqldelight:sqlite-json-module")
  }
  val sqlite_3_18 get() = sqldelightDependency("app.cash.sqldelight:sqlite-3-18-dialect")
  val sqlite_3_24 get() = sqldelightDependency("app.cash.sqldelight:sqlite-3-24-dialect")
  val sqlite_3_25 get() = sqldelightDependency("app.cash.sqldelight:sqlite-3-25-dialect")
  val sqlite_3_30 get() = sqldelightDependency("app.cash.sqldelight:sqlite-3-30-dialect")
  val sqlite_3_35 get() = sqldelightDependency("app.cash.sqldelight:sqlite-3-35-dialect")
  val sqlite_3_38 get() = sqldelightDependency("app.cash.sqldelight:sqlite-3-38-dialect")
}

object AdapterDependencies {
  val primitives = sqldelightDependency("app.cash.sqldelight:primitive-adapters")
}
