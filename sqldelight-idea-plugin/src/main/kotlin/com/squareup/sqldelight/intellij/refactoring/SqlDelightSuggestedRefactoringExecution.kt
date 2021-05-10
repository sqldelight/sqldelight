package com.squareup.sqldelight.intellij.refactoring

import com.intellij.ide.util.DirectoryUtil
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ReadOnlyModificationException
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.refactoring.suggested.SuggestedChangeSignatureData
import com.intellij.refactoring.suggested.SuggestedRefactoringExecution
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Parameter
import com.intellij.util.IncorrectOperationException
import com.squareup.sqldelight.core.SqlDelightFileIndex
import com.squareup.sqldelight.core.SqlDelightProjectService
import com.squareup.sqldelight.core.lang.MigrationFile
import com.squareup.sqldelight.core.lang.MigrationFileType
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.util.findChildrenOfType
import com.squareup.sqldelight.intellij.refactoring.SqlDelightSuggestedRefactoringStateChanges.ColumnConstraints
import com.squareup.sqldelight.intellij.refactoring.strategy.SqlGeneratorStrategy

class SqlDelightSuggestedRefactoringExecution(
  refactoringSupport: SuggestedRefactoringSupport
) : SuggestedRefactoringExecution(refactoringSupport) {

  class SuggestedMigrationData(
    val declarationPointer: SmartPsiElementPointer<PsiElement>,
    val newestMigrationFile: MigrationFile?,
    val preparedMigration: String
  )

  override fun prepareChangeSignature(data: SuggestedChangeSignatureData): Any? {
    val declaration = data.declarationPointer
    val oldSignature = data.oldSignature
    val newSignature = data.newSignature

    val file = declaration.containingFile as SqlDelightFile? ?: return null

    val dialect = SqlDelightProjectService.getInstance(file.project).dialectPreset
    val strategy = SqlGeneratorStrategy.create(dialect)

    val module = ModuleUtil.findModuleForFile(file) ?: return null
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    val migrationFile = fileIndex.sourceFolders(file)
      .flatMap { it.findChildrenOfType<MigrationFile>() }
      .maxByOrNull { it.version }

    val oldList = oldSignature.parameters
    val newList = newSignature.parameters
    val oldColumnDefList = oldList.map { it.columnDef() }

    val tableNameChanged = oldSignature.name != newSignature.name
    val newColumns = newColumns(oldList, newList)
    val removedColumns = removedColumns(oldList, newList)
    val columnNameChanges = columnNameChanges(oldList, newList)

    val migration = mutableListOf<String>()
    if (tableNameChanged) {
      migration += strategy.tableNameChanged(oldSignature.name, newSignature.name)
    }
    newColumns.forEach { col ->
      val columnDef = col.columnDef()
      migration += strategy.columnAdded(oldSignature.name, columnDef)
    }
    removedColumns.forEach { col ->
      migration += strategy.columnRemoved(newSignature.name, col.name, oldColumnDefList)
    }
    columnNameChanges.forEach { (oldName, newName) ->
      migration += strategy.columnNameChanged(newSignature.name, oldName, newName, oldColumnDefList)
    }

    return SuggestedMigrationData(
      declarationPointer = declaration,
      newestMigrationFile = migrationFile,
      preparedMigration = migration.joinToString("\n").trim()
    )
  }

  private fun Parameter.columnDef(): String {
    val columnConstraints = additionalData as ColumnConstraints?
    return buildString {
      append("$name $type")
      if (columnConstraints != null) {
        append(" ")
        append(columnConstraints)
      }
    }
  }

  private fun newColumns(
    oldList: List<Parameter>,
    newList: List<Parameter>
  ): List<Parameter> {
    return newList.intersectBy(oldList, Parameter::id)
  }

  private fun removedColumns(oldList: List<Parameter>, newList: List<Parameter>): List<Parameter> {
    return oldList.intersectBy(newList, Parameter::id)
  }

  private inline fun <T, R : Any> List<T>.intersectBy(other: List<T>, f: (T) -> R): List<T> {
    val set = other.mapTo(mutableSetOf(), f)
    return filter { f(it) !in set }
  }

  private fun columnNameChanges(
    oldList: List<Parameter>,
    newList: List<Parameter>
  ): List<Pair<String, String>> {
    val old = oldList.mapTo(mutableSetOf(), Parameter::id)
    val new = newList.mapTo(mutableSetOf(), Parameter::id)
    val oldList = oldList.filter { it.id in new }
    val newList = newList.filter { it.id in old }

    return oldList.zip(newList)
      .filter { (old, new) -> old.name != new.name }
      .map { (old, new) -> old.name to new.name }
  }

  override fun performChangeSignature(
    data: SuggestedChangeSignatureData,
    newParameterValues: List<NewParameterValue>,
    preparedData: Any?
  ) {
    val migrationData = preparedData as SuggestedMigrationData
    val declarationPointer = migrationData.declarationPointer
    val project = declarationPointer.project
    val newestMigrationFile = migrationData.newestMigrationFile
    val migration = migrationData.preparedMigration

    if (newestMigrationFile == null) {
      writeToFile(declarationPointer, project.createMigrationFile(migration, 1), migration)
      return
    }

    val list = listOf("Create new", "Add to ${newestMigrationFile.name}")
    JBPopupFactory.getInstance()
      .createPopupChooserBuilder(list)
      .setTitle("Choose migration file")
      .setMovable(true)
      .setResizable(true)
      .setItemChosenCallback {
        val migrationFile = when (list.indexOf(it)) {
          0 -> {
            val version = newestMigrationFile.version.plus(1)
            project.createMigrationFile(migration, version)
          }
          else -> newestMigrationFile
        }
        writeToFile(declarationPointer, migrationFile, migration)
      }
      .createPopup()
      .showInFocusCenter()
  }

  private fun Project.createMigrationFile(migration: String, version: Int): MigrationFile {
    val fileName = "$version.${MigrationFileType.EXTENSION}"
    return PsiFileFactory.getInstance(this)
      .createFileFromText(fileName, MigrationFileType, migration) as MigrationFile
  }

  private fun writeToFile(
    declaration: SmartPsiElementPointer<PsiElement>,
    migrationFile: MigrationFile,
    migration: String
  ) {
    val project = declaration.project
    val element = declaration.element ?: return
    val module = ModuleUtil.findModuleForPsiElement(element) ?: return
    val fileIndex = SqlDelightFileIndex.getInstance(module)

    val sourceDir = fileIndex.sourceFolders(element.containingFile as SqlDelightFile)
      .first()
      .virtualFile
      .path
    val path = "$sourceDir/migrations"

    WriteCommandAction.writeCommandAction(project).run<ReadOnlyModificationException> {
      val psiDirectory = DirectoryUtil.mkdirs(PsiManager.getInstance(project), path)
      try {
        psiDirectory.add(migrationFile)
      } catch (ignored: IncorrectOperationException) {
      }
      if (migrationFile.virtualFile != null) {
        val editor = EditorHelper.openInEditor(migrationFile)
        val document = editor.document
        val s = if (document.textLength == 0) "" else "\n"
        document.insertString(document.textLength, s)
        document.insertString(document.textLength, migration)
      } else {
        fileIndex.sourceFolders(element.containingFile as SqlDelightFile)
          .flatMap { it.findChildrenOfType<MigrationFile>() }
          .firstOrNull { it.name == migrationFile.name }
          ?.let { EditorHelper.openInEditor(it) }
      }
    }
  }
}
