package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sqlite.psi.core.psi.SqliteIdentifier
import com.intellij.psi.PsiNamedElement

interface StmtIdentifier : PsiNamedElement {
  fun identifier(): SqliteIdentifier?
}