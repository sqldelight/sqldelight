package com.alecstrong.sqlite.android.lang;

import com.alecstrong.sqlite.android.SQLiteParser;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.parser.AntlrParser;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class SqliteParser extends AntlrParser<SQLiteParser> {
	protected SqliteParser() {
		super(SqliteLanguage.INSTANCE);
	}

	@Override
	protected SQLiteParser createParserImpl(TokenStream tokenStream, IElementType root, PsiBuilder builder) {
		SQLiteParser parser = new SQLiteParser(tokenStream);
		parser.removeErrorListeners();
		parser.addErrorListener(new SyntaxErrorListener());
		return parser;
	}

	@Override
	protected ParseTree parseImpl(SQLiteParser parser, IElementType root, PsiBuilder builder) {
		return parser.parse();
	}
}
