package com.squareup.sqldelight.intellij.util

import com.intellij.openapi.vfs.VirtualFile

internal fun VirtualFile.isAncestorOf(child: VirtualFile): Boolean {
  if (child in children) return true
  return isAncestorOf(child.parent ?: return false)
}