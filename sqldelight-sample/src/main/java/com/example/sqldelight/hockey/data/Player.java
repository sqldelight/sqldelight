package com.example.sqldelight.hockey.data;

import com.google.auto.value.AutoValue;
import com.squareup.sqldelight.ColumnAdapter;
import com.squareup.sqldelight.EnumColumnAdapter;
import com.squareup.sqldelight.RowMapper;
import java.util.Calendar;

@AutoValue public abstract class Player implements PlayerModel {
  public enum Shoots {
    RIGHT, LEFT
  }

  public enum Position {
    LEFT_WING, RIGHT_WING, CENTER, DEFENSE, GOALIE
  }

  private static final DateAdapter DATE_ADAPTER = new DateAdapter();
  private static final ColumnAdapter<Shoots> SHOOTS_ADAPTER = EnumColumnAdapter.create(Shoots.class);
  private static final ColumnAdapter<Position> POSITION_ADAPTER = EnumColumnAdapter.create(Position.class);

  public static final Factory<Player> FACTORY = new Factory<>(new Creator<Player>() {
    @Override
    public Player create(long id, String firstName, String lastName, int number, Long team, int age,
        float weight, Calendar birthDate, Shoots shoots, Position position) {
      return new AutoValue_Player(id, firstName, lastName, number, team, age, weight, birthDate,
          shoots, position);
    }
  }, DATE_ADAPTER, SHOOTS_ADAPTER, POSITION_ADAPTER);

  public static final RowMapper<ForTeam> FOR_TEAM_MAPPER = FACTORY.for_teamMapper(
      new For_teamCreator<Player, Team, ForTeam>() {
        @Override public ForTeam create(Player player, Team team) {
          return new AutoValue_Player_ForTeam(player, team);
        }
      }, Team.FACTORY);

  public static Marshal marshal() {
    return new Marshal(DATE_ADAPTER, SHOOTS_ADAPTER, POSITION_ADAPTER);
  }

  @AutoValue public static abstract class ForTeam implements For_teamModel<Player, Team> { }
}
