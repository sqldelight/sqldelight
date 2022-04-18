package app.cash.sqldelight.intellij.run.android

import app.cash.sqldelight.core.lang.util.rawSqlText
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlStmtList
import com.android.tools.idea.sqlite.DatabaseInspectorProjectService
import com.android.tools.idea.sqlite.annotator.RunSqliteStatementGutterIconAction
import com.android.tools.idea.sqlite.ui.DatabaseInspectorViewsFactoryImpl
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ui.EmptyIcon
import icons.StudioIcons
import javax.swing.Icon

class SqlDelightRunAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element.parent !is SqlStmtList || element !is SqlStmt) return

    val text = element.rawSqlText().trim().replace("\\s+".toRegex(), " ")
    val s = """
      |package com.example;
      |
      |import android.database.sqlite.SQLiteDatabase;
      |
      |class Util {
      |    void f(SQLiteDatabase db) {
      |        db.execSQL("$text");
      |    }
      |}
    """.trimMargin()

    val psiFile = PsiFileFactory.getInstance(element.project)
      .createFileFromText("Util.java", JavaLanguage.INSTANCE, s)

    val host = PsiTreeUtil.findChildOfType(psiFile, PsiLanguageInjectionHost::class.java) ?: return

    if (!PsiTreeUtil.hasErrorElements(psiFile)) {
      holder.newAnnotation(HighlightSeverity.INFORMATION, "")
        .gutterIconRenderer(RunSqliteStatementGutterIconRenderer(host))
        .create()
    }
  }

  /**
   * Shows an icon in the gutter when a SQLite statement is recognized. eg. Room @Query annotations.
   */
  private data class RunSqliteStatementGutterIconRenderer(private val element: PsiElement) : GutterIconRenderer() {
    private val sqliteExplorerProjectService = DatabaseInspectorProjectService.getInstance(element.project)

    override fun getIcon(): Icon {
      return if (sqliteExplorerProjectService.hasOpenDatabase()) {
        StudioIcons.DatabaseInspector.NEW_QUERY
      } else {
        EmptyIcon.ICON_0
      }
    }

    override fun getTooltipText(): String {
      return "Run Sqlite statement in Database Inspector"
    }

    override fun isNavigateAction(): Boolean {
      return sqliteExplorerProjectService.hasOpenDatabase()
    }

    override fun getClickAction(): AnAction {
      return RunSqliteStatementGutterIconAction(element.project, element, DatabaseInspectorViewsFactoryImpl.getInstance())
    }
  }
}
