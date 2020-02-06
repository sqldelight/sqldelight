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

import com.alecstrong.sqlite.psi.core.DialectPreset
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.File

@JsonClass(generateAdapter = true)
class SqlDelightPropertiesFile(
  val databases: List<SqlDelightDatabaseProperties>
) {
  fun toFile(file: File) {
    file.writeText(adapter.toJson(this))
  }

  companion object {
    const val NAME = ".sqldelight"

    private val adapter by lazy {
      val moshi = Moshi.Builder().build()

      return@lazy moshi.adapter(SqlDelightPropertiesFile::class.java)
    }

    fun fromFile(file: File): SqlDelightPropertiesFile = fromText(file.readText())!!
    fun fromText(text: String) = adapter.fromJson(text)
  }
}

@JsonClass(generateAdapter = true)
data class SqlDelightDatabaseProperties(
  val packageName: String,
  val compilationUnits: List<SqlDelightCompilationUnit>,
  val outputDirectory: String,
  val className: String,
  val dependencies: List<SqlDelightDatabaseName>,
  val dialectPreset: DialectPreset = DialectPreset.SQLITE
)

/**
 * A compilation unit represents the group of .sq files which will be compiled all at once. A
 * single database can have multiple compilation units, depending on which gradle task is invoked.
 *
 * For example, a multiplatform module has separate compilation units for ios and android. An
 * android module has separate compilation units for different variants. Only one compilation unit
 * will be worked on during compilation time.
 */
@JsonClass(generateAdapter = true)
data class SqlDelightCompilationUnit(
  val name: String,
  val sourceFolders: List<SqlDelightSourceFolder>
)

@JsonClass(generateAdapter = true)
data class SqlDelightSourceFolder(
  val path: String,
  val dependency: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SqlDelightDatabaseName(
  val packageName: String,
  val className: String
)