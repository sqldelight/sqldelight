package app.cash.sqldelight.gradle

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails

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

interface SqlDelightAndroidVariantNameAttribute : Named {

  companion object {
    val ATTR = Attribute.of(
      "app.cash.sqldelight.gradle.android.variant",
      SqlDelightDatabaseNameAttribute::class.java,
    )
  }
}
