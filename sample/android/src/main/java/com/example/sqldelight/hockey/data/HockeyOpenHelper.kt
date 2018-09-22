package com.example.sqldelight.hockey.data

import android.content.Context
import com.example.sqldelight.hockey.QueryWrapper
import com.squareup.sqldelight.android.AndroidSqlDatabase

object HockeyDb {
  private var queryWrapper: QueryWrapper? = null

  fun getInstance(context: Context): QueryWrapper {
    queryWrapper?.let { return it }

    return createQueryWrapper(AndroidSqlDatabase(Schema, context)).also {
      queryWrapper = it
    }
  }
}
