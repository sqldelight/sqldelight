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
package app.cash.sqldelight.core.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType

const val SQLDELIGHT_EXTENSION = "sq"

object SqlDelightFileType : LanguageFileType(SqlDelightLanguage) {
  private val ICON = AllIcons.Providers.Sqlite

  override fun getName() = "SqlDelight"
  override fun getDescription() = "SqlDelight"
  override fun getDefaultExtension() = SQLDELIGHT_EXTENSION
  override fun getIcon() = ICON
}
