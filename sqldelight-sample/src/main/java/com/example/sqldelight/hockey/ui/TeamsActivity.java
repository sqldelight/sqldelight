package com.example.sqldelight.hockey.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.example.sqldelight.hockey.R;
import com.example.sqldelight.hockey.data.HockeyOpenHelper;
import com.example.sqldelight.hockey.data.Team;
import com.squareup.sqldelight.SqlDelightStatement;

public final class TeamsActivity extends Activity {
  @BindView(R.id.list) ListView teams;

  private Cursor teamsCursor;
  private Adapter adapter;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.list);
    ButterKnife.bind(this);

    SupportSQLiteDatabase db = HockeyOpenHelper.getInstance(this).getReadableDatabase();
    SqlDelightStatement selectAllStatement = Team.FACTORY.select_all();
    teamsCursor = db.rawQuery(selectAllStatement.statement, selectAllStatement.args);
    adapter = new Adapter(this, teamsCursor);
    teams.setAdapter(adapter);
    teams.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(TeamsActivity.this, PlayersActivity.class);
        intent.putExtra(PlayersActivity.TEAM_ID, id);
        startActivity(intent);
      }
    });
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    teamsCursor.close();
  }

  private static final class Adapter extends CursorAdapter {
    public Adapter(Context context, Cursor c) {
      super(context, c, false);
    }

    @Override public View newView(Context context, Cursor cursor, ViewGroup parent) {
      return View.inflate(context, R.layout.team_row, null);
    }

    @Override public void bindView(View view, Context context, Cursor cursor) {
      ((TeamRow) view).populate(Team.MAPPER.map(cursor));
    }
  }
}
