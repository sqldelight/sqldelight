package com.alecstrong.sqlite.android;

import com.alecstrong.sqlite.android.lang.SqliteLanguage;
import com.alecstrong.sqlite.android.model.Column;
import com.alecstrong.sqlite.android.model.Table;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.PsiFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.antlr.intellij.adaptor.lexer.ElementTypeFactory;
import org.jetbrains.annotations.Nullable;

public class SqlDocumentAnnotator extends ExternalAnnotator<Table, Table> {
	@Nullable
	@Override
	public Table collectInformation(PsiFile file) {
		return forFile(file);
	}

	@Nullable
	@Override
	public Table doAnnotate(Table table) {
		return table;
	}

	@Override
	public void apply(PsiFile file, Table table, AnnotationHolder holder) {
		if (table != null) {
			LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
			File directory = new File(file.getProject().getBasePath() + "/build/generated-src");
			SqliteCompiler.write(table, directory);
			localFileSystem.findFileByIoFile(directory).refresh(true, true);
		}
		super.apply(file, table, holder);
	}

	private static Table forFile(PsiFile file) {
		if (file.getChildren().length == 0) return null;
		ASTNode parse = childrenForRules(file.getNode(), SQLiteParser.RULE_parse)[0];
		ASTNode sqlStatementList = childrenForRules(parse, SQLiteParser.RULE_sql_stmt_list)[0];
		ASTNode sqlStatement = childrenForRules(sqlStatementList, SQLiteParser.RULE_sql_stmt)[0];
		ASTNode createStatement = childrenForRules(sqlStatement, SQLiteParser.RULE_create_table_stmt)[0];

		String tableName = childrenForRules(createStatement, SQLiteParser.RULE_table_name)[0].getText();
		Table table = new Table("com.derp.derp", tableName);

		ASTNode[] columns = childrenForRules(createStatement, SQLiteParser.RULE_column_def);
		for (ASTNode column : columns) {
			String columnName = childrenForRules(column, SQLiteParser.RULE_column_name)[0].getText();
			Column.Type type = Column.Type.valueOf(childrenForRules(column, SQLiteParser.RULE_type_name)[0].getText());
			table.addColumn(new Column(columnName, type));
		}

		return table;
	}

	private static final List<String> RULES = Arrays.asList(SQLiteParser.ruleNames);

	private static ASTNode[] childrenForRules(ASTNode node, int... rules) {
		return node.getChildren(ElementTypeFactory.createRuleSet(SqliteLanguage.INSTANCE, RULES, rules));
	}
}
