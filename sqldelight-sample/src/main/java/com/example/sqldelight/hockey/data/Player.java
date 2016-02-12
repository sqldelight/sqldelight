package com.example.sqldelight.hockey.data;

import com.google.auto.value.AutoValue;
import java.util.Calendar;

@AutoValue public abstract class Player implements PlayerModel {
  public enum Shoots {
    RIGHT, LEFT
  }

  public enum Position {
    LEFT_WING, RIGHT_WING, CENTER, DEFENSE, GOALIE
  }

  private static final DateAdapter DATE_ADAPTER = new DateAdapter();

  public static final Mapper<Player> MAPPER = new Mapper<>(new Mapper.Creator<Player>() {
    @Override
    public Player create(long id, String firstName, String lastName, int number, Long team, int age,
        float weight, Calendar birthDate, Shoots shoots, Position position) {
      return new AutoValue_Player(id, firstName, lastName, number, team, age, weight, birthDate,
          shoots, position);
    }
  }, DATE_ADAPTER);

  public static PlayerMarshal marshal() {
    return new PlayerMarshal(DATE_ADAPTER);
  }
}
