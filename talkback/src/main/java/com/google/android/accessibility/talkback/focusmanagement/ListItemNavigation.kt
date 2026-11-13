/*
 * Copyright 2026 SandFly contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback.focusmanagement

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.Role
import com.google.android.accessibility.utils.WebInterfaceUtils
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils

/**
 * Finds the next or previous item of the list that holds focus, for the List items reading control
 * outside web content. An item is a row of the list: a child of the list, with everything in it,
 * such as a post with its Share button, so moving by item passes over the controls inside items.
 */
object ListItemNavigation {

  /**
   * Returns the nearest ancestor of [node] that is a list: a list or grid, a scroll view, a
   * collection, or a view that scrolls, other than a pager or a spinner. Null outside lists and in
   * web content.
   */
  @JvmStatic
  fun listOf(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
    if (WebInterfaceUtils.supportsWebActions(node)) return null
    return AccessibilityNodeInfoUtils.getMatchingAncestor(node, filter { isList(it) })
  }

  /** Whether [node] is a list whose children are its items. */
  @JvmStatic
  fun isList(node: AccessibilityNodeInfoCompat): Boolean =
    when (Role.getRole(node)) {
      Role.ROLE_LIST,
      Role.ROLE_GRID,
      Role.ROLE_SCROLL_VIEW,
      Role.ROLE_HORIZONTAL_SCROLL_VIEW -> true
      Role.ROLE_PAGER,
      Role.ROLE_DROP_DOWN_LIST,
      Role.ROLE_SEEK_CONTROL -> false
      else -> node.collectionInfo != null || AccessibilityNodeInfoUtils.isScrollable(node)
    }

  /**
   * Returns the node to focus for the item after the one holding [pivot] in [list], going
   * [forward], or before it: the item itself when it can be focused, otherwise the first node in it
   * that can. Null when no other item of the list is on screen.
   */
  @JvmStatic
  fun target(
    pivot: AccessibilityNodeInfoCompat,
    list: AccessibilityNodeInfoCompat,
    forward: Boolean,
  ): AccessibilityNodeInfoCompat? {
    val strategy = strategy(list)
    val focusable = focusable(strategy)
    val from = itemOf(pivot, list, focusable) ?: return null
    val direction =
      if (forward) TraversalStrategy.SEARCH_FOCUS_FORWARD
      else TraversalStrategy.SEARCH_FOCUS_BACKWARD
    val found =
      TraversalStrategyUtils.searchFocus(
        strategy,
        pivot,
        direction,
        filter {
          focusable.accept(it) &&
            itemOf(it, list, focusable).let { item -> item != null && isOtherItem(item, from) }
        },
      ) ?: return null
    val item = itemOf(found, list, focusable) ?: return null
    return focusTarget(strategy, focusable, item)
  }

  /**
   * Returns the node to focus for the item of [list] that holds [node], as [target] does, or null
   * when [node] isn't in [list].
   */
  @JvmStatic
  fun itemTarget(
    node: AccessibilityNodeInfoCompat,
    list: AccessibilityNodeInfoCompat,
  ): AccessibilityNodeInfoCompat? {
    val strategy = strategy(list)
    val focusable = focusable(strategy)
    val item = itemOf(node, list, focusable) ?: return null
    return focusTarget(strategy, focusable, item)
  }

  /**
   * Returns the item of [list] that holds [node]: the child of [list] that is [node] or holds it.
   * In a list that isn't a collection, such as a scroll view, an only child that can't be focused,
   * such as the layout in a scroll view, is passed through to its children.
   */
  private fun itemOf(
    node: AccessibilityNodeInfoCompat,
    list: AccessibilityNodeInfoCompat,
    focusable: Filter<AccessibilityNodeInfoCompat>,
  ): AccessibilityNodeInfoCompat? {
    // The path from node up to the child of the list, last.
    val path = ArrayList<AccessibilityNodeInfoCompat>()
    var current: AccessibilityNodeInfoCompat? = node
    while (current != null && current != list) {
      if (path.size >= MAX_ITEM_DEPTH) return null
      path.add(current)
      current = current.parent
    }
    if (current == null || path.isEmpty()) return null
    var index = path.size - 1
    val role = Role.getRole(list)
    if (role != Role.ROLE_LIST && role != Role.ROLE_GRID && list.collectionInfo == null) {
      var parent = list
      while (index > 0 && parent.childCount == 1 && !focusable.accept(path[index])) {
        parent = path[index]
        index--
      }
    }
    return path[index]
  }

  /** Whether [item] is another item than [from], rather than the same one or a part of it. */
  private fun isOtherItem(item: AccessibilityNodeInfoCompat, from: AccessibilityNodeInfoCompat) =
    item != from &&
      !AccessibilityNodeInfoUtils.hasAncestor(item, from) &&
      !AccessibilityNodeInfoUtils.hasAncestor(from, item)

  private fun focusTarget(
    strategy: OrderedTraversalStrategy,
    focusable: Filter<AccessibilityNodeInfoCompat>,
    item: AccessibilityNodeInfoCompat,
  ): AccessibilityNodeInfoCompat? =
    if (focusable.accept(item)) item
    else
      TraversalStrategyUtils.findFirstFocusInNodeTree(
        strategy,
        item,
        TraversalStrategy.SEARCH_FOCUS_FORWARD,
        filter { focusable.accept(it) && AccessibilityNodeInfoUtils.hasAncestor(it, item) },
      )

  /** The most levels between a node and its list looked through, for a broken tree. */
  private const val MAX_ITEM_DEPTH = 50

  private fun strategy(list: AccessibilityNodeInfoCompat) =
    OrderedTraversalStrategy(
      list,
      /* includeChildrenOfNodesWithWebActions= */ true,
      /* makeFabFirst= */ false,
    )

  private fun focusable(strategy: OrderedTraversalStrategy): Filter<AccessibilityNodeInfoCompat> =
    NavigationTarget.createNodeFilter(NavigationTarget.TARGET_DEFAULT, strategy.speakingNodesCache)

  private fun filter(accept: (AccessibilityNodeInfoCompat) -> Boolean) =
    object : Filter<AccessibilityNodeInfoCompat>() {
      override fun accept(node: AccessibilityNodeInfoCompat?): Boolean =
        node != null && accept(node)
    }
}
