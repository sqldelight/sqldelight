package org.antlr.intellij.adaptor.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the base class for implementations of {@link com.intellij.lexer.Lexer} backed by an ANTLR 4 lexer.
 *
 * <p>For lexers which do not store custom state information, the default implementation {@link SimpleAntlrAdapter} can
 * be used.</p>
 *
 * @author Sam Harwell
 */
public abstract class AntlrLexerAdapter<State extends AntlrLexerState> extends com.intellij.lexer.LexerBase {
	/**
	 * Gets the {@link Language} supported by this lexer. This value is passed to {@link ElementTypeFactory} to ensure
	 * the correct collection of {@link IElementType} is used for assigning element types to tokens in
	 * {@link #getTokenType}.
	 */
	private final Language language;
	/**
	 * This field caches the collection of element types returned by {@link ElementTypeFactory#getTokenElementTypes} for
	 * optimum efficiency of the {@link #getTokenType} method.
	 */
	private final List<? extends IElementType> tokenElementTypes;
	/**
	 * This is the backing field for {@link #getLexer()}.
	 */
	private final Lexer lexer;

	/**
	 * Provides a map from a {@code State} object &rarr; state index tracked by IntelliJ. This field provides for an
	 * efficient implementation of {@link #getState}.
	 */
	private final Map<State, Integer> stateCacheMap = new HashMap<State, Integer>();
	/**
	 * Provides a map from a state index tracked by IntelliJ &rarr; {@code State} object describing the ANTLR lexer
	 * state. This field provides for an efficient implementation of {@link #toLexerState}.
	 */
	private final List<State> stateCache = new ArrayList<State>();

	/**
	 * Caches the {@code buffer} provided in the call to {@link #start}, as required for implementing
	 * {@link #getBufferSequence}.
	 */
	private CharSequence buffer;
	/**
	 * Caches the {@code endOffset} provided in the call to {@link #start}, as required for implementing
	 * {@link #getBufferEnd}.
	 */
	private int endOffset;

	/**
	 * This field tracks the "exposed" lexer state, which differs from the actual current state of the lexer returned by
	 * {@link #getLexer()} by one token.
	 *
	 * <p>Due to the way IntelliJ requests token information, the ANTLR {@link Lexer} is always positioned one token
	 * past the token whose information is returned by calls to {@link #getTokenType}, {@link #getTokenType}, etc. When
	 * {@link #getState} is called, IntelliJ expects a state which is able to reproduce the {@link #currentToken}, but
	 * the ANTLR lexer has already moved past it. This field is assigned based in {@link #advance} based on the lexer
	 * state <em>before</em> the current token, after which {@link Lexer#nextToken} can be called to obtain
	 * {@link #currentToken}.</p>
	 */
	private State currentState;
	/**
	 * This field tracks the "exposed" lexer token. This is the result of the most recent call to
	 * {@link Lexer#nextToken} on the underlying ANTLR lexer, and is the source of information for
	 * {@link #getTokenStart}, {@link #getTokenType}, etc.
	 *
	 * @see #currentState
	 */
	private Token currentToken;

	/**
	 * Constructs a new instance of {@link AntlrLexerAdapter} with the specified {@link Language} and underlying
	 * ANTLR {@link Lexer}.
	 *
	 * @param language The language.
	 * @param lexer The underlying ANTLR lexer.
	 */
	public AntlrLexerAdapter(Language language, Lexer lexer) {
		this.language = language;
		this.tokenElementTypes = ElementTypeFactory.getTokenElementTypes(language, Arrays.asList(lexer.getTokenNames()));
		this.lexer = lexer;
	}

	/**
	 * Gets the ANTLR {@link Lexer} used for actual tokenization of the input.
	 *
	 * @return the ANTLR {@link Lexer} instance
	 */
	protected Lexer getLexer() {
		return lexer;
	}

	/**
	 * Gets the {@link Token} object providing information for calls to {@link #getTokenStart}, {@link #getTokenType},
	 * etc.
	 *
	 * @return The current {@link Token} instance.
	 */
	protected Token getCurrentToken() {
		return currentToken;
	}

	@Override
	public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
		this.buffer = buffer;
		this.endOffset = endOffset;

		CharStream in = new CharSequenceCharStream(buffer, endOffset, IntStream.UNKNOWN_SOURCE_NAME);
		in.seek(startOffset);

		State state;
		if (startOffset == 0 && initialState == 0) {
			state = getInitialState();
		} else {
			state = currentState != null ? currentState : getInitialState();
		}

		applyLexerState(in, state);
		advance();
	}

	@Nullable
	@Override
	public IElementType getTokenType() {
		int type = currentToken.getType();
		if (type == Token.EOF) {
			// return null when lexing is finished
			return null;
		}

		return tokenElementTypes.get(type);
	}

	@Override
	public void advance() {
		currentState = getLexerState(lexer);
		currentToken = lexer.nextToken();
	}

	@Override
	public int getState() {
		State state = currentState != null ? currentState : getInitialState();
		Integer existing = stateCacheMap.get(state);
		if (existing == null) {
			existing = stateCache.size();
			stateCache.add(state);
			stateCacheMap.put(state, existing);
		}

		return existing;
	}

	@Override
	public int getTokenStart() {
		return currentToken.getStartIndex();
	}

	@Override
	public int getTokenEnd() {
		return currentToken.getStopIndex() + 1;
	}

	@Override
	public CharSequence getBufferSequence() {
		return buffer;
	}

	@Override
	public int getBufferEnd() {
		return endOffset;
	}

	/**
	 * Update the current lexer to use the specified {@code input} stream starting in the specified {@code state}.
	 *
	 * <p>The current lexer may be obtained by calling {@link #getLexer}. The default implementation calls
	 * {@link Lexer#setInputStream} to set the input stream, followed by {@link AntlrLexerState#apply} to initialize the
	 * state of the lexer.</p>
	 *
	 * @param input The new input stream for the lexer.
	 * @param state A {@code State} instance containing the starting state for the lexer.
	 */
	protected void applyLexerState(CharStream input, State state) {
		lexer.setInputStream(input);
		state.apply(lexer);
	}

	/**
	 * Get the initial {@code State} of the lexer.
	 *
	 * @return a {@code State} instance representing the state of the lexer at the beginning of an input.
	 */
	protected abstract State getInitialState();

	/**
	 * Get a {@code State} instance representing the current state of the specified lexer.
	 *
	 * @param lexer The lexer.
	 * @return A {@code State} instance containing the current state of the lexer.
	 */
	protected abstract State getLexerState(Lexer lexer);

	/**
	 * Gets the {@code State} corresponding to the specified IntelliJ {@code state}.
	 *
	 * @param state The lexer state provided by IntelliJ.
	 * @return The {@code State} instance corresponding to the specified state.
	 */
	protected State toLexerState(int state) {
		return stateCache.get(state);
	}
}