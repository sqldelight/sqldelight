package app.cash.sqldelight.tests

import app.cash.sqldelight.gradle.DatabaseNameAttribute
import app.cash.sqldelight.gradle.PlatformTypeAttribute
import app.cash.sqldelight.gradle.SourceNameAttribute
import app.cash.sqldelight.gradle.configureSqlDelightAttributesSchema
import com.google.common.truth.Truth.assertThat
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.junit.Before
import org.junit.Test

class SchemaAttributesTest {
  private lateinit var project: Project
  private lateinit var declarable: Configuration

  @Before fun setup() {
    project = ProjectBuilder.builder().build()
    project.dependencies.configureSqlDelightAttributesSchema()
    declarable = project.configurations.create("declarable") {
      it.isCanBeResolved = false
      it.isCanBeConsumed = false
      it.dependencies.add(project.dependencies.create(project))
    }
  }

  @Test fun `platformType compatibility - androidJvm can resolve jvm`() {
    val consumable = createConsumable {
      attributes.attribute(PlatformTypeAttribute, KotlinPlatformType.jvm.name)
    }
    val resolvable = createResolvable {
      attributes.attribute(PlatformTypeAttribute, KotlinPlatformType.androidJvm.name)
    }

    assertThat(resolvable.resolveSingleConfiguration()).isEqualTo(consumable.name)
  }

  @Test fun `platformType compatibility - jvm cannot resolve androidJvm`() {
    createConsumable {
      attributes.attribute(PlatformTypeAttribute, KotlinPlatformType.androidJvm.name)
    }
    val resolvable = createResolvable {
      attributes.attribute(PlatformTypeAttribute, KotlinPlatformType.jvm.name)
    }

    assertThat(
      resolvable
        .resolvedConfiguration
        .lenientConfiguration
        .firstLevelModuleDependencies
        .isEmpty(),
    ).isTrue()
  }

  @Test fun `platformType compatibility - common producer is always compatible`() {
    val consumable = createConsumable {
      attributes.attribute(PlatformTypeAttribute, KotlinPlatformType.common.name)
    }
    val resolvable = createResolvable {
      attributes.attribute(PlatformTypeAttribute, KotlinPlatformType.jvm.name)
    }

    assertThat(resolvable.resolveSingleConfiguration()).isEqualTo(consumable.name)
  }

  @Test fun `source compatibility - commonMain producer is always compatible`() {
    val consumable = createConsumable { attributes.attribute(SourceNameAttribute, "commonMain") }
    val resolvable = createResolvable { attributes.attribute(SourceNameAttribute, "demo") }

    assertThat(resolvable.resolveSingleConfiguration()).isEqualTo(consumable.name)
  }

  @Test fun `multiple candidates resolve to the correct variant`() {
    // This has three exact matches with nativeResolvable, but has a different database name.
    createConsumable("nativeConsumable") {
      attributes.apply {
        attributes.attribute(DatabaseNameAttribute, "Other")
        attributes.attribute(SourceNameAttribute, "iosMain")
        attributes.attribute(PlatformTypeAttribute, KotlinPlatformType.native.name)
      }
    }
    // This has two exact matches, and two compatible matches with nativeResolvable.
    val commonConsumable = createConsumable("commonConsumable") {
      attributes.apply {
        commonDatabaseAttribute()
        attribute(SourceNameAttribute, "commonMain")
        attribute(PlatformTypeAttribute, KotlinPlatformType.common.name)
      }
    }

    val jvmConsumable = createConsumable("jvmConsumable") {
      attributes.apply {
        commonDatabaseAttribute()
        attribute(SourceNameAttribute, "main")
        attribute(PlatformTypeAttribute, KotlinPlatformType.jvm.name)
      }
    }

    // Will lose to jvmConsumable due to non-matching source.
    createConsumable("androidDebugConsumable") {
      attributes.apply {
        commonDatabaseAttribute()
        attribute(SourceNameAttribute, "debug")
        attribute(PlatformTypeAttribute, KotlinPlatformType.androidJvm.name)
      }
    }

    val androidResolvable = createResolvable("androidResolvable") {
      attributes.apply {
        commonDatabaseAttribute()
        attribute(SourceNameAttribute, "main")
        attribute(PlatformTypeAttribute, KotlinPlatformType.androidJvm.name)
      }
    }

    // androidResolvable matches to jvmConsumable due to the matching source and compatible platform type.
    assertThat(androidResolvable.resolveSingleConfiguration()).isEqualTo(jvmConsumable.name)

    val nativeResolvable = createResolvable("nativeResolvable") {
      attributes.apply {
        commonDatabaseAttribute()
        attribute(SourceNameAttribute, "iosMain")
        attribute(PlatformTypeAttribute, KotlinPlatformType.native.name)
      }
    }

    // Even though nativeResolvable has three exact matches with nativeConsumable, it loses
    // to commonConsumable as it has four compatible matches.
    assertThat(nativeResolvable.resolveSingleConfiguration()).isEqualTo(commonConsumable.name)
  }

  private fun createConsumable(
    name: String = "producer",
    block: Configuration.() -> Unit = {},
  ): Configuration = project.configurations.create(name, block).apply {
    isCanBeResolved = false
    extendsFrom(declarable)
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, "sqldelight-schema"))
  }

  private fun createResolvable(
    name: String = "consumer",
    block: Configuration.() -> Unit = {},
  ): Configuration = project.configurations.create(name, block).apply {
    isCanBeConsumed = false
    extendsFrom(declarable)
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, "sqldelight-schema"))
  }

  private fun AttributeContainer.commonDatabaseAttribute() {
    attribute(DatabaseNameAttribute, "CommonDb")
  }

  private fun Configuration.resolveSingleConfiguration(): String = resolvedConfiguration
    .firstLevelModuleDependencies
    .single()
    .configuration
}
