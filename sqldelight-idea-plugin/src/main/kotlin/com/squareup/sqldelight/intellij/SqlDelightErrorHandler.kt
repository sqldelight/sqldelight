/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sqldelight.intellij

import com.bugsnag.Client
import com.bugsnag.MetaData
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.util.Consumer
import com.squareup.sqldelight.VERSION
import java.awt.Component

class SqlDelightErrorHandler : ErrorReportSubmitter() {
  val bugsnag = Client(BUGSNAG_KEY, false)

  init {
    bugsnag.setAppVersion(VERSION)
    bugsnag.setOsVersion(System.getProperty("os.version"))
    bugsnag.setProjectPackages("com.squareup.sqldelight")
  }

  override fun getReportActionText() = "Send to Square"
  override fun submit(
    events: Array<out IdeaLoggingEvent>,
    additionalInfo: String?,
    parentComponent: Component,
    consumer: Consumer<SubmittedReportInfo>
  ): Boolean {
    for (event in events) {
      val metaData = MetaData()
      metaData.addToTab("Data", "message", event.message)
      metaData.addToTab("Data", "additional info", additionalInfo)
      metaData.addToTab("Data", "stacktrace", event.throwableText)
      metaData.addToTab("Device", "JRE", System.getProperty("java.version"))
      metaData.addToTab("Device", "IDE Version", ApplicationInfo.getInstance().fullVersion)
      metaData.addToTab("Device", "IDE Build #", ApplicationInfo.getInstance().build)
      PluginManagerCore.getPlugins().forEach {
        metaData.addToTab("Plugins", it.name, "${it.pluginId} : ${it.version}")
      }
      if (BUGSNAG_KEY.isNotBlank()) {
        bugsnag.notify(event.throwable, "error", metaData)
      }
    }
    return true
  }
}
