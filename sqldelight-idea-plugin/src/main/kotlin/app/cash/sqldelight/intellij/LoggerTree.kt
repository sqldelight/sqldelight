package app.cash.sqldelight.intellij

import com.intellij.openapi.diagnostic.Logger
import timber.log.Timber

internal class LoggerTree(
  private val logger: Logger
) : Timber.Tree() {
  override fun v(message: String?, vararg args: Any?) {
    logger.info(String.format(message!!, args))
  }

  override fun v(t: Throwable?, message: String?, vararg args: Any?) {
    logger.info(String.format(message!!, args), t!!)
  }

  override fun v(t: Throwable?) {
    logger.info(t!!)
  }

  override fun d(message: String?, vararg args: Any?) {
    logger.debug(String.format(message!!, args))
  }

  override fun d(t: Throwable?, message: String?, vararg args: Any?) {
    logger.debug(String.format(message!!, args), t!!)
  }

  override fun d(t: Throwable?) {
    logger.debug(t!!)
  }

  override fun i(message: String?, vararg args: Any?) {
    logger.info(String.format(message!!, args))
  }

  override fun i(t: Throwable?, message: String?, vararg args: Any?) {
    logger.info(String.format(message!!, args), t)
  }

  override fun i(t: Throwable?) {
    logger.info(t!!)
  }

  override fun w(message: String?, vararg args: Any?) {
    logger.warn(String.format(message!!, args))
  }

  override fun w(t: Throwable?, message: String?, vararg args: Any?) {
    logger.warn(String.format(message!!, args), t!!)
  }

  override fun w(t: Throwable?) {
    logger.warn(t!!)
  }

  override fun e(message: String?, vararg args: Any?) {
    logger.error(String.format(message!!, args))
  }

  override fun e(t: Throwable?, message: String?, vararg args: Any?) {
    logger.error(String.format(message!!, args), t!!)
  }

  override fun e(t: Throwable?) {
    logger.error(t!!)
  }

  override fun wtf(message: String?, vararg args: Any?) {
    logger.error(String.format(message!!, args))
  }

  override fun wtf(t: Throwable?, message: String?, vararg args: Any?) {
    logger.error(String.format(message!!, args), t!!)
  }

  override fun wtf(t: Throwable?) {
    logger.error(t!!)
  }

  override fun log(priority: Int, message: String?, vararg args: Any?) {
    throw UnsupportedOperationException()
  }

  override fun log(priority: Int, t: Throwable?, message: String?, vararg args: Any?) {
    throw UnsupportedOperationException()
  }

  override fun log(priority: Int, t: Throwable?) {
    throw UnsupportedOperationException()
  }

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    throw UnsupportedOperationException()
  }
}
