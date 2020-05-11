package com.example.sqldelight.hockey.ui

import com.example.sqldelight.hockey.BaseTest
import kotlin.test.Test
import kotlin.test.assertTrue

class TeamDataTest : BaseTest() {
  @Test
  fun someData() {
    assertTrue(TeamData.teams().isNotEmpty())
  }
}
