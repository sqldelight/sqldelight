package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.intellij.util.dialectPreset
import app.cash.sqldelight.intellij.util.isSqlite
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

internal interface ConnectionManager {
  fun getConnection(): Connection

  companion object {
    fun getInstance(project: Project): ConnectionManager {
      return ServiceManager.getService(project, ConnectionManager::class.java)!!
    }
  }
}

internal class ConnectionManagerImpl(private val project: Project) : ConnectionManager {

  private val connectionOptions = ConnectionOptions(project)

  init {
    Class.forName("org.sqlite.JDBC")
  }

  override fun getConnection(): Connection {
    val dialect = project.dialectPreset
    if (!dialect.isSqlite) {
      throw SQLException("Unsupported dialect $dialect")
    }

    val connectionType = connectionOptions.connectionType
    if (connectionType != ConnectionType.FILE) {
      throw SQLException("Unsupported connection type $connectionType")
    }

    val filePath = connectionOptions.filePath
    if (filePath.isEmpty()) {
      throw SQLException("The file path is empty")
    }

    return DriverManager.getConnection("jdbc:sqlite:$filePath")
  }
}
