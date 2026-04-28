package app.cash.sqldelight.gradle

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute

interface SqlDelightPackageAttribute : Named {

  companion object {
    val ATTR = Attribute.of(
      "app.cash.sqldelight.gradle.package",
      SqlDelightPackageAttribute::class.java,
    )
  }
}

interface SqlDelightDatabaseNameAttribute : Named {

  companion object {
    val ATTR = Attribute.of(
      "app.cash.sqldelight.gradle.database.name",
      SqlDelightDatabaseNameAttribute::class.java,
    )
  }
}
