package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.SQLiteLexer;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import java.util.Arrays;
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.SimpleAntlrAdapter;
import org.jetbrains.annotations.NotNull;

public class SqliteParserDefinition implements ParserDefinition {
	public static final IFileElementType FILE =
			new IFileElementType(SqliteLanguage.INSTANCE);
	@NotNull
	@Override
	public Lexer createLexer(Project project) {
		return new SimpleAntlrAdapter(SqliteLanguage.INSTANCE, new SQLiteLexer(null));
	}

	@Override
	public PsiParser createParser(Project project) {
		return new SqliteParser();
	}

	@NotNull
	@Override
	public TokenSet getWhitespaceTokens() {
		return ElementTypeFactory.createTokenSet(SqliteLanguage.INSTANCE, Arrays.asList(SQLiteLexer.tokenNames), SQLiteLexer.SPACES);
	}

	@NotNull
	@Override
	public TokenSet getCommentTokens() {
		return ElementTypeFactory.createTokenSet(SqliteLanguage.INSTANCE, Arrays.asList(SQLiteLexer.tokenNames), SQLiteLexer.SINGLE_LINE_COMMENT, SQLiteLexer.MULTILINE_COMMENT);
	}

	@NotNull
	@Override
	public TokenSet getStringLiteralElements() {
		return ElementTypeFactory.createTokenSet(SqliteLanguage.INSTANCE, Arrays.asList(SQLiteLexer.tokenNames), SQLiteLexer.SINGLE_LINE_COMMENT, SQLiteLexer.STRING_LITERAL);
	}

	@Override
	public IFileElementType getFileNodeType() {
		return FILE;
	}

	@Override
	public PsiFile createFile(FileViewProvider viewProvider) {
		return new SqliteFile(viewProvider);
	}

	@NotNull
	@Override
	public PsiElement createElement(ASTNode node) {
		return SqliteASTFactory.createInternalParseTreeNode(node);
	}

	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
		return SpaceRequirements.MAY;
	}
}
