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
package com.squareup.sqldelight.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.squareup.sqldelight.intellij.lang.SqlDelightFileViewProvider
import com.squareup.sqldelight.intellij.lang.SqliteContentIterator
import com.squareup.sqldelight.intellij.lang.SqliteFile
import com.squareup.sqldelight.types.SymbolTable

class SqlDelightStartupActivity : StartupActivity {
  override fun runActivity(project: Project) {
    val files = arrayListOf<SqliteFile>()
    VirtualFileManager.getInstance().addVirtualFileListener(SqlDelightVirtualFileListener())
    ApplicationManager.getApplication().runReadAction {
      ProjectRootManager.getInstance(project).fileIndex
          .iterateContent(SqliteContentIterator(PsiManager.getInstance(project)) { file ->
            files.add(file)
            true
          })
      files.forEach { file ->
        val manager = SqlDelightManager.getInstance(file) ?: return@forEach
          file.parseThen({ parsed ->
            manager.symbolTable += SymbolTable(parsed, file.virtualFile, file.relativePath)
          })
      }
      files.forEach { file ->
        ApplicationManager.getApplication().executeOnPooledThread {
          WriteCommandAction.runWriteCommandAction(project, {
            (file.viewProvider as SqlDelightFileViewProvider).generateJavaInterface()
          })
        }
      }
    }
  }
}
