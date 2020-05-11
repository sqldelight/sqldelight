package com.squareup.sqlite.migrations

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import schemacrawler.schema.Catalog
import schemacrawler.schemacrawler.SchemaCrawlerOptions
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder
import schemacrawler.utility.SchemaCrawlerUtility

class CatalogDatabase private constructor(
  internal val catalog: Catalog
) : Database() {

  companion object {

    private val schemaCrawlerOptions = SchemaCrawlerOptions().apply {
      schemaInfoLevel = SchemaInfoLevelBuilder.maximum()
      routineTypes = emptyList() // SQLite does not support stored procedures ("routines" in JBDC)
    }

    fun withInitStatements(initStatements: List<String>): CatalogDatabase {
      return fromFile("", initStatements)
    }

    fun fromFile(path: String, initStatements: List<String>): CatalogDatabase {
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

    private fun Connection.init(initStatements: List<String>) = apply {
      initStatements.forEach { sqlText ->
        prepareStatement(sqlText).execute()
      }
    }
  }
}
