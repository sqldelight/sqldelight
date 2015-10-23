package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.lang.SqliteLanguage;
import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.SqlStmt;
import com.alecstrong.sqlite.android.model.Table;
import com.google.common.base.Joiner;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TableGenerator {
	private final List<String> RULES = Arrays.asList(SQLiteParser.ruleNames);
	private final List<String> TOKENS = Arrays.asList(SQLiteParser.tokenNames);

	Table<ASTNode> generateTable(PsiFile file) {
		if (file.getChildren().length == 0 || childrenForRules(file.getNode(), SQLiteParser.RULE_parse).length == 0)
			return null;
		ASTNode parse = childrenForRules(file.getNode(), SQLiteParser.RULE_parse)[0];
		ASTNode sqlStatementList = childrenForRules(parse, SQLiteParser.RULE_sql_stmt_list)[0];
		ASTNode sqlStatement = childrenForRules(sqlStatementList, SQLiteParser.RULE_sql_stmt)[0];
		ASTNode createStatement =
				childrenForRules(sqlStatement, SQLiteParser.RULE_create_table_stmt)[0];

		String packageName = getPackageName(childrenForRules(parse, SQLiteParser.RULE_package_stmt)[0]);
		String tableName = childrenForRules(createStatement, SQLiteParser.RULE_table_name)[0].getText();
		Table<ASTNode> table = new Table<>(packageName, tableName, createStatement, new File(file.getProject().getBasePath() + "/build/generated-src"));

		ASTNode[] columns = childrenForRules(createStatement, SQLiteParser.RULE_column_def);
		for (ASTNode column : columns) {
			String columnName = childrenForRules(column, SQLiteParser.RULE_column_name)[0].getText();
			Column.Type type =
					Column.Type.valueOf(childrenForRules(column, SQLiteParser.RULE_type_name)[0].getText());
			table.addColumn(new Column<>(columnName, type, column));
		}

		ASTNode[] sqlStmts = childrenForRules(sqlStatementList, SQLiteParser.RULE_sql_stmt);
		for (ASTNode sqlStmt : sqlStmts) {
			ASTNode[] children = childrenForTokens(sqlStmt, SQLiteParser.IDENTIFIER);
			if (children.length == 0) continue;
			String identifier = children[0].getText();
			String stmt = sqlStmt.getLastChildNode().getText();
			table.addSqlStmt(new SqlStmt<>(identifier, stmt, sqlStmt));
		}

		return table;
	}

	private String getPackageName(ASTNode packageNode) {
		List<String> names = new ArrayList<>();
		for (ASTNode name : childrenForRules(packageNode, SQLiteParser.RULE_name)) {
			names.add(name.getText());
		}
		return Joiner.on('.').join(names);
	}


	private ASTNode[] childrenForRules(ASTNode node, int... rules) {
		return node.getChildren(
				ElementTypeFactory.createRuleSet(SqliteLanguage.INSTANCE, RULES, rules));
	}


	private ASTNode[] childrenForTokens(ASTNode node, int... tokens) {
		return node.getChildren(
				ElementTypeFactory.createTokenSet(SqliteLanguage.INSTANCE, TOKENS, tokens));
	}
}
