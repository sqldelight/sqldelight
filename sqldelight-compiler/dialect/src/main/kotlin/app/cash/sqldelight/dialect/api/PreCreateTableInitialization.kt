package app.cash.sqldelight.dialect.api

import com.intellij.psi.PsiElement

/**
 * A Dialect implements this interface on statement Mixin classes that must be executed before `CREATE TABLE` statements
 * in `.sq` files and where a schema is not derived from `.sqm` files.
 * Only used in `app.cash.sqldelight.core.lang.util.TreeUtilKt.forInitializationStatements` for use by `Schema.create`
 */
interface PreCreateTableInitialization : PsiElement
