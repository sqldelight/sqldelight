package com.squareup.sqlite.android.model

sealed class ColumnConstraint<T>(originatingElement: T) : SqlElement<T>(originatingElement) {
  public class NotNullConstraint<T>(originatingElement: T) : ColumnConstraint<T>(originatingElement)
}
