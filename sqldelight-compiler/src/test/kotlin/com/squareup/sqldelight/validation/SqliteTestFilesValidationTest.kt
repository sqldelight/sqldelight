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
package com.squareup.sqldelight.validation

import com.squareup.sqldelight.SqliteLexer
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.Status.ValidationStatus.Invalid
import com.squareup.sqldelight.types.SymbolTable
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.junit.AssumptionViolatedException
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.text.Charsets.UTF_8
import kotlin.text.RegexOption.DOT_MATCHES_ALL

@Ignore
@RunWith(Parameterized::class)
class SqliteTestFilesValidationTest {
  @Parameter(0)
  @JvmField var name: String? = null

  @Parameter(1)
  @JvmField var statement: String? = null

  @Test fun execute() {
    val lexer = SqliteLexer(ANTLRInputStream(statement))
    lexer.removeErrorListeners()
    lexer.addErrorListener(object : BaseErrorListener() {
      override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int,
          charPositionInLine: Int, msg: String?, e: RecognitionException?) {
        throw AssumptionViolatedException("$name line $line:$charPositionInLine - $msg")
      }
    })

    val parser = SqliteParser(CommonTokenStream(lexer))
    parser.removeErrorListeners()
    parser.addErrorListener(object : BaseErrorListener() {
      override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int,
          charPositionInLine: Int, msg: String?, e: RecognitionException?) {
        throw AssumptionViolatedException("$name line $line:$charPositionInLine - $msg")
      }
    })
    val parsed = parser.parse()

    val table = SymbolTable(parsed, parsed)
    val result = SqlDelightValidator().validate(parsed, table)
    if (result is Invalid) {
      throw AssertionError()
    }
  }

  companion object {
    private val KNOWN_FAILING = setOf<String>()

    @Suppress("unused") // Used by Parameterized JUnit runner reflectively.
    @Parameters(name = "{0}")
    @JvmStatic fun parameters() =
        ZipInputStream(FileInputStream("src/test/sqlite-src-3110100.zip")).use { zis ->
          zis.entries()
              .map { it.name }
              .filter { it.contains("test/") && it.endsWith(".test") }
              .map { it.substring(it.lastIndexOf('/') + 1, it.lastIndexOf('.'))}
              .sorted()
              .map { it to zis.readBytes().toString(UTF_8) }
              .toList() // Eagerly consume all zip entries inside the `use` block.
        }
        .filter { !KNOWN_FAILING.contains(it.first) }
        .flatMap {
          parseStatements(it.second)
              .map { result ->
                arrayOf("${it.first}-line${result.first}", result.second)
              }
        }

    private fun ZipInputStream.entries(): Sequence<ZipEntry> {
      return object : Sequence<ZipEntry> {
        override fun iterator(): Iterator<ZipEntry> {
          return object : Iterator<ZipEntry> {
            var next: ZipEntry? = null

            override fun hasNext(): Boolean {
              next = nextEntry
              return next != null
            }

            override fun next() = next!!
          }
        }
      }
    }

    private val EVAL1 = "eval \"(.*?)(?<!\\\\)\"".toRegex()
    private val EVAL2 = "eval \\{(.*?)\\}".toRegex(DOT_MATCHES_ALL)
    private val EXEC = "execsql \\{(.*?)\\}".toRegex(DOT_MATCHES_ALL)

    private fun parseStatements(test: String) = (EVAL1.findAll(test) + EVAL2.findAll(test) + EXEC.findAll(test))
        .map { it.groups[1]!! }
        .map { test.substring(0..it.range.start).split("\n").size - 1 to it.value }
        .toList()
  }
}
