package com.alecstrong.sqlite.android;

import com.intellij.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

/**
 * Created by alec on 15-10-18.
 */
public class Component implements ApplicationComponent {
  private static final String COMPONENT_NAME = "Generate Sql";

  @Override public void initComponent() {
    final MessageBus bus = ApplicationManager.getApplication().getMessageBus();
    final MessageBusConnection connection = bus.connect();
    connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new SaveActionManager());
  }

  @Override public void disposeComponent() {

  }

  @NotNull @Override public String getComponentName() {
    return COMPONENT_NAME;
  }
}
