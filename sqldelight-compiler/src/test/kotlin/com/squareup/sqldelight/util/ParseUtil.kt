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
package com.squareup.sqldelight.util

import com.squareup.sqldelight.SqliteLexer
import com.squareup.sqldelight.SqliteParser
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import java.io.File
import java.io.FileInputStream

internal fun parse(file: File): SqliteParser.ParseContext {
  FileInputStream(file).use { inputStream ->
    val lexer = SqliteLexer(ANTLRInputStream(inputStream))
    lexer.removeErrorListeners()

    val parser = SqliteParser(CommonTokenStream(lexer))
    parser.removeErrorListeners()
    return parser.parse()
  }
}

internal fun SqliteParser.ParseContext.statementWithName(name: String): ParserRuleContext {
  val child = sql_stmt_list().sql_stmt().find({ it.sql_stmt_name().text == name })
  return child?.getChild(child.childCount - 1) as ParserRuleContext
}