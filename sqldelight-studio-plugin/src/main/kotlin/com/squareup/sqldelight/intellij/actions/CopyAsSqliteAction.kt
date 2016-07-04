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
package com.squareup.sqldelight.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.squareup.sqldelight.SqliteCompiler
import com.squareup.sqldelight.intellij.lang.SqliteFile
import com.squareup.sqldelight.model.sqliteText
import java.awt.datatransfer.StringSelection

class CopyAsSqliteAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val caret = e.getData(LangDataKeys.CARET)!!
    val elementAt = (e.getData(LangDataKeys.PSI_FILE) as? SqliteFile)?.elementAt(caret.offset) ?: return
    CopyPasteManager.getInstance().setContents(StringSelection(elementAt.sqliteText()))
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible =
        e.getData(PlatformDataKeys.VIRTUAL_FILE)?.extension == SqliteCompiler.FILE_EXTENSION
     && e.getData(LangDataKeys.PSI_FILE) is SqliteFile
  }
}
