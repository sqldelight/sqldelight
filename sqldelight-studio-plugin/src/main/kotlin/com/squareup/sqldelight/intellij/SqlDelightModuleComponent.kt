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

import com.android.tools.idea.gradle.parser.BuildFileKeyType
import com.android.tools.idea.gradle.parser.GradleBuildFile
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleComponent
import com.intellij.util.text.VersionComparatorUtil
import com.squareup.sqldelight.VERSION
import com.squareup.sqldelight.intellij.util.iterateClasspath
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
import javax.swing.event.HyperlinkEvent

class SqlDelightModuleComponent(val module: Module) : ModuleComponent {
  val properties = PropertiesComponent.getInstance(module.project)

  override fun projectClosed() { }
  override fun projectOpened() { }
  override fun moduleAdded() { }
  override fun disposeComponent() { }
  override fun getComponentName() = "SqlDelightModuleComponent"
  override fun initComponent() {
    iterateClasspath(module) { classpath ->
      val classpathValue = BuildFileKeyType.STRING.getValue(classpath) as String
      if (classpathValue == GradleBuildFile.UNRECOGNIZED_VALUE) return@iterateClasspath

      val gradleVersion = classpathValue.substringAfterLast(':')

      if (GRADLE_PREFIX != classpathValue.substringBeforeLast(':')
          || gradleVersion == VERSION
          || VERSION == properties.getValue(SUPPRESSED)
          || VersionComparatorUtil.compare(VERSION, gradleVersion) <= 0) {
        return@iterateClasspath
      }

      val message = "<p>Your version of SQLDelight gradle plugin is $gradleVersion, while IDE" +
          " version is $VERSION. Gradle plugin should be updated to avoid compatibility problems." +
          "</p><p><a href=\"update\">Update Gradle</a> <a href=\"ignore\">Ignore</a></p>";

      Notifications.Bus.notify(Notification("Outdated SQLDelight Gradle Plugin",
          "Outdated SQLDelight Gradle Plugin", message,
          NotificationType.WARNING, NotificationListener { notification, event ->
        if (event.eventType != HyperlinkEvent.EventType.ACTIVATED) return@NotificationListener
        when (event.description) {
          "update" -> updateGradlePlugin(classpath)
          "ignore" -> properties.setValue(SUPPRESSED, VERSION);
          else -> throw IllegalStateException("Unknown event description ${event.description}");
        }
        notification.expire();
      }), module.project);
    }
  }

  private fun updateGradlePlugin(element: GroovyPsiElement) {
    WriteCommandAction.runWriteCommandAction(module.project, {
      BuildFileKeyType.STRING.setValue(element, CURRENT_GRADLE)
    })
  }

  companion object {
    private val GRADLE_PREFIX = "com.squareup.sqldelight:gradle-plugin"
    private val CURRENT_GRADLE = "$GRADLE_PREFIX:$VERSION"
    private val SUPPRESSED = "sqldelight.outdate.runtime.suppressed.version";
  }
}