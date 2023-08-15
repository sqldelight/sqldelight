package app.cash.sqldelight.core.lang.types

import app.cash.sqldelight.core.lang.SqlDelightFile
import com.alecstrong.sql.psi.core.psi.SqlAnnotatedElement

val SqlAnnotatedElement.typeResolver
  get() = (containingFile as SqlDelightFile).typeResolver

val SqlAnnotatedElement.validator
  get() = (containingFile as SqlDelightFile).validator
