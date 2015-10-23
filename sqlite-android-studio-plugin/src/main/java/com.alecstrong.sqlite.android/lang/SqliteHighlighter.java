package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.SQLiteLexer;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.lexer.SimpleAntlrAdapter;
import org.antlr.intellij.adaptor.lexer.TokenElementType;
import org.jetbrains.annotations.NotNull;

public class SqliteHighlighter extends SyntaxHighlighterBase {
	private static final TextAttributesKey SQLITE_KEYWORD = TextAttributesKey.createTextAttributesKey(
			"SQLITE.KEYWORD",
			DefaultLanguageHighlighterColors.KEYWORD
	);
	private static final TextAttributesKey SQLITE_NUMBER = TextAttributesKey.createTextAttributesKey(
			"SQLITE.NUMBER",
			DefaultLanguageHighlighterColors.NUMBER
	);
	private static final TextAttributesKey SQLITE_IDENTIFIER = TextAttributesKey.createTextAttributesKey(
			"SQLITE.IDENTIFIER",
			DefaultLanguageHighlighterColors.IDENTIFIER
	);
	private static final TextAttributesKey SQLITE_STRING = TextAttributesKey.createTextAttributesKey(
			"SQLITE.STRING",
			DefaultLanguageHighlighterColors.STRING
	);
	private static final TextAttributesKey SQLITE_LINE_COMMENT = TextAttributesKey.createTextAttributesKey(
			"SQLITE.LINE_COMMENT",
			DefaultLanguageHighlighterColors.LINE_COMMENT
	);
	private static final TextAttributesKey SQLITE_MULTILINE_COMMENT = TextAttributesKey.createTextAttributesKey(
			"SQLITE.MULTILINE_COMMENT",
			DefaultLanguageHighlighterColors.BLOCK_COMMENT
	);
	private static final TextAttributesKey SQLITE_OPERATOR = TextAttributesKey.createTextAttributesKey(
			"SQLITE.OPERATOR",
			DefaultLanguageHighlighterColors.OPERATION_SIGN
	);
	private static final TextAttributesKey SQLITE_PAREN = TextAttributesKey.createTextAttributesKey(
			"SQLITE.PAREN",
			DefaultLanguageHighlighterColors.PARENTHESES
	);
	private static final TextAttributesKey SQLITE_DOT = TextAttributesKey.createTextAttributesKey(
			"SQLITE.DOT",
			DefaultLanguageHighlighterColors.DOT
	);
	private static final TextAttributesKey SQLITE_SEMICOLON = TextAttributesKey.createTextAttributesKey(
			"SQLITE.SEMICOLON",
			DefaultLanguageHighlighterColors.SEMICOLON
	);
	private static final TextAttributesKey SQLITE_COMMA = TextAttributesKey.createTextAttributesKey(
			"SQLITE.COMMA",
			DefaultLanguageHighlighterColors.COMMA
	);

	private static final int LAST_TOKEN = SQLiteLexer.UNEXPECTED_CHAR;
	private static final TextAttributesKey[] textAttributesKey = new TextAttributesKey[LAST_TOKEN+1];

	static {
		for (int i = SQLiteLexer.K_ABORT; i <= SQLiteLexer.K_WITHOUT; i++) {
			textAttributesKey[i] = SQLITE_KEYWORD;
		}
		for (int i = SQLiteLexer.ASSIGN; i <= SQLiteLexer.NOT_EQ2; i++) {
			textAttributesKey[i] = SQLITE_OPERATOR;
		}
		textAttributesKey[SQLiteLexer.NUMERIC_LITERAL] = SQLITE_NUMBER;
		textAttributesKey[SQLiteLexer.IDENTIFIER] = SQLITE_IDENTIFIER;
		textAttributesKey[SQLiteLexer.STRING_LITERAL] = SQLITE_STRING;
		textAttributesKey[SQLiteLexer.SINGLE_LINE_COMMENT] = SQLITE_LINE_COMMENT;
		textAttributesKey[SQLiteLexer.MULTILINE_COMMENT] = SQLITE_MULTILINE_COMMENT;
		textAttributesKey[SQLiteLexer.OPEN_PAR] = SQLITE_PAREN;
		textAttributesKey[SQLiteLexer.CLOSE_PAR] = SQLITE_PAREN;
		textAttributesKey[SQLiteLexer.DOT] = SQLITE_DOT;
		textAttributesKey[SQLiteLexer.SCOL] = SQLITE_SEMICOLON;
		textAttributesKey[SQLiteLexer.COMMA] = SQLITE_COMMA;
		textAttributesKey[SQLiteLexer.UNEXPECTED_CHAR] = HighlighterColors.BAD_CHARACTER;
	}

	@NotNull
	@Override
	public Lexer getHighlightingLexer() {
		return new SimpleAntlrAdapter(SqliteLanguage.INSTANCE, new SQLiteLexer(null));
	}

	@NotNull
	@Override
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
		if (tokenType instanceof TokenElementType) {
			return SyntaxHighlighterBase.pack(textAttributesKey[((TokenElementType) tokenType).getType()]);
		}
		return new TextAttributesKey[0];
	}
}
