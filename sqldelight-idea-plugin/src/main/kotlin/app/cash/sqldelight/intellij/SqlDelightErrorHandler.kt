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
package app.cash.sqldelight.intellij

import app.cash.sqldelight.VERSION
import com.bugsnag.Bugsnag
import com.bugsnag.Severity
import com.intellij.diagnostic.AbstractMessage
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.util.Consumer
import java.awt.Component

class SqlDelightErrorHandler : ErrorReportSubmitter() {
  val bugsnag = Bugsnag(BUGSNAG_KEY, false)

  init {
    bugsnag.setAutoCaptureSessions(false)
    bugsnag.startSession()
    bugsnag.setAppVersion(VERSION)
    bugsnag.setProjectPackages("app.cash.sqldelight")
    bugsnag.addCallback {
      it.addToTab("Device", "osVersion", System.getProperty("os.version"))
      it.addToTab("Device", "JRE", System.getProperty("java.version"))
      it.addToTab("Device", "IDE Version", ApplicationInfo.getInstance().fullVersion)
      it.addToTab("Device", "IDE Build #", ApplicationInfo.getInstance().build)
      PluginManagerCore.getPlugins().forEach { plugin ->
        it.addToTab("Plugins", plugin.name, "${plugin.pluginId} : ${plugin.version}")
      }
    }
  }

  override fun getReportActionText() = "Send to Square"
  override fun submit(
    events: Array<out IdeaLoggingEvent>,
    additionalInfo: String?,
    parentComponent: Component,
    consumer: Consumer<SubmittedReportInfo>
  ): Boolean {
    for (event in events) {
      if (BUGSNAG_KEY.isNotBlank()) {
        val throwable = (event.data as? AbstractMessage)?.throwable ?: event.throwable
        bugsnag.notify(throwable, Severity.ERROR) {
          it.addToTab("Data", "message", event.message)
          it.addToTab("Data", "additional info", additionalInfo)
          it.addToTab("Data", "stacktrace", event.throwableText)
        }
      }
    }
    return true
  }
}
