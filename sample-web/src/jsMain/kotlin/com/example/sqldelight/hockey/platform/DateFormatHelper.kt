package com.example.sqldelight.hockey.platform

import com.example.sqldelight.hockey.data.Date

@JsModule("dateformat")
external fun dateFormat(date: Date, format: String): String
