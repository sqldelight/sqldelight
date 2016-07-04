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
package com.squareup.sqldelight.intellij.lang

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.ElementPatternCondition
import com.intellij.patterns.InitialPatternCondition
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.squareup.javapoet.TypeName
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.intellij.SqlDelightManager
import com.squareup.sqldelight.resolution.ResolutionError
import com.squareup.sqldelight.resolution.Resolver
import com.squareup.sqldelight.types.SymbolTable
import com.squareup.sqldelight.validation.SqlDelightValidator
import org.antlr.v4.runtime.ParserRuleContext

private val DUMMY_IDENTIFIER = "sql_delight_dummy_identifier"

class SqlDelightCompletionContributor : CompletionContributor() {
  init {
    extend(
        CompletionType.BASIC,
        SqlDelightPattern(),
        SqlDelightCompletionProvider()
    )
  }

  override fun beforeCompletion(context: CompletionInitializationContext) {
    context.dummyIdentifier = DUMMY_IDENTIFIER
  }
}

private class SqlDelightCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?,
      result: CompletionResultSet) {
    val manager = SqlDelightManager.getInstance(parameters.position) ?: return
    (parameters.position.containingFile as SqliteFile).elementAt(parameters.offset)
        ?.getAvailableValues(result, manager)
    // No reason to do any other completion for SQLDelight files. Might save some time.
    result.stopHere()
  }

  private fun ParserRuleContext.getAvailableValues(
      result: CompletionResultSet, manager: SqlDelightManager
  ) {
    try {
      var symbolTable = manager.symbolTable
      if (this is SqliteParser.Create_table_stmtContext) {
        // It's likely the create table statement will fail to compile, so we need
        // to give a fake type for it in the symbol table so resolution will run.
        symbolTable += SymbolTable(tableTypes = mapOf(table_name().text to TypeName.OBJECT), tag = this)
      }
      val resolver = Resolver(symbolTable)
      SqlDelightValidator().validate(this, resolver)
      result.addAllElements(resolver.errors
          .filter { it.originatingElement.text.endsWith(DUMMY_IDENTIFIER) }
          .flatMap { lookupElements(it) })
    } catch (e: Throwable) {
    }
  }

  /**
   * Given a resolution error, returns the possible column/table names that would resolve
   * that error.
   */
  private fun lookupElements(error: ResolutionError) =
      when (error) {
        is ResolutionError.TableNameNotFound -> error.availableTableNames
            .map {
              LookupElementBuilder.create(it)
            }
        is ResolutionError.ColumnNameNotFound -> error.availableColumns
            .flatMap { it.columnNames() }
            .filterNotNull()
            .distinct()
            .map {
              LookupElementBuilder.create(it)
            }
        is ResolutionError.ColumnOrTableNameNotFound -> error.availableColumns
            .filter { error.tableName == null || error.tableName == it.name }
            .flatMap { it.columnNames() + it.tableNames() }
            .filterNotNull()
            .distinct()
            .map {
              LookupElementBuilder.create(it)
            }
        else -> emptyList()
      }
}

/**
 * The completion provider acts on all PsiElements so this pattern is effectively always true.
 * Since the completion provider is already scoped to only .sq files the behavior should be safe.
 */
private class SqlDelightPattern : ElementPattern<PsiElement> {
  override fun accepts(o: Any?, context: ProcessingContext?) = true
  override fun accepts(o: Any?) = true
  override fun getCondition() = ElementPatternCondition(
      object : InitialPatternCondition<PsiElement>(PsiElement::class.java) {})
}
