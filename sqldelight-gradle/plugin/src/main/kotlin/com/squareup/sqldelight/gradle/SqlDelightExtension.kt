package com.squareup.sqldelight.gradle

import org.gradle.api.file.FileCollection

open class SqlDelightExtension(
  var packageName: String? = null,
  var sourceSet: FileCollection? = null
)