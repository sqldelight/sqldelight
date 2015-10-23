package com.alecstrong.sqlite.android.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.SingleLazyInstanceSyntaxHighlighterFactory;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import org.jetbrains.annotations.NotNull;

public class SqliteLanguage extends Language {
	public static SqliteLanguage INSTANCE = new SqliteLanguage();

	public SqliteLanguage() {
		super("Sqlite", "text/sqlite");
		SyntaxHighlighterFactory.LANGUAGE_FACTORY.addExplicitExtension(this, new SingleLazyInstanceSyntaxHighlighterFactory() {
			@NotNull
			protected SyntaxHighlighter createHighlighter() {
				return new SqliteHighlighter();
			}
		});
	}
}
