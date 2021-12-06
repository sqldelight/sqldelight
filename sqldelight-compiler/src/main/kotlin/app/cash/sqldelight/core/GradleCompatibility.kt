package app.cash.sqldelight.core

import app.cash.sqldelight.VERSION

object GradleCompatibility {
  fun validate(propertiesFile: SqlDelightPropertiesFile): CompatibilityReport {
    val (minimumIdeVersion, currentGradleVersion) = try {
      SemVer(propertiesFile.minimumSupportedVersion) to SemVer(propertiesFile.currentVersion)
    } catch (e: Throwable) {
      // If we can't even read the properties file version, it is not compatibile
      return CompatibilityReport.Incompatible(
        reason = "Plugin 'SQLDelight' is incompatible with the current version of the SQLDelight Gradle plugin. Upgrade the version of app.cash.sqldelight:gradle-plugin.",
      )
    }

    val currentVersion = SemVer(VERSION)

    if (currentVersion < minimumIdeVersion) {
      return CompatibilityReport.Incompatible(
        reason = "Plugin 'SQLDelight' is no longer compatible with the current version of the SQLDelight Gradle plugin. Use version $minimumIdeVersion or later.",
      )
    }

    val minimumGradleVersion = SemVer(MINIMUM_SUPPORTED_VERSION)

    if (currentGradleVersion < minimumGradleVersion) {
      return CompatibilityReport.Incompatible(
        reason = "Gradle plugin 'app.cash.sqldelight:gradle-plugin' is incompatible with the current SQLDelight IntelliJ Plugin version. Use version $minimumGradleVersion or later."
      )
    }

    return CompatibilityReport.Compatible
  }

  private data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val snapshot: Boolean,
  ) {
    constructor(semVerText: String) : this(
      major = semVerText.split('.')[0].toInt(),
      minor = semVerText.split('.')[1].toInt(),
      patch = semVerText.split('.')[2].removeSuffix("-SNAPSHOT").toInt(),
      snapshot = semVerText.endsWith("-SNAPSHOT"),
    )

    operator fun compareTo(other: SemVer): Int {
      val majorCompare = major.compareTo(other.major)
      if (majorCompare != 0) return majorCompare
      val minorCompare = minor.compareTo(other.minor)
      if (minorCompare != 0) return minorCompare
      return patch.compareTo(other.patch)
    }

    override fun toString() = "$major.$minor.$patch"
  }

  sealed class CompatibilityReport {
    object Compatible : CompatibilityReport()
    data class Incompatible(
      val reason: String
    ) : CompatibilityReport()
  }
}
