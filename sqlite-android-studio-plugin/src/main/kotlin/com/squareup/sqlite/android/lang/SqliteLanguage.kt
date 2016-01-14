package com.squareup.sqlite.android.lang

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.SingleLazyInstanceSyntaxHighlighterFactory
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory

class SqliteLanguage : Language("Sqlite", "text/sqlite") {
  init {
    SyntaxHighlighterFactory.LANGUAGE_FACTORY.addExplicitExtension(this,
        object : SingleLazyInstanceSyntaxHighlighterFactory() {
          override fun createHighlighter() = SqliteHighlighter()
        })
  }

  companion object {
    var INSTANCE = SqliteLanguage()
  }
}
