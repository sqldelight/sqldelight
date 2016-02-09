package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;

/** This TokenStream is just a {@link CommonTokenStream} that can be
 *  cut off at a particular index, such as the cursor in an IDE. I
 *  had to override more than I wanted to get this to work, but it seems okay.
 *
 *  All parsers used within the plug-in should use token streams of this type.
 */
public class TokenStreamSubset extends CommonTokenStream {
	public static final int STOP_TOKEN_TYPE = -3;
	//	protected int indexOfLastToken = -1;
	protected Token saveToken;

	public TokenStreamSubset(TokenSource tokenSource) {
		super(tokenSource);
	}

	public void setIndexOfLastToken(int indexOfLastToken) {
		System.out.println("setIndexOfLastToken("+indexOfLastToken+")");
		if ( indexOfLastToken<0 ) {
			System.out.println("replacing "+saveToken.getTokenIndex()+" with "+saveToken);
			tokens.set(saveToken.getTokenIndex(), saveToken);
//			this.indexOfLastToken = indexOfLastToken;
			return;
		}
		int i = indexOfLastToken + 1; // we want to keep token at indexOfLastToken
		sync(i);
		saveToken = tokens.get(i);
		System.out.println("saving "+saveToken);
		CommonToken stopToken = new CommonToken(saveToken);
		stopToken.setType(STOP_TOKEN_TYPE);
		System.out.println("setting "+i+" to "+stopToken);
		tokens.set(i, stopToken);
//		this.indexOfLastToken = indexOfLastToken;
	}
}