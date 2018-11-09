package com.sample;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.squareup.sqldelight.prerelease.RowMapper;
import com.squareup.sqldelight.prerelease.SqlDelightStatement;
import java.lang.Deprecated;
import java.lang.Override;
import java.lang.String;

public interface MessageModel {
  @Deprecated
  String TABLE_NAME = "message";

  @Deprecated
  String MID = "mid";

  @Deprecated
  String FID = "fid";

  String CREATE_TABLE = ""
      + "CREATE TABLE message (\n"
      + "    mid         INTEGER PRIMARY KEY NOT NULL,\n"
      + "    fid         INTEGER NOT NULL\n"
      + ")";

  long mid();

  long fid();

  interface Creator<T extends MessageModel> {
    T create(long mid, long fid);
  }

  final class Mapper<T extends MessageModel> implements RowMapper<T> {
    private final Factory<T> messageModelFactory;

    public Mapper(@NonNull Factory<T> messageModelFactory) {
      this.messageModelFactory = messageModelFactory;
    }

    @Override
    public T map(@NonNull Cursor cursor) {
      return messageModelFactory.creator.create(
          cursor.getLong(0),
          cursor.getLong(1)
      );
    }
  }

  final class Factory<T extends MessageModel> {
    public final Creator<T> creator;

    public Factory(@NonNull Creator<T> creator) {
      this.creator = creator;
    }
  }

  final class Delete_orphans extends SqlDelightStatement {
    public Delete_orphans(@NonNull SupportSQLiteDatabase database) {
      super("folder", database.compileStatement(""
              + "DELETE FROM folder WHERE folder.fid IN (\n"
              + "  SELECT folder.fid FROM folder\n"
              + "  LEFT JOIN message ON message.fid=folder.fid\n"
              + ")"));
    }
  }

  final class Delete_orphans_2 extends SqlDelightStatement {
    public Delete_orphans_2(@NonNull SupportSQLiteDatabase database) {
      super("folder", database.compileStatement(""
              + "DELETE FROM folder WHERE folder.fid IN (\n"
              + "  SELECT folder.fid FROM folder WHERE fid = fid\n"
              + ")"));
    }
  }
}
