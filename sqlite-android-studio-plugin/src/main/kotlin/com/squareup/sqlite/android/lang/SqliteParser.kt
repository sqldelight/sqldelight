/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqlite.android.lang

import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.squareup.sqlite.android.SqliteParser
import org.antlr.intellij.adaptor.parser.AntlrParser
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener
import org.antlr.v4.runtime.TokenStream

class SqliteParser internal constructor() : AntlrParser<SqliteParser>(SqliteLanguage.INSTANCE) {
  override fun createParserImpl(tokenStream: TokenStream, root: IElementType,
      builder: PsiBuilder): SqliteParser {
    val parser = SqliteParser(tokenStream)
    parser.removeErrorListeners()
    parser.addErrorListener(SyntaxErrorListener())
    return parser
  }

  override fun parseImpl(parser: SqliteParser, root: IElementType, builder: PsiBuilder)
      = parser.parse()
}
