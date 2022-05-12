package app.cash.sqldelight.dialects.sqlite_3_18

import app.cash.sqldelight.dialect.api.ConnectionManager
import app.cash.sqldelight.dialect.api.ConnectionManager.ConnectionProperties
import com.intellij.openapi.project.Project
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class SqliteConnectionManager : ConnectionManager {
  override fun createNewConnectionProperties(
    project: Project,
    prefilledProperties: ConnectionProperties?
  ): ConnectionProperties? {
    val dialog = SelectConnectionTypeDialog(project)
    if (!dialog.showAndGet()) return null
    return dialog.connectionProperties()
  }

  override fun getConnection(connectionProperties: ConnectionProperties): Connection {
    val path = connectionProperties.serializedProperties
    val previousContextLoader = Thread.currentThread().contextClassLoader
    return try {
      // When it iterates the ServiceLoader we want to make sure its on the plugins classpath.
      Thread.currentThread().contextClassLoader = this::class.java.classLoader
      DriverManager.getConnection("jdbc:sqlite:$path")
    } catch (e: SQLException) {
      DriverManager.getConnection("jdbc:sqlite:$path")
    } finally {
      Thread.currentThread().contextClassLoader = previousContextLoader
    }
  }
}
