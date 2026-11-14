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

import android.os.Build;
import android.os.Trace;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.focusmanagement.record.NodePathDescription.Deadline;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.ConcurrentNodeQueries;
import com.google.android.accessibility.utils.NodeQueryExecutor;
import com.google.android.accessibility.utils.AccessibilityNode;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.FocusFinder;
import com.google.android.accessibility.utils.PackageManagerUtils;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Objects;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
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
  private final @Nullable NodePathDescription nodePathDescription;
  /** For a node in web content, the node path with positions among siblings. Shared by copies. */
  private final @Nullable PathWithSiblingContext siblingContext;
  /** The complete path is independent of the action metadata used to speak the focus event. */
  private final @Nullable DeferredWebPath deferredWebPath;
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
    this(focusedNode, extraInfo, actionTime,
        canReadPathInBackground(focusedNode) ? PATH_EXECUTOR : null, null);
  }

  @VisibleForTesting
  FocusActionRecord(
      @NonNull AccessibilityNodeInfoCompat focusedNode,
      @NonNull FocusActionInfo extraInfo,
      long actionTime,
      @Nullable Executor executor,
      @Nullable Supplier<NodePathDescription> pathReader) {
    this.focusedNode = focusedNode;
    if (executor != null) {
      AccessibilityNodeInfoCompat copy = AccessibilityNodeInfoCompat.obtain(focusedNode);
      deferredWebPath = new DeferredWebPath(
          executor, pathReader != null ? pathReader
              : () -> NodePathDescription.obtainWithoutSiblingContext(copy));
      nodePathDescription = null;
      siblingContext = null;
    } else if (WebInterfaceUtils.supportsWebActions(focusedNode)) {
      // Web content is where the path is deepest, and where swipes are answered by the page itself,
      // without a reading order that would make the app's nodes ready for the path.
      nodePathDescription = NodePathDescription.obtainWithoutSiblingContext(focusedNode);
      siblingContext = new PathWithSiblingContext(nodePathDescription);
      deferredWebPath = null;
    } else {
      nodePathDescription = NodePathDescription.obtain(focusedNode);
      siblingContext = null;
      deferredWebPath = null;
    }
    this.extraInfo = extraInfo;
    this.actionTime = actionTime;
    this.uniqueId = compoundPackageNameAndUniqueId(focusedNode);
  }

  /** Constructs FocusActionRecord. Used internally by {@link #copy(FocusActionRecord)}. */
  private FocusActionRecord(
      @NonNull AccessibilityNodeInfoCompat focusedNode,
      @Nullable NodePathDescription nodePathDescription,
      @Nullable PathWithSiblingContext siblingContext,
      @Nullable DeferredWebPath deferredWebPath,
      @NonNull FocusActionInfo extraInfo,
      long actionTime,
      String uniqueId) {
    this.focusedNode = focusedNode;
    this.nodePathDescription = nodePathDescription == null ? null : new NodePathDescription(nodePathDescription);
    this.siblingContext = siblingContext;
    this.deferredWebPath = deferredWebPath;
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
    return getNodePathDescription(Deadline.afterSearchTimeLimit());
  }

  /**
   * Like {@link #getNodePathDescription()}, but stops asking the app for siblings at {@code
   * deadline}. The positions not read by then are read by the next call.
   */
  private @NonNull NodePathDescription getNodePathDescription(@NonNull Deadline deadline) {
    return getNodePathDescription(deadline, new NodePathDescription.Recovery());
  }

  private @NonNull NodePathDescription getNodePathDescription(
      Deadline deadline, NodePathDescription.Recovery recovery) {
    if (deferredWebPath != null) return deferredWebPath.get(deadline).withSiblings.get(deadline, recovery);
    return siblingContext == null ? Objects.requireNonNull(nodePathDescription)
        : siblingContext.get(deadline, recovery);
  }

  /**
   * Returns the focused node and its ancestors, which is enough to find whether a node is one of
   * them with {@link NodePathDescription#containsNodeByHashAndIdentity}. Waits for a web snapshot
   * that is still being captured; optional cache warming uses {@link #getReadyAncestorNodes}.
   */
  public @NonNull NodePathDescription getAncestorIdentityPath() {
    return getAncestorIdentityPath(Deadline.afterSearchTimeLimit());
  }

  private @NonNull NodePathDescription getAncestorIdentityPath(Deadline deadline) {
    return deferredWebPath != null ? deferredWebPath.get(deadline).base
        : Objects.requireNonNull(nodePathDescription);
  }

  /** Cache warming is optional: it must never wait for an independent history snapshot. */
  public List<AccessibilityNodeInfoCompat> getReadyAncestorNodes() {
    if (deferredWebPath != null && !deferredWebPath.task.isDone()) return Collections.emptyList();
    return getAncestorIdentityPath().getSavedAncestorNodes();
  }

  /** A still-existing item in this same document can anchor navigation if web focus vanishes. */
  public @Nullable AccessibilityNodeInfoCompat getSurvivingWebPivot(
      @NonNull AccessibilityNodeInfoCompat documentRoot) {
    if (focusedNode.getWindowId() != documentRoot.getWindowId()
        || uniqueId == null || focusedNode.equals(documentRoot)
        || !WebInterfaceUtils.supportsWebActions(focusedNode)
        || !"rootWebArea".contentEquals(
            String.valueOf(AccessibilityNodeInfoUtils.getChromeRole(documentRoot)))) {
      LogUtils.d(TAG, "Lost web focus: pivot preconditions differ, recordWindow=%d rootWindow=%d recordId=%s rootId=%s rootChromeRole=%s",
          focusedNode.getWindowId(), documentRoot.getWindowId(), uniqueId,
          documentRoot.getUniqueId(), AccessibilityNodeInfoUtils.getChromeRole(documentRoot));
      return null;
    }
    String documentId = compoundPackageNameAndUniqueId(documentRoot);
    if (documentId == null) return null;
    boolean sameDocument = false;
    for (AccessibilityNodeInfoCompat ancestor : getReadyAncestorNodes()) {
      if ("rootWebArea".contentEquals(
              String.valueOf(AccessibilityNodeInfoUtils.getChromeRole(ancestor)))
          && documentId.equals(compoundPackageNameAndUniqueId(ancestor))) {
        sameDocument = true;
        break;
      }
    }
    if (!sameDocument) {
      LogUtils.d(TAG, "Lost web focus: no ready matching document %s", documentId);
      return null;
    }
    // Do not search, wait for a path snapshot, or use a removed/recycled item as a pivot.
    AccessibilityNodeInfoCompat copy = AccessibilityNodeInfoCompat.obtain(focusedNode);
    boolean exists = copy.refresh();
    boolean sameItem = exists && uniqueId.equals(compoundPackageNameAndUniqueId(copy));
    if (!sameItem) LogUtils.d(TAG, "Lost web focus: saved item exists=%s sameIdentity=%s", exists, sameItem);
    return sameItem && WebInterfaceUtils.supportsWebActions(copy) ? copy : null;
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
        record.deferredWebPath,
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
   * and the node path search together stop after {@link NodePathDescription#SEARCH_TIME_LIMIT_MS}.
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
    Deadline deadline = timeLimited ? Deadline.afterSearchTimeLimit() : Deadline.none();
    NodePathDescription.Recovery recovery = new NodePathDescription.Recovery();
    try {
      AccessibilityNodeInfoCompat recorded = focusActionRecord.getFocusedNode();
      AccessibilityNodeInfoCompat lastFocusedNode = ConcurrentNodeQueries.canRead(recorded)
          ? AccessibilityNodeInfoCompat.obtain(recorded) : recorded;
      boolean refreshed = false;
      boolean refreshAttempted = false;

      // 1. Unique id match
      @Nullable String uniqueId = focusActionRecord.getUniqueId();
      if (uniqueId != null) {
        AccessibilityNodeInfoCompat uniqueIdNode = null;
        if (deadline.shouldStop()) return null;
        refreshed = lastFocusedNode.refresh();
        refreshAttempted = true;
        recovery.refreshed.put(recorded, refreshed ? AccessibilityNode.takeOwnership(lastFocusedNode) : null);
        recovery.confirmed.put(recorded, refreshed ? lastFocusedNode : null);
        if (refreshed && uniqueId.equals(compoundPackageNameAndUniqueId(lastFocusedNode))) {
          uniqueIdNode = lastFocusedNode;
        } else {
          AccessibilityNodeInfoCompat searchRoot = root;
          AccessibilityNodeInfoCompat document =
              focusActionRecord.getAncestorIdentityPath(deadline).getSavedChromiumDocument();
          if (document != null) {
            String documentId = compoundPackageNameAndUniqueId(document);
            searchRoot = findRecordedDocument(root, document, deadline, recovery);
            LogUtils.d(TAG, "Historical web document %s found=%s", documentId, searchRoot != null);
          }
          uniqueIdNode = searchWithinDeadline(searchRoot, new Filter<>() {
            @Override public boolean accept(AccessibilityNodeInfoCompat node) {
              return uniqueId.equals(compoundPackageNameAndUniqueId(node));
            }
          }, null, deadline);
        }
        if (uniqueIdNode != null && AccessibilityNodeInfoUtils.shouldFocusNode(uniqueIdNode)) {
          LogUtils.d(TAG, "getFocusableNodeFromFocusRecord from uniqueId=%s", uniqueId);
          return uniqueIdNode;
        }
      }
      // 2. Focused node match. Don't use cached node if it's in collection because collection reuse
      // views.
      if (deadline.shouldStop()) return null;
      if (!refreshAttempted) {
        refreshed = lastFocusedNode.refresh();
        recovery.refreshed.put(recorded, refreshed ? AccessibilityNode.takeOwnership(lastFocusedNode) : null);
        recovery.confirmed.put(recorded, refreshed ? lastFocusedNode : null);
      }
      if (refreshed
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
      // Identity lookup, sibling reads, and path search share this restoration's time limit.
      Deadline siblingDeadline = deadline;
      @Nullable NodePathDescription nodePath =
          focusActionRecord.getNodePathDescription(siblingDeadline, recovery); // Not owner
      if (nodePath == null) {
        return null;
      }
      Deadline searchDeadline = deadline;
      @Nullable AccessibilityNodeInfoCompat nodeAtSamePosition =
          nodePath.findNodeToRefocus(root, focusFinder, searchDeadline, recovery);
      stopped.set(siblingDeadline.stopped() || searchDeadline.stopped());
      if ((nodeAtSamePosition != null)
          && AccessibilityNodeInfoUtils.shouldFocusNode(nodeAtSamePosition)) {
        LogUtils.d(
            TAG, "getFocusableNodeFromFocusRecord from nodeAtSamePosition in nodePath=%s", nodePath);
        return nodeAtSamePosition;
      }
      return null;
    } finally {
      stopped.set(deadline.shouldStop());
    }
  }

  /** Finds the same document after a host move, without entering unrelated WebViews. */
  static @Nullable AccessibilityNodeInfoCompat findRecordedDocument(
      @Nullable AccessibilityNodeInfoCompat root, @NonNull AccessibilityNodeInfoCompat document) {
    return findRecordedDocument(root, document, Deadline.afterSearchTimeLimit(),
        new NodePathDescription.Recovery());
  }

  private static @Nullable AccessibilityNodeInfoCompat findRecordedDocument(
      @Nullable AccessibilityNodeInfoCompat root, @NonNull AccessibilityNodeInfoCompat document,
      Deadline deadline, NodePathDescription.Recovery recovery) {
    if (deadline.shouldStop()) return null;
    String documentId = compoundPackageNameAndUniqueId(document);
    if (documentId == null || root == null) {
      return null;
    }
    if (document.getWindowId() == root.getWindowId()) {
      if (!recovery.refreshed.containsKey(document)) {
        AccessibilityNodeInfoCompat copy = ConcurrentNodeQueries.canRead(document)
            ? AccessibilityNodeInfoCompat.obtain(document) : document;
        AccessibilityNodeInfoCompat current = copy.refresh() ? copy : null;
        recovery.confirmed.put(document, current);
        recovery.refreshed.put(document, AccessibilityNode.takeOwnership(current));
      }
      AccessibilityNode current = recovery.refreshed.get(document);
      if (current != null && documentId.equals(compoundPackageNameAndUniqueId(current.getCompat()))) {
        return current.getCompat();
      }
    }
    Filter<AccessibilityNodeInfoCompat> matchesDocument = new Filter<>() {
      @Override
      public boolean accept(AccessibilityNodeInfoCompat node) {
        return documentId.equals(compoundPackageNameAndUniqueId(node))
            && "rootWebArea".contentEquals(String.valueOf(AccessibilityNodeInfoUtils.getChromeRole(node)));
      }
    };
    ConcurrentNodeQueries.DocumentSearch search =
        ConcurrentNodeQueries.findDocument(root, matchesDocument, deadline.remainingMs());
    if (search != null) return search.getNode();
    return searchWithinDeadline(root, matchesDocument,
        new Filter<>() {
          @Override
          public boolean accept(AccessibilityNodeInfoCompat node) {
            return WebInterfaceUtils.supportsWebActions(node) && !matchesDocument.accept(node);
          }
        }, deadline);
  }

  /** Checks the budget before every child IPC, including a wide node's children. */
  private static @Nullable AccessibilityNodeInfoCompat searchWithinDeadline(
      @Nullable AccessibilityNodeInfoCompat root, Filter<AccessibilityNodeInfoCompat> matches,
      @Nullable Filter<AccessibilityNodeInfoCompat> prune, Deadline deadline) {
    if (root == null || deadline.shouldStop()) return null;
    ArrayDeque<AccessibilityNodeInfoCompat> pending = new ArrayDeque<>();
    HashSet<AccessibilityNodeInfoCompat> seen = new HashSet<>();
    pending.add(root);
    while (!pending.isEmpty() && !deadline.shouldStop()) {
      AccessibilityNodeInfoCompat node = pending.removeFirst();
      if (!seen.add(node)) continue;
      if (matches.accept(node)) return node;
      if (prune != null && prune.accept(node)) continue;
      for (int i = 0; i < node.getChildCount() && !deadline.shouldStop(); i++) {
        // A scoped document lookup must not implicitly prefetch the unrelated web page
        // that its prune filter deliberately refuses to enter.
        AccessibilityNodeInfoCompat child = prune == null ? node.getChild(i) : node.getChild(i, 0);
        if (child != null && !seen.contains(child)) pending.addLast(child);
      }
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
    private final ReentrantLock lock = new ReentrantLock();
    private volatile NodePathDescription published;

    PathWithSiblingContext(@NonNull NodePathDescription withoutSiblingContext) {
      this.path = new NodePathDescription(withoutSiblingContext);
      published = new NodePathDescription(path);
    }

    @NonNull NodePathDescription get(
        @NonNull Deadline deadline, NodePathDescription.Recovery recovery) {
      boolean acquired = false;
      try {
        acquired = lock.tryLock(Math.min(NodePathDescription.SEARCH_TIME_LIMIT_MS,
            deadline.remainingMs()), TimeUnit.MILLISECONDS);
        if (!acquired) return published;
        if (!completed) {
          try {
            nextToRead = path.addSiblingContext(nextToRead, deadline, recovery);
            completed = (nextToRead >= path.size());
          } catch (RuntimeException e) {
            LogUtils.w(TAG, "Cannot read the siblings of the focus path: %s", e);
            completed = true;
          }
          // Readers never see the mutable path while another reader adds sibling context.
          published = new NodePathDescription(path);
        }
        return published;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return published;
      } finally {
        if (acquired) lock.unlock();
      }
    }
  }

  private static boolean canReadPathInBackground(AccessibilityNodeInfoCompat node) {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && node.getWindowId() >= 0
        && WebInterfaceUtils.supportsWebActions(node)
        && !PackageManagerUtils.isTalkBackPackage(node.getPackageName());
  }

  // Path reads may themselves use the independent query workers, so they have a separate pool.
  private static final NodeQueryExecutor PATH_EXECUTOR = new NodeQueryExecutor("WebFocusPath", 4, 16);

  public static void startNodeQueries() { PATH_EXECUTOR.start(); }

  public static void stopNodeQueries() {
    PATH_EXECUTOR.shutdown();
    NodeDescription.clearWebPositions();
  }

  private static final class RecordedWebPath {
    final NodePathDescription base;
    final PathWithSiblingContext withSiblings;
    RecordedWebPath(NodePathDescription base) {
      this.base = base;
      withSiblings = new PathWithSiblingContext(base);
    }
  }

  /** Copies share one completed snapshot and its original deferred sibling context. */
  private static final class DeferredWebPath {
    final FutureTask<RecordedWebPath> task;
    final Executor executor;
    DeferredWebPath(Executor executor, Supplier<NodePathDescription> reader) {
      this.executor = executor;
      task = new FutureTask<>(() -> {
        Trace.beginSection("WebFocusPath.capture");
        try { return new RecordedWebPath(reader.get()); }
        finally { Trace.endSection(); }
      });
      try { executor.execute(task); }
      catch (RejectedExecutionException e) {
        if (executor instanceof NodeQueryExecutor queries && !queries.isAccepting()) task.cancel(false);
        else task.run();
      }
    }
    private final AtomicReference<RecordedWebPath> resolved = new AtomicReference<>();

    RecordedWebPath get(Deadline deadline) {
      RecordedWebPath existing = resolved.get();
      if (existing != null) return existing;
      RecordedWebPath result;
      try {
        result = task.get(Math.min(NodePathDescription.SEARCH_TIME_LIMIT_MS,
            deadline.remainingMs()), TimeUnit.MILLISECONDS);
      } catch (InterruptedException | TimeoutException e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        // The capture keeps running for a later caller with a longer budget, such as a
        // restoration started right after a swipe. Only this caller goes without a path.
        LogUtils.w(TAG, "Web focus path was not available within its budget: %s", e);
        return new RecordedWebPath(NodePathDescription.empty());
      } catch (ExecutionException | CancellationException e) {
        LogUtils.w(TAG, "Web focus path could not be captured: %s", e);
        // Pin the fallback for copies and future callers: the capture cannot complete.
        result = new RecordedWebPath(NodePathDescription.empty());
      }
      resolved.compareAndSet(null, result);
      return resolved.get();
    }
  }

  // Hash only immediate immutable metadata; equality also checks the captured identity path.
  @Override
  public int hashCode() {
    // Metadata hashing must never join a worker, and remains stable if capture times out.
    return Objects.hash(actionTime, focusedNode, extraInfo, uniqueId);
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
    if (!focusedNode.equals(otherRecord.focusedNode) || !extraInfo.equals(otherRecord.extraInfo)
        || actionTime != otherRecord.actionTime || !Objects.equals(uniqueId, otherRecord.uniqueId)) {
      return false;
    }
    if (deferredWebPath != null && deferredWebPath == otherRecord.deferredWebPath) return true;
    Deadline deadline = Deadline.afterSearchTimeLimit();
    return getAncestorIdentityPath(deadline).equals(otherRecord.getAncestorIdentityPath(deadline));
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
