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

import app.cash.sqldelight.dialect.api.SqlDelightDialect
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface SqlDelightProjectService {
  var dialect: SqlDelightDialect
  var treatNullAsUnknownForEquality: Boolean

  fun setDialect(dialect: SqlDelightDialect, shouldInvalidate: Boolean) {
    this.dialect = dialect
  }

  fun module(vFile: VirtualFile): Module?

  fun fileIndex(module: Module): SqlDelightFileIndex

  fun resetIndex()

  fun clearIndex()

  companion object {
    fun getInstance(project: Project): SqlDelightProjectService {
      return ServiceManager.getService(project, SqlDelightProjectService::class.java)!!
    }
  }
}
