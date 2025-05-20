package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.core.lang.MigrationFile
import app.cash.sqldelight.core.lang.SqlDelightQueriesFile
import app.cash.sqldelight.core.lang.psi.parameterValue
import app.cash.sqldelight.core.lang.util.migrationFiles
import app.cash.sqldelight.intellij.refactoring.SqlDelightSignatureBuilder
import app.cash.sqldelight.intellij.refactoring.SqlDelightSuggestedRefactoringExecution
import app.cash.sqldelight.intellij.refactoring.SqlDelightSuggestedRefactoringExecution.SuggestedMigrationData
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport.Signature

internal class SchemaNeedsMigrationInspection : LocalInspectionTool() {
  private val refactoringExecutor = SqlDelightSuggestedRefactoringExecution()
  private val signatureBuilder = SqlDelightSignatureBuilder()

  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
  ) = ensureReady(holder.file) {
    object : SqlVisitor() {
      override fun visitCreateTableStmt(createTable: SqlCreateTableStmt) = ignoreInvalidElements {
        if (sqlDelightFile !is SqlDelightQueriesFile) return

        val dbFile = sqlDelightFile.findDbFile() ?: return
        val topMigrationFile = fileIndex.sourceFolders(sqlDelightFile).asSequence()
          .flatMap { it.migrationFiles() }
          .maxByOrNull { it.version }

        val tables = (topMigrationFile ?: dbFile).tables(true)
        val tableWithSameName = tables.find { it.tableName.name == createTable.tableName.name }
        val signature = signatureBuilder.signature(createTable) ?: return

        if (tableWithSameName == null) {
        /*
         * TODO: Reenable this once it performs faster. Evaluating oldQuery.query is expensive.
        val tableWithDifferentName = tables.find { oldQuery ->
          val oldColumns = oldQuery.query.columns
          oldColumns.size == signature.parameters.size &&
            oldColumns.map { it.parameterValue() }.containsAll(signature.parameters)
        }

        if (tableWithDifferentName != null) {
          holder.registerProblem(
            createTable.tableName, "Table needs to be renamed in a migration", GENERIC_ERROR,
            AlterTableNameMigrationQuickFix(
              createTable, tableWithDifferentName.tableName.name, topMigrationFile
            )
          )
          return
        }*/

          holder.registerProblem(
            createTable.tableName,
            "Table needs to be added in a migration",
            GENERIC_ERROR,
            CreateTableMigrationQuickFix(createTable, topMigrationFile),
          )
          return
        }

        val oldSignature = Signature.create(
          name = tableWithSameName.tableName.name,
          type = null,
          parameters = tableWithSameName.query.columns.mapNotNull { it.parameterValue() },
          additionalData = null,
        ) ?: return

        if (oldSignature != signature) {
          holder.registerProblem(
            createTable.tableName,
            "Table needs to be altered in a migration",
            GENERIC_ERROR,
            AlterTableMigrationQuickFix(createTable, oldSignature, signature),
          )
        }
      }
    }
  }

  inner class AlterTableNameMigrationQuickFix(
    createTableStmt: SqlCreateTableStmt,
    private val oldName: String,
    private val newestMigrationFile: MigrationFile?,
  ) : LocalQuickFixOnPsiElement(createTableStmt) {
    private val createTableRef = SmartPointerManager.getInstance(createTableStmt.project)
      .createSmartPsiElementPointer(createTableStmt, createTableStmt.containingFile)

    override fun getFamilyName(): String = name

    override fun getText(): String = "Add migration for ${createTableRef.element?.tableName?.text.orEmpty()}"

    override fun invoke(
      project: Project,
      file: PsiFile,
      startElement: PsiElement,
      endElement: PsiElement,
    ) {
      val strategy = SqlDelightProjectService.getInstance(file.project).dialect.migrationStrategy
      val changeName = strategy.tableNameChanged(
        oldName = oldName,
        newName = createTableRef.element?.tableName?.name ?: return,
      )
      refactoringExecutor.performChangeSignature(
        SuggestedMigrationData(
          declarationPointer = createTableRef,
          newestMigrationFile = newestMigrationFile,
          preparedMigration = changeName,
        ),
      )
    }
  }

  inner class CreateTableMigrationQuickFix(
    createTableStmt: SqlCreateTableStmt,
    private val newestMigrationFile: MigrationFile?,
  ) : LocalQuickFixOnPsiElement(createTableStmt) {
    private val createTableRef = SmartPointerManager.getInstance(createTableStmt.project)
      .createSmartPsiElementPointer(createTableStmt, createTableStmt.containingFile)

    override fun getFamilyName(): String = name

    override fun getText(): String = "Add migration for ${createTableRef.element?.tableName?.text.orEmpty()}"

    override fun invoke(
      project: Project,
      file: PsiFile,
      startElement: PsiElement,
      endElement: PsiElement,
    ) {
      refactoringExecutor.performChangeSignature(
        SuggestedMigrationData(
          declarationPointer = createTableRef,
          newestMigrationFile = newestMigrationFile,
          preparedMigration = "${createTableRef.element?.text.orEmpty()};",
        ),
      )
    }
  }

  inner class AlterTableMigrationQuickFix(
    createTableStmt: SqlCreateTableStmt,
    private val oldSignature: Signature,
    private val newSignature: Signature,
  ) : LocalQuickFixOnPsiElement(createTableStmt) {
    private val createTableRef = SmartPointerManager.getInstance(createTableStmt.project)
      .createSmartPsiElementPointer(createTableStmt, createTableStmt.containingFile)

    override fun getFamilyName(): String = name

    override fun getText(): String = "Add migration for ${createTableRef.element?.tableName?.text.orEmpty()}"

    override fun invoke(
      project: Project,
      file: PsiFile,
      startElement: PsiElement,
      endElement: PsiElement,
    ) {
      val migrationData = refactoringExecutor.prepareChangeSignature(
        declaration = createTableRef,
        oldSignature = oldSignature,
        newSignature = newSignature,
      ) ?: return
      refactoringExecutor.performChangeSignature(migrationData)
    }
  }
}
