package org.antlr.intellij.adaptor.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.Utils;

import java.util.ArrayList;
import java.util.List;

/** Traps errors from parsing language of plugin. E.g., for a Java plugin,
 *  this would catch errors when people type invalid Java code into .java file.
 *  For ANTLRv4 plugin, it traps errors for erroneous grammars.
 *  This swallows the errors as the PSI tree has error nodes.
 *
 *  Instance created by GrammarParser.createParserImpl().
 */
public class SyntaxErrorListener extends BaseErrorListener {
	private final List<SyntaxError> syntaxErrors = new ArrayList<SyntaxError>();

	public SyntaxErrorListener() {
	}

	public List<SyntaxError> getSyntaxErrors() {
		return syntaxErrors;
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer,
													Object offendingSymbol,
													int line, int charPositionInLine,
													String msg, RecognitionException e)
	{
		syntaxErrors.add(new SyntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e));
	}

	@Override
	public String toString() {
		return Utils.join(syntaxErrors.iterator(), "\n");
	}
}