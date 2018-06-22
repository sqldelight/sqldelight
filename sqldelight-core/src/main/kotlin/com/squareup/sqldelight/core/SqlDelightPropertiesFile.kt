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

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.File

@JsonClass(generateAdapter = true)
class SqlDelightPropertiesFile(
  val packageName: String,
  val sourceSets: List<List<String>>,
  val outputDirectory: String
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
