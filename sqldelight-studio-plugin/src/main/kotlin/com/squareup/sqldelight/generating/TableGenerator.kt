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

import com.intellij.lang.ASTNode
import com.intellij.openapi.module.ModuleUtil
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
import com.squareup.sqldelight.lang.SqliteLanguage
import com.squareup.sqldelight.lang.SqliteTokenTypes
import com.squareup.sqldelight.model.Column
import com.squareup.sqldelight.model.ColumnConstraint.NotNullConstraint
import com.squareup.sqldelight.model.SqlStmt.Replacement
import com.squareup.sqldelight.util.RULES
import com.squareup.sqldelight.util.childrenWithRules
import com.squareup.sqldelight.util.childrenWithTokens
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory
import org.antlr.intellij.adaptor.lexer.TokenElementType

class TableGenerator constructor(parse: ASTNode, packageName: String?, fileName: String, modulePath: String)
: com.squareup.sqldelight.TableGenerator<ASTNode, ASTNode, ASTNode, ASTNode, ASTNode>
(parse, packageName, fileName, modulePath) {

  override fun sqlStatementElements(originatingElement: ASTNode) = originatingElement
      .childrenWithRules(RULE_sql_stmt_list)[0].childrenWithRules(RULE_sql_stmt).asList()

  override fun tableElement(sqlStatementElement: ASTNode) = sqlStatementElement
      .childrenWithRules(RULE_sql_stmt_list).firstOrNull()
      ?.childrenWithRules(RULE_create_table_stmt)?.firstOrNull()

  override fun identifier(sqlStatementElement: ASTNode) =
      sqlStatementElement.childrenWithRules(RULE_sql_stmt_name)[0].text

  override fun columnElements(tableElement: ASTNode) =
      tableElement.childrenWithRules(RULE_column_def).asList()

  override fun tableName(tableElement: ASTNode) =
      tableElement.childrenWithRules(RULE_table_name)[0].text

  override fun columnName(columnElement: ASTNode) =
      columnElement.childrenWithRules(RULE_column_name)[0].text

  override fun classLiteral(columnElement: ASTNode) = columnElement
      .childrenWithRules(RULE_type_name)[0]
      .childrenWithRules(RULE_sqlite_class_name).firstOrNull()
      ?.childrenWithTokens(SqliteParser.STRING_LITERAL)?.firstOrNull()?.text

  override fun typeName(columnElement: ASTNode) = columnElement
      .childrenWithRules(RULE_type_name)[0]
      .childrenWithRules(RULE_sqlite_class_name).firstOrNull()
      ?.firstChildNode?.text ?: columnElement.childrenWithRules(RULE_type_name)[0].text

  override fun replacementFor(columnElement: ASTNode, type: Column.Type): Replacement {
    val typeRange = columnElement.childrenWithRules(RULE_type_name)[0].textRange
    return Replacement(typeRange.startOffset, typeRange.endOffset, type.replacement)
  }

  override fun constraintElements(columnElement: ASTNode) =
      columnElement.childrenWithRules(SqliteParser.RULE_column_constraint).asList()

  override fun constraintFor(constraintElement: ASTNode, replacements: List<Replacement>) =
      constraintElement.getChildren(null)
          .map { it.elementType }
          .filterIsInstance<TokenElementType>()
          .mapNotNull {
            when (it.type) {
              SqliteParser.K_NOT -> NotNullConstraint(constraintElement)
              else -> null
            }
          }
          .firstOrNull()

  override fun text(sqliteStatementElement: ASTNode) =
      when (sqliteStatementElement.elementType) {
        SqliteTokenTypes.RULE_ELEMENT_TYPES[RULE_sql_stmt] -> sqliteStatementElement.lastChildNode
        else -> sqliteStatementElement
      }.text

  override fun startOffset(sqliteStatementElement: ASTNode) =
      when (sqliteStatementElement.elementType) {
        SqliteTokenTypes.RULE_ELEMENT_TYPES[RULE_create_table_stmt] -> sqliteStatementElement
        else -> sqliteStatementElement.lastChildNode
      }.startOffset

  companion object {
    fun create(file: PsiFile): TableGenerator {
      val parse = file.node.getChildren(ElementTypeFactory
          .createRuleSet(SqliteLanguage.INSTANCE, RULES, SqliteParser.RULE_parse))[0]
      return TableGenerator(parse,
          parse.childrenWithRules(SqliteParser.RULE_package_stmt).firstOrNull()
              ?.childrenWithRules(SqliteParser.RULE_name)
              ?.joinToString(separator = ".", transform = { it.text }),
          file.name,
          ModuleUtil.findModuleForPsiElement(file)!!.moduleFile!!.parent.path + "/")
    }
  }
}
