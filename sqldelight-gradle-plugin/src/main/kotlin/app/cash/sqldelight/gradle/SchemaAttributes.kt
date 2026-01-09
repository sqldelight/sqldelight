package app.cash.sqldelight.gradle

import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ConsumableConfiguration
import org.gradle.api.artifacts.DependencyScopeConfiguration
import org.gradle.api.artifacts.LegacyConfiguration
import org.gradle.api.artifacts.ResolvableConfiguration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeCompatibilityRule
import org.gradle.api.attributes.CompatibilityCheckDetails
import org.gradle.util.GradleVersion

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

internal fun DependencyHandler.configureSqlDelightAttributesSchema() {
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

/**
 * Registers a declarable bucket configuration.
 *
 * Declarable configurations collect dependencies, dependency constraints, and exclude rules to be
 * used by resolvable and consumable configurations.
 *
 * @return A provider which creates a [DependencyScopeConfiguration] on Gradle 8.4+ and a
 * [LegacyConfiguration] on older versions.
 *
 * @throws InvalidUserDataException If a configuration with the given [name] already exists in this container.
 */
@Suppress("UnstableApiUsage")
internal fun ConfigurationContainer.registerDeclarable(
  name: String,
  configure: Configuration.() -> Unit = {},
): NamedDomainObjectProvider<out Configuration> = when {
  GradleVersion.current() >= MinNewConfigurationApi -> dependencyScope(name, configure)
  else -> register(name) { config ->
    if (GradleVersion.current() >= MinDeclarableApi) {
      config.isCanBeDeclared = true
    }
    config.isCanBeResolved = false
    config.isCanBeConsumed = false
    configure(config)
  }
}

/**
 * Registers a resolvable consumer configuration.
 *
 * Resolvable configurations resolve dependency graphs and their artifacts.
 *
 * @return A provider which creates a [ResolvableConfiguration] on Gradle 8.4+ and a
 * [LegacyConfiguration] on older versions.
 *
 * @throws InvalidUserDataException If a configuration with the given [name] already exists in this container.
 */
@Suppress("UnstableApiUsage")
internal fun ConfigurationContainer.registerResolvable(
  name: String,
  configure: Configuration.() -> Unit = {},
): NamedDomainObjectProvider<out Configuration> = when {
  GradleVersion.current() >= MinNewConfigurationApi -> resolvable(name, configure)
  else -> register(name) { config ->
    if (GradleVersion.current() >= MinDeclarableApi) {
      config.isCanBeDeclared = false
    }
    config.isCanBeResolved = true
    config.isCanBeConsumed = false
    configure(config)
  }
}

/**
 * Registers a consumable producer configuration.
 *
 * Consumable configurations expose built artifacts for other projects to consume.
 *
 * @return A provider which creates a [ConsumableConfiguration] on Gradle 8.4+ and a
 * [LegacyConfiguration] on older versions.
 *
 * @throws InvalidUserDataException If a configuration with the given [name] already exists in this container.
 */
@Suppress("UnstableApiUsage")
internal fun ConfigurationContainer.registerConsumable(
  name: String,
  configure: Configuration.() -> Unit = {},
): NamedDomainObjectProvider<out Configuration> = when {
  GradleVersion.current() >= MinNewConfigurationApi -> consumable(name, configure)
  else -> register(name) { config ->
    if (GradleVersion.current() >= MinDeclarableApi) {
      config.isCanBeDeclared = false
    }
    config.isCanBeResolved = false
    config.isCanBeConsumed = true
    configure(config)
  }
}

internal val PackageNameAttribute: Attribute<String> = Attribute.of("app.cash.sqldelight.packageName", String::class.java)
internal val DatabaseNameAttribute: Attribute<String> = Attribute.of("app.cash.sqldelight.database", String::class.java)
internal val SourceNameAttribute: Attribute<String> = Attribute.of("app.cash.sqldelight.source", String::class.java)
internal val PlatformTypeAttribute: Attribute<String> = Attribute.of("app.cash.sqldelight.platformType", String::class.java)

private val MinNewConfigurationApi = GradleVersion.version("8.4")
private val MinDeclarableApi = GradleVersion.version("8.2")
