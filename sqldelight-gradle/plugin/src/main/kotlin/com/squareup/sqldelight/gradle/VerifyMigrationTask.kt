package com.squareup.sqldelight.gradle

import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.core.SqlDelightEnvironment
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.util.forInitializationStatements
import com.squareup.sqldelight.core.lang.util.rawSqlText
import de.danielbechler.diff.ObjectDifferBuilder
import de.danielbechler.diff.node.DiffNode
import de.danielbechler.diff.node.DiffNode.State.ADDED
import de.danielbechler.diff.node.DiffNode.State.REMOVED
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import schemacrawler.schema.Catalog
import schemacrawler.schema.CrawlInfo
import schemacrawler.schema.JdbcDriverInfo
import schemacrawler.schemacrawler.SchemaCrawlerOptions
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder
import schemacrawler.utility.SchemaCrawlerUtility
import java.io.File
import java.sql.DriverManager
import java.sql.SQLException

open class VerifyMigrationTask : SourceTask() {
  @Suppress("unused") // Required to invalidate the task on version updates.
  @Input fun pluginVersion() = VERSION

  lateinit var sourceFolders: Iterable<File>

  private val schemaCrawlerOptions = SchemaCrawlerOptions().apply {
    schemaInfoLevel = SchemaInfoLevelBuilder.maximum()
  }

  private val environment by lazy {
    SqlDelightEnvironment(sourceFolders = sourceFolders.filter { it.exists() })
  }

  @TaskAction
  fun verifyMigrations() {
    val currentDb = try {
      DriverManager.getConnection("jdbc:sqlite:")
    } catch (e: SQLException) {
      DriverManager.getConnection("jdbc:sqlite:")
    }

    val sourceFiles = ArrayList<SqlDelightFile>()
    environment.forSourceFiles { file -> sourceFiles.add(file as SqlDelightFile) }
    sourceFiles.forInitializationStatements { sqlText ->
      currentDb.prepareStatement(sqlText).execute()
    }
    val catalog = SchemaCrawlerUtility.getCatalog(currentDb, schemaCrawlerOptions)

    val folders = sourceFolders.toMutableList()
    while (folders.isNotEmpty()) {
      val folder = folders.removeAt(0)
      folder.listFiles()?.forEach { file ->
        if (file.name.endsWith(".db")) {
          checkMigration(file, catalog)
        } else if (file.isDirectory) {
          folders.add(file)
        }
      }
    }
  }

  private fun checkMigration(dbFile: File, currentDb: Catalog) {
    val version = dbFile.nameWithoutExtension.toInt()
    val copy = dbFile.copyTo(File("${project.buildDir}/sqldelight/${dbFile.name}"))
    val connection = DriverManager.getConnection("jdbc:sqlite:${copy.absolutePath}")
    environment.forMigrationFiles {
      if (version > it.version) return@forMigrationFiles
      it.sqlStmtList!!.statementList.forEach {
        connection.prepareStatement(it.rawSqlText()).execute()
      }
    }
    val actualCatalog = SchemaCrawlerUtility.getCatalog(connection, schemaCrawlerOptions)

    val diff = differBuilder().compare(currentDb, actualCatalog)

    val sb = StringBuilder()
    diff.visit { node, visit ->
      if (CrawlInfo::class.java.isAssignableFrom(node.valueType) ||
          JdbcDriverInfo::class.java.isAssignableFrom(node.valueType)) {
        visit.dontGoDeeper()
        return@visit
      }
      if (node.childCount() == 0) {
        sb.append("${node.path} - ${node.state}\n")
      } else if (node.state == ADDED || node.state == REMOVED) {
        sb.append("${node.path} - ${node.state}\n")
        visit.dontGoDeeper()
      }
    }
    try {
      if (sb.isNotEmpty()) {
        throw IllegalStateException("Error migrating from ${dbFile.name}, fresh database looks" +
            " different from migration database:\n$sb")
      }
    } finally {
      copy.delete()
      connection.close()
    }
  }

  private fun differBuilder() = ObjectDifferBuilder.startBuilding().apply {
    filtering().omitNodesWithState(DiffNode.State.UNTOUCHED)
    filtering().omitNodesWithState(DiffNode.State.CIRCULAR)
    inclusion().exclude().apply {
      propertyName("fullName")
      propertyName("parent")
      propertyName("exportedForeignKeys")
      propertyName("importedForeignKeys")
      propertyName("deferrable")
      propertyName("initiallyDeferred")
      // Definition changes aren't important, its just things like comments or whitespace.
      propertyName("definition")
    }
    // Partial columns are used for unresolved columns to avoid cycles. Matching based on string
    // is fine for our purposes.
    comparison().ofType(Class.forName("schemacrawler.crawl.ColumnPartial")).toUseEqualsMethod()
  }.build()
}
