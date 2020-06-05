package com.squareup.sqldelight.core.dialects

import com.alecstrong.sql.psi.core.DialectPreset
import com.alecstrong.sql.psi.core.DialectPreset.HSQL
import com.alecstrong.sql.psi.core.DialectPreset.MYSQL
import com.alecstrong.sql.psi.core.DialectPreset.POSTGRESQL
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_18
import com.alecstrong.sql.psi.core.DialectPreset.SQLITE_3_24

internal val DialectPreset.textType
  get() = when (this) {
    MYSQL, SQLITE_3_24, SQLITE_3_18, POSTGRESQL -> "TEXT"
    HSQL -> "VARCHAR(8)"
  }

internal val DialectPreset.intType
  get() = when (this) {
    MYSQL, SQLITE_3_24, SQLITE_3_18, POSTGRESQL, HSQL -> "INTEGER"
  }
