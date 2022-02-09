package app.cash.sqlite.migrations

import schemacrawler.schema.Catalog
import schemacrawler.schemacrawler.SchemaCrawlerOptions
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder
import schemacrawler.utility.SchemaCrawlerUtility
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class CatalogDatabase private constructor(
  internal val catalog: Catalog
) : Database() {

  companion object {

    private val schemaCrawlerOptions = SchemaCrawlerOptions().apply {
      schemaInfoLevel = SchemaInfoLevelBuilder.maximum()
      routineTypes = emptyList() // SQLite does not support stored procedures ("routines" in JBDC)
    }

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
