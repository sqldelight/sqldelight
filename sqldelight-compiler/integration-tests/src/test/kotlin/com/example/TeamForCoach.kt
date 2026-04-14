@file:Suppress("RedundantVisibilityModifier", "ASSIGNED_VALUE_IS_NEVER_READ")

package com.example

import kotlin.Long

public data class TeamForCoach(
  public val name: Team.Name,
  public val captain: Long,
)
