package app.cash.sqlite.migrations

import org.sqlite.SQLiteDataSource
import schemacrawler.schema.Catalog
import schemacrawler.schemacrawler.LimitOptionsBuilder
import schemacrawler.schemacrawler.LoadOptionsBuilder
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder
import schemacrawler.tools.utility.SchemaCrawlerUtility
import us.fatehi.utility.datasource.DatabaseConnectionSource
import us.fatehi.utility.datasource.DatabaseConnectionSources
import java.sql.Connection
import java.sql.SQLException

class CatalogDatabase private constructor(
  internal val catalog: Catalog,
) : Database() {

  companion object {

    private val schemaCrawlerOptions = SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
      .withLimitOptions(
        LimitOptionsBuilder.builder()
          .routineTypes(emptyList())
          .toOptions(),
      )
      .withLoadOptions(
        LoadOptionsBuilder.builder()
          .withSchemaInfoLevel(SchemaInfoLevelBuilder.maximum())
          .toOptions(),
      )

    fun withInitStatements(initStatements: List<InitStatement>): CatalogDatabase {
      return fromFile("", initStatements)
    }

    fun fromFile(path: String, initStatements: List<InitStatement>): CatalogDatabase {
      return createConnection(path).also { it.get().init(initStatements) }.use {
        CatalogDatabase(SchemaCrawlerUtility.getCatalog(it, schemaCrawlerOptions))
      }
    }

    private fun createConnection(path: String): DatabaseConnectionSource {
      return try {
        DatabaseConnectionSources.fromDataSource(SQLiteDataSource().apply { url = "jdbc:sqlite:$path" })
      } catch (e: SQLException) {
        DatabaseConnectionSources.fromDataSource(SQLiteDataSource().apply { url = "jdbc:sqlite:$path" })
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
