package com.alecstrong.sqlite.android;

public final class SqlitePluginException extends IllegalStateException {
  final Object originatingElement;

  public SqlitePluginException(Object originatingElement, String message) {
    super(message);
    this.originatingElement = originatingElement;
  }
}
