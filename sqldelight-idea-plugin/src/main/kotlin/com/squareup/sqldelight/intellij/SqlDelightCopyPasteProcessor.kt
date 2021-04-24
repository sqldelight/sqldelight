package com.squareup.sqldelight.intellij

import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.ReferenceData
import com.intellij.codeInsight.editorActions.ReferenceTransferableData
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.ReadOnlyModificationException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.psi.JavaTypeMixin
import com.squareup.sqldelight.core.lang.util.findChildOfType
import com.squareup.sqldelight.core.psi.SqlDelightImportStmtList
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

class SqlDelightCopyPasteProcessor : CopyPastePostProcessor<ReferenceTransferableData>() {

  override fun collectTransferableData(
    file: PsiFile,
    editor: Editor,
    startOffsets: IntArray,
    endOffsets: IntArray
  ): List<ReferenceTransferableData> {
    if (file !is SqlDelightFile) {
      return emptyList()
    }

    val result = mutableListOf<ReferenceData>()
    var refOffset = 0

    for (j in startOffsets.indices) {
      refOffset += startOffsets[j]
      val elementsInRange = CollectHighlightsUtil.getElementsInRange(file, startOffsets[j], endOffsets[j])
      for (element in elementsInRange) {
        val data = referenceData(element, refOffset)
        if (data != null) {
          result += data
        }
      }
      refOffset -= endOffsets[j] + 1
    }
    return listOf(ReferenceTransferableData(result.toTypedArray()))
  }

  private fun referenceData(
    element: PsiElement,
    refOffset: Int
  ): ReferenceData? {
    if (element !is JavaTypeMixin) {
      return null
    }

    val resolvedClass = element.reference.resolve() as? PsiClass ?: return null
    val qualifiedName = resolvedClass.qualifiedName ?: return null
    val range = element.textRange
    return ReferenceData(
      range.startOffset - refOffset,
      range.endOffset - refOffset,
      qualifiedName,
      null
    )
  }

  override fun extractTransferableData(content: Transferable): List<ReferenceTransferableData> {
    try {
      val dataFlavor = ReferenceData.getDataFlavor() ?: return emptyList()
      val referenceData = content.getTransferData(dataFlavor) as ReferenceTransferableData
      return listOf(referenceData)
    } catch (ignored: UnsupportedFlavorException) {
    } catch (ignored: IOException) {
    }
    return super.extractTransferableData(content)
  }

  override fun processTransferableData(
    project: Project,
    editor: Editor,
    bounds: RangeMarker,
    caretOffset: Int,
    indented: Ref<Boolean>,
    values: List<ReferenceTransferableData>
  ) {
    if (DumbService.getInstance(project).isDumb) {
      return
    }

    val document = editor.document
    val documentManager = PsiDocumentManager.getInstance(project)
    val file = documentManager.getPsiFile(document)
    if (file !is SqlDelightFile) {
      return
    }
    val elementAtCaret = file.findElementAt(caretOffset)
    val insideCreateTableStmt = elementAtCaret?.parentOfType<SqlCreateTableStmt>() != null
    val hasCreateTableSibling = elementAtCaret?.parentOfType<SqlStmt>() != null &&
      PsiTreeUtil.findSiblingBackward(elementAtCaret, SqlTypes.CREATE_TABLE_STMT, false, null) != null
    if (!insideCreateTableStmt && !hasCreateTableSibling) {
      return
    }
    documentManager.commitAllDocuments()

    WriteCommandAction.writeCommandAction(project).run<ReadOnlyModificationException> {
      val qClassNames = values.first().data.map(ReferenceData::qClassName)
      val importStmtList = file.findChildOfType<SqlDelightImportStmtList>()?.importStmtList.orEmpty()
      val oldImports = importStmtList.map { it.javaType.text }
      val newImports = (qClassNames + oldImports).distinct()
        .sorted()
        .joinToString("\n") { "import $it;" }
      if (oldImports.isEmpty()) {
        document.insertString(0, "$newImports\n\n")
      } else {
        val endOffset = importStmtList.maxOfOrNull { it.textOffset + it.textLength } ?: 0
        document.replaceString(0, endOffset, newImports)
      }
    }
  }
}
