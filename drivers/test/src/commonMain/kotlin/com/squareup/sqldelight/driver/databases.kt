package com.squareup.sqldelight.driver

import com.squareup.sqldelight.db.SqlDatabase

expect fun setupDatabase(schema: SqlDatabase.Schema): SqlDatabase
