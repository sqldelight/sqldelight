package com.example.sqldelight.hockey.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.example.sqldelight.hockey.R;
import com.example.sqldelight.hockey.data.Team;
import java.text.SimpleDateFormat;

public final class TeamRow extends LinearLayout {
  @Bind(R.id.team_name) TextView teamName;
  @Bind(R.id.coach_name) TextView coachName;
  @Bind(R.id.founded) TextView founded;

  private SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");

  public TeamRow(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this);
  }

  public void populate(Team team) {
    teamName.setText(team.name());
    coachName.setText(team.coach());
    founded.setText(getContext().getString(R.string.founded, df.format(team.founded().getTime())));
  }
}
