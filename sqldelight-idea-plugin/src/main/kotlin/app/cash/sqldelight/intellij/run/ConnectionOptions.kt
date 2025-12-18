package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.dialect.api.ConnectionManager.ConnectionProperties
import com.intellij.ide.util.propComponentProperty
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class ConnectionOptions(val project: Project) {
  private var storedOptions: String by propComponentProperty(project, "stored_options", "")

  private fun <T> withCurrentDialect(action: DialectConnections.() -> T): T {
    val currentOptions =
      if (storedOptions.isEmpty()) {
        StoredOptions()
      } else {
        json.decodeFromString(StoredOptions.serializer(), storedOptions)
      }

    val dialectKey = SqlDelightProjectService.getInstance(project).dialect::class.qualifiedName
      ?: throw IllegalStateException("SqlDelightDialect subclass must be fully qualified.")
    val connections =
      currentOptions.dialectConnections.getOrPut(dialectKey) { DialectConnections() }
    val result = connections.action()

    storedOptions = json.encodeToString(StoredOptions.serializer(), currentOptions)
    return result
  }

  fun selectedOption() = withCurrentDialect { selectedOption }
  fun selectOption(key: String) = withCurrentDialect { selectedOption = key }
  fun unselectOption() = withCurrentDialect { selectedOption = null }
  fun removeOption(key: String) = withCurrentDialect {
    if (selectedOption == key) selectedOption = null
    orderedOptions.remove(key)
    serializedProperties.remove(key)
  }

  fun getKeys(): Collection<String> = withCurrentDialect { orderedOptions }

  fun reorderKeys(keys: List<String>) = withCurrentDialect {
    orderedOptions = keys.toMutableList()
  }

  fun addOption(properties: ConnectionProperties) = withCurrentDialect {
    serializedProperties[properties.key] = properties.serializedProperties
    orderedOptions.add(properties.key)
    selectedOption = properties.key
  }

  fun replaceOption(properties: ConnectionProperties) = withCurrentDialect {
    serializedProperties[properties.key] = properties.serializedProperties
  }

  fun currentOption() = withCurrentDialect {
    ConnectionProperties(selectedOption!!, serializedProperties[selectedOption]!!)
  }

  companion object {
    private val json = Json {
      ignoreUnknownKeys = true
    }
  }

  @Serializable
  internal class StoredOptions(
    val dialectConnections: MutableMap<String, DialectConnections> = mutableMapOf(),
  )

  @Serializable
  internal class DialectConnections(
    var selectedOption: String? = null,
    var orderedOptions: MutableList<String> = ArrayList(),
    val serializedProperties: MutableMap<String, String> = mutableMapOf(),
  )
}
