/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.android.accessibility.utils.AccessibilityNodeInfoUtils.toStringShort;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.focusmanagement.record.NodePathDescription.Deadline;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.FocusFinder;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A record of TalkBack performing {@link AccessibilityNodeInfoCompat#ACTION_ACCESSIBILITY_FOCUS}
 * action.
 */
public class FocusActionRecord {
  private static final String TAG = "FocusActionRecord";

  /**
   * Time when the accessibility focus action is performed. Initialized with
   * SystemClock.uptimeMillis().
   */
  private final long actionTime;
  /** Node being accessibility focused. */
  private final @NonNull AccessibilityNodeInfoCompat focusedNode;
  /**
   * Describes how to find focused node from root node. In web content, it leaves out the ancestors'
   * positions among their siblings, which {@link #siblingContext} adds when they are needed.
   */
  private final @NonNull NodePathDescription nodePathDescription;
  /** For a node in web content, the node path with positions among siblings. Shared by copies. */
  private final @Nullable PathWithSiblingContext siblingContext;
  /** Extra information about source focus action. */
  private final @NonNull FocusActionInfo extraInfo;
  /** Unique id for the node */
  private final String uniqueId;

  private static @Nullable String compoundPackageNameAndUniqueId(
      @Nullable AccessibilityNodeInfoCompat nodeInfo) {
    if (nodeInfo == null) {
      return null;
    }
    String compoundId = nodeInfo.getUniqueId();
    if (compoundId != null) {
      compoundId = String.valueOf(nodeInfo.getPackageName()) + ':' + compoundId;
    }
    return compoundId;
  }

  /**
   * Constructs a FocusActionRecord.
   *
   * @param focusedNode Node being accessibility focused.
   * @param extraInfo Extra information defined by action performer.
   * @param actionTime Time when focus action happens. Got by {@link SystemClock#uptimeMillis()}.
   */
  public FocusActionRecord(
      @NonNull AccessibilityNodeInfoCompat focusedNode,
      @NonNull FocusActionInfo extraInfo,
      long actionTime) {
    this.focusedNode = focusedNode;
    if (WebInterfaceUtils.supportsWebActions(focusedNode)) {
      // Web content is where the path is deepest, and where swipes are answered by the page itself,
      // without a reading order that would make the app's nodes ready for the path.
      nodePathDescription = NodePathDescription.obtainWithoutSiblingContext(focusedNode);
      siblingContext = new PathWithSiblingContext(nodePathDescription);
    } else {
      nodePathDescription = NodePathDescription.obtain(focusedNode);
      siblingContext = null;
    }
    this.extraInfo = extraInfo;
    this.actionTime = actionTime;
    this.uniqueId = compoundPackageNameAndUniqueId(focusedNode);
  }

  /** Constructs FocusActionRecord. Used internally by {@link #copy(FocusActionRecord)}. */
  private FocusActionRecord(
      @NonNull AccessibilityNodeInfoCompat focusedNode,
      @NonNull NodePathDescription nodePathDescription,
      @Nullable PathWithSiblingContext siblingContext,
      @NonNull FocusActionInfo extraInfo,
      long actionTime,
      String uniqueId) {
    this.focusedNode = focusedNode;
    this.nodePathDescription = new NodePathDescription(nodePathDescription);
    this.siblingContext = siblingContext;
    this.extraInfo = extraInfo;
    this.actionTime = actionTime;
    this.uniqueId = uniqueId;
  }

  /** Returns an instance of the focused node. */
  public @NonNull AccessibilityNodeInfoCompat getFocusedNode() {
    return focusedNode;
  }

  /**
   * Returns reference to node-path. In web content, the first call asks the app for the siblings of
   * the focused node's ancestors.
   */
  public @NonNull NodePathDescription getNodePathDescription() {
    return getNodePathDescription(Deadline.none());
  }

  /**
   * Like {@link #getNodePathDescription()}, but stops asking the app for siblings at {@code
   * deadline}. The positions not read by then are read by the next call.
   */
  private @NonNull NodePathDescription getNodePathDescription(@NonNull Deadline deadline) {
    return (siblingContext == null) ? nodePathDescription : siblingContext.get(deadline);
  }

  /**
   * Returns the focused node and its ancestors, which is enough to find whether a node is one of
   * them with {@link NodePathDescription#containsNodeByHashAndIdentity}. Does not ask the app for
   * anything.
   */
  public @NonNull NodePathDescription getAncestorIdentityPath() {
    return nodePathDescription;
  }

  /** Returns extra information of the focus action. */
  public @NonNull FocusActionInfo getExtraInfo() {
    return extraInfo;
  }

  /**
   * Returns the time when the accessibility focus action happens, which is initialized with {@link
   * SystemClock#uptimeMillis()}.
   */
  public long getActionTime() {
    return actionTime;
  }

  /** Returns the stored unique id which is created in the FocusActionRecord constructor. */
  public @Nullable String getUniqueId() {
    return uniqueId;
  }

  /** Returns a copied instance of another FocusActionRecord. */
  public static @Nullable FocusActionRecord copy(FocusActionRecord record) {
    if (record == null) {
      return null;
    }
    return new FocusActionRecord(
        record.focusedNode,
        record.nodePathDescription,
        record.siblingContext,
        record.extraInfo,
        record.actionTime,
        record.uniqueId);
  }

  /**
   * Returns the focusable node from the focus record with the following priority:
   *
   * <ol>
   *   <li>Unique id match
   *   <li>Focused node match
   *   <li>Node path score match
   * </ol>
   *
   * <p>With {@code timeLimited}, reading the positions of the path's ancestors among their siblings
   * and the node path search each stop after {@link NodePathDescription#SEARCH_TIME_LIMIT_MS}.
   *
   * @param root The root node of the current tree.
   * @param focusFinder The focus finder to find the focusable node.
   * @param focusActionRecord The focus action record to get the focusable node from.
   * @param timeLimited Whether reading the siblings and the node path search have a time limit.
   * @param stopped Set to whether the node path search, or the reading of the siblings for it,
   *     stopped at its time limit.
   * @return The focusable node from the focus record.
   */
  static @Nullable AccessibilityNodeInfoCompat getFocusableNodeFromFocusRecord(
      @Nullable AccessibilityNodeInfoCompat root,
      @NonNull FocusFinder focusFinder,
      @NonNull FocusActionRecord focusActionRecord,
      boolean timeLimited,
      @NonNull AtomicBoolean stopped) {
    stopped.set(false);
    AccessibilityNodeInfoCompat lastFocusedNode = focusActionRecord.getFocusedNode();

    // 1. Unique id match
    Deadline uniqueIdDeadline = timeLimited ? Deadline.afterSearchTimeLimit() : Deadline.none();
    @Nullable String uniqueId = focusActionRecord.getUniqueId();
    if (uniqueId != null) {
      AccessibilityNodeInfoCompat uniqueIdNode = null;
      if (lastFocusedNode.refresh()
          && uniqueId.equals(compoundPackageNameAndUniqueId(lastFocusedNode))) {
        uniqueIdNode = lastFocusedNode;
      } else {
        uniqueIdNode =
            AccessibilityNodeInfoUtils.searchFromBfs(
                root,
                new Filter<AccessibilityNodeInfoCompat>() {
                  @Override
                  public boolean accept(AccessibilityNodeInfoCompat node) {
                    return uniqueId.equals(compoundPackageNameAndUniqueId(node));
                  }
                },
                new Filter<AccessibilityNodeInfoCompat>() {
                  @Override
                  public boolean accept(AccessibilityNodeInfoCompat node) {
                    // Once the deadline has passed, the remaining queued nodes are skipped
                    // without reading their children.
                    return uniqueIdDeadline.shouldStop();
                  }
                });
      }
      if (uniqueIdNode != null && AccessibilityNodeInfoUtils.shouldFocusNode(uniqueIdNode)) {
        LogUtils.d(TAG, "getFocusableNodeFromFocusRecord from uniqueId=%s", uniqueId);
        return uniqueIdNode;
      }
    }
    // 2. Focused node match. Don't use cached node if it's in collection because collection reuse
    // views.
    if (lastFocusedNode.refresh()
        && AccessibilityNodeInfoUtils.shouldFocusNode(lastFocusedNode)
        && !AccessibilityNodeInfoUtils.isInCollection(lastFocusedNode)) {
      LogUtils.d(
          TAG,
          "getFocusableNodeFromFocusRecord from focused node in focusActionRecord=%s",
          focusActionRecord);
      return lastFocusedNode;
    }
    // 3. Node path score match
    if (root == null) {
      return null;
    }
    // Reading the siblings of the path, done once per record, and searching for it each have their
    // own time limit.
    Deadline siblingDeadline = timeLimited ? Deadline.afterSearchTimeLimit() : Deadline.none();
    @Nullable NodePathDescription nodePath =
        focusActionRecord.getNodePathDescription(siblingDeadline); // Not owner
    if (nodePath == null) {
      return null;
    }
    Deadline searchDeadline = timeLimited ? Deadline.afterSearchTimeLimit() : Deadline.none();
    @Nullable AccessibilityNodeInfoCompat nodeAtSamePosition =
        nodePath.findNodeToRefocus(root, focusFinder, searchDeadline);
    stopped.set(
        uniqueIdDeadline.stopped() || siblingDeadline.stopped() || searchDeadline.stopped());
    if ((nodeAtSamePosition != null)
        && AccessibilityNodeInfoUtils.shouldFocusNode(nodeAtSamePosition)) {
      LogUtils.d(
          TAG, "getFocusableNodeFromFocusRecord from nodeAtSamePosition in nodePath=%s", nodePath);
      return nodeAtSamePosition;
    }
    return null;
  }

  /**
   * A node path completed with positions among siblings the first time it is needed. If reading
   * them fails, the path as read so far is kept, and is not read again. If reading them reaches the
   * deadline, the next call reads the ancestors not yet read.
   */
  private static final class PathWithSiblingContext {
    /** The path, with the positions read so far. Copied for callers until all are read. */
    private final @NonNull NodePathDescription path;

    /** The index in {@link #path} of the first ancestor whose position is not read yet. */
    private int nextToRead = 1;

    private boolean completed = false;

    PathWithSiblingContext(@NonNull NodePathDescription withoutSiblingContext) {
      this.path = new NodePathDescription(withoutSiblingContext);
    }

    synchronized @NonNull NodePathDescription get(@NonNull Deadline deadline) {
      if (!completed) {
        try {
          nextToRead = path.addSiblingContext(nextToRead, deadline);
          completed = (nextToRead >= path.size());
        } catch (RuntimeException e) {
          LogUtils.w(TAG, "Cannot read the siblings of the focus path: %s", e);
          completed = true;
        }
        if (!completed) {
          return new NodePathDescription(path);
        }
      }
      return path;
    }
  }

  // Compare and hash the path as it was read when focus moved, which asks the app for nothing.
  @Override
  public int hashCode() {
    return Objects.hash(actionTime, focusedNode, nodePathDescription, extraInfo);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof FocusActionRecord)) {
      return false;
    }
    FocusActionRecord otherRecord = (FocusActionRecord) other;
    return (focusedNode.equals(otherRecord.focusedNode))
        && (nodePathDescription.equals(otherRecord.nodePathDescription))
        && (extraInfo.equals(otherRecord.extraInfo))
        && (actionTime == otherRecord.actionTime)
        && Objects.equals(uniqueId, otherRecord.uniqueId);
  }

  public boolean focusedNodeEquals(AccessibilityNodeInfoCompat targetNode) {
    if (focusedNode == null || targetNode == null) {
      return false;
    }

    // Unique id (if it exists) dominates the focus record comparison.
    String targetUniqueId = compoundPackageNameAndUniqueId(targetNode);
    if (!Objects.equals(uniqueId, targetUniqueId)) {
      return false;
    } else if (uniqueId != null) {
      return true;
    }

    return (focusedNode == targetNode) || focusedNode.equals(targetNode);
  }

  @Override
  public String toString() {
    return "FocusActionRecord: \n    "
        + "node="
        + toStringShort(focusedNode)
        + "\n    "
        + "time="
        + actionTime
        + "\n    "
        + "extraInfo="
        + extraInfo.toString()
        + "\n    "
        + "uniqueId="
        + uniqueId;
  }
}
