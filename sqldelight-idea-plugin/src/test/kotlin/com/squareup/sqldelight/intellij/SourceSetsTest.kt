package com.squareup.sqldelight.intellij

import com.google.common.truth.Truth.assertThat
import com.squareup.sqldelight.core.lang.SqlDelightQueriesFile

class SourceSetsTest : SqlDelightProjectTestCase() {
  fun testDebugDoesntSeeRelease() {
    val debugFile = myFixture.configureFromTempProjectFile("src/debug/sqldelight/com/example/Debug.sq") as SqlDelightQueriesFile

    val filesSeen = mutableListOf<String>()
    debugFile.iterateSqlFiles {
      filesSeen.add(it.name)
    }
    assertThat(filesSeen).containsExactly("Main.sq", "Debug.sq")
  }

  fun testProductionDoesntSeeInternal() {
    val debugFile = myFixture.configureFromTempProjectFile("src/production/sqldelight/com/example/Production.sq") as SqlDelightQueriesFile

    val filesSeen = mutableListOf<String>()
    debugFile.iterateSqlFiles {
      filesSeen.add(it.name)
    }
    assertThat(filesSeen).containsExactly("Main.sq", "Production.sq")
  }

  fun testProductionReleaseSeesOnlyItsSourceSet() {
    val debugFile = myFixture.configureFromTempProjectFile("src/productionRelease/sqldelight/com/example/ProductionRelease.sq") as SqlDelightQueriesFile

    val filesSeen = mutableListOf<String>()
    debugFile.iterateSqlFiles {
      filesSeen.add(it.name)
    }
    assertThat(filesSeen).containsExactly("Main.sq", "Production.sq", "Release.sq", "ProductionRelease.sq")
  }
}
