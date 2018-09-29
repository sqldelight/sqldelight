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
package com.squareup.sqldelight.core.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader

object SqlDelightFileType : LanguageFileType(SqlDelightLanguage) {
  private val ICON = try {
    IconLoader.getIcon("/icons/sqldelight.png")
  } catch (e : Throwable) {
    null
  }

  const val EXTENSION = "sq"
  const val FOLDER_NAME = "sqldelight"

  override fun getName() = "SqlDelight"
  override fun getDescription() = "SqlDelight"
  override fun getDefaultExtension() = EXTENSION
  override fun getIcon() = ICON
}