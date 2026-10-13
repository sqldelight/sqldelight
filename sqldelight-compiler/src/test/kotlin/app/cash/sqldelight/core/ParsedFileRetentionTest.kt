package app.cash.sqldelight.core

import app.cash.sqldelight.test.util.FixtureCompiler
import app.cash.sqldelight.test.util.fixtureRoot
import com.alecstrong.sql.psi.core.SqlCoreEnvironment
import com.alecstrong.sql.psi.core.SqlFileBase
import com.google.common.truth.Truth.assertThat
import java.lang.ref.WeakReference
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ParsedFileRetentionTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test fun `query only files survive garbage collection after generation`() {
    FixtureCompiler.writeSql(
      """
      |CREATE TABLE player(
      |  id INTEGER NOT NULL PRIMARY KEY,
      |  name TEXT NOT NULL
      |);
      """.trimMargin(),
      tempFolder,
      "Schema.sq",
    )
    FixtureCompiler.writeSql(
      """
      |insertPlayer:
      |INSERT INTO player VALUES (?, ?);
      |
      |selectAll:
      |SELECT * FROM player;
      """.trimMargin(),
      tempFolder,
      "Queries.sq",
    )

    val result = FixtureCompiler.generateFixture(tempFolder.fixtureRoot().path)
    assertThat(result.errors).isEmpty()

    val file = WeakReference(result.environment.findFile("Queries.sq"))
    val tree = WeakReference(file.get()!!.node)

    forceGarbageCollection()

    assertThat(file.get()).isNotNull()
    assertThat(tree.get()).isNotNull()
    val reloaded = result.environment.findFile("Queries.sq")
    assertThat(reloaded).isSameInstanceAs(file.get())
    assertThat(reloaded.node).isSameInstanceAs(tree.get())
  }

  private fun SqlCoreEnvironment.findFile(name: String): SqlFileBase {
    var file: SqlFileBase? = null
    forSourceFiles<SqlFileBase> { if (it.name == name) file = it }
    return file!!
  }

  private fun forceGarbageCollection() {
    val sentinel = unreachableSentinel()
    while (sentinel.get() != null) {
      System.gc()
      Thread.sleep(100)
    }
    System.gc()
  }

  private fun unreachableSentinel() = WeakReference(Any())
}
