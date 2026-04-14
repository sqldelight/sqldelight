@file:Suppress("RedundantVisibilityModifier", "ASSIGNED_VALUE_IS_NEVER_READ")

package app.cash.sqldelight.core.integration

enum class Shoots {
  LEFT,
  RIGHT,
  ;

  enum class Type {
    ONE,
    TWO,
  }
}
