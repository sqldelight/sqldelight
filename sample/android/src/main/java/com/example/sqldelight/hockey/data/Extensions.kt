package com.example.sqldelight.hockey

import android.content.Context
import android.os.Looper
import com.example.sqldelight.hockey.data.JvmDb
import com.example.sqldelight.hockey.data.Db
import com.example.sqldelight.hockey.data.Schema
import com.squareup.sqldelight.android.AndroidSqliteDriver

fun Db.getInstance(contex:Context):HockeyDb{
  //Context routing is a little ugly here. All calls to this
  //method need to be from main thread
  checkMainThread()

  if(JvmDb.driver == null){
    JvmDb.driver = AndroidSqliteDriver(Schema, contex)
  }
  return Db.instance
}

internal fun checkMainThread(){
  if(Looper.getMainLooper() !== Looper.myLooper())
    throw IllegalStateException("Not main thread")
}