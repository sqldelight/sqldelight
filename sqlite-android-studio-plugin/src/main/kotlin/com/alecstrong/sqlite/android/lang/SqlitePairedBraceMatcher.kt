package com.alecstrong.sqlite.android.lang

import com.alecstrong.sqlite.android.SQLiteLexer
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes.TOKEN_ELEMENT_TYPES
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
