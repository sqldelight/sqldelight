package app.sqltest.shared.common.data

expect class SqlDriverFactoryProvider {
  fun getDriverFactory(name: String = "SqlTest.db"): DriverFactory
}
