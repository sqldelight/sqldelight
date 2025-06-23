package app.cash.sqlite.migrations

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import schemacrawler.schema.Catalog
import schemacrawler.schemacrawler.LimitOptionsBuilder
import schemacrawler.schemacrawler.LoadOptionsBuilder
import schemacrawler.schemacrawler.SchemaCrawlerOptions
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder
import schemacrawler.schemacrawler.SchemaInfoLevel
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder
import schemacrawler.tools.utility.SchemaCrawlerUtility

class CatalogDatabase private constructor(
  internal val catalog: Catalog,
) : Database() {

  companion object {

    private fun createSchemaCrawlerOptions(migrationVerificationLevel: String): SchemaCrawlerOptions {
      return SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
        .withLimitOptions(
          LimitOptionsBuilder.builder()
            .routineTypes(emptyList())
            .toOptions(),
        )
        .withLoadOptions(
          LoadOptionsBuilder.builder()
            .withSchemaInfoLevel(createSchemaInfoLevel(migrationVerificationLevel))
            .toOptions(),
        )
    }

    fun withInitStatements(initStatements: List<InitStatement>, migrationVerificationLevel: String = "maximum"): CatalogDatabase {
      return fromFile("", initStatements, migrationVerificationLevel)
    }

    fun fromFile(path: String, initStatements: List<InitStatement>, migrationVerificationLevel: String = "maximum"): CatalogDatabase {
      return createConnection(path).init(initStatements).use {
        CatalogDatabase(SchemaCrawlerUtility.getCatalog(it, createSchemaCrawlerOptions(migrationVerificationLevel)))
      }
    }

    private fun createConnection(path: String): Connection {
      return try {
        DriverManager.getConnection("jdbc:sqlite:$path")
      } catch (e: SQLException) {
        DriverManager.getConnection("jdbc:sqlite:$path")
      }
    }

    private fun createSchemaInfoLevel(migrationVerificationLevel: String): SchemaInfoLevel {
      return when (migrationVerificationLevel.lowercase()) {
        "minimum" -> SchemaInfoLevelBuilder.minimum()
        "standard" -> SchemaInfoLevelBuilder.standard()
        "detailed" -> SchemaInfoLevelBuilder.detailed()
        "maximum" -> SchemaInfoLevelBuilder.maximum()
        else -> throw IllegalArgumentException("Invalid migrationVerificationLevel, must be one of: minimum, standard, detailed, maximum")
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
