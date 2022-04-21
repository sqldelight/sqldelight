package app.cash.sqldelight.dialect.api

import com.intellij.openapi.project.Project
import java.sql.Connection

interface ConnectionManager {
  fun createNewConnectionProperties(project: Project): ConnectionProperties?

  fun getConnection(connectionProperties: ConnectionProperties): Connection

  data class ConnectionProperties(
    val key: String,
    val serializedProperties: String
  )
}
