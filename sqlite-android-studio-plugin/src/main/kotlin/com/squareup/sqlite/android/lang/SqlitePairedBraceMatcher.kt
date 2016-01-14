package com.squareup.sqlite.android.lang

import com.squareup.sqlite.android.SQLiteLexer
import com.squareup.sqlite.android.lang.SqliteTokenTypes.TOKEN_ELEMENT_TYPES
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class SqlitePairedBraceMatcher : PairedBraceMatcher {
  private val BRACE_PAIRS = arrayOf(BracePair(TOKEN_ELEMENT_TYPES[SQLiteLexer.OPEN_PAR],
      TOKEN_ELEMENT_TYPES[SQLiteLexer.CLOSE_PAR], false))

  override fun getPairs() = BRACE_PAIRS
  override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType,
      contextType: IElementType?) = true
  override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int) = openingBraceOffset
}
