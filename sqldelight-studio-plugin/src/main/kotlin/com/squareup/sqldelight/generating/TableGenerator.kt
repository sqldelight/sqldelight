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
package com.squareup.sqldelight.generating

import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.squareup.sqldelight.SqliteParser
import com.squareup.sqldelight.SqliteParser.RULE_column_def
import com.squareup.sqldelight.SqliteParser.RULE_column_name
import com.squareup.sqldelight.SqliteParser.RULE_create_table_stmt
import com.squareup.sqldelight.SqliteParser.RULE_sql_stmt
import com.squareup.sqldelight.SqliteParser.RULE_sql_stmt_list
import com.squareup.sqldelight.SqliteParser.RULE_sql_stmt_name
import com.squareup.sqldelight.SqliteParser.RULE_sqlite_class_name
import com.squareup.sqldelight.SqliteParser.RULE_table_name
import com.squareup.sqldelight.SqliteParser.RULE_type_name
import com.squareup.sqldelight.lang.SqliteTokenTypes
import com.squareup.sqldelight.model.Column
import com.squareup.sqldelight.model.ColumnConstraint.NotNullConstraint
import com.squareup.sqldelight.model.SqlStmt.Replacement
import com.squareup.sqldelight.psi.ParseElement
import com.squareup.sqldelight.relativePath
import com.squareup.sqldelight.util.childOfType
import com.squareup.sqldelight.util.childrenForRule
import com.squareup.sqldelight.util.elementType
import org.antlr.intellij.adaptor.lexer.TokenElementType

class TableGenerator constructor(parse: ParseElement, fileName: String, modulePath: String)
: com.squareup.sqldelight.TableGenerator<PsiElement, PsiElement, PsiElement, PsiElement, PsiElement>
(parse, fileName, modulePath) {

  override fun sqlStatementElements(originatingElement: PsiElement) = originatingElement
      .childrenForRule(RULE_sql_stmt_list)[0].childrenForRule(RULE_sql_stmt)

  override fun tableElement(sqlStatementElement: PsiElement) = sqlStatementElement
      .childrenForRule(RULE_sql_stmt_list).firstOrNull()
      ?.childrenForRule(RULE_create_table_stmt)?.firstOrNull()

  override fun identifier(sqlStatementElement: PsiElement) =
      sqlStatementElement.childrenForRule(RULE_sql_stmt_name)[0].text

  override fun columnElements(tableElement: PsiElement) =
      tableElement.childrenForRule(RULE_column_def)

  override fun tableName(tableElement: PsiElement) =
      tableElement.childrenForRule(RULE_table_name)[0].text

  override fun columnName(columnElement: PsiElement) =
      columnElement.childrenForRule(RULE_column_name)[0].text

  override fun classLiteral(columnElement: PsiElement) = columnElement
      .childrenForRule(RULE_type_name)[0]
      .childrenForRule(RULE_sqlite_class_name).firstOrNull()
      ?.childrenForRule(SqliteParser.STRING_LITERAL)?.firstOrNull()?.text

  override fun typeName(columnElement: PsiElement) = columnElement
      .childrenForRule(RULE_type_name)[0]
      .childrenForRule(RULE_sqlite_class_name).firstOrNull()
      ?.firstChild?.text ?: columnElement.childrenForRule(RULE_type_name)[0].text

  override fun replacementFor(columnElement: PsiElement, type: Column.Type): Replacement {
    val typeRange = columnElement.childrenForRule(RULE_type_name)[0].textRange
    return Replacement(typeRange.startOffset, typeRange.endOffset, type.replacement)
  }

  override fun constraintElements(columnElement: PsiElement) =
      columnElement.childrenForRule(SqliteParser.RULE_column_constraint)

  override fun constraintFor(constraintElement: PsiElement, replacements: List<Replacement>) =
      constraintElement.children
          .map { it.elementType }
          .filterIsInstance<TokenElementType>()
          .mapNotNull {
            when (it.type) {
              SqliteParser.K_NOT -> NotNullConstraint(constraintElement)
              else -> null
            }
          }
          .firstOrNull()

  override fun text(sqliteStatementElement: PsiElement) =
      when (sqliteStatementElement.elementType) {
        SqliteTokenTypes.RULE_ELEMENT_TYPES[RULE_sql_stmt] -> sqliteStatementElement.lastChild
        else -> sqliteStatementElement
      }.text

  override fun startOffset(sqliteStatementElement: PsiElement) =
      when (sqliteStatementElement.elementType) {
        SqliteTokenTypes.RULE_ELEMENT_TYPES[RULE_create_table_stmt] -> sqliteStatementElement
        else -> sqliteStatementElement.lastChild
      }.startOffsetInParent

  companion object {
    fun create(file: PsiFile): TableGenerator {
      val parse = file.childOfType<ParseElement>()!!
      return TableGenerator(parse, file.virtualFile.path.relativePath(),
          ModuleUtil.findModuleForPsiElement(file)!!.moduleFile!!.parent.path + "/")
    }
  }
}
