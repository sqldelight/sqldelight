package app.cash.sqldelight.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

interface ConnectionManager {
  fun getConnection(path: String): Connection

  companion object {
    fun getInstance(project: Project): ConnectionManager {
      val dialect = SqlDelightProjectService.getInstance(project).dialect
      if (dialect.isSqlite) {
        return ServiceManager.getService(project, SqliteConnectionManager::class.java)!!
      } else {
        throw IllegalArgumentException("Unsupported dialect")
      }
    }
  }
}

@Service
internal class SqliteConnectionManager : ConnectionManager {

  override fun getConnection(path: String): Connection {
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
