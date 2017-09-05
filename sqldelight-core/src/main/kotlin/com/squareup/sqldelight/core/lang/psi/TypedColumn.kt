package com.squareup.sqldelight.core.lang.psi

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName

internal interface TypedColumn {
  /**
   * @return the java type for this column.
   */
  fun type(): TypeName

  /**
   * @return the adapter property which will include the type and name of the adapter. If this
   * column does not require an adapter, returns null.
   */
  fun adapter(): PropertySpec?
}
