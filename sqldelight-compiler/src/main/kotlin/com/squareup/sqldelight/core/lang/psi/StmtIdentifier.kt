package com.squareup.sqldelight.core.lang.psi

import com.alecstrong.sql.psi.core.psi.SqlIdentifier
import com.intellij.psi.PsiNamedElement

interface StmtIdentifier : PsiNamedElement {
  fun identifier(): SqlIdentifier?
}
