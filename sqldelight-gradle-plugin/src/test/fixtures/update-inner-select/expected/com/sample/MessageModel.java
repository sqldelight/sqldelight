package com.sample;

import android.database.Cursor;
import android.support.annotation.NonNull;
import com.squareup.sqldelight.prerelease.RowMapper;
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
      + "    mid INTEGER PRIMARY KEY NOT NULL,\n"
      + "    fid INTEGER NOT NULL\n"
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
}
