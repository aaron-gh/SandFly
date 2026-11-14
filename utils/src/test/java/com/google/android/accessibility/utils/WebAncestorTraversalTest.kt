/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WebAncestorTraversalTest {
  private class Node(web: Boolean = false) : AccessibilityNodeInfoCompat(
    AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication()))) {
    var owner: Node? = null
    var ordinaryReads = 0
    val flags = mutableListOf<Int>()
    init { if (web) addAction(ACTION_NEXT_HTML_ELEMENT) }
    override fun getParent(): AccessibilityNodeInfoCompat? { ordinaryReads++; return owner }
    override fun getParent(strategy: Int): AccessibilityNodeInfoCompat? {
      flags.add(strategy); return owner
    }
  }

  @Test fun webAncestorSearchPrefetchesOnlyTheAncestorsItActuallyWalks() {
    val root = Node()
    val middle = Node(true).apply { owner = root }
    val leaf = Node(true).apply { owner = middle }
    assertSame(root, AccessibilityNodeInfoUtils.getMatchingAncestor(leaf,
      Filter.node { it === root }))
    val wanted = AccessibilityNodeInfoCompat.FLAG_PREFETCH_ANCESTORS or
      AccessibilityNodeInfoCompat.FLAG_PREFETCH_UNINTERRUPTIBLE
    assertEquals(listOf(wanted), leaf.flags)
    assertEquals(listOf(wanted), middle.flags)
    assertEquals(0, leaf.ordinaryReads + middle.ordinaryReads)
  }

  @Test fun nativeAncestorSearchAndCycleDetectionAreUnchanged() {
    val a = Node(); val b = Node(); a.owner = b; b.owner = a
    assertNull(AccessibilityNodeInfoUtils.getMatchingAncestor(a, Filter.node { false }))
    assertEquals(1, a.ordinaryReads)
    assertEquals(1, b.ordinaryReads)
    assertTrue(a.flags.isEmpty() && b.flags.isEmpty())
  }
}
