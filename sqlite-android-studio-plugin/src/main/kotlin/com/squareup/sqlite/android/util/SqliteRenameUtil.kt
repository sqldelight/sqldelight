package com.squareup.sqlite.android.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.source.PsiClassImpl
import com.intellij.psi.impl.source.PsiFieldImpl
import com.intellij.psi.impl.source.PsiMethodImpl
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.refactoring.rename.RenameUtil.doRename
import com.intellij.usageView.UsageInfo
import com.squareup.sqlite.android.lang.SqliteFile
import com.squareup.sqlite.android.model.Column
import com.squareup.sqlite.android.model.SqlStmt
import com.squareup.sqlite.android.psi.SqliteElement.ColumnNameElement
import com.squareup.sqlite.android.psi.SqliteElement.SqlStmtNameElement
import java.util.ArrayList

/**
 * Gather usage info for the given named element, where [PsiNamedElement.getName] is
 * a valid column name used for generating Java code. This returns a [SqliteUsageInfo]
 * containing sqlite usages, field usages and method usages (which could be the interface or
 * marshal method).
 * @param newElementName If this method is being used as part of a rename, this should be the
 * name the element is being renamed to as that information is stored in the returned usage info.
 */
internal fun PsiNamedElement.findUsages(newElementName: String): SqliteUsageInfo {
  val generatedFile = (containingFile as SqliteFile).generatedFile
  val sqliteUsages = RenameUtil.findUsages(this, newElementName, false, false, emptyMap())
  val fieldUsages = ArrayList<UsageInfo>()
  val methodUsages = ArrayList<UsageInfo>()
  val overridingMethods = ArrayList<PsiMethod>()
  val mapperUsages = ArrayList<UsageInfo>()
  val marshalUsages = ArrayList<UsageInfo>()

  generatedFile?.processElements {
    if (parent is ColumnNameElement && it.isColumnFieldFor(this)) {
      fieldUsages.addAll(notInsideFile(RenameUtil.findUsages(it, Column.fieldName(newElementName),
          false, false, emptyMap()), generatedFile))
    } else if (parent is ColumnNameElement && it.isColumnMethodFor(this)) {
      methodUsages.addAll(notInsideFile(RenameUtil.findUsages(it, Column.methodName(newElementName),
          false, false, emptyMap()), generatedFile))
      OverridingMethodsSearch.search(it as PsiMethod).toCollection(overridingMethods)
    } else if (parent is SqlStmtNameElement && it.isSqlStmtFieldFor(this)) {
      fieldUsages.addAll(notInsideFile(RenameUtil.findUsages(it, SqlStmt.fieldName(newElementName),
          false, false, emptyMap()), generatedFile))
    } else if (parent is ColumnNameElement && it.isColumnMapperFor(this)) {
      mapperUsages.addAll(notInsideFile(RenameUtil.findUsages(it,
          Column.mapperName(newElementName), false, false, emptyMap()), generatedFile))
    } else if (parent is ColumnNameElement && it.isColumnMarshalFor(this)) {
      marshalUsages.addAll(notInsideFile(RenameUtil.findUsages(it,
          Column.marshalName(newElementName), false, false, emptyMap()), generatedFile))
    }
    true
  }
  return SqliteUsageInfo(fieldUsages, methodUsages, overridingMethods,
      mapperUsages, marshalUsages, sqliteUsages)
}

/**
 * Find all the Java PsiElements for a given named element. [PsiNamedElement.getName]
 * represents the name of the column we are matching PsiElements against.
 */
internal fun PsiNamedElement.getSecondaryElements() =
    (containingFile as SqliteFile).generatedFile?.collectElements {
      when (this) {
        is ColumnNameElement -> it.isColumnFieldFor(this) || it.isColumnMethodFor(this)
        is SqlStmtNameElement -> it.isSqlStmtFieldFor(this)
        else -> false
      }
    } ?: emptyArray()

/**
 * Rename the given element by using the [SqliteUsageInfo] provided. It performs three
 * separate rename batches: field usages, method usages and sqlite usages. This function should
 * be called from a single command, so that undo functions properly.
 */
internal fun PsiNamedElement.doRename(newElementName: String, usageInfo: SqliteUsageInfo,
    originatingFile: SqliteFile, listener: RefactoringElementListener?) {
  when (parent) {
    is ColumnNameElement -> {
      usageInfo.fieldUsages.forEach { RenameUtil.rename(it, Column.fieldName(newElementName)) }
      usageInfo.methodUsages.forEach { RenameUtil.rename(it, Column.methodName(newElementName)) }
      usageInfo.overridingMethods.forEach {
        RenameUtil.doRename(it, Column.methodName(newElementName), emptyArray(), it.project, null)
      }
      usageInfo.mapperUsages.forEach { RenameUtil.rename(it, Column.mapperName(newElementName)) }
      usageInfo.marshalUsages.forEach { RenameUtil.rename(it, Column.marshalName(newElementName)) }
    }
    is SqlStmtNameElement -> {
      usageInfo.fieldUsages.forEach { RenameUtil.rename(it, SqlStmt.fieldName(newElementName)) }
    }
  }
  doRename(this, newElementName, usageInfo.sqliteUsages, originatingFile.project, listener)
}

private fun notInsideFile(original: Array<UsageInfo>, file: PsiFile)
    = original.filter { it.file != file }

private fun PsiElement.isColumnMethodFor(element: PsiNamedElement) =
  this is PsiMethodImpl && name == Column.methodName(element.name!!)

private fun PsiElement.isColumnFieldFor(element: PsiNamedElement) =
  this is PsiFieldImpl && name == Column.fieldName(element.name!!)

private fun PsiElement.isColumnMapperFor(element: PsiNamedElement) =
    this is PsiClassImpl && name == Column.mapperName(element.name!!)

private fun PsiElement.isColumnMarshalFor(element: PsiNamedElement) =
    this is PsiClassImpl && name == Column.marshalName(element.name!!)

private fun PsiElement.isSqlStmtFieldFor(element: PsiNamedElement) =
  this is PsiFieldImpl && name == SqlStmt.fieldName(element.name!!)

data class SqliteUsageInfo(
    internal val fieldUsages: List<UsageInfo>,
    internal val methodUsages: List<UsageInfo>,
    internal val overridingMethods: List<PsiMethod>,
    internal val mapperUsages: List<UsageInfo>,
    internal val marshalUsages: List<UsageInfo>,
    internal val sqliteUsages: Array<UsageInfo>)
