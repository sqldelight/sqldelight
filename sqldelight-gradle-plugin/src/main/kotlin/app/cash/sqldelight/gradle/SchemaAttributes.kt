package app.cash.sqldelight.gradle

import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails

private abstract class PlatformTypeCompatibilityRule : AttributeCompatibilityRule<String> {
  override fun execute(details: CompatibilityCheckDetails<String>) = with(details) {
    when {
      consumerValue == producerValue -> compatible()
      producerValue == "common" -> compatible()
      consumerValue == "androidJvm" && producerValue == "jvm" -> compatible()
      else -> incompatible()
    }
  }
}

private abstract class SourceNameCompatibilityRule : AttributeCompatibilityRule<String> {
  override fun execute(details: CompatibilityCheckDetails<String>) = with(details) {
    when {
      consumerValue == producerValue -> compatible()
      producerValue == "commonMain" -> compatible()
      else -> incompatible()
    }
  }
}

internal fun DependencyHandler.registerSqlDelightAttributesSchema() {
  attributesSchema { schema ->
    schema.apply {
      attribute(PlatformTypeAttribute) {
        it.compatibilityRules.add(PlatformTypeCompatibilityRule::class.java)
      }
      attribute(SourceNameAttribute) {
        it.compatibilityRules.add(SourceNameCompatibilityRule::class.java)
      }
    }
  }
}

internal val PackageNameAttribute: Attribute<String> = Attribute.of("app.cash.sqldelight.packageName", String::class.java)
internal val DatabaseNameAttribute: Attribute<String> = Attribute.of("app.cash.sqldelight.database", String::class.java)
internal val SourceNameAttribute: Attribute<String> = Attribute.of("app.cash.sqldelight.source", String::class.java)
internal val PlatformTypeAttribute: Attribute<String> = Attribute.of("app.cash.sqldelight.platformType", String::class.java)
