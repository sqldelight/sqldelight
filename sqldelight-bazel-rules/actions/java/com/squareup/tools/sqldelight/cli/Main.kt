package com.squareup.tools.sqldelight.cli

import com.beust.jcommander.JCommander
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun main(args: Array<String>) {
  JCommander.newBuilder()
    .addObject(Args)
    .programName("sqldelightc")
    .build()
    .also { it.parse(*args) }
    .validate()
  val tmpOut = Files.createTempDirectory("sqldelight")
  val files = CompilerWrapper(Args.packageName!!, tmpOut.toFile(), Args.moduleName!!)
    .generate(Args.srcDirs)

  val srcJar = Args.srcJar.toPath()
    .also { Files.createDirectories(it.parent) }
  ZipOutputStream(
    BufferedOutputStream(Files.newOutputStream(srcJar, StandardOpenOption.CREATE_NEW))
  ).use { zos ->
    for (p in files) {
      zos.putNextEntry(
        ZipEntry(
          tmpOut.relativize(p).toString()
        )
      )
      zos.write(Files.readAllBytes(p))
      zos.closeEntry()
    }
  }
}
