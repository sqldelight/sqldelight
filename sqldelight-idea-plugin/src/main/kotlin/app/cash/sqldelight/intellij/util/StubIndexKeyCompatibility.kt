package app.cash.sqldelight.intellij.util

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.AbstractStubIndex
import com.intellij.psi.stubs.StubIndexKey
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

internal inline fun <P : PsiElement, reified T : AbstractStubIndex<String, P>> KClass<T>.compatibleKey(): StubIndexKey<String, P> {
  // read the HELPER variable reflectively (2023.2)
  try {
    val helperField = this.java.getField("Helper")
    val helper = helperField.get(null)
    if (helper != null) {
      val keyMethod = helper.javaClass.getMethod("getIndexKey")
      val key = keyMethod.invoke(helper)
      @Suppress("UNCHECKED_CAST") // Reflection that will go away when our minimum version is >= 2023.2
      if (key != null) return key as StubIndexKey<String, P>
    }
  } catch (e: Exception) {
    /* intentionally empty, fall back to getInstance() call in case of errors */
  }

  // read the INSTANCE variable reflectively first (newer Kotlin plugins)
  try {
    val instanceField = this.java.getField("INSTANCE")
    val instance = instanceField.get(null)
    if (instance is T) {
      val keyMethod = instance.javaClass.getMethod("getKey")
      val key = keyMethod.invoke(instance)
      @Suppress("UNCHECKED_CAST") // Reflection that will go away when our minimum version is >= 2023.2
      if (key != null) return key as StubIndexKey<String, P>
    }
  } catch (e: Exception) {
    /* intentionally empty, fall back to getInstance() call in case of errors */
  }

  // Call the method getInstance on the companion type.
  val companionMethod =
    this.companionObject!!.java.getMethod("getInstance")
  val instance = companionMethod.invoke(this.companionObjectInstance!!)
    as T
  val keyMethod = instance.javaClass.getMethod("getKey")
  val key = keyMethod.invoke(instance)
  @Suppress("UNCHECKED_CAST") // Reflection that will go away when our minimum version is >= 2023.2
  return key as StubIndexKey<String, P>
}
