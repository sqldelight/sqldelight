package app.cash.sqlite.migrations

import schemacrawler.schema.Catalog
import schemacrawler.schemacrawler.LimitOptionsBuilder
import schemacrawler.schemacrawler.LoadOptionsBuilder
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder
import schemacrawler.tools.utility.SchemaCrawlerUtility
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class CatalogDatabase private constructor(
  internal val catalog: Catalog
) : Database() {

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
