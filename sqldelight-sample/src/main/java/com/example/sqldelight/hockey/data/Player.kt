package com.example.sqldelight.hockey.data

interface PlayerVals {
  enum class Shoots {
    RIGHT,
    LEFT
  }

  enum class Position {
    LEFT_WING,
    RIGHT_WING,
    CENTER,
    DEFENSE,
    GOALIE
  }
}
