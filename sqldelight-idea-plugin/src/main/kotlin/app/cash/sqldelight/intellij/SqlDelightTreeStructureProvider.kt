package app.cash.sqldelight.intellij

import app.cash.sqldelight.core.SqlDelightFileIndex
import com.intellij.icons.AllIcons
import com.intellij.ide.DeleteProvider
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.ProjectRootsUtil
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.DeleteHandler
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import java.util.ArrayList
import org.jetbrains.kotlin.konan.file.File

internal class SqlDelightTreeStructureProvider(
  private val project: Project,
) : TreeStructureProvider {
  override fun modify(
    parent: AbstractTreeNode<*>,
    children: MutableCollection<AbstractTreeNode<*>>,
    settings: ViewSettings?,
  ): MutableCollection<AbstractTreeNode<*>> {
    if (settings !is ProjectTreeStructure || !settings.isHideEmptyMiddlePackages) {
      return children
    }
    if (parent !is PsiDirectoryNode || ProjectRootsUtil.isSourceRoot(parent.value)) {
      return children
    }

    val module = ModuleUtil.findModuleForPsiElement(parent.value) ?: return children
    val fileIndex = SqlDelightFileIndex.getInstance(module)
    if (!fileIndex.isConfigured) {
      return children
    }
    val packagePrefix = fileIndex.packageName.substringBefore(".")
    if (packagePrefix.isEmpty()) {
      return children
    }
    val srcPaths = fileIndex.sourceFolders(true)
      .flatMap { sourceFolders -> sourceFolders.map { it.folder.toPath() } }

    val path = try {
      parent.value.virtualFile.toNioPath().toAbsolutePath()
    } catch (exception: UnsupportedOperationException) {
      return children
    }
    if (srcPaths.none { path.startsWith(it) }) {
      return children
    }
    val srcDirName = parent.value.name

    val result = mutableListOf<AbstractTreeNode<*>>()
    val psiManager = PsiManager.getInstance(project)
    for (child in children) {
      if (child is PsiDirectoryNode && child.value.name == packagePrefix) {
        val leafDir = findLeafDirectory(child)
        val psiFile = psiManager.findDirectory(leafDir) ?: return children
        result.add(SqlDelightPackageNode(srcDirName, project, psiFile, settings))
      } else {
        result.add(child)
      }
    }
    return result
  }

  private fun findLeafDirectory(node: PsiDirectoryNode): VirtualFile {
    var virtualFile = node.value.virtualFile
    VfsUtilCore.iterateChildrenRecursively(virtualFile, VirtualFileFilter.ALL) { file ->
      virtualFile = file
      val filteredChildren = file.children.filter { !it.name.startsWith(".") }
      if (filteredChildren.size == 1) {
        filteredChildren.first().isDirectory
      } else {
        false
      }
    }
    return virtualFile
  }

  override fun getData(selected: MutableCollection<out AbstractTreeNode<*>>, dataId: String): Any? {
    if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER.`is`(dataId)) {
      if (selected.any { it is SqlDelightPackageNode }) {
        return SqlDelightDeleteProvider(selected)
      }
    }
    return null
  }

  private class SqlDelightDeleteProvider(
    selected: Collection<AbstractTreeNode<*>>,
  ) : DeleteProvider {
    private val elements = ArrayList<PsiElement>().apply {
      selected.forEach {
        if (it is SqlDelightPackageNode) {
          addAll(it.elements)
        } else if (it.value is PsiElement) {
          add(it.value as PsiElement)
        }
      }
    }.toTypedArray()

    override fun deleteElement(dataContext: DataContext) {
      val project = CommonDataKeys.PROJECT.getData(dataContext)
      DeleteHandler.deletePsiElement(elements, project)
    }

    override fun canDeleteElement(dataContext: DataContext): Boolean {
      return DeleteHandler.shouldEnableDeleteAction(elements)
    }
  }

  private class SqlDelightPackageNode(
    private val srcDirName: String,
    project: Project,
    value: PsiDirectory,
    viewSettings: ViewSettings?,
  ) : PsiDirectoryNode(project, value, viewSettings) {
    val elements = collectPsiElements()

    override fun updateImpl(data: PresentationData) {
      data.presentableText = value.virtualFile.path.substringAfterLast(srcDirName)
        .replace(File.separator, ".")
        .removePrefix(".")
      data.locationString =
        ProjectViewDirectoryHelper.getInstance(project).getLocationString(value, false, false)
      data.setIcon(AllIcons.Nodes.Package)
    }

    private fun collectPsiElements(): Array<PsiElement> {
      val result = mutableSetOf<PsiElement>()
      result.addAll(children.map { it.value as PsiElement })
      generateSequence(value) { it.parent }
        .takeWhile { it.name != srcDirName }
        .forEach { result.add(it) }
      return result.toTypedArray()
    }
  }
}
