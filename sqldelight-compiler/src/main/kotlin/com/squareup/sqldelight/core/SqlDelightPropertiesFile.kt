/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.core

import com.alecstrong.sql.psi.core.DialectPreset
import java.io.File
import java.io.Serializable

interface SqlDelightPropertiesFile : Serializable {
  val databases: List<SqlDelightDatabaseProperties>
}

interface SqlDelightDatabaseProperties : Serializable {
  val packageName: String
  val compilationUnits: List<SqlDelightCompilationUnit>
  val className: String
  val dependencies: List<SqlDelightDatabaseName>
  val dialectPresetName: String
  val deriveSchemaFromMigrations: Boolean
  val outputDirectoryFile: List<SqlDelightSourceDirectory>
  val rootDirectory: File
}

data class SqlDelightSourceDirectory(
        val name: String,
        val sourcePath: String
) : Serializable

/**
 * Each sourceSet has its source directory, but in ide we can only pick one of them as source.
 * By default we use debug source flavor.
 */
const val DEFAULT_SOURCE_TYPE = "debug"

val SqlDelightDatabaseProperties.dialectPreset: DialectPreset
  get() = DialectPreset.valueOf(dialectPresetName)

/**
 * A compilation unit represents the group of .sq files which will be compiled all at once. A
 * single database can have multiple compilation units, depending on which gradle task is invoked.
 *
 * For example, a multiplatform module has separate compilation units for ios and android. An
 * android module has separate compilation units for different variants. Only one compilation unit
 * will be worked on during compilation time.
 */
interface SqlDelightCompilationUnit : Serializable {
  val name: String
  val sourceFolders: List<SqlDelightSourceFolder>
}

interface SqlDelightSourceFolder : Serializable {
  val folder: File
  val dependency: Boolean
}

interface SqlDelightDatabaseName : Serializable {
  val packageName: String
  val className: String
}
