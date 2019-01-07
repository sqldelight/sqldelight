package com.squareup.sqldelight.gradle

import org.gradle.api.file.FileCollection
import java.io.File

open class SqlDelightExtension(
  var packageName: String? = null,
  var className: String? = null,
  var sourceSet: FileCollection? = null,
  var schemaOutputDirectory: File? = null
)