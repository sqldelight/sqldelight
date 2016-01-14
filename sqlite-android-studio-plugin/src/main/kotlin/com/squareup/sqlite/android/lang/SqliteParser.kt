package com.squareup.sqlite.android.lang

import com.squareup.sqlite.android.SQLiteParser
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import org.antlr.intellij.adaptor.parser.AntlrParser
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener
import org.antlr.v4.runtime.TokenStream

class SqliteParser internal constructor() : AntlrParser<SQLiteParser>(SqliteLanguage.INSTANCE) {
  override fun createParserImpl(tokenStream: TokenStream, root: IElementType,
      builder: PsiBuilder): SQLiteParser {
    val parser = SQLiteParser(tokenStream)
    parser.removeErrorListeners()
    parser.addErrorListener(SyntaxErrorListener())
    return parser
  }

  override fun parseImpl(parser: SQLiteParser, root: IElementType, builder: PsiBuilder)
      = parser.parse()
}
