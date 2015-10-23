package org.antlr.intellij.adaptor.lexer;

import com.intellij.lang.PsiBuilder;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenFactory;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.misc.Pair;

public class PsiTokenSource implements TokenSource {
	protected PsiBuilder builder;
	protected TokenFactory factory = CommonTokenFactory.DEFAULT;

	public PsiTokenSource(PsiBuilder builder) {
		this.builder = builder;
	}

	/* Colin: "the parsing lexer still has to return tokens that completely
	 cover the file (i.e. no gaps). This is one of the most significant
	 differences from a traditional compiler parser/lexer."
	  after lots of trial and error I finally just put the BAD_TOKEN
	  into the white space class so that they do not come to the parser
	  but that IDEA still knows about them.
	  parrt: this seems no longer to be true after Sam's re-factoring
	 */
	@Override
	public Token nextToken() {
		TokenElementType ideaTType = (TokenElementType)builder.getTokenType();
		int type;
		if ( ideaTType==null ) {
			type = Token.EOF;
		}
		else {
			type = ideaTType.getType();
		}

		int channel = Token.DEFAULT_CHANNEL;
		Pair<TokenSource, CharStream> source = new Pair<TokenSource, CharStream>(this, null);
		String text = builder.getTokenText();
		int start = builder.getCurrentOffset();
		int length = text != null ? text.length() : 0;
		int stop = start + length - 1;
		// PsiBuilder doesn't provide line, column info
		int line = 0;
		int charPositionInLine = 0;
		Token t = factory.create(source, type, text, channel, start, stop, line, charPositionInLine);
		builder.advanceLexer();
//		System.out.println("TOKEN: "+t);
		return t;
	}

	@Override
	public int getLine() {
		return 0;
	}

	@Override
	public int getCharPositionInLine() {
		return 0;
	}

	@Override
	public CharStream getInputStream() {
		return null;
	}

	@Override
	public String getSourceName() {
		return "IDEA PsiBuilder";
	}

	@Override
	public void setTokenFactory(TokenFactory<?> factory) {
		this.factory = factory;
	}

	@Override
	public TokenFactory<?> getTokenFactory() {
		return factory;
	}
}