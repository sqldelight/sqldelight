package com.sample

public data class PreferKotlinType(
  public val max_person: Person?,
  public val min_person: Person?,
  public val coalesce_person: Person,
  public val if_null_person: Person,
)
