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

import android.annotation.SuppressLint
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.NodeOverrides
import com.google.android.accessibility.utils.WebInterfaceUtils
import com.google.android.accessibility.utils.traversal.NodeCachedBoundsCalculator
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.ReorderedChildrenIterator
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.function.Supplier

/**
 * The reading order of a window, worked out around the nodes that navigation asks about instead of
 * read from the whole window.
 *
 * [OrderedTraversalStrategy] reads every node of the window and puts them in one order: each node
 * before its children, and the children of each node in the order of [ReorderedChildrenIterator],
 * which moves a child that can't be focused by where its focusable descendants are on screen. Nodes
 * that set their place with traversal before or after, or by a script, are moved after that. The
 * next node after a node is therefore its first child, or else the next child after it or after
 * its nearest ancestor that has one. The previous node is the last descendant of the child before
 * it, or else its parent. This class finds them the same way, reading only those nodes and the
 * children of their parents, with the same reordering of children.
 *
 * A node that sets its place before or after a sibling, as an action bar does before the content
 * beside it, is moved here as the whole order moves it: before, it takes the sibling's place, and
 * the sibling goes after its own children; after, it goes after the sibling's children.
 *
 * It answers only where the answer is the same as the whole order's. It builds the whole order with
 * [buildFull] and answers from it from then on when:
 * - a node it reads sets its place other than next to a sibling, or is in web content;
 * - the search reaches the start or end of the window, where the whole order decides about other
 *   windows and wrapping around;
 * - a node is listed by two parents, or none of a node's children can be read;
 * - a parent has more than [MAX_CHILDREN] children, or it has read [MAX_NODES] nodes.
 *
 * The nodes read are kept in a [Tree] for later navigation in the window until an event changes
 * it, as the saved whole order is.
 *
 * Nodes elsewhere in the window can also set their place next to the ones read here. This is only
 * used in a window whose whole order, last built, had none, see [TraversalTreeCache], and when the
 * whole order is built after steps were answered here, those steps are checked against it:
 * [MismatchException] is thrown if one differs, for the navigation to be done again.
 *
 * Filter logcat by the "BacktalkLocalOrder" tag in debug builds to see how often it answers.
 */
class LocalTraversalOrder
private constructor(
  private val tree: Tree,
  private val buildFull: Supplier<OrderedTraversalStrategy>?,
) : OrderedTraversalStrategy() {

  /**
   * The nodes of one window read around focused nodes, and their order. Kept for later navigation
   * in that window until an event changes it, as the saved order is; see [TraversalTreeCache].
   */
  class Tree internal constructor(val root: AccessibilityNodeInfoCompat) {
    internal val speakingNodes = HashMap<AccessibilityNodeInfoCompat, Boolean>()
    internal val bounds =
      NodeCachedBoundsCalculator().also { it.setSpeakingNodesCache(speakingNodes) }

    /** The parent of each node read here, under which it is in the order. */
    internal val parents = HashMap<AccessibilityNodeInfoCompat, AccessibilityNodeInfoCompat>()

    /** The children of nodes read here, in their order. */
    internal val ordered =
      HashMap<AccessibilityNodeInfoCompat, List<AccessibilityNodeInfoCompat>>()

    /** The sibling that goes after the children of a node, moved there by traversal order. */
    internal val appended = HashMap<AccessibilityNodeInfoCompat, AccessibilityNodeInfoCompat>()

    /** Parents whose children were read from the app together. */
    internal val prefetched = HashSet<AccessibilityNodeInfoCompat>()

    /** Reads other than the nodes whose bounds [bounds] works out. */
    internal var ownReads = 0

    /** Whether a search stopped partway through reading, which can leave this partly read. */
    var broken = false
      internal set

    internal fun reads(): Int = ownReads + bounds.nodeCount()

    /** Whether [node] was read here and put in the order. */
    fun holds(node: AccessibilityNodeInfoCompat): Boolean =
      node == root || parents.containsKey(node)
  }

  private val root = tree.root
  private val bounds = tree.bounds
  private val parents = tree.parents
  private val ordered = tree.ordered
  private val appended = tree.appended
  private val prefetched = tree.prefetched

  /** How many nodes [tree] had read before this navigation. */
  private val readsAtStart = tree.reads()

  /** Why the order could not be worked out here. */
  private class Unsure(val reason: String) :
    RuntimeException(reason, null, /* enableSuppression= */ false, /* writableStackTrace= */ false)

  /**
   * Thrown when the whole order, built after this order answered some steps, puts a node elsewhere
   * than this order did, such as when a node read later sets its place next to one read before. A
   * search that used those steps may have a different result than with the whole order, so the
   * navigation is done again with the whole order, which is saved by then.
   */
  class MismatchException internal constructor(reason: String) :
    RuntimeException(reason, null, /* enableSuppression= */ false, /* writableStackTrace= */ false)

  /** The steps answered here, as start node, direction and answer, checked against [full]. */
  private val answers = ArrayList<Answer>()

  private class Answer(
    val start: AccessibilityNodeInfoCompat,
    val direction: Int,
    val node: AccessibilityNodeInfoCompat,
  )

  private var full: OrderedTraversalStrategy? = null
  private var unsureReason: String? = null
  private var steps = 0
  private var localNanos = 0L
  private var fullMs = 0L
  private var finished = false
  private var mismatched = false

  override fun findFocus(
    startNode: AccessibilityNodeInfoCompat,
    @TraversalStrategy.SearchDirection direction: Int,
  ): AccessibilityNodeInfoCompat? {
    full?.let {
      return it.findFocus(startNode, direction)
    }
    if (unsureReason == null) {
      val start = System.nanoTime()
      try {
        val result =
          when (direction) {
            TraversalStrategy.SEARCH_FOCUS_FORWARD -> next(startNode)
            TraversalStrategy.SEARCH_FOCUS_BACKWARD -> previous(startNode)
            else -> throw Unsure("not forward or back")
          }
        steps++
        answers.add(Answer(startNode, direction, result))
        return result
      } catch (e: Unsure) {
        unsureReason = e.reason
        tree.broken = true
      } catch (e: NodeCachedBoundsCalculator.TooManyNodesException) {
        unsureReason = "more than $MAX_NODES nodes"
        tree.broken = true
      } finally {
        localNanos += System.nanoTime() - start
      }
    }
    return fullOrder()?.findFocus(startNode, direction)
  }

  override fun focusFirst(
    rootNode: AccessibilityNodeInfoCompat,
    @TraversalStrategy.SearchDirection direction: Int,
  ): AccessibilityNodeInfoCompat? =
    fullOrder("first or last node")?.focusFirst(rootNode, direction)

  override fun focusInitial(rootNode: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? =
    fullOrder("initial focus")?.focusInitial(rootNode)

  override fun containsNode(node: AccessibilityNodeInfoCompat): Boolean =
    fullOrder("whether a node is in the order")?.containsNode(node) == true

  override fun getSpeakingNodesCache(): MutableMap<AccessibilityNodeInfoCompat, Boolean> =
    tree.speakingNodes

  override fun missesChildren(): Boolean = full?.missesChildren() == true

  override fun movesOnlyAmongSiblings(): Boolean = full?.movesOnlyAmongSiblings() == true

  override fun dumpTree(): List<AccessibilityNodeInfoCompat> =
    fullOrder("the whole order")?.dumpTree() ?: emptyList()

  /** The whole order, built now if it hasn't been, because of [reason]. */
  private fun fullOrder(reason: String): OrderedTraversalStrategy? {
    if (unsureReason == null) {
      unsureReason = reason
    }
    return fullOrder()
  }

  private fun fullOrder(): OrderedTraversalStrategy? {
    full?.let {
      return it
    }
    val builder = buildFull ?: return null
    val start = SystemClock.uptimeMillis()
    val built = builder.get()
    full = built
    fullMs = SystemClock.uptimeMillis() - start
    // The steps answered here must be the whole order's, or the searches that took them may have
    // ended elsewhere.
    for (answer in answers) {
      if (built.findFocus(answer.start, answer.direction) != answer.node) {
        mismatched = true
        throw MismatchException("${answer.node} is not next to ${answer.start} in the whole order")
      }
    }
    answers.clear()
    return built
  }

  /** The node after [node] in the order. */
  private fun next(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat {
    checkInOrder(node)
    children(node).firstOrNull()?.let {
      return it
    }
    var current = node
    while (true) {
      if (current == root) {
        throw Unsure("end of the window")
      }
      val parent = parents.getValue(current)
      val siblings = children(parent)
      val index = indexIn(siblings, current)
      if (index + 1 < siblings.size) {
        return siblings[index + 1]
      }
      current = parent
    }
  }

  /** The node before [node] in the order. */
  private fun previous(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat {
    checkInOrder(node)
    if (node == root) {
      throw Unsure("start of the window")
    }
    val parent = parents.getValue(node)
    val siblings = children(parent)
    val index = indexIn(siblings, node)
    if (index > 0) {
      var last = siblings[index - 1]
      while (true) {
        last = children(last).lastOrNull() ?: return last
      }
    }
    if (parent == root) {
      throw Unsure("start of the window")
    }
    return parent
  }

  private fun indexIn(
    siblings: List<AccessibilityNodeInfoCompat>,
    node: AccessibilityNodeInfoCompat,
  ): Int {
    val index = siblings.indexOf(node)
    if (index < 0) {
      throw Unsure("node not among its parent's children")
    }
    return index
  }

  /**
   * Checks that [node] is in the order where its parents say: each ancestor up to the root lists
   * the one below it, and none of them is in web content. Where a parent's children set their
   * place next to each other, the parent's children are put in order now, from the root down, as a
   * node moved after its sibling's children is only known from the parent above.
   */
  private fun checkInOrder(node: AccessibilityNodeInfoCompat) {
    if (node == root || parents.containsKey(node)) {
      return
    }
    if (hasOrderSet(root)) {
      throw Unsure("traversal order set on the root")
    }
    val chain = ArrayList<AccessibilityNodeInfoCompat>()
    val parentsInChain = ArrayList<AccessibilityNodeInfoCompat>()
    val orderSet = ArrayList<Boolean>()
    var current = node
    while (current != root && !parents.containsKey(current)) {
      read()
      val parent = current.parent ?: throw Unsure("node not in the window")
      if (parent == current || parent in chain) {
        throw Unsure("looped parents")
      }
      if (WebInterfaceUtils.supportsWebActions(parent)) {
        throw Unsure("web content")
      }
      val listed = findAmongChildren(parent, current)
      if (listed == Listed.NO) {
        throw Unsure("node not among its parent's children")
      }
      chain.add(current)
      parentsInChain.add(parent)
      orderSet.add(listed == Listed.WITH_ORDER_SET)
      current = parent
    }
    for (index in chain.indices.reversed()) {
      if (orderSet[index]) {
        children(parentsInChain[index])
        if (!parents.containsKey(chain[index])) {
          throw Unsure("node not among its parent's children")
        }
      } else {
        parents[chain[index]] = parentsInChain[index]
      }
    }
  }

  private enum class Listed {
    NO,
    YES,
    /** Listed, and a child sets its place in the order. */
    WITH_ORDER_SET,
  }

  /** Whether [parent] lists [child] among its children, and whether a child sets its place. */
  private fun findAmongChildren(
    parent: AccessibilityNodeInfoCompat,
    child: AccessibilityNodeInfoCompat,
  ): Listed {
    if (ordered.containsKey(parent)) {
      // All of the children in that order are in parents already.
      return Listed.NO
    }
    checkWidth(parent)
    prefetchChildren(parent)
    var listed = false
    var orderSet = false
    for (index in 0 until parent.childCount) {
      read()
      val sibling = parent.getChild(index) ?: continue
      if (sibling == child) {
        listed = true
      }
      if (hasOrderSet(sibling)) {
        orderSet = true
      }
    }
    return when {
      !listed -> Listed.NO
      orderSet -> Listed.WITH_ORDER_SET
      else -> Listed.YES
    }
  }

  /** The children of [parent] in the order, as [OrderedTraversalStrategy] puts them. */
  private fun children(parent: AccessibilityNodeInfoCompat): List<AccessibilityNodeInfoCompat> {
    ordered[parent]?.let {
      return it
    }
    val moved = appended[parent]
    // The order does not go into web content, which is read through its own actions.
    if (parent.childCount == 0 || WebInterfaceUtils.supportsWebActions(parent)) {
      val children = listOfNotNull(moved)
      ordered[parent] = children
      return children
    }
    checkWidth(parent)
    val left = MAX_NODES - (tree.reads() - readsAtStart)
    if (parent.childCount > left) {
      throw Unsure("more than $MAX_NODES nodes")
    }
    bounds.setMaxNodes(bounds.nodeCount() + left)
    prefetchChildren(parent)
    val iterator = ReorderedChildrenIterator.createAscendingIterator(parent, bounds)!!
    if (iterator.missedAllChildren()) {
      throw Unsure("children could not be read")
    }
    val children = ArrayList<AccessibilityNodeInfoCompat>(parent.childCount + 1)
    while (iterator.hasNext()) {
      val child = iterator.next() ?: continue
      if (child == root || child == parent) {
        throw Unsure("looped children")
      }
      val listedUnder = parents[child]
      if ((listedUnder != null && listedUnder != parent) || child in children) {
        throw Unsure("node listed twice")
      }
      children.add(child)
    }
    for (child in children) {
      parents[child] = parent
    }
    moveAmongSiblings(children)
    if (moved != null) {
      children.add(moved)
    }
    ordered[parent] = children
    return children
  }

  /**
   * Moves the nodes among [children] that set their place before or after a sibling as
   * [OrderedTraversalStrategy] does: before, a node takes the sibling's place and the sibling goes
   * after the node's children; after, the node goes after the sibling's children. The whole order
   * makes each move in turn, which gives the same as long as no node is in two moves.
   */
  private fun moveAmongSiblings(children: MutableList<AccessibilityNodeInfoCompat>) {
    class Move(
      val node: AccessibilityNodeInfoCompat,
      val sibling: AccessibilityNodeInfoCompat,
      val before: Boolean,
    )
    var moves: ArrayList<Move>? = null
    val moving = HashSet<AccessibilityNodeInfoCompat>()
    for (child in children) {
      if (!hasOrderSet(child)) {
        continue
      }
      if (NodeOverrides.order(child) != null) {
        throw Unsure("traversal order set by a script")
      }
      val before = child.traversalBefore
      val sibling = before ?: child.traversalAfter ?: throw Unsure("traversal order changed")
      if (sibling == child || sibling !in children) {
        throw Unsure("traversal order set other than next to a sibling")
      }
      if (!moving.add(child) || !moving.add(sibling)) {
        throw Unsure("node in two traversal order moves")
      }
      (moves ?: ArrayList<Move>().also { moves = it }).add(Move(child, sibling, before != null))
    }
    for (move in moves ?: return) {
      children.remove(move.node)
      if (move.before) {
        children[children.indexOf(move.sibling)] = move.node
        appended[move.node] = move.sibling
        parents[move.sibling] = move.node
      } else {
        appended[move.sibling] = move.node
        parents[move.node] = move.sibling
      }
    }
  }

  /**
   * Has the children of [parent] read from the app with one request, as far as Android's node
   * cache doesn't hold them, rather than with one request each.
   */
  private fun prefetchChildren(parent: AccessibilityNodeInfoCompat) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !prefetched.add(parent)) {
      return
    }
    val info = parent.unwrap()
    if (info.childCount > 0) {
      info.getChild(0, PREFETCH_CHILDREN)
    }
  }

  /**
   * Leaves a parent with more than [MAX_CHILDREN] children to the whole order, which is saved for
   * later navigation, rather than read all of them on every navigation.
   */
  private fun checkWidth(parent: AccessibilityNodeInfoCompat) {
    if (parent.childCount > MAX_CHILDREN) {
      throw Unsure("more than $MAX_CHILDREN children")
    }
  }

  /** Whether [node] sets its place in the order apart from the node tree. */
  private fun hasOrderSet(node: AccessibilityNodeInfoCompat): Boolean =
    node.traversalBefore != null || node.traversalAfter != null || NodeOverrides.order(node) != null

  private fun read() {
    if (tree.reads() - readsAtStart >= MAX_NODES) {
      throw Unsure("more than $MAX_NODES nodes")
    }
    tree.ownReads++
  }

  /** Whether [node] was read for this order and put in it. */
  @VisibleForTesting fun holds(node: AccessibilityNodeInfoCompat): Boolean = tree.holds(node)

  /** Logs what this order answered, once. Called when the navigation that used it is over. */
  fun finish() {
    if (finished) {
      return
    }
    finished = true
    if (steps == 0 && unsureReason == null) {
      return
    }
    val prediction = buildFull == null
    if (!prediction) {
      if (unsureReason == null) answered++ else fellBack++
    }
    if (!LogUtils.shouldLog(Log.DEBUG)) {
      return
    }
    val nodes = tree.reads() - readsAtStart
    val localMs = "%.1f".format(localNanos / 1_000_000.0)
    val line =
      if (unsureReason == null) {
        "Answered: $steps steps, $nodes nodes read, $localMs ms"
      } else {
        "Fell back after $steps steps, $nodes nodes read, $localMs ms: $unsureReason" +
          (if (full != null) "; whole order $fullMs ms" else "") +
          if (mismatched) "; steps differed, navigating again" else ""
      }
    if (prediction) {
      Log.d(TAG, "Prediction: $line")
    } else {
      Log.d(TAG, "Navigation: $line ($answered answered, $fellBack fell back)")
    }
  }

  companion object {
    private const val TAG = "BacktalkLocalOrder"

    /** Reads a child with its siblings and some of its descendants, on Android 13 and later. */
    @SuppressLint("InlinedApi")
    private const val PREFETCH_CHILDREN =
      AccessibilityNodeInfo.FLAG_PREFETCH_SIBLINGS or
        AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID

    /** Most nodes read before the whole order is built instead. */
    @VisibleForTesting const val MAX_NODES = 150

    /**
     * Most children of a parent read here. A scroll view that keeps all of its content can have
     * hundreds, which Android often no longer has after a scroll, and reading them one by one took
     * longer than building the whole order, which reads the window in parallel.
     */
    @VisibleForTesting const val MAX_CHILDREN = 50

    private var answered = 0
    private var fellBack = 0

    /**
     * Starts an order of the window whose root is [root] around [pivot], or returns null when
     * [pivot] is not in that order as far as can be worked out here: then the whole order is
     * needed anyway. When a later question can't be answered here, [buildFull] builds the whole
     * order, or, when it is null, the answer is null. Carries on with the nodes read for the last
     * navigation in that window, if nothing there has changed since.
     */
    @JvmStatic
    fun start(
      pivot: AccessibilityNodeInfoCompat,
      root: AccessibilityNodeInfoCompat,
      buildFull: Supplier<OrderedTraversalStrategy>?,
    ): LocalTraversalOrder? {
      val tree = TraversalTreeCache.localTreeFor(root) ?: Tree(root)
      val order = LocalTraversalOrder(tree, buildFull)
      val start = System.nanoTime()
      try {
        order.checkInOrder(pivot)
        TraversalTreeCache.putLocal(tree)
        return order
      } catch (e: Unsure) {
        order.unsureReason = e.reason
        tree.broken = true
      } finally {
        order.localNanos += System.nanoTime() - start
      }
      order.finish()
      return null
    }
  }
}
