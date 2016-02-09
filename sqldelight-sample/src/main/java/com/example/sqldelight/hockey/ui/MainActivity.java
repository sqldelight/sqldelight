package com.example.sqldelight.hockey.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.example.sqldelight.hockey.R;

public final class MainActivity extends Activity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);
    ButterKnife.bind(this);
  }

  @OnClick(R.id.list) public void showPlayers() {
    startActivity(new Intent(this, PlayersActivity.class));
  }

  @OnClick(R.id.teams) public void showTeams() {
    startActivity(new Intent(this, TeamsActivity.class));
  }
}
