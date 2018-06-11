package com.squareup.sqldelight.intellij.intentions

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.intellij.migrations.DatabaseDiffGenerator
import com.squareup.sqldelight.intellij.migrations.MigrationScriptsGenerator
import com.squareup.sqldelight.intellij.migrations.MigrationScriptsWriter
import com.squareup.sqldelight.intellij.migrations.MigrationsContext

class GenerateMigrationQuickFix : BaseIntentionAction() {

  override fun getFamilyName() = INTENTIONS_FAMILY_NAME_MIGRATIONS

  override fun getText() = INTENTIONS_GENERATE_MIGRATION

  override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile) = true

  override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
    ApplicationManager.getApplication().invokeLater {
      object : WriteCommandAction.Simple<Project>(project) {
        override fun run() {
          val sqlDelightFile = psiFile as SqlDelightFile
          val context = MigrationsContext.fromCurrentFile(sqlDelightFile)
          if (context.latestDbFile != null) {
            val diff = DatabaseDiffGenerator.generateDatabaseDiff(sqlDelightFile, context)
            if (diff != null) {
              val migrations = MigrationScriptsGenerator.generateMigrationScripts(diff)
              val migrationFilePath = context.latestDbFile.absolutePath.replace(".db", ".sqm")
              MigrationScriptsWriter.writeMigrationScripts(migrationFilePath, migrations)
            }
          }
        }
      }.execute()
    }
  }
}
