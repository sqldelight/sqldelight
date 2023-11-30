package app.cash.sqldelight.intellij.notifications

import app.cash.sqldelight.core.SqlDelightProjectService
import app.cash.sqldelight.core.lang.MigrationFileType
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification.UnconfiguredReason.GradleSyncing
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification.UnconfiguredReason.Incompatible
import app.cash.sqldelight.intellij.notifications.FileIndexingNotification.UnconfiguredReason.Syncing
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.util.function.Function
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTextArea
import javax.swing.UIManager
import javax.swing.text.DefaultCaret
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class FileIndexingNotification(
  private val project: Project,
) : DumbAware, EditorNotificationProvider {
  internal var unconfiguredReason: UnconfiguredReason = GradleSyncing
    set(value) {
      field = value
      EditorNotifications.getInstance(project).updateAllNotifications()
    }

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?> {
    return Function { createNotificationPanel(file, project) }
  }

  private fun createNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel? {
    if (file.fileType != SqlDelightFileType && file.fileType != MigrationFileType) return null

    val service = SqlDelightProjectService.getInstance(project)
    val module = service.module(file) ?: return null

    if (!service.fileIndex(module).isConfigured) {
      val message = when (val unconfiguredReason = unconfiguredReason) {
        is GradleSyncing -> "SQLDelight is waiting for Gradle to finish syncing."
        is Syncing -> "SQLDelight is setting up..."
        is Incompatible -> unconfiguredReason.errorMessage
      }
      return EditorNotificationPanel(Color.decode("#7e56c2")).apply {
        text = message
        val unconfiguredReason = unconfiguredReason
        if (unconfiguredReason is Incompatible && unconfiguredReason.exception != null) {
          createActionLabel("Show error") {
            val errorText = """
              ${unconfiguredReason.exception.message}:
                ${unconfiguredReason.exception.stackTraceToString()}
            """.trimIndent()

            JBPopupFactory.getInstance().createComponentPopupBuilder(PopupComponent(errorText), null)
              .setTitle("Error connecting to SQLDelight.").createPopup().showInFocusCenter()
          }
        }
      }
    }

    if (DumbService.isDumb(project)) {
      return EditorNotificationPanel(Color.decode("#7e56c2")).apply {
        text = "Symbols unavailable during indexing"
      }
    }

    return null
  }

  companion object {
    fun getInstance(project: Project): FileIndexingNotification {
      return DumbService.getDumbAwareExtensions(project, EditorNotificationProvider.EP_NAME).firstIsInstance()
    }
  }

  internal sealed interface UnconfiguredReason {
    object GradleSyncing : UnconfiguredReason
    data class Incompatible(
      val errorMessage: String,
      val exception: Exception?,
    ) : UnconfiguredReason
    object Syncing : UnconfiguredReason
  }

  private class PopupComponent(text: String) : JComponent() {
    private val textArea: JTextArea

    init {
      layout = BorderLayout()
      textArea = JTextArea(text)
      textArea.isEditable = false
      textArea.lineWrap = true
      textArea.wrapStyleWord = true
      textArea.background = UIManager.getColor("Panel.background")
      textArea.font = UIManager.getFont("Label.font")
      val scrollPane = JBScrollPane(textArea)
      scrollPane.preferredSize = Dimension(500, 500)
      val caret = textArea.caret as DefaultCaret
      caret.updatePolicy = DefaultCaret.NEVER_UPDATE
      add(scrollPane, BorderLayout.CENTER)
      val copyButton = JButton("Copy")
      copyButton.addActionListener {
        val stringSelection = StringSelection(textArea.text)
        CopyPasteManager.getInstance().setContents(stringSelection)
        JBPopupFactory.getInstance().createBalloonBuilder(JLabel("Stacktrace copied to clipboard!")).createBalloon().showInCenterOf(copyButton)
      }
      add(copyButton, BorderLayout.SOUTH)
    }
  }
}
