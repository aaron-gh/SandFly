/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.google.android.accessibility.talkback.focusmanagement.record;

import android.text.TextUtils;
import android.util.LruCache;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;
import com.google.android.accessibility.talkback.focusmanagement.record.NodePathDescription.Deadline;
import com.google.android.accessibility.utils.AccessibilityNode;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import java.util.Objects;
import java.util.ArrayDeque;
import java.util.HashSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Describes a single {@link AccessibilityNodeInfoCompat} with some immutable information. */
public class NodeDescription {
  /** Hints only: every remembered position is checked against the parent's current child. */
  private static final LruCache<Integer, Integer> webChildPositions = new LruCache<>(1000);

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Constants

  static final int UNDEFINED_INDEX = -1;

  /** Indicates the index directly got from parent.getChild(index). */
  static final int INDEX_TYPE_RAW = 0;
  /**
   * Indicates the row/column index got from {@link
   * android.view.accessibility.AccessibilityNodeInfo.CollectionItemInfo}.
   */
  static final int INDEX_TYPE_COLLECTION = 1;

  static final CharSequence OUT_OF_RANGE = "OUT_OF_RANGE";

  static final int MAX_TEXT_COLLECT_NODES = 5;

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Member data

  // Immutable identity information from View.
  final CharSequence className;
  final String viewIdResourceName;

  // Content and surrounding content.
  public final @Nullable CharSequence text;
  public final @Nullable CharSequence previousSiblingText;
  public final @Nullable CharSequence nextSiblingText;

  // Index from AccessibilityNodeInfo.getChild(index).
  final int rowIndex;
  // Index from CollectionItemInfo.
  final int columnIndex;
  final int rawIndexInParent;

  // Mutable information from AccessibilityNodeInfoCompat.
  // The information stays immutable as long as the view stays visible on screen.
  // Ideally we want AccessibilityNodeInfo.mSourceNodeId but that is an hidden API. We work around
  // this by using hashCode(), which is generated based on mSourceNodeId.
  final int nodeInfoHashCode;
  public final @Nullable AccessibilityNode savedNode;

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Construction methods

  @VisibleForTesting
  NodeDescription(
      CharSequence className,
      String viewIdResourceName,
      int rowIndex,
      int columnIndex,
      int rawIndexInParent,
      int nodeInfoHashCode) {
    this.className = className;
    this.viewIdResourceName = viewIdResourceName;
    this.rowIndex = rowIndex;
    this.columnIndex = columnIndex;
    this.rawIndexInParent = rawIndexInParent;
    this.nodeInfoHashCode = nodeInfoHashCode;
    this.savedNode = null;
    this.text = null;
    this.previousSiblingText = null;
    this.nextSiblingText = null;
  }

  public NodeDescription(@NonNull NodeDescription original) {
    this.className = original.className;
    this.viewIdResourceName = original.viewIdResourceName;
    this.rowIndex = original.rowIndex;
    this.columnIndex = original.columnIndex;
    this.rawIndexInParent = original.rawIndexInParent;
    this.nodeInfoHashCode = original.nodeInfoHashCode;
    this.savedNode = original.savedNode;
    this.text = original.text;
    this.previousSiblingText = original.previousSiblingText;
    this.nextSiblingText = original.nextSiblingText;
  }

  /**
   * Describes {@code node}, with its position among the children of {@code parent}, which is the
   * parent of {@code node} or null.
   */
  NodeDescription(
      @NonNull AccessibilityNodeInfoCompat node,
      boolean isPathEnd,
      @Nullable AccessibilityNode parent) {
    this(
        withoutSiblingContext(node, isPathEnd),
        parent,
        isPathEnd,
        Integer.MAX_VALUE,
        /* deadline= */ null);
  }

  /**
   * Describes {@code node} without its position among its siblings and their text, which take a
   * request to the app for each sibling read. {@link #withSiblingContext} adds them.
   */
  static NodeDescription withoutSiblingContext(
      @NonNull AccessibilityNodeInfoCompat node, boolean isPathEnd) {
    return withoutSiblingContext(node, isPathEnd, Deadline.none());
  }

  static NodeDescription withoutSiblingContext(
      @NonNull AccessibilityNodeInfoCompat node, boolean isPathEnd, Deadline deadline) {
    @NonNull AccessibilityNode savedNode = AccessibilityNode.takeOwnership(node);
    // Get position data from collection-data.
    CollectionItemInfoCompat itemInfo = node.getCollectionItemInfo();
    return new NodeDescription(
        savedNode,
        node.hashCode(),
        getText(savedNode, isPathEnd, deadline),
        node.getClassName(),
        node.getViewIdResourceName(),
        itemInfo == null ? UNDEFINED_INDEX : itemInfo.getRowIndex(),
        itemInfo == null ? UNDEFINED_INDEX : itemInfo.getColumnIndex(),
        UNDEFINED_INDEX,
        /* previousSiblingText= */ null,
        /* nextSiblingText= */ null);
  }

  /**
   * Returns this description with the node's position among the children of {@code parent}, and
   * the text of the siblings beside it. {@code parent} is the node's parent, as it was when this
   * description was made, or null. Once {@code deadline} has passed, no more children of {@code
   * parent} are read, and the position and the text of the siblings stay undefined.
   */
  NodeDescription withSiblingContext(
      @Nullable AccessibilityNode parent, boolean isPathEnd, @NonNull Deadline deadline) {
    return new NodeDescription(this, parent, isPathEnd, Integer.MAX_VALUE, deadline);
  }

  /**
   * Like {@link #withSiblingContext(AccessibilityNode, boolean, Deadline)}, without a deadline, but
   * reads at most {@code maxChildrenRead} children of {@code parent} to find the node's position.
   * Further on, the position and the text of the siblings stay undefined.
   */
  NodeDescription withSiblingContext(
      @Nullable AccessibilityNode parent, boolean isPathEnd, int maxChildrenRead) {
    return new NodeDescription(this, parent, isPathEnd, maxChildrenRead, /* deadline= */ null);
  }

  NodeDescription withSiblingContext(
      @Nullable AccessibilityNode parent, boolean isPathEnd, int maxChildrenRead, Deadline deadline) {
    return new NodeDescription(this, parent, isPathEnd, maxChildrenRead, deadline);
  }

  static void clearWebPositions() { webChildPositions.evictAll(); }

  private NodeDescription(
      @NonNull NodeDescription original,
      @Nullable AccessibilityNode parent,
      boolean isPathEnd,
      int maxChildrenRead,
      @Nullable Deadline deadline) {
    this.savedNode = original.savedNode;
    this.nodeInfoHashCode = original.nodeInfoHashCode;
    this.text = original.text;
    this.className = original.className;
    this.viewIdResourceName = original.viewIdResourceName;
    this.rowIndex = original.rowIndex;
    this.columnIndex = original.columnIndex;

    // Get data from node's position within parent-node.
    this.rawIndexInParent =
        (savedNode == null)
            ? UNDEFINED_INDEX
            : getRawIndexInParent(savedNode, parent, maxChildrenRead, deadline);
    if (rawIndexInParent == UNDEFINED_INDEX) {
      this.previousSiblingText = null;
      this.nextSiblingText = null;
    } else {
      this.previousSiblingText = getChildText(parent, rawIndexInParent - 1, isPathEnd, deadline);
      this.nextSiblingText = getChildText(parent, rawIndexInParent + 1, isPathEnd, deadline);
    }
  }

  private NodeDescription(
      @Nullable AccessibilityNode savedNode,
      int nodeInfoHashCode,
      @Nullable CharSequence text,
      CharSequence className,
      String viewIdResourceName,
      int rowIndex,
      int columnIndex,
      int rawIndexInParent,
      @Nullable CharSequence previousSiblingText,
      @Nullable CharSequence nextSiblingText) {
    this.savedNode = savedNode;
    this.nodeInfoHashCode = nodeInfoHashCode;
    this.text = text;
    this.className = className;
    this.viewIdResourceName = viewIdResourceName;
    this.rowIndex = rowIndex;
    this.columnIndex = columnIndex;
    this.rawIndexInParent = rawIndexInParent;
    this.previousSiblingText = previousSiblingText;
    this.nextSiblingText = nextSiblingText;
  }

  /** Returns the same recorded context with its independently collected focused-item text. */
  NodeDescription withText(@Nullable CharSequence focusedText) {
    return new NodeDescription(savedNode, nodeInfoHashCode, focusedText, className,
        viewIdResourceName, rowIndex, columnIndex, rawIndexInParent,
        previousSiblingText, nextSiblingText);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Methods for finding a matching node

  public boolean identityMatches(AccessibilityNodeInfoCompat node) {
    return (node != null)
        && TextUtils.equals(node.getClassName(), className)
        && TextUtils.equals(node.getViewIdResourceName(), viewIdResourceName);
  }

  public boolean indexMatches(AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }
    if (hasCollectionIndex()) {
      return matchesCollectionIndices(node);
    } else {
      AccessibilityNodeInfoCompat parent = node.getParent();
        if (parent == null || parent.getChildCount() <= rawIndexInParent) {
          return false;
        }

      AccessibilityNodeInfoCompat child = parent.getChild(rawIndexInParent);
      return node.equals(child);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NodeDescription)) {
      return false;
    }

    NodeDescription that = (NodeDescription) o;
    // node & nodeInfoHashCode may change as the View goes off/on screen, so don't compare them.
    return (rowIndex == that.rowIndex)
        && (columnIndex == that.columnIndex)
        && (rawIndexInParent == that.rawIndexInParent)
        && TextUtils.equals(className, that.className)
        && TextUtils.equals(viewIdResourceName, that.viewIdResourceName)
        && TextUtils.equals(this.text, that.text)
        && TextUtils.equals(this.previousSiblingText, that.previousSiblingText)
        && TextUtils.equals(this.nextSiblingText, that.nextSiblingText);
  }

  @Override
  public int hashCode() {
    // nodeInfoHashCode is mutable as the View goes off/on screen, thus we don't add it here.
    return Objects.hash(
        className,
        viewIdResourceName,
        rowIndex,
        columnIndex,
        rawIndexInParent,
        text,
        previousSiblingText,
        nextSiblingText);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Methods for extracting data from a node

  private static @Nullable CharSequence getChildText(
      @Nullable AccessibilityNode parent, int childIndex, boolean isPathEnd, @Nullable Deadline deadline) {
    if ((parent == null) || (childIndex < 0) || (parent.getChildCount() <= childIndex)) {
      return OUT_OF_RANGE;
    }
    if (deadline != null && deadline.shouldStop()) return null;
    @Nullable AccessibilityNode child = parent.getChild(childIndex);
    if (child != null && WebInterfaceUtils.supportsWebActions(child.getCompat())) {
      webChildPositions.put(child.getCompat().hashCode(), childIndex);
    }
    return (child == null) ? null : getText(child, isPathEnd, deadline == null ? Deadline.none() : deadline);
  }

  public static @Nullable CharSequence getText(
      @Nullable AccessibilityNode node, boolean isPathEnd) {
    return getText(node, isPathEnd, Deadline.none());
  }

  /** Same DFS text order, without starting another child read after the caller's deadline. */
  static @Nullable CharSequence getText(
      @Nullable AccessibilityNode node, boolean isPathEnd, Deadline deadline) {
    if (node == null) return null;
    CharSequence description = node.getContentDescription();
    if (!TextUtils.isEmpty(description) && TextUtils.getTrimmedLength(description) > 0) return description;
    if (!isPathEnd) return node.getText();
    LeafTextCollector collector = new LeafTextCollector();
    record Frame(AccessibilityNode node, int next) {}
    ArrayDeque<Frame> stack = new ArrayDeque<>();
    HashSet<AccessibilityNode> seen = new HashSet<>();
    AccessibilityNode current = node;
    while (current != null && !deadline.shouldStop()) {
      if (seen.add(current)) {
        if (collector.accept(current.getCompat())) break;
        stack.addLast(new Frame(current, 0));
      }
      current = null;
      while (!stack.isEmpty() && current == null && !deadline.shouldStop()) {
        Frame frame = stack.removeLast();
        if (frame.next() < frame.node().getChildCount()) {
          stack.addLast(new Frame(frame.node(), frame.next() + 1));
          current = frame.node().getChild(frame.next());
        }
      }
    }
    return collector.getText();
  }

  /** Node-callback that collects first {@code MAX_TEXT_COLLECT_NODES} subtree texts. */
  private static class LeafTextCollector extends Filter<AccessibilityNodeInfoCompat> {

    private final StringBuilder text = new StringBuilder();
    private int numNodes = 0;

    @Override
    public boolean accept(AccessibilityNodeInfoCompat node) {
      @Nullable CharSequence nodeText = AccessibilityNodeInfoUtils.getNodeText(node);
      if (!TextUtils.isEmpty(nodeText)) {
        if (!TextUtils.isEmpty(text)) {
          text.append(", ");
        }
        text.append(nodeText);
      }
      ++numNodes;
      return (MAX_TEXT_COLLECT_NODES < numNodes); // Limit number of nodes to search.
    }

    public String getText() {
      return text.toString();
    }
  }

  /** Returns whether node matches row/column-index. Caller keeps ownership of node-argument. */
  public boolean matchesCollectionIndices(@Nullable AccessibilityNode node) {
    if (!hasCollectionIndex() || (node == null)) {
      return false;
    }
    @Nullable CollectionItemInfoCompat itemInfo = node.getCollectionItemInfo();
    return (itemInfo != null)
        && (itemInfo.getRowIndex() == rowIndex)
        && (itemInfo.getColumnIndex() == columnIndex);
  }

  /** Returns whether node matches row/column-index. Caller keeps ownership of node-argument. */
  public boolean matchesCollectionIndices(@Nullable AccessibilityNodeInfoCompat node) {
    if (!hasCollectionIndex() || (node == null)) {
      return false;
    }
    @Nullable CollectionItemInfoCompat itemInfo = node.getCollectionItemInfo();
    return (itemInfo != null)
        && (itemInfo.getRowIndex() == rowIndex)
        && (itemInfo.getColumnIndex() == columnIndex);
  }

  public boolean hasCollectionIndex() {
    return (rowIndex != UNDEFINED_INDEX) || (columnIndex != UNDEFINED_INDEX);
  }

  /** Returns the node's position among the children of {@code parent}, read until {@code deadline}. */
  static int getRawIndexInParent(
      @NonNull AccessibilityNode node,
      @Nullable AccessibilityNode parent,
      @NonNull Deadline deadline) {
    return getRawIndexInParent(node, parent, Integer.MAX_VALUE, deadline);
  }

  private static int getRawIndexInParent(
      @NonNull AccessibilityNode node,
      @Nullable AccessibilityNode parent,
      int maxChildrenRead,
      @Nullable Deadline deadline) {
    if (parent == null || (deadline != null && deadline.shouldStop())) {
      return UNDEFINED_INDEX;
    }

    // When the parent node is a view pager, the child index should be replaced with item info's
    // column index.
    if (parent.getRole() == Role.ROLE_PAGER) {
      CollectionItemInfoCompat itemInfoCompat = node.getCollectionItemInfo();
      if (itemInfoCompat != null) {
        return itemInfoCompat.getColumnIndex();
      }
    }

    int childrenRead = Math.min(parent.getChildCount(), maxChildrenRead);
    if ((deadline != null) && deadline.shouldStop()) {
      return UNDEFINED_INDEX;
    }
    if (WebInterfaceUtils.supportsWebActions(node.getCompat())) {
      Integer remembered = webChildPositions.get(node.getCompat().hashCode());
      if (remembered != null && remembered < childrenRead) {
        if (node.equals(parent.getChild(remembered))) {
          return remembered;
        }
        webChildPositions.remove(node.getCompat().hashCode());
      }
    }
    for (int i = 0; i < childrenRead; i++) {
      if ((deadline != null) && deadline.shouldStop()) {
        return UNDEFINED_INDEX;
      }
      AccessibilityNode child = parent.getChild(i);
      if (child != null && WebInterfaceUtils.supportsWebActions(child.getCompat())) {
        webChildPositions.put(child.getCompat().hashCode(), i);
      }
      if (node.equals(child)) {
        return i;
      }
    }
    return UNDEFINED_INDEX;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Methods for logging

  @Override
  public String toString() {
    return StringBuilderUtils.joinFields(
        StringBuilderUtils.optionalInt("index", rawIndexInParent, UNDEFINED_INDEX),
        StringBuilderUtils.optionalInt("rowIndex", rowIndex, UNDEFINED_INDEX),
        StringBuilderUtils.optionalInt("columnIndex", columnIndex, UNDEFINED_INDEX),
        StringBuilderUtils.optionalText("className", className),
        StringBuilderUtils.optionalText("viewIdResourceName", viewIdResourceName),
        StringBuilderUtils.optionalText("text", text),
        StringBuilderUtils.optionalText("previousSiblingText", previousSiblingText),
        StringBuilderUtils.optionalText("nextSiblingText", nextSiblingText),
        StringBuilderUtils.optionalInt("hashcode", nodeInfoHashCode, 0));
  }
}
