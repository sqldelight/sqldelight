package org.antlr.intellij.adaptor.lexer;

import net.jcip.annotations.Immutable;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.misc.IntegerStack;
import org.antlr.v4.runtime.misc.MurmurHash;
import org.antlr.v4.runtime.misc.ObjectEqualityComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class stores the state of an ANTLR lexer, such that it can be applied back to the lexer instance at a later
 * time.
 *
 * <p>The default implementation stores the following fields, which provides support for any ANTLR 4 single- or
 * multi-mode lexer that does not rely on custom state information for semantic predicates, custom embedded actions,
 * and/or overridden methods such as {@link Lexer#nextToken} or {@link Lexer#emit}.</p>
 *
 * <ul>
 *     <li>{@link Lexer#_mode}: The current lexer mode.</li>
 *     <li>{@link Lexer#_modeStack}: The current lexer mode stack.</li>
 * </ul>
 *
 * <p>If your lexer requires additional information to be stored, this class must be extended in the following ways.</p>
 *
 * <ol>
 *     <li>Override {@link #apply} to ensure that the additional state information is applied to the provided lexer
 *     instance.</li>
 *     <li>Override {@link #hashCodeImpl} and {@link #equals} to ensure that the caching features provided by
 *     {@link AbstractAntlrAdapter} are able to efficiently store the resulting state instances.</li>
 * </ol>
 */
@Immutable
public class AntlrLexerState {
	/**
	 * This is the backing field for {@link #getMode}.
	 */
	private final int mode;
	/**
	 * This is the backing field for {@link #getModeStack}.
	 */
	@Nullable
	private final int[] modeStack;

	/**
	 * This field stores the cached hash code to maximize the efficiency of {@link #hashCode}.
	 */
	private int cachedHashCode;

	/**
	 * Constructs a new instance of {@link AntlrLexerState} containing the mode and mode stack information for an ANTLR
	 * lexer.
	 *
	 * @param mode The current lexer mode, {@link Lexer#_mode}.
	 * @param modeStack The lexer mode stack, {@link Lexer#_modeStack}, or {@code null} .
	 */
	public AntlrLexerState(int mode, @Nullable IntegerStack modeStack) {
		this.mode = mode;
		this.modeStack = modeStack != null ? modeStack.toArray() : null;
	}

	/**
	 * Gets the value of {@link Lexer#_mode} for the current lexer state.
	 *
	 * @return The value of {@link Lexer#_mode} for the current lexer state.
	 */
	public int getMode() {
		return mode;
	}

	/**
	 * Gets the mode stack stored in {@link Lexer#_modeStack} for the current lexer state.
	 *
	 * @return The mode stack stored in {@link Lexer#_modeStack} for the current lexer state, or {@code null} if the
	 * mode stack is empty.
	 */
	@Nullable
	public int[] getModeStack() {
		return modeStack;
	}

	public void apply(@NotNull Lexer lexer) {
		lexer._mode = getMode();
		lexer._modeStack.clear();
		if (getModeStack() != null) {
			lexer._modeStack.addAll(getModeStack());
		}
	}

	@Override
	public final int hashCode() {
		if (cachedHashCode == 0) {
			cachedHashCode = hashCodeImpl();
		}

		return cachedHashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof AntlrLexerState)) {
			return false;
		}

		AntlrLexerState other = (AntlrLexerState)obj;
		return this.mode == other.mode
				&& ObjectEqualityComparator.INSTANCE.equals(this.modeStack, other.modeStack);
	}

	protected int hashCodeImpl() {
		int hash = MurmurHash.initialize();
		hash = MurmurHash.update(hash, mode);
		hash = MurmurHash.update(hash, modeStack);
		return MurmurHash.finish(hash, 2);
	}
}