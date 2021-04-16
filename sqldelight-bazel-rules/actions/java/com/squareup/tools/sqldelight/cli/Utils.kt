package com.squareup.tools.sqldelight.cli

import com.beust.jcommander.JCommander
import java.lang.IllegalStateException
import kotlin.system.exitProcess

// General types

internal class SqlDelightException(message: String) : IllegalStateException(message)

// CLI flag processing conveniences

internal interface Validating {
  fun validate(context: JCommander): Int?
}

/**
 * Executes a supplied validator function which returns an exit code (including clean exit) or null
 * if the args are valid.
 */
internal fun JCommander.validate(): JCommander {
  for (arg in this.objects) {
    if (arg is Validating) (arg).validate(this)?.let {
      exitProcess(it)
    }
  }
  return this
}
