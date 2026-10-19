package com.google.android.accessibility.talkback.actor;

import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Feedback.NodeAction;
import com.google.android.accessibility.talkback.compositor.PreparedFocusSpeech;
import com.google.android.accessibility.talkback.focusmanagement.TraversalTreeCache;
import com.google.android.accessibility.utils.AccessibilityNode;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Node action performer. */
public class NodeActionPerformer {

  private static final String TAG = "NodeActionPerformer";

  private @Nullable NodeActionRecord actionRecord = null;

  /**
   * Sends clicks on keyboard keys, one at a time and in order. A keyboard answers a click by asking
   * the text field about its text. When the field is in one of Backtalk's own windows, such as a
   * settings screen, it answers on Backtalk's main thread, so a click sent from the main thread
   * waited for the keyboard while the keyboard waited for it, until both timed out.
   */
  private static final ExecutorService keyClicks =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "BacktalkKeyClicks");
            thread.setDaemon(true);
            return thread;
          });

  /** Creates node-action-record. */
  public static class NodeActionRecord {
    public final int actionId;
    private final AccessibilityNode targetNode;
    // SystemClock.uptimeMillis(), used to compare with AccessibilityEvent.getEventTime().
    public final long actionTime;

    public NodeActionRecord(int actionId, @NonNull AccessibilityNode targetNode, long actionTime) {
      this.actionId = actionId;
      this.targetNode = targetNode;
      this.actionTime = actionTime;
    }

    public boolean actionedNodeMatches(AccessibilityNodeInfo node) {
      return targetNode.equalTo(node);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  // Read-only interface

  /** Limited read-only interface to pull state data. */
  public class StateReader {
    public NodeActionRecord getNodeActionRecord() {
      return NodeActionPerformer.this.actionRecord;
    }
  }

  /** Read-only interface for pulling state data. */
  public final StateReader stateReader = new StateReader();

  ///////////////////////////////////////////////////////////////////////////////////////
  // Methods

  public boolean performAction(@NonNull NodeAction nodeAction, @Nullable EventId eventId) {
    LogUtils.d(TAG, "perform Action, nodeAction = %s", nodeAction);
    AccessibilityNode nodeActionTarget = nodeAction.target();
    boolean success = true;
    if (nodeActionTarget != null) {
      TraversalTreeCache.onNodeAction(nodeAction.actionId());
      PreparedFocusSpeech.onNodeAction(nodeAction.actionId());
      long time = SystemClock.uptimeMillis();
      if (nodeAction.actionId() == AccessibilityNodeInfoCompat.ACTION_CLICK
          && AccessibilityNodeInfoUtils.isKeyboard(nodeActionTarget.getCompat())) {
        setNodeActionRecord(new NodeActionRecord(nodeAction.actionId(), nodeActionTarget, time));
        keyClicks.execute(
            () -> {
              if (!nodeActionTarget.performAction(
                  nodeAction.actionId(), nodeAction.args(), eventId)) {
                LogUtils.w(TAG, "The keyboard did not take the click on %s", nodeActionTarget);
              }
            });
        return true;
      }
      success = nodeActionTarget.performAction(nodeAction.actionId(), nodeAction.args(), eventId);
      Performance.getInstance()
          .onAccessibilityActionPerformed(
              eventId, nodeAction.actionId(), time, success, nodeActionTarget);
      if (success) {
        setNodeActionRecord(new NodeActionRecord(nodeAction.actionId(), nodeActionTarget, time));
      }
    }
    return success;
  }

  private void setNodeActionRecord(NodeActionRecord record) {
    actionRecord = record;
  }
}
