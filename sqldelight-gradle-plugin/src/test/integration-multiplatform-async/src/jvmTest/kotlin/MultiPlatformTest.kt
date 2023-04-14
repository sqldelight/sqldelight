import app.sqltest.shared.common.data.SqlDriverFactoryProvider

actual abstract class MultiPlatformTest actual constructor() {
  actual fun getTestSqlDriverFactory() = SqlDriverFactoryProvider().getDriverFactory()
}
