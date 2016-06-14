package com.example.sqldelight.hockey.data;

import com.google.auto.value.AutoValue;
import com.squareup.sqldelight.RowMapper;
import java.util.Calendar;

@AutoValue
public abstract class Team implements TeamModel {
  private static final DateAdapter DATE_ADAPTER = new DateAdapter();

  public static final Factory<Team> FACTORY = new Factory<>(new Creator<Team>() {
    @Override public Team create(long Id, String name, Calendar founded, String coach, Long captain,
        boolean wonCup) {
      return new AutoValue_Team(Id, name, founded, coach, captain, wonCup);
    }
  }, DATE_ADAPTER);

  public static final RowMapper<Team> MAPPER = FACTORY.select_allMapper();

  public static Marshal marshal() {
    return new Marshal(DATE_ADAPTER);
  }
}
