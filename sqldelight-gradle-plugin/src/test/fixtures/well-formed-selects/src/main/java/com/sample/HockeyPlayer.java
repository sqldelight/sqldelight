package com.sample;

import com.squareup.sqldelight.ColumnAdapter;
import java.util.Calendar;

public abstract class HockeyPlayer implements HockeyPlayerModel {
  public enum Shoots {
    RIGHT, LEFT
  }

  public enum Position {
    LEFT_WING, RIGHT_WING, CENTER, DEFENSE, GOALIE
  }
}
