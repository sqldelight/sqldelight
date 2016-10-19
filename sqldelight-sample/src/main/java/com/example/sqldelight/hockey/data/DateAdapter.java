package com.example.sqldelight.hockey.data;

import android.support.annotation.NonNull;
import com.squareup.sqldelight.ColumnAdapter;
import java.util.Calendar;

public final class DateAdapter implements ColumnAdapter<Calendar, Long> {
  @Override public Long encode(@NonNull Calendar date) {
    return date.getTimeInMillis();
  }

  @Override public Calendar decode(Long data) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(data);
    return calendar;
  }
}
