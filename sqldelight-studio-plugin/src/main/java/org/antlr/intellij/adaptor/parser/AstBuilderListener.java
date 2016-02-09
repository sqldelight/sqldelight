package org.antlr.intellij.adaptor.parser;

import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import org.antlr.intellij.adaptor.lexer.ElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.RuleElementType;
import org.antlr.intellij.adaptor.lexer.TokenElementType;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/** This is how we build an intellij PSI parse tree (which they erroneously call
 *  an AST).  We let the ANTLR parser build its kind of ParseTree and then
 *  we convert to a PSI tree in one go using a standard ANTLR ParseTreeListener.
 */
public class AstBuilderListener implements ParseTreeListener {
	private final Language language;
	private final PsiBuilder builder;
	private final List<SyntaxError> syntaxErrors = new ArrayList<SyntaxError>();
	private final Deque<PsiBuilder.Marker> markers = new ArrayDeque<PsiBuilder.Marker>();

	private final List<TokenElementType> tokenElementTypes;
	private final List<RuleElementType> ruleElementTypes;

	private int nextSyntaxError;

	public AstBuilderListener(Language language, Parser parser, PsiBuilder builder) {
		this.language = language;
		this.builder = builder;

		this.tokenElementTypes = ElementTypeFactory.getTokenElementTypes(language, Arrays.asList(parser.getTokenNames()));
		this.ruleElementTypes = ElementTypeFactory.getRuleElementTypes(language, Arrays.asList(parser.getRuleNames()));

		for (ANTLRErrorListener listener : parser.getErrorListeners()) {
			if (listener instanceof SyntaxErrorListener) {
				syntaxErrors.addAll(((SyntaxErrorListener)listener).getSyntaxErrors());
			}
		}

		Collections.sort(syntaxErrors, new Comparator<SyntaxError>() {

			@Override
			public int compare(SyntaxError o1, SyntaxError o2) {
				return Integer.valueOf(getStart(o1)).compareTo(getStart(o2));
			}
		});
	}

	protected final Language getLanguage() {
		return language;
	}

	protected final PsiBuilder getBuilder() {
		return builder;
	}

	protected final Deque<PsiBuilder.Marker> getMarkers() {
		return markers;
	}

	protected final List<TokenElementType> getTokenElementTypes() {
		return tokenElementTypes;
	}

	protected final List<RuleElementType> getRuleElementTypes() {
		return ruleElementTypes;
	}

	@Override
	public void visitTerminal(TerminalNode node) {
		visitTerminalImpl(node);
	}

	@Override
	public void visitErrorNode(ErrorNode node) {
		visitTerminalImpl(node);
	}

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
		markers.push(getBuilder().mark());
	}

	@Override
	public void exitEveryRule(ParserRuleContext ctx) {
		PsiBuilder.Marker marker = markers.pop();
		marker.done(getRuleElementTypes().get(ctx.getRuleIndex()));
	}

	protected void visitTerminalImpl(TerminalNode node) {
		if (node.getSymbol().getType() == Token.EOF) {
			return;
		}

		// properly recovers from parse tree alterations due to error recovery efforts
		while (builder.getCurrentOffset() <= node.getSymbol().getStartIndex()) {
			PsiBuilder.Marker errorMarker = null;
			if (nextSyntaxError < syntaxErrors.size() && builder.getCurrentOffset() >= getStart(syntaxErrors.get(nextSyntaxError))) {
				errorMarker = builder.mark();
			}

			builder.advanceLexer();
			if (errorMarker != null) {
				StringBuilder message = new StringBuilder();
				while (nextSyntaxError < syntaxErrors.size() && builder.getCurrentOffset() >= getStart(syntaxErrors.get(nextSyntaxError))) {
					message.append(String.format("%s%n", syntaxErrors.get(nextSyntaxError).getMessage()));
					nextSyntaxError++;
				}

				errorMarker.error(message.toString());
			}
		}
	}

	private static int getStart(SyntaxError syntaxError) {
		if (syntaxError.getOffendingSymbol() instanceof Token) {
			return ((Token)syntaxError.getOffendingSymbol()).getStartIndex();
		}

		return 0;
	}
}