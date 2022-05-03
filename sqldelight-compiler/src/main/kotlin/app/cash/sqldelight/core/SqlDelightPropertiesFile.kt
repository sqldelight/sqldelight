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
package app.cash.sqldelight.core

import java.io.File
import java.io.Serializable

val MINIMUM_SUPPORTED_VERSION = "2.0.0"

interface SqlDelightPropertiesFile : Serializable {
  val databases: List<SqlDelightDatabaseProperties>
  val dialectJar: File
  val moduleJars: Collection<File>
  val minimumSupportedVersion: String
  val currentVersion: String
}

interface SqlDelightDatabaseProperties : Serializable {
  val packageName: String
  val compilationUnits: List<SqlDelightCompilationUnit>
  val className: String
  val dependencies: List<SqlDelightDatabaseName>
  val deriveSchemaFromMigrations: Boolean
  val treatNullAsUnknownForEquality: Boolean
  val rootDirectory: File
}

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
  val outputDirectoryFile: File
}

interface SqlDelightSourceFolder : Serializable {
  val folder: File
  val dependency: Boolean
}

interface SqlDelightDatabaseName : Serializable {
  val packageName: String
  val className: String
}
