package com.sample;

import java.util.Calendar;

public abstract class HockeyPlayer implements HockeyPlayerModel {
  public enum Shoots {
    RIGHT, LEFT
  }

  public enum Position {
    LEFT_WING, RIGHT_WING, CENTER, DEFENSE, GOALIE
  }
}
