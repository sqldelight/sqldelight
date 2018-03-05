package com.example.sqldelight.hockey.ui;

import android.app.Activity;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.example.sqldelight.hockey.R;
import com.example.sqldelight.hockey.data.HockeyDb;
import com.example.sqldelight.hockey.data.Player;
import com.squareup.sqldelight.SqlDelightQuery;

public final class PlayersActivity extends Activity {
  public static final String TEAM_ID = "team_id";

  @BindView(R.id.list) ListView players;

  private Cursor playersCursor;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.list);
    ButterKnife.bind(this);

    SupportSQLiteDatabase db = HockeyDb.getInstance(this).getReadableDatabase();
    long teamId = getIntent().getLongExtra(TEAM_ID, -1);
    if (teamId == -1) {
      SqlDelightQuery selectAllStatement = Player.FACTORY.select_all();
      playersCursor = db.query(selectAllStatement);
    } else {
      SqlDelightQuery playerForTeam = Player.FACTORY.for_team(teamId);
      playersCursor = db.query(playerForTeam);
    }
    players.setAdapter(new PlayersAdapter(this, playersCursor));
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    playersCursor.close();
  }

  private static final class PlayersAdapter extends CursorAdapter {
    PlayersAdapter(Context context, Cursor c) {
      super(context, c, false);
    }

    @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
      return View.inflate(context, R.layout.player_row, null);
    }

    @Override public void bindView(View view, Context context, Cursor cursor) {
      Player.ForTeam playerForTeam = Player.FOR_TEAM_MAPPER.map(cursor);
      ((PlayerRow) view).populate(playerForTeam.player(), playerForTeam.team());
    }
  }
}
