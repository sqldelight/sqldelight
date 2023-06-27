plugins {
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  implementation(libs.plugins.sqldelight.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
}
