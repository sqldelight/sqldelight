plugins {
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  compileOnly(libs.sqliteJdbc)

  implementation(libs.plugins.sqldelight.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
}
