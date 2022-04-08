package app.cash.sqldelight.dialect.api

interface SqlDelightModule {
  fun typeResolver(parentResolver: TypeResolver): TypeResolver
}
