package com.alecstrong.sqlite.android.model;

public class SqlElement<T> {
	private final T originatingElement;

	public SqlElement(T originatingElement) {
		this.originatingElement = originatingElement;
	}

	public T getOriginatingElement() {
		return originatingElement;
	}
}
