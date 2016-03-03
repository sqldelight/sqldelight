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
package com.squareup.sqldelight

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.util.messages.Topic

class SqlDelightStartupActivity : StartupActivity {
  interface SqlDelightStartupListener {
    fun startupCompleted(project: Project)
  }

  override fun runActivity(project: Project) {
    ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).startupCompleted(project)
  }

  companion object {
    val TOPIC = Topic.create("SqlDelight plugin completed startup",
        SqlDelightStartupListener::class.java)
  }
}