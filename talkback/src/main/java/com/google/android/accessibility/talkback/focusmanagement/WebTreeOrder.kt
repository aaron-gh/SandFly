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

import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Role
import com.google.android.accessibility.utils.WebInterfaceUtils

/**
 * Finds the next or previous item of web content in the order of the page's node tree, as
 * Chromium's own next and previous item search does.
 *
 * Chromium sometimes skips items or finds none: in a page that was just loaded, its search can
 * pass over paragraphs and quotes, and say there is nothing before an item with text before it.
 * The tree it gives screen readers has those items all the same. Swipes and arrow keys move by
 * this order, and ask the page only where the tree leaves the next item in doubt.
 *
 * As in Chromium, an item is a node without children that has text or can be acted on, or a
 * control with children, such as a link, whose children are items only if they can be acted on.
 * Any other node with children that can be acted on, such as a clickable section, leaves the search
 * in doubt. So does running out of reads.
 */
object WebTreeOrder {

  /** Most nodes that one search reads from the app. The search runs on the main thread. */
  @VisibleForTesting const val MAX_NODES_READ = 100

  /** Most remembered child positions, after which they are forgotten. */
  private const val MAX_POSITIONS = 1000

  /**
   * Chromium roles of controls, whose children are not items unless they can be acted on
   * themselves. Role names as in [AccessibilityNodeInfoUtils.getChromeRole].
   */
  private val CONTROL_ROLES =
    setOf(
      "link",
      "button",
      "toggleButton",
      "popUpButton",
      "checkBox",
      "radioButton",
      "switch",
      "tab",
      "menuItem",
      "menuItemCheckBox",
      "menuItemRadio",
      "menuListOption",
      "listBoxOption",
      "disclosureTriangle",
      "comboBoxMenuButton",
      "comboBoxSelect",
    )

  /** What a node is to the search. */
  enum class Kind {
    /** A node without children that has text: an item, except inside a control. */
    TEXT,
    /** A node without children that can be acted on: an item. */
    ACTION,
    /** A node without children that is not an item, such as white space. */
    SKIP,
    /** A node whose children are searched, such as a paragraph or a list. */
    CONTAINER,
    /** A control with children, such as a link: an item before its children. */
    CONTROL,
    /** Any other node with children that can be acted on. The page decides where focus goes. */
    UNSURE,
  }

  /** The node tree, read from the app or, in tests, made up. */
  interface Tree<N : Any> {
    fun parent(node: N): N?

    fun childCount(node: N): Int

    fun child(node: N, index: Int): N?

    /** Whether [node] is web content: an item, a container, a frame or the page itself. */
    fun isWeb(node: N): Boolean

    /** Whether [node] is the page itself or one of its frames. */
    fun isFrame(node: N): Boolean

    fun kind(node: N, childCount: Int): Kind
  }

  /**
   * Where children were found in their parents, so that going on from an item that a search
   * found needs no search of its siblings. Checked with one read before use.
   */
  private val positions = HashMap<Any, Int>()

  /**
   * The next item after [pivot] in web content, going [forward] or back, or `null` when the page
   * should be asked: no item in the page in that direction, an item in doubt, or too many nodes to
   * read.
   */
  @JvmStatic
  fun find(pivot: AccessibilityNodeInfoCompat, forward: Boolean): AccessibilityNodeInfoCompat? =
    find(CompatTree, pivot, forward)

  @VisibleForTesting
  @JvmStatic
  fun <N : Any> find(tree: Tree<N>, pivot: N, forward: Boolean): N? =
    Search(tree, forward).run(pivot)

  @VisibleForTesting
  @JvmStatic
  fun forgetPositions() {
    positions.clear()
  }

  private class Search<N : Any>(private val tree: Tree<N>, private val forward: Boolean) {
    private var readsLeft = MAX_NODES_READ
    private var found: N? = null
    private var unsure = false

    /** Whether the search is over: found, in doubt or out of reads. */
    private val done: Boolean
      get() = found != null || unsure

    fun run(pivot: N): N? {
      // The pivot and its ancestors up to the page.
      val chain = ArrayList<N>()
      var node: N = pivot
      while (true) {
        if (!tree.isWeb(node)) {
          return null
        }
        chain.add(node)
        val parent = read { tree.parent(node) }
        if (unsure) {
          return null
        }
        if (parent == null || (tree.isFrame(node) && !tree.isFrame(parent))) {
          // The page itself.
          break
        }
        node = parent
      }

      // Children of a control are items only if they can be acted on, up to the outermost
      // control around the pivot.
      var outermostControl = 0
      for (index in 1 until chain.size) {
        when (kindOf(chain[index])) {
          Kind.UNSURE -> return null
          Kind.CONTROL -> outermostControl = index
          else -> {}
        }
      }

      val pivotKind = kindOf(pivot)
      if (pivotKind == Kind.UNSURE) {
        return null
      }
      // Going forward, the pivot's own children come first. The page itself is a pivot only
      // going forward, from where it is entered.
      if (forward && childCount(pivot) > 0) {
        visitChildren(
          pivot,
          from = 0,
          insideControl = pivotKind == Kind.CONTROL || outermostControl > 0,
        )
        if (done) {
          return found
        }
      } else if (!forward && chain.size == 1) {
        return null
      }

      for (level in 0 until chain.size - 1) {
        val child = chain[level]
        val parent = chain[level + 1]
        val index = indexOf(parent, child)
        if (unsure || index < 0) {
          return null
        }
        val insideControl = level + 1 <= outermostControl
        visitChildren(parent, from = if (forward) index + 1 else index - 1, insideControl)
        if (done) {
          return found
        }
        // Going back, the parent comes after its earlier children.
        if (!forward && kindOf(parent) == Kind.CONTROL) {
          return parent
        }
      }
      return null
    }

    /** Visits the children of [parent] from [from] on, in the direction of the search. */
    private fun visitChildren(parent: N, from: Int, insideControl: Boolean) {
      val count = childCount(parent)
      var index = from
      while (index in 0 until count && !done) {
        val child = read { tree.child(parent, index) }
        if (unsure) {
          return
        }
        if (child != null) {
          remember(child, index)
          visit(child, insideControl)
        }
        index += if (forward) 1 else -1
      }
    }

    /** Visits [node] and its descendants in the order of the search. */
    private fun visit(node: N, insideControl: Boolean) {
      when (kindOf(node)) {
        Kind.TEXT ->
          if (!insideControl) {
            found = node
          }
        Kind.ACTION -> found = node
        Kind.SKIP -> {}
        Kind.CONTAINER ->
          visitChildren(node, from = if (forward) 0 else childCount(node) - 1, insideControl)
        Kind.CONTROL ->
          if (forward) {
            found = node
          } else {
            visitChildren(node, from = childCount(node) - 1, insideControl = true)
            if (!done) {
              found = node
            }
          }
        Kind.UNSURE -> unsure = true
      }
    }

    /** What [node] is. Frames are containers, and a frame without content is passed over. */
    private fun kindOf(node: N): Kind {
      val count = childCount(node)
      return when {
        !tree.isFrame(node) -> tree.kind(node, count)
        count > 0 -> Kind.CONTAINER
        else -> Kind.SKIP
      }
    }

    /** The child count comes with the node, so it costs no read. */
    private fun childCount(node: N): Int = tree.childCount(node)

    /** Where [child] is among the children of [parent], or -1. */
    private fun indexOf(parent: N, child: N): Int {
      val count = childCount(parent)
      val remembered = positions[child]
      if (remembered != null && remembered < count) {
        if (read { tree.child(parent, remembered) } == child) {
          return remembered
        }
        if (unsure) {
          return -1
        }
      }
      for (index in 0 until count) {
        val node = read { tree.child(parent, index) }
        if (unsure) {
          return -1
        }
        if (node == child) {
          remember(child, index)
          return index
        }
      }
      return -1
    }

    private fun remember(child: N, index: Int) {
      if (positions.size >= MAX_POSITIONS) {
        positions.clear()
      }
      positions[child] = index
    }

    /** Reads from the app, and leaves the search in doubt when it has read too much. */
    private inline fun <T> read(block: () -> T): T? {
      if (readsLeft <= 0) {
        unsure = true
        return null
      }
      readsLeft--
      return block()
    }
  }

  /** The nodes that the app gives. */
  private object CompatTree : Tree<AccessibilityNodeInfoCompat> {
    override fun parent(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? =
      node.parent

    override fun childCount(node: AccessibilityNodeInfoCompat): Int = node.childCount

    override fun child(node: AccessibilityNodeInfoCompat, index: Int): AccessibilityNodeInfoCompat? =
      node.getChild(index)

    override fun isWeb(node: AccessibilityNodeInfoCompat): Boolean =
      WebInterfaceUtils.supportsWebActions(node) || isFrame(node)

    override fun isFrame(node: AccessibilityNodeInfoCompat): Boolean =
      Role.getRole(node) == Role.ROLE_WEB_VIEW

    override fun kind(node: AccessibilityNodeInfoCompat, childCount: Int): Kind =
      kindOf(
        actionable =
          node.isFocusable ||
            node.isClickable ||
            node.isLongClickable ||
            node.isCheckable ||
            node.isEditable ||
            node.isScreenReaderFocusable,
        text = hasText(node.text) || hasText(node.contentDescription),
        control =
          childCount > 0 &&
            AccessibilityNodeInfoUtils.getChromeRole(node)?.toString() in CONTROL_ROLES,
        childCount = childCount,
      )
  }

  /**
   * What a node is from whether it can be [actionable], has [text] and has a [control] role. Every
   * web node has the web navigation actions, so those don't count.
   */
  @VisibleForTesting
  @JvmStatic
  fun kindOf(actionable: Boolean, text: Boolean, control: Boolean, childCount: Int): Kind =
    when {
      childCount == 0 ->
        when {
          actionable -> Kind.ACTION
          text -> Kind.TEXT
          else -> Kind.SKIP
        }
      !actionable -> Kind.CONTAINER
      control -> Kind.CONTROL
      else -> Kind.UNSURE
    }

  /** Whether [text] has anything but white space, which Chromium doesn't stop on. */
  @VisibleForTesting
  @JvmStatic
  fun hasText(text: CharSequence?): Boolean = text != null && text.isNotBlank()
}
