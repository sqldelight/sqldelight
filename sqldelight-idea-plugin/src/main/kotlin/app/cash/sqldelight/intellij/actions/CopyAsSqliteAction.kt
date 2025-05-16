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
package app.cash.sqldelight.intellij.actions

import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.core.lang.util.rawSqlText
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class CopyAsSqliteAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val element = e.sqlElementAtCaret() ?: return
    CopyPasteManager.getInstance().setContents(StringSelection(element.rawSqlText()))
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible =
      e.getData(PlatformDataKeys.VIRTUAL_FILE)?.extension == SqlDelightFileType.defaultExtension &&
      e.sqlElementAtCaret() != null
  }

  private fun AnActionEvent.sqlElementAtCaret(): SqlStmt? {
    val caret = getData(LangDataKeys.CARET)
      ?: return getData(LangDataKeys.PSI_ELEMENT)?.getStrictParentOfType()
    val file = (getData(LangDataKeys.PSI_FILE) as? SqlDelightFile)
    return file?.findElementAt(caret.offset)?.getStrictParentOfType()
  }
}
