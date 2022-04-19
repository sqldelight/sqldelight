package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.lang.SqlDelightFile
import com.alecstrong.sql.psi.core.psi.InvalidElementDetectedException
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException

internal inline fun LocalInspectionTool.ensureReady(file: PsiFile, block: InspectionProperties.() -> PsiElementVisitor): PsiElementVisitor {
  return ensureReady(file, { PsiElementVisitor.EMPTY_VISITOR }, block)
}

internal inline fun LocalInspectionTool.ensureFileReady(file: PsiFile, block: InspectionProperties.() -> Array<ProblemDescriptor>): Array<ProblemDescriptor> {
  return ensureReady(file, { emptyArray() }, block)
}

@Suppress("unused") // Receiver to enforce usage.
internal inline fun PsiElementVisitor.ignoreInvalidElements(block: () -> Unit) {
  try {
    block()
  } catch (_: InvalidElementDetectedException) {
  } catch (_: PsiInvalidElementAccessException) {
  }
}

private inline fun <T> Any.ensureReady(
  file: PsiFile,
  defaultValue: () -> T,
  block: InspectionProperties.() -> T
): T {
  val sqlDelightFile = file as? SqlDelightFile ?: return defaultValue()
  val module = file.module ?: return defaultValue()
  val fileIndex = SqlDelightFileIndex.getInstance(module)
  if (!fileIndex.isConfigured) return defaultValue()

  try {
    return InspectionProperties(module, sqlDelightFile, fileIndex).block()
  } catch (_: InvalidElementDetectedException) {
    return defaultValue()
  } catch (_: PsiInvalidElementAccessException) {
    return defaultValue()
  }
}

internal data class InspectionProperties(
  val module: Module,
  val sqlDelightFile: SqlDelightFile,
  val fileIndex: SqlDelightFileIndex,
)
