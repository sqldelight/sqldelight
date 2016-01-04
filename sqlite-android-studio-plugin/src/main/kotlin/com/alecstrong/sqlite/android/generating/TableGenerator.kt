package com.alecstrong.sqlite.android.generating

import com.alecstrong.sqlite.android.SQLiteParser
import com.alecstrong.sqlite.android.SQLiteParser.IDENTIFIER
import com.alecstrong.sqlite.android.SQLiteParser.K_KEY_VALUE
import com.alecstrong.sqlite.android.SQLiteParser.RULE_column_def
import com.alecstrong.sqlite.android.SQLiteParser.RULE_column_name
import com.alecstrong.sqlite.android.SQLiteParser.RULE_create_table_stmt
import com.alecstrong.sqlite.android.SQLiteParser.RULE_sql_stmt
import com.alecstrong.sqlite.android.SQLiteParser.RULE_sql_stmt_list
import com.alecstrong.sqlite.android.SQLiteParser.RULE_sqlite_class_name
import com.alecstrong.sqlite.android.SQLiteParser.RULE_table_name
import com.alecstrong.sqlite.android.SQLiteParser.RULE_type_name
import com.alecstrong.sqlite.android.lang.SqliteLanguage
import com.alecstrong.sqlite.android.lang.SqliteTokenTypes
import com.alecstrong.sqlite.android.model.Column
import com.alecstrong.sqlite.android.model.ColumnConstraint.NotNullConstraint
import com.alecstrong.sqlite.android.model.SqlStmt.Replacement
import com.alecstrong.sqlite.android.util.RULES
import com.alecstrong.sqlite.android.util.childrenWithRules
import com.alecstrong.sqlite.android.util.childrenWithTokens
import com.intellij.lang.ASTNode
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiFile
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory
import org.antlr.intellij.adaptor.lexer.TokenElementType

class TableGenerator constructor(parse: ASTNode, packageName: String?, fileName: String, modulePath: String)
: com.alecstrong.sqlite.android.TableGenerator<ASTNode, ASTNode, ASTNode, ASTNode, ASTNode>
(parse, packageName, fileName, modulePath) {

  override fun sqlStatementElements(originatingElement: ASTNode) = originatingElement
      .childrenWithRules(RULE_sql_stmt_list)[0].childrenWithRules(RULE_sql_stmt).asList()

  override fun tableElement(sqlStatementElement: ASTNode) = sqlStatementElement
      .childrenWithRules(RULE_sql_stmt_list).firstOrNull()
      ?.childrenWithRules(RULE_create_table_stmt)?.firstOrNull()

  override fun identifier(sqlStatementElement: ASTNode) =
      sqlStatementElement.childrenWithTokens(IDENTIFIER)[0].text

  override fun columnElements(tableElement: ASTNode) =
      tableElement.childrenWithRules(RULE_column_def).asList()

  override fun tableName(tableElement: ASTNode) =
      tableElement.childrenWithRules(RULE_table_name)[0].text

  override fun isKeyValue(tableElement: ASTNode) =
      tableElement.childrenWithTokens(K_KEY_VALUE).size == 1

  override fun columnName(columnElement: ASTNode) =
      columnElement.childrenWithRules(RULE_column_name)[0].text

  override fun classLiteral(columnElement: ASTNode) = columnElement
      .childrenWithRules(RULE_type_name)[0]
      .childrenWithRules(RULE_sqlite_class_name).firstOrNull()
      ?.childrenWithTokens(SQLiteParser.STRING_LITERAL)?.firstOrNull()?.text

  override fun typeName(columnElement: ASTNode) = columnElement
      .childrenWithRules(RULE_type_name)[0]
      .childrenWithRules(RULE_sqlite_class_name).firstOrNull()
      ?.firstChildNode?.text ?: columnElement.childrenWithRules(RULE_type_name)[0].text

  override fun replacementFor(columnElement: ASTNode, type: Column.Type): Replacement {
    val typeRange = columnElement.childrenWithRules(RULE_type_name)[0].textRange
    return Replacement(typeRange.startOffset, typeRange.endOffset, type.replacement)
  }

  override fun constraintElements(columnElement: ASTNode) =
      columnElement.childrenWithRules(SQLiteParser.RULE_column_constraint).asList()

  override fun constraintFor(constraintElement: ASTNode, replacements: List<Replacement>) =
      constraintElement.getChildren(null)
          .map { it.elementType }
          .filterIsInstance<TokenElementType>()
          .mapNotNull {
            when (it.type) {
              SQLiteParser.K_NOT -> NotNullConstraint(constraintElement)
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
          .createRuleSet(SqliteLanguage.INSTANCE, RULES, SQLiteParser.RULE_parse))[0]
      return TableGenerator(parse,
          parse.childrenWithRules(SQLiteParser.RULE_package_stmt).firstOrNull()
              ?.childrenWithRules(SQLiteParser.RULE_name)
              ?.joinToString(separator = ".", transform = { it.text }),
          file.name,
          ModuleUtil.findModuleForPsiElement(file)!!.moduleFile!!.parent.path + "/")
    }
  }
}
