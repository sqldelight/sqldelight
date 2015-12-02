package com.alecstrong.sqlite.android.model;

public abstract class SqlElement<T> {
	private final T originatingElement;

	protected SqlElement(T originatingElement) {
		this.originatingElement = originatingElement;
	}

	public T getOriginatingElement() {
		return originatingElement;
	}
}
