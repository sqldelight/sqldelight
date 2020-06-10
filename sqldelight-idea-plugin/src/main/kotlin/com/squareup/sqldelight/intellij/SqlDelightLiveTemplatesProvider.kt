package com.squareup.sqldelight.intellij

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider

class SqlDelightLiveTemplatesProvider : DefaultLiveTemplatesProvider {

  override fun getDefaultLiveTemplateFiles(): Array<String> = arrayOf("liveTemplates/SqlDelight")

  override fun getHiddenLiveTemplateFiles(): Array<String>? = null
}
