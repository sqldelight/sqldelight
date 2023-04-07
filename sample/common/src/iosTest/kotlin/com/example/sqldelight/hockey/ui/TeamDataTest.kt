package com.example.sqldelight.hockey.ui

import com.example.sqldelight.hockey.testing
import kotlin.test.Test
import kotlin.test.assertTrue

class TeamDataTest {
  @Test
  fun someData() = testing {
    assertTrue(TeamData.teams().isNotEmpty())
  }
}
