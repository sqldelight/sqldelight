/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.sqldelight.intellij.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingRegistry
import com.squareup.sqldelight.SqliteCompiler

class SqliteFileType private constructor() : LanguageFileType(SqliteLanguage.INSTANCE) {
  override fun getName() = "Sqlite"
  override fun getDescription() = "Sqlite"
  override fun getDefaultExtension() = SqliteCompiler.FILE_EXTENSION
  override fun getIcon() = ICON
  override fun getCharset(file: VirtualFile, content: ByteArray) =
      (EncodingRegistry.getInstance().getDefaultCharsetForPropertiesFiles(file) ?:
          CharsetToolkit.getDefaultSystemCharset()).name()

  companion object {
    private val ICON = IconLoader.getIcon("/icons/sqlite.png")

    val INSTANCE: LanguageFileType = SqliteFileType()
  }
}
