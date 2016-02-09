package com.example.sqldelight.hockey.data;

import android.content.ContentValues;
import android.database.Cursor;
import java.util.Calendar;

public final class DateMarshalMapper implements PlayerModel.PlayerMarshal.BirthDateMarshal,
    PlayerModel.Mapper.BirthDateMapper, TeamModel.TeamMarshal.FoundedMarshal,
    TeamModel.Mapper.FoundedMapper {
  @Override public void marshal(ContentValues contentValues, String columnName, Calendar date) {
    contentValues.put(columnName, date.getTimeInMillis());
  }

  @Override public Calendar map(Cursor cursor, int columnIndex) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(cursor.getLong(columnIndex));
    return calendar;
  }
}
