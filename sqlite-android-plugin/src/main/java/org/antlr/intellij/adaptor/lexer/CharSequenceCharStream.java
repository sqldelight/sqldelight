package org.antlr.intellij.adaptor.lexer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.misc.Interval;

/**
 * This class provides a basic implementation of
 * {@link org.antlr.v4.runtime.CharStream} backed by an arbitrary
 * {@link CharSequence}.
 *
 * It also handles the situation where intellij intellij adds
 */
class CharSequenceCharStream implements CharStream {
	private final CharSequence buffer;
	/**
	 * If greater than or equal to 0, this value overrides the value returned by
	 * {@link #buffer}{@code .}{@link CharSequence#length()}.
	 */
	private final int endOffset;
	private final String sourceName;

	private int position;

	public CharSequenceCharStream(CharSequence buffer, int endOffset, String sourceName) {
		this.buffer = buffer;
		this.sourceName = sourceName;
		this.endOffset = endOffset;
	}

	protected final CharSequence getBuffer() {
		return buffer;
	}

	protected final int getPosition() {
		return position;
	}

	protected final void setPosition(int position) {
		this.position = position;
	}

	@Override
	public String getText(Interval interval) {
		int start = interval.a;
		int stop = interval.b;
		int n = size();
		if ( stop >= n ) stop = n-1;
		if ( start >= n ) return "";
		return buffer.subSequence(start, stop + 1).toString();
	}

	@Override
	public void consume() {
		if (position == size()) {
			throw new IllegalStateException("attempted to consume EOF");
		}

		position++;
	}

	@Override
	public int LA(int i) {
		if (i > 0) {
			int index = position + i - 1;
			if (index >= size() ) {
				return IntStream.EOF;
			}

			return buffer.charAt(index);
		}
		else if (i < 0) {
			int index = position + i;
			if (index < 0) {
				return 0;
			}

			return buffer.charAt(index);
		}
		else {
			return 0;
		}
	}

	@Override
	public int mark() {
		return 0;
	}

	@Override
	public void release(int marker) {
	}

	@Override
	public int index() {
		return position;
	}

	@Override
	public void seek(int index) {
		if (index < 0) {
			throw new IllegalArgumentException("index cannot be negative");
		}

		index = Math.min(index, size());
		position = index;
	}

	@Override
	public int size() {
		if (endOffset >= 0) {
			return endOffset;
		}

		int n = buffer.length();
		return n;
	}

	@Override
	public String getSourceName() {
		return sourceName;
	}
}