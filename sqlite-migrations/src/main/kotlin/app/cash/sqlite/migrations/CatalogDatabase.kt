package app.cash.sqlite.migrations

import schemacrawler.schema.Catalog
import schemacrawler.schema.Table
import schemacrawler.schemacrawler.LimitOptionsBuilder
import schemacrawler.schemacrawler.LoadOptionsBuilder
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder
import schemacrawler.tools.formatter.serialize.JsonSerializedCatalog
import schemacrawler.tools.utility.SchemaCrawlerUtility
import java.io.File
import java.io.ObjectOutputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class CatalogDatabase private constructor(
  internal val catalog: Catalog
) : Database() {

  fun serialize(output: File) {
    output.outputStream().use {
      ObjectOutputStream(it).use { it.writeObject(catalog) }
    }
  }

  fun toJson(output: File) {
    output.outputStream().use {
      val jsonCatalog = JsonSerializedCatalog(catalog)
      jsonCatalog.save(it)
    }
  }

  private val replaceMermaidType = """\([\d ,]+\)|\[[\d ,]+]|\s+""".toRegex()
  private val Table.mermaidName get() = fullName.replace(".", "-")

  fun mermaidDiagram(output: File) {
    val diagramm = buildString {
      appendLine("erDiagram")
      for (table in catalog.tables) {
        appendLine("${table.mermaidName} {")
        for (column in table.columns) {
          val type = column.columnDataType.name.replace(replaceMermaidType, "")
          append(type)
          append(" ${column.name}")
          when {
            column.isPartOfPrimaryKey -> append(" PK")
            column.isPartOfForeignKey -> append(" FK")
          }
          appendLine()
        }
        appendLine("}")
      }
      for (table in catalog.tables) {
        for (childTable in table.referencingTables) {
          appendLine("${table.mermaidName} ||--o{ ${childTable.mermaidName}")
        }
      }
    }
    output.writeText(diagramm)
  }

  companion object {

    private val schemaCrawlerOptions = SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
      .withLimitOptions(
        LimitOptionsBuilder.builder()
          .routineTypes(emptyList())
          .toOptions()
      )
      .withLoadOptions(
        LoadOptionsBuilder.builder()
          .withSchemaInfoLevel(SchemaInfoLevelBuilder.maximum())
          .toOptions()
      )

    fun withInitStatements(initStatements: List<InitStatement>): CatalogDatabase {
      return fromFile("", initStatements)
    }

    fun fromFile(path: String, initStatements: List<InitStatement>): CatalogDatabase {
      return createConnection(path).init(initStatements).use {
        CatalogDatabase(SchemaCrawlerUtility.getCatalog(it, schemaCrawlerOptions))
      }
    }

    private fun createConnection(path: String): Connection {
      return try {
        DriverManager.getConnection("jdbc:sqlite:$path")
      } catch (e: SQLException) {
        DriverManager.getConnection("jdbc:sqlite:$path")
      }
    }

    private fun Connection.init(initStatements: List<InitStatement>) = apply {
      initStatements.forEach { (sqlText, fileLocation) ->
        try {
          prepareStatement(sqlText).execute()
        } catch (e: Throwable) {
          throw IllegalStateException("Error compiling $fileLocation", e)
        }
      }
    }
  }

  data class InitStatement(
    val statement: String,
    val fileLocation: String,
  )
}
