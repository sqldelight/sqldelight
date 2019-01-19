package com.example.sqldelight.hockey.ui

import com.example.sqldelight.hockey.setDriver
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class TeamDataTest{
  @BeforeTest
  fun initDb(){
    setDriver()
  }

  @Test
  fun someData() {
    TeamData {
      assertTrue { it.size > 0 }
    }
  }
}