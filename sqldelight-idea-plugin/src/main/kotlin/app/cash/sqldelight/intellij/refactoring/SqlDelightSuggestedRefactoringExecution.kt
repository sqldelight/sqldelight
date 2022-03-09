package app.cash.sqldelight.intellij.refactoring

import app.cash.sqldelight.core.SqlDelightFileIndex
import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.core.lang.MigrationFile
import app.cash.sqldelight.core.lang.MigrationFileType
import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.psi.ColumnConstraints
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import app.cash.sqldelight.intellij.refactoring.strategy.SqlGeneratorStrategy
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
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Parameter
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Signature
import com.intellij.util.IncorrectOperationException

internal class SqlDelightSuggestedRefactoringExecution {
  class SuggestedMigrationData(
    val declarationPointer: SmartPsiElementPointer<out PsiElement>,
    val newestMigrationFile: MigrationFile?,
    val preparedMigration: String
  )

  fun prepareChangeSignature(
    declaration: SmartPsiElementPointer<out PsiElement>,
    oldSignature: Signature,
    newSignature: Signature
  ): SuggestedMigrationData? {
    val file = declaration.containingFile as SqlDelightFile? ?: return null

    val dialect = SqlDelightProjectService.getInstance(file.project).dialectPreset
    val strategy = SqlGeneratorStrategy.create(dialect)

    val fileIndex = SqlDelightFileIndex.getInstance(file.module ?: return null)
    val migrationFile = fileIndex.sourceFolders(file)
      .flatMap { it.findChildrenOfType<MigrationFile>() }
      .maxByOrNull { it.version }

    var oldList = oldSignature.parameters
    var newList = newSignature.parameters
    val oldColumnDefList = oldList.map { it.columnDef() }

    val tableNameChanged = oldSignature.name != newSignature.name
    val columnNameChanges = columnNameChanges(oldList, newList)
    oldList = oldList.filterNot { old -> old.name in columnNameChanges.map { it.first } }
    newList = newList.filterNot { new -> new.name in columnNameChanges.map { it.second } }

    val newColumns = newColumns(oldList, newList)
    val removedColumns = removedColumns(oldList, newList)

    val migration = mutableListOf<String>()
    if (tableNameChanged) {
      migration += strategy.tableNameChanged(oldSignature.name, newSignature.name)
    }
    columnNameChanges.forEach { (oldName, newName) ->
      migration += strategy.columnNameChanged(newSignature.name, oldName, newName, oldColumnDefList)
    }
    newColumns.forEach { col ->
      val columnDef = col.columnDef()
      migration += strategy.columnAdded(oldSignature.name, columnDef)
    }
    removedColumns.forEach { col ->
      migration += strategy.columnRemoved(newSignature.name, col.name, oldColumnDefList)
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

  private fun removedColumns(
    oldList: List<Parameter>,
    newList: List<Parameter>
  ): List<Parameter> {
    return oldList.intersectBy(newList, Parameter::id)
  }

  private inline fun <T, R : Any> List<T>.intersectBy(
    other: List<T>,
    f: (T) -> R
  ): List<T> {
    val set = other.mapTo(mutableSetOf(), f)
    return filter { f(it) !in set }
  }

  @Suppress("NAME_SHADOWING")
  private fun columnNameChanges(
    oldList: List<Parameter>,
    newList: List<Parameter>
  ): List<Pair<String, String>> {
    val old = oldList.mapTo(mutableSetOf(), Parameter::name)
    val new = newList.mapTo(mutableSetOf(), Parameter::name)
    val oldList = oldList.filterNot { it.name in new }
    val newList = newList.filterNot { it.name in old }

    return oldList.mapNotNull { old ->
      val matchingDefinition = newList.find { new ->
        new.type == old.type && new.additionalData == old.additionalData
      }
      if (matchingDefinition != null) old.name to matchingDefinition.name
      else null
    }
  }

  fun performChangeSignature(
    migrationData: SuggestedMigrationData
  ) {
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
      .setTitle("Choose Migration File")
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
    declaration: SmartPsiElementPointer<out PsiElement>,
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
