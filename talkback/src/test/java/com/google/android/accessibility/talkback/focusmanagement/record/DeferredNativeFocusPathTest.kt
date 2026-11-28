/*
 * Copyright 2026 SandFly contributors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0
 */
package com.google.android.accessibility.talkback.focusmanagement.record

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * The focus path of a native item is read on a worker thread, whole, as it was read on the main
 * thread before: the ancestors' positions are not read again when the path is used.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeferredNativeFocusPathTest {

  /** A native node in a tree that counts the children read from it. */
  private class Node(label: String, private val window: Int = 4) :
    AccessibilityNodeInfoCompat(
      AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication()))
    ) {
    var owner: Node? = null
    val children = mutableListOf<Node>()
    var childReads = 0

    init {
      text = label
      className = "android.view.View"
      packageName = "example.app"
    }

    fun add(vararg nodes: Node) = nodes.forEach {
      it.owner = this
      children.add(it)
    }

    override fun getParent(): AccessibilityNodeInfoCompat? = owner

    override fun getParent(prefetchingStrategy: Int): AccessibilityNodeInfoCompat? = owner

    override fun getChildCount() = children.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat? {
      childReads++
      return children.getOrNull(index)
    }

    override fun getChild(index: Int, prefetchingStrategy: Int) = getChild(index)

    override fun getWindowId() = window
  }

  /** root > (top, group > (first, leaf, last)). */
  private class Tree(window: Int = 4) {
    val root = Node("root", window)
    val top = Node("top", window)
    val group = Node("group", window)
    val first = Node("first", window)
    val leaf = Node("leaf", window)
    val last = Node("last", window)

    init {
      root.add(top, group)
      group.add(first, leaf, last)
    }

    val childReads
      get() = root.childReads + group.childReads
  }

  private fun descriptions(path: NodePathDescription): List<NodeDescription> =
    ReflectionHelpers.getField(path, "nodeDescriptions")

  private fun canReadInBackground(node: AccessibilityNodeInfoCompat): Boolean =
    ReflectionHelpers.callStaticMethod(
      FocusActionRecord::class.java,
      "canReadPathInBackground",
      ReflectionHelpers.ClassParameter.from(AccessibilityNodeInfoCompat::class.java, node))

  @Test
  fun nativeNodesInAppWindowsAreReadInBackground() {
    assertTrue(canReadInBackground(Tree().leaf))
    assertFalse(canReadInBackground(Tree(window = -1).leaf))
  }

  @Test
  fun recordingNativeFocusReadsNothingOnTheCallerThread() {
    val tree = Tree()
    val work = mutableListOf<Runnable>()
    var reads = 0
    val record = FocusActionRecord(tree.leaf, FocusActionInfo.builder().build(), 1000,
      { work.add(it) }) { reads++; NodePathDescription.obtain(tree.leaf) }

    assertEquals(0, reads)
    assertEquals(0, tree.childReads)
    assertTrue(record.isPathCapturePending)
    assertTrue(record.readyAncestorNodes.isEmpty())

    work.single().run()
    assertEquals(1, reads)
    assertFalse(record.isPathCapturePending)
    assertFalse(record.readyAncestorNodes.isEmpty())
  }

  @Test
  fun theWorkerPathIsTheWholePathAndIsNotReadAgainWhenUsed() {
    val tree = Tree()
    val expected = NodePathDescription.obtain(tree.leaf)
    val work = mutableListOf<Runnable>()
    val record = FocusActionRecord(tree.leaf, FocusActionInfo.builder().build(), 1000,
      { work.add(it) }) { NodePathDescription.obtain(tree.leaf) }
    work.single().run()

    // The tree changes before the path is used, as after a screen change.
    tree.group.children.removeAt(0)
    tree.root.children.removeAt(0)
    val readsBefore = tree.childReads

    val path = record.nodePathDescription
    assertEquals(expected, path)
    assertSame(record.ancestorIdentityPath, path)
    assertEquals(readsBefore, tree.childReads)
    val nodes = descriptions(path)
    assertEquals(listOf("leaf", "group", "root"), nodes.map { it.text.toString() })
    assertEquals(1, nodes[0].rawIndexInParent)
    assertEquals("first", nodes[0].previousSiblingText.toString())
    assertEquals("last", nodes[0].nextSiblingText.toString())
    assertEquals(1, nodes[1].rawIndexInParent)
    assertEquals("top", nodes[1].previousSiblingText.toString())
    assertTrue(path.containsNodeByHashAndIdentity(tree.group))
  }

  @Test
  fun copiesShareTheWorkerPath() {
    val tree = Tree()
    val work = mutableListOf<Runnable>()
    var reads = 0
    val record = FocusActionRecord(tree.leaf, FocusActionInfo.builder().build(), 1000,
      { work.add(it) }) { reads++; NodePathDescription.obtain(tree.leaf) }
    val copy = FocusActionRecord.copy(record)!!
    assertTrue(record.sharesPathCapture(copy))
    work.single().run()
    assertSame(record.nodePathDescription, copy.nodePathDescription)
    assertEquals(record, copy)
    assertEquals(1, reads)
  }

  @Test
  fun aNativePathWithoutAWorkerIsReadAtOnce() {
    val tree = Tree(window = -1)
    val record = FocusActionRecord(tree.leaf, FocusActionInfo.builder().build(), 1000)
    assertFalse(record.isPathCapturePending)
    assertEquals(NodePathDescription.obtain(tree.leaf), record.nodePathDescription)
    assertFalse(record.readyAncestorNodes.isEmpty())
  }
}
