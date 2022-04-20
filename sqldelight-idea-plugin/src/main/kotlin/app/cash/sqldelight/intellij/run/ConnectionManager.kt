package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.SqlDelightProjectService
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import java.sql.Connection
import java.sql.DriverManager

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
    if (!SqlDelightProjectService.getInstance(project).dialect.isSqlite) {
      throw IllegalArgumentException("Unsupported dialect")
    }

    val connectionType = connectionOptions.connectionType
    if (connectionType != ConnectionType.FILE) {
      throw IllegalArgumentException("Unsupported connection type $connectionType")
    }

    val filePath = connectionOptions.filePath
    if (filePath.isEmpty()) {
      throw IllegalArgumentException("The file path is empty")
    }

    return DriverManager.getConnection("jdbc:sqlite:$filePath")
  }
}
