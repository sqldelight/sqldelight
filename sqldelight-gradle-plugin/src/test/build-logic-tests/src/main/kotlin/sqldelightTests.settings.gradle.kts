pluginManagement {
    repositories {
        maven(url = "file://${settingsDir.absolutePath}/../../../../build/localMaven")
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
  versionCatalogs.register("libs") {
    from(files("../../../../gradle/libs.versions.toml"))

    val overwriteKotlinVersion: String? by settings
    if (overwriteKotlinVersion != null) {
      version("kotlin", overwriteKotlinVersion!!)
    }

    // This version is set in the GradleRunner during test setup using the current (SNAPSHOT) version.
    // If you want to use the test projects as standalone samples, link the Gradle project in IntelliJ
    // and overwrite this version.
    val sqldelightVersion: String by settings
    plugin("sqldelight", "app.cash.sqldelight").version(sqldelightVersion)
  }

  repositories {
    maven(url = "file://${rootDir}/../../../../build/localMaven")
    mavenCentral()
    google()
  }
}
