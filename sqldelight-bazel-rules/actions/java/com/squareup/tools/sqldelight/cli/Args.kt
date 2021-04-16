package com.squareup.tools.sqldelight.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import java.io.File

/** Holds the CLI flags */
object Args : Validating {
  @Parameter(names = ["--help"], help = true)
  var help = false

  @Parameter(
    names = ["--output_srcjar", "-o"],
    description = "Srcjar of generated code.",
    required = true
  )
  lateinit var srcJar: File

  @Parameter(description = "<list of SQL files to process>")
  lateinit var fileNames: List<String>

  @Parameter(
    names = ["--src_dirs"],
    description = "Directories containing all srcs",
    required = true
  )
  lateinit var srcDirs: List<File>

  @Parameter(
    names = ["--package_name"],
    required = false,
    description = "Package into which the code will be generated (not required for legacy)"
  )
  var packageName: String? = null

  @Parameter(
    names = ["--module_name"],
    required = false,
    description = "Module Name for Kotlin compilation (not required for legacy)"
  )
  var moduleName: String? = null

  override fun validate(context: JCommander): Int? {
    if (context.objects.size != 1) throw AssertionError("Processed wrong number of Args classes.")

    if (moduleName == null || packageName == null) {
      context.usage()
    }

    if (help) {
      context.usage()
      return 0 // valid run, but should terminate early.
    }

    if (fileNames.isEmpty()) {
      println("Must set at least one file.")
      context.usage()
      return 1
    }

    return null
  }
}
