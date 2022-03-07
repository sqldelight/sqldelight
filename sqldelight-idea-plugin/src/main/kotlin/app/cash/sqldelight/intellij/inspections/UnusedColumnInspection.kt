package app.cash.sqldelight.intellij.inspections

import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.core.lang.util.findChildOfType
import app.cash.sqldelight.core.lang.util.findChildrenOfType
import com.alecstrong.sql.psi.core.psi.AliasElement
import com.alecstrong.sql.psi.core.psi.SqlColumnDef
import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.alecstrong.sql.psi.core.psi.SqlCreateTableStmt
import com.alecstrong.sql.psi.core.psi.SqlTypes
import com.alecstrong.sql.psi.core.psi.SqlVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ReadOnlyModificationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil

internal class UnusedColumnInspection : LocalInspectionTool() {

  override fun buildVisitor(
    holder: ProblemsHolder,
    isOnTheFly: Boolean,
    session: LocalInspectionToolSession
  ): PsiElementVisitor = object : SqlVisitor() {
    override fun visitCreateTableStmt(o: SqlCreateTableStmt) {
      val candidates = o.findChildrenOfType<SqlColumnDef>()
        .filter { columnDef ->
          val columnName = columnDef.columnName
          ReferencesSearch.search(columnName, columnName.useScope)
            .allMatch { reference -> reference.element.parent is SqlColumnDef }
        }
        .toMutableList()

      if (candidates.isEmpty()) {
        return
      }

      val project = o.project
      val psiManager = PsiManager.getInstance(project)

      fun PsiElement.columnDef(): SqlColumnDef? {
        if (this is SqlColumnName) return parent.columnDef()
        if (this is SqlColumnDef) return this
        if (this is AliasElement) return source().columnDef()
        return null
      }

      FileTypeIndex.getFiles(SqlDelightFileType, GlobalSearchScope.allScope(project))
        .mapNotNull { vFile -> psiManager.findFile(vFile) as SqlDelightFile? }
        .flatMap { file -> file.sqlStmtList?.stmtList.orEmpty() }
        .flatMap { stmt -> stmt.compoundSelectStmt?.queryExposed().orEmpty() }
        .flatMap { queryResult -> queryResult.columns }
        .forEach { column ->
          column.element.columnDef()?.let(candidates::remove)
        }

      candidates.forEach { columnDef ->
        holder.registerProblem(
          columnDef.columnName, "Unused symbol", ProblemHighlightType.LIKE_UNUSED_SYMBOL,
          SafeDeleteQuickFix(o, columnDef)
        )
      }
    }
  }

  class SafeDeleteQuickFix(createTableStmt: SqlCreateTableStmt, columnDef: SqlColumnDef) :
    LocalQuickFixOnPsiElement(createTableStmt) {

    private val createTableRef = SmartPointerManager.getInstance(createTableStmt.project)
      .createSmartPsiElementPointer(createTableStmt, createTableStmt.containingFile)

    private val columnDefRef = SmartPointerManager.getInstance(columnDef.project)
      .createSmartPsiElementPointer(columnDef, columnDef.containingFile)

    override fun getFamilyName(): String = name

    override fun getText(): String = "Safe delete ${columnDefRef.element?.columnName?.text.orEmpty()}"

    override fun invoke(
      project: Project,
      file: PsiFile,
      startElement: PsiElement,
      endElement: PsiElement
    ) {
      WriteCommandAction.writeCommandAction(project).run<ReadOnlyModificationException> {
        val createTable = createTableRef.element ?: return@run
        val columnDef = columnDefRef.element ?: return@run

        val columnDefString = createTable.columnDefList.filter { it !== columnDef }
          .joinToString(",\n  ") { it.text.trim() }

        val startTable = PsiTreeUtil.findSiblingForward(createTable.firstChild, SqlTypes.LP, false, null) ?: return@run
        val endTable = PsiTreeUtil.findSiblingBackward(createTable.lastChild, SqlTypes.RP, false, null) ?: return@run
        createTable.deleteChildRange(startTable.nextSibling, endTable.prevSibling)

        if (columnDefString.isEmpty()) {
          return@run
        }

        val factory = PsiFileFactory.getInstance(file.project)
        val createTableStmt = """
          |CREATE TABLE ${createTable.tableName.id?.text}(
          |  $columnDefString
          |)
        """.trimMargin()
        val dummyFile = factory.createFileFromText(
          "_Dummy_.${SqlDelightFileType.EXTENSION}",
          SqlDelightFileType, createTableStmt
        )

        val newCreateTable = dummyFile.findChildOfType<SqlCreateTableStmt>() ?: return@run
        val newStart = PsiTreeUtil.findSiblingForward(newCreateTable.firstChild, SqlTypes.LP, false, null) ?: return@run
        val newEnd = PsiTreeUtil.findSiblingBackward(newCreateTable.lastChild, SqlTypes.RP, false, null) ?: return@run

        createTable.addRangeAfter(newStart.nextSibling, newEnd.prevSibling, startTable)
      }
    }
  }
}
