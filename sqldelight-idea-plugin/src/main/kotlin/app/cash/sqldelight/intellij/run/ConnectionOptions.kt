package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.dialect.api.ConnectionManager.ConnectionProperties
import com.intellij.ide.util.propComponentProperty
import com.intellij.openapi.project.Project
import com.squareup.moshi.Moshi

internal class ConnectionOptions(val project: Project) {
  private var options: String by propComponentProperty(project, "connection_options", "")
  var selectedOption: String by propComponentProperty(project, "selected_option", "")

  fun addOption(properties: ConnectionProperties) {
    val currentOptions = if (options.isEmpty()) StoredOptions() else adapter.fromJson(options)!!
    currentOptions.map[properties.key] = properties.serializedProperties
    options = adapter.toJson(currentOptions)
    selectedOption = properties.key
  }

  fun getKeys(): Collection<String> {
    return adapter.fromJson(options.ifEmpty { return emptyList() })!!.map.keys
  }

  fun selectedProperties(): ConnectionProperties {
    val currentOptions = adapter.fromJson(options)!!
    return ConnectionProperties(selectedOption, currentOptions.map[selectedOption]!!)
  }

  companion object {
    private val adapter = Moshi.Builder().build()
      .adapter(StoredOptions::class.java)
  }

  private class StoredOptions(
    val map: MutableMap<String, String> = linkedMapOf()
  )
}