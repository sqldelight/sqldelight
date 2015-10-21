package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.SQLiteLexer;
import com.alecstrong.sqlite.android.SQLiteParser;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.extapi.psi.PsiElementBase;
import com.intellij.lang.*;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.SimpleAntlrAdapter;
import org.antlr.intellij.adaptor.lexer.TokenElementType;
import org.antlr.intellij.adaptor.parser.AntlrParser;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

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
		return new ASTWrapperPsiElement(node);
	}

	@Override
	public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
		return SpaceRequirements.MAY;
	}
}
