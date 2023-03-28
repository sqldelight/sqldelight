import app.sqltest.shared.common.data.DriverFactory

expect abstract class MultiPlatformTest() {
  fun getTestSqlDriverFactory(): DriverFactory
}
