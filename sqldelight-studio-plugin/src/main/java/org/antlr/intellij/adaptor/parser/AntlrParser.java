package org.antlr.intellij.adaptor.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.lexer.PsiTokenSource;
import org.antlr.intellij.plugin.parsing.TokenStreamSubset;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;

/** An adaptor that makes an ANTLR parser look like a PsiParser */
public abstract class AntlrParser<TParser extends Parser> implements PsiParser {
	private final Language language;

	protected AntlrParser(Language language) {
		this.language = language;
	}

	public Language getLanguage() {
		return language;
	}

	@NotNull
	@Override
	public ASTNode parse(IElementType root, PsiBuilder builder) {
		TParser parser = createParser(root, builder);
		ParseTree parseTree = parse(parser, root, builder);
		AstBuilderListener listener = createListener(parser, root, builder);
		walkParseTree(parseTree, listener, root, builder);
		return builder.getTreeBuilt();
	}

	protected TParser createParser(IElementType root, PsiBuilder builder) {
		TokenStream tokenStream = createTokenStreamImpl(root, builder);
		return createParserImpl(tokenStream, root, builder);
	}

	protected TokenStream createTokenStreamImpl(IElementType root, PsiBuilder builder) {
		TokenSource source = new PsiTokenSource(builder);
		return new TokenStreamSubset(source);
	}

	protected abstract TParser createParserImpl(TokenStream tokenStream, IElementType root, PsiBuilder builder);

	protected ParseTree parse(TParser parser, IElementType root, PsiBuilder builder) {
		PsiBuilder.Marker rollbackMarker = builder.mark();
		try {
			return parseImpl(parser, root, builder);
		}
		finally {
			rollbackMarker.rollbackTo();
		}
	}

	protected abstract ParseTree parseImpl(TParser parser, IElementType root, PsiBuilder builder);

	protected AstBuilderListener createListener(TParser parser, IElementType root, PsiBuilder builder) {
		return new AstBuilderListener(language, parser, builder);
	}

	/** Convert ANTLR parse tree to PSI tree. */
	protected void walkParseTree(ParseTree parseTree, AstBuilderListener listener, IElementType root, PsiBuilder builder) {
		PsiBuilder.Marker rootMarker = builder.mark();
		ParseTreeWalker.DEFAULT.walk(listener, parseTree);
		while (!builder.eof()) {
			builder.advanceLexer();
		}
		rootMarker.done(root);
	}
}