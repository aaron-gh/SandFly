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

package com.google.android.accessibility.talkback.actor;

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;
import static com.google.android.accessibility.utils.input.CursorGranularity.DEFAULT;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.dialog.WordSelectionDialog;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Continuous copy: moves focus from the focused item through every following one, as read from
 * next item does, scrolling lists and pages on the way, but without speaking, and collects each
 * item's text. At the last item, at the limits, or when the user touches the screen, presses a key
 * or runs another command, the texts are shown in a window, one check box each, to save the
 * checked ones to the clipboard.
 */
public final class ContinuousCopy {

  private static final String TAG = "ContinuousCopy";
  private static final String WAKE_LOCK_TAG = "SandFly:ContinuousCopy";

  /** The most items collected. */
  @VisibleForTesting static final int MAX_ITEMS = 2000;

  /** The most characters collected, of all items together. */
  @VisibleForTesting static final int MAX_CHARS = 200_000;

  /** The longest a collection runs. */
  private static final long MAX_DURATION_MS = 10 * 60_000L;

  /** Time between moving to an item and moving on, for the app's focus events to arrive. */
  private static final long STEP_DELAY_MS = 20;

  /** How often focus is looked at while a scroll brings the next item on screen. */
  private static final long POLL_MS = 50;

  /** How many times focus is looked at before the collection ends, 3 seconds. */
  private static final int MAX_POLLS = 60;

  /**
   * How long after the user stopped a collection the command does not start one again: the touch
   * or key that stops it may be the one that runs the command.
   */
  private static final long RESTART_BLOCK_MS = 2000;

  /** The texts collected, without empty texts and without a text equal to the one before it. */
  @VisibleForTesting
  static final class Lines {
    final List<String> texts = new ArrayList<>();
    int chars;
    boolean truncated;

    /**
     * Adds {@code text}, trimmed, cut to the room left; returns false once {@link #MAX_ITEMS} or
     * {@link #MAX_CHARS} is reached, and sets {@link #truncated}.
     */
    boolean add(@Nullable CharSequence text) {
      if (truncated || isFull()) {
        truncated = true;
        return false;
      }
      String line = (text == null) ? "" : text.toString().trim();
      // The same text again is the item focused again after a scroll.
      if (line.isEmpty() || (!texts.isEmpty() && texts.get(texts.size() - 1).equals(line))) {
        return true;
      }
      int room = MAX_CHARS - chars;
      if (line.length() > room) {
        int end = room;
        if (end > 0 && Character.isHighSurrogate(line.charAt(end - 1))) {
          end--;
        }
        line = line.substring(0, end);
        truncated = true;
      }
      texts.add(line);
      chars += line.length();
      if (truncated || isFull()) {
        truncated = true;
        return false;
      }
      return true;
    }

    boolean isFull() {
      return texts.size() >= MAX_ITEMS || chars >= MAX_CHARS;
    }
  }

  private final TalkBackService service;
  private final AccessibilityFocusMonitor focusMonitor;
  private final FocusActor focuser;
  private final FullScreenReadActor continuousReader;
  private final PowerManager.WakeLock wakeLock;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable step = this::step;
  private final Runnable checkFocus = this::checkFocus;
  private Pipeline.@Nullable FeedbackReturner pipeline;

  /** The texts collected so far, null when no collection runs. */
  private @Nullable Lines lines;

  /** The item collected last. */
  private @Nullable AccessibilityNodeInfoCompat last;

  private int polls;
  private long startTime;
  private long userStopTime = -RESTART_BLOCK_MS;

  @SuppressWarnings("deprecation") // SCREEN_DIM_WAKE_LOCK, as continuous reading uses.
  public ContinuousCopy(
      TalkBackService service,
      AccessibilityFocusMonitor focusMonitor,
      FocusActor focuser,
      FullScreenReadActor continuousReader) {
    this.service = service;
    this.focusMonitor = focusMonitor;
    this.focuser = focuser;
    this.continuousReader = continuousReader;
    wakeLock =
        ((PowerManager) service.getSystemService(Context.POWER_SERVICE))
            .newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,
                WAKE_LOCK_TAG);
    wakeLock.setReferenceCounted(false);
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
  }

  public boolean isActive() {
    return lines != null;
  }

  /**
   * Starts collecting from the focused item, or ends the collection that runs and shows its texts.
   * Returns false when nothing has focus.
   */
  public boolean start(EventId eventId) {
    if (isActive()) {
      finish(/* show= */ true);
      return true;
    }
    if (SystemClock.uptimeMillis() - userStopTime < RESTART_BLOCK_MS) {
      // The touch or key of this command ended a collection already.
      return true;
    }
    Pipeline.@Nullable FeedbackReturner feedback = pipeline;
    if (feedback == null) {
      return false;
    }
    @Nullable AccessibilityNodeInfoCompat node =
        focusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (node == null) {
      feedback.returnFeedback(
          eventId, Feedback.speech(service.getString(R.string.continuous_copy_no_focus)));
      return false;
    }
    if (continuousReader.isActive()) {
      continuousReader.interrupt();
    }
    feedback.returnFeedback(eventId, Feedback.part().setInterruptAllFeedback(true));
    Lines collected = new Lines();
    lines = collected;
    last = node;
    startTime = SystemClock.uptimeMillis();
    focuser.setMuteFocus(true);
    wakeLock.acquire(MAX_DURATION_MS + 10_000L);
    if (collected.add(itemText(node))) {
      handler.post(step);
    } else {
      finish(/* show= */ true);
    }
    return true;
  }

  /** Ends the collection that runs, as the user's touch, key or command does, and shows it. */
  public void stopByUser() {
    if (isActive()) {
      userStopTime = SystemClock.uptimeMillis();
      finish(/* show= */ true);
    }
  }

  /** Ends the collection that runs and drops its texts, as when the screen turns off. */
  public void cancel() {
    finish(/* show= */ false);
  }

  private void step() {
    @Nullable Lines collected = lines;
    Pipeline.@Nullable FeedbackReturner feedback = pipeline;
    if (collected == null || feedback == null) {
      return;
    }
    if (SystemClock.uptimeMillis() - startTime > MAX_DURATION_MS) {
      collected.truncated = true;
      finish(/* show= */ true);
      return;
    }
    boolean moved;
    try {
      moved =
          feedback.returnFeedback(
              EVENT_ID_UNTRACKED,
              Feedback.focusDirection(SEARCH_FOCUS_FORWARD)
                  .setGranularity(DEFAULT)
                  .setScroll(true));
    } catch (RuntimeException e) {
      LogUtils.w(TAG, "Cannot move to the next item: %s", e);
      moved = false;
    }
    if (!moved) {
      // The last item.
      finish(/* show= */ true);
      return;
    }
    polls = 0;
    checkFocus();
  }

  /** Collects the item focus moved to, or waits for a scroll to move it. */
  private void checkFocus() {
    @Nullable Lines collected = lines;
    if (collected == null) {
      return;
    }
    @Nullable AccessibilityNodeInfoCompat node;
    try {
      node = focusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    } catch (RuntimeException e) {
      LogUtils.w(TAG, "Cannot get the focused item: %s", e);
      node = null;
    }
    if (node != null && !node.equals(last)) {
      last = node;
      if (collected.add(itemText(node))) {
        handler.postDelayed(step, STEP_DELAY_MS);
      } else {
        finish(/* show= */ true);
      }
      return;
    }
    if (++polls > MAX_POLLS) {
      // A scroll that brought nothing new, or focus that didn't move.
      finish(/* show= */ true);
      return;
    }
    handler.postDelayed(checkFocus, POLL_MS);
  }

  private void finish(boolean show) {
    @Nullable Lines collected = lines;
    if (collected == null) {
      return;
    }
    lines = null;
    last = null;
    handler.removeCallbacks(step);
    handler.removeCallbacks(checkFocus);
    focuser.setMuteFocus(false);
    if (wakeLock.isHeld()) {
      wakeLock.release();
    }
    LogUtils.d(
        TAG,
        "Collected %d items, %d characters, truncated=%s, shown=%s",
        collected.texts.size(),
        collected.chars,
        collected.truncated,
        show);
    Pipeline.@Nullable FeedbackReturner feedback = pipeline;
    if (!show || feedback == null || !TalkBackService.isServiceActive()) {
      return;
    }
    if (collected.texts.isEmpty()) {
      feedback.returnFeedback(
          EVENT_ID_UNTRACKED, Feedback.speech(service.getString(R.string.no_text_on_screen)));
      return;
    }
    WordSelectionDialog.showForLines(service, feedback, collected.texts, collected.truncated);
  }

  /** The text Copy copies from {@code node}: its own, or that of the items inside it. */
  private static @Nullable CharSequence itemText(AccessibilityNodeInfoCompat node) {
    if (node.isPassword()) {
      return null;
    }
    @Nullable CharSequence text = AccessibilityNodeInfoUtils.getNodeText(node);
    return TextUtils.isEmpty(text) ? TextEditActor.childrenText(node) : text;
  }
}
