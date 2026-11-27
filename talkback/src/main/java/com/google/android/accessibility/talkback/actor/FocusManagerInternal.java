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

package com.google.android.accessibility.talkback.actor;

import static android.view.accessibility.AccessibilityNodeInfo.FOCUS_ACCESSIBILITY;
import static android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.INITIAL_FOCUS_FIRST_CONTENT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.INITIAL_FOCUS_FOLLOW_INPUT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.INITIAL_FOCUS_RESTORE;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.ActorStateWritable;
import com.google.android.accessibility.talkback.CursorGranularityManager;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Feedback.Focus;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.TouchInteractionMonitor;
import com.google.android.accessibility.talkback.actor.helper.FocusActorHelper;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.talkback.focusmanagement.FocusProcessorForScreenStateChange;
import com.google.android.accessibility.talkback.focusmanagement.SheetsGrid;
import com.google.android.accessibility.talkback.focusmanagement.FocusProcessorForLogicalNavigation;
import com.google.android.accessibility.talkback.focusmanagement.TraversalTreeCache;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenState;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenStateMonitor;
import com.google.android.accessibility.talkback.focusmanagement.record.AccessibilityFocusActionHistory;
import com.google.android.accessibility.talkback.focusmanagement.record.AccessibilityFocusActionHistory.ActivatedItem;
import com.google.android.accessibility.talkback.focusmanagement.record.AccessibilityFocusActionHistory.WindowIdentifier;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionRecord;
import com.google.android.accessibility.talkback.report.ReportNotes;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils;
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.FocusFinder;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.accessibility.utils.input.CursorGranularity;
import com.google.android.accessibility.utils.input.WindowEventInterpreter;
import com.google.android.accessibility.utils.monitor.InputModeTracker;
import com.google.android.accessibility.utils.output.LowLatencyAudio;
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Helps FocusActor execute focus actions. */
// TODO: Merge FocusActor with FocusManagerInternal.
public class FocusManagerInternal {

  private static final String TAG = "FocusManagerInternal";

  // How long after an app starts recording focus is left alone when the app changes its controls.
  private static final long RECORDING_START_MS = 5000;

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Member variables

  private final AccessibilityService service;
  private final FocusFinder focusFinder;
  private final ScreenStateMonitor.State screenState;

  @VisibleForTesting protected boolean muteNextFocus = false;

  /** Whether every focus set is silent, as while Continuous copy collects the items' text. */
  private boolean muteFocus = false;

  /** Writable focus-history. */
  private final AccessibilityFocusActionHistory history;

  /** Actor-state passed in from pipeline, which encapsulates {@code history}. */
  private ActorStateWritable actorState;

  private final AccessibilityFocusMonitor accessibilityFocusMonitor;

  private Pipeline.FeedbackReturner pipeline;

  // To avoid the endless repeat of Ensure A11y focus-on-screen, the [sourceAction] is recorded and
  // validate before doing so. In case the focused node disappears by user's operation but framework
  // does not report the a11y focus clear, the ensure focus procedure will escape unexpectedly. Here
  // the new flag to determine the validity of source action; suppose when user touch the screen,
  // the ensure focus would need perform again.
  private boolean skipRefocus = false;

  // An app that draws an item again and again, as Facebook does while images load, takes focus off
  // it each time. Focus returns to the same item this many times between touches.
  private static final int MAX_SAME_ITEM_RESTORES = 3;
  private int sameItemRestores = 0;

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Construction methods

  public FocusManagerInternal(
      AccessibilityService service,
      FocusFinder focusFinder,
      ScreenStateMonitor.State screenState,
      AccessibilityFocusActionHistory history,
      AccessibilityFocusMonitor accessibilityFocusMonitor) {
    this.service = service;
    this.focusFinder = focusFinder;
    this.screenState = screenState;
    this.history = history;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
  }

  public void setActorState(ActorStateWritable actorState) {
    this.actorState = actorState;
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
  }

  /** Told when accessibility focus has been set, before the app's focus event for it arrives. */
  public interface FocusSetListener {
    /**
     * Told just before the focus action on {@code node}, which waits for the app. Returns whether
     * the focus was spoken now. If it was, {@link #onAccessibilityFocusNotSet} follows when the
     * action fails.
     *
     * @param actionTime when the focus action started, as for {@link #onAccessibilityFocusSet}
     */
    default boolean beforeAccessibilityFocusSet(
        AccessibilityNodeInfoCompat node,
        FocusActionInfo info,
        @Nullable EventId eventId,
        long actionTime) {
      return false;
    }

    /**
     * @param actionTime when the focus action started, in {@link SystemClock#uptimeMillis()} time,
     *     before the app could send its focus event for it
     */
    void onAccessibilityFocusSet(
        AccessibilityNodeInfoCompat node,
        FocusActionInfo info,
        @Nullable EventId eventId,
        long actionTime);

    /** Told when the focus action on {@code node}, spoken before it, failed. */
    default void onAccessibilityFocusNotSet(AccessibilityNodeInfoCompat node) {}
  }

  private @Nullable FocusSetListener focusSetListener;

  public void setFocusSetListener(@Nullable FocusSetListener listener) {
    focusSetListener = listener;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Methods

  public void setMuteNextFocus() {
    muteNextFocus = true;
  }

  /** Makes every focus set silent until called with false. */
  public void setMuteFocus(boolean mute) {
    muteFocus = mute;
  }

  public void renewEnsureFocus() {
    skipRefocus = false;
    sameItemRestores = 0;
  }

  /**
   * Tries to set accessibility focus on the given node. It's used by {@link FocusActor} to set
   * accessibility focus.
   *
   * <p>This method attempts to focus the node only when the node is not accessibility focus or when
   * {@code forceRefocusIfAlreadyFocused} is {@code true}.
   *
   * @param node Node to be focused.
   * @param forceRefocusIfAlreadyFocused Whether we should perform ACTION_ACCESSIBILITY_FOCUS if the
   *     node is already accessibility focused.
   * @param eventId The EventId for performance tracking.
   * @return Whether the node is already accessibility focused or we successfully put accessibility
   *     focus on the node.
   */
  boolean setAccessibilityFocus(
      @NonNull AccessibilityNodeInfoCompat node,
      boolean forceRefocusIfAlreadyFocused,
      @NonNull FocusActionInfo focusActionInfo,
      @Nullable EventId eventId) {
    SheetsGrid.onFocusAction(focusActionInfo.sourceAction);
    if (focusActionInfo.sourceAction == FocusActionInfo.LOGICAL_NAVIGATION) {
      // Keyboard and braille display users never touch the screen between such restores.
      sameItemRestores = 0;
    }
    if (isAccessibilityFocused(node)) {
      // The isAccessibilityFocused() method for a node inside a WebView sometimes does not work
      // correctly, so always clearing a11y focus then performing an a11y focused action on the node
      // to ensure the Talkback focus indicator can move to the expected node.
      if (forceRefocusIfAlreadyFocused || WebInterfaceUtils.isWebContainer(node)) {
        pipeline.returnFeedback(
            eventId,
            Feedback.nodeAction(
                node, AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS));
      } else {
        return true;
      }
    }

    // Setting input focus first to decrease lag between a11y focus moving and input focus moving.
    boolean shouldSyncInputFocus =
        FocusActorHelper.shouldSyncInputFocusToAccessibilityFocus(service, node, focusActionInfo);
    if (shouldSyncInputFocus) {
      setFocus(eventId, node);
    }

    boolean isAccessibilityFocusActionPerformedSuccessfully =
        performAccessibilityFocusActionInternal(
            node, focusActionInfo, eventId, /* maySpeakFirst= */ !shouldSyncInputFocus);
    if (isAccessibilityFocusActionPerformedSuccessfully
        && !shouldSyncInputFocus
        && FocusActorHelper.shouldClearFocusAfterSetAccessibilityFocus(service, focusActionInfo)) {
      clearFocus(eventId, /* includeEditable= */ false);
    }
    return isAccessibilityFocusActionPerformedSuccessfully;
  }

  void completePendingWebFocus(@NonNull AccessibilityNodeInfoCompat pivot) {
    if (pivot.isAccessibilityFocused() && WebInterfaceUtils.supportsWebActions(pivot)) {
      history.completePendingWebFocusFromPivot(pivot);
    }
  }

  void updateFocusHistory(
      @NonNull AccessibilityNodeInfoCompat pivot, @NonNull FocusActionInfo focusActionInfo,
      long actionTime) {
    if (WebInterfaceUtils.supportsWebActions(pivot)
        && focusActionInfo.navigationAction != null
        && focusActionInfo.navigationAction.originalNavigationGranularity == CursorGranularity.DEFAULT) {
      // The browser sends the actual target in its focus event. Looking it up immediately
      // reads the old cached focus, refreshes it, and builds a path before that event clears
      // the cache again. Use the existing event-driven history path for default navigation.
      history.onPendingWebNavigationAction(
          updateFocusActionInfoIfNecessary(focusActionInfo, null), actionTime,
          screenState.getStableScreenState(), pivot);
      return;
    }
    // Cache the accessibility focus action history.
    long currentTime = SystemClock.uptimeMillis();
    AccessibilityNodeInfoCompat newFocus = focusFinder.findFocusCompat(FOCUS_ACCESSIBILITY);

    LogUtils.d(
        TAG,
        "Navigate in web:result=%s\nNode:%s\nFocusActionInfo:%s",
        newFocus,
        pivot,
        focusActionInfo);

    FocusActionInfo updatedFocusActionInfo =
        updateFocusActionInfoIfNecessary(focusActionInfo, newFocus);

    if (newFocus == null || pivot.equals(newFocus)) {
      // The focus should have been changed, otherwise we have to wait for the next
      // TYPE_VIEW_ACCESSIBILITY_FOCUSED event to get the correct focused node.
      // Usually this logic will not be invoked. A known case for this is navigating in Firefox.
      history.onPendingAccessibilityFocusActionOnWebElement(
          updatedFocusActionInfo, currentTime, screenState.getStableScreenState());
    } else {
      history.onAccessibilityFocusAction(
          newFocus, updatedFocusActionInfo, currentTime, screenState.getStableScreenState());
    }
  }

  /**
   * Checks the accessibility focus on the current screen. If no focused is found, requests the
   * initial focus by the following order:
   *
   * <ol>
   *   <li>Restore focus from the last focus record
   *   <li>Restore focus on the active window
   *   <li>Input focus on editable view (or any view on TV).
   *   <li>The first non-title content on the active window.
   * </ol>
   */
  boolean ensureAccessibilityFocusOnScreen(EventId eventId) {
    // Restoring the last focus and its failover to INITIAL_FOCUS_RESTORE both seek the node of the
    // last focus record. The search is not repeated while the change is handled.
    history.startRefocus();
    try {
      return ensureFocusOnScreen(eventId);
    } finally {
      history.endRefocus();
    }
  }

  private boolean ensureFocusOnScreen(EventId eventId) {
    final String subTag = "EnsureOnScreen";
    if (pipeline == null
        || !screenState.areMainWindowsStable()
        || screenState.getStableScreenState() == null) {
      LogUtils.d(TAG, String.format("%s: Return, windows are not stable yet.", subTag));
      return false;
    }

    // Before the window change settles: placed now, the item is kept by the window change.
    if (restoreActivatedItemAfterBack(eventId)) {
      return true;
    }

    ScreenState state = screenState.getStableScreenState();
    long windowEventInterpreterDelayTimeMs =
        SystemClock.uptimeMillis() - state.getScreenTransitionStartTime();
    if (windowEventInterpreterDelayTimeMs <= WindowEventInterpreter.getWindowChangeDelayMs()) {
      LogUtils.d(TAG, String.format("%s: Return, the initial focus is not stable yet.", subTag));
      return false;
    }

    if (focusFinder.findFocusCompat(FOCUS_ACCESSIBILITY) != null) {
      // Has focus on screen.
      LogUtils.d(TAG, String.format("%s: Return, accessibility focus is already there.", subTag));
      return true;
    }

    if (FocusProcessorForLogicalNavigation.swipeScrollPending()) {
      LogUtils.d(TAG, String.format("%s: Return, a swipe's scroll is still to focus an item.", subTag));
      return false;
    }
    // Scrolling ahead moved the focused item off screen; the next swipe goes on from it.
    AccessibilityWindowInfo activeWindow = state.getActiveWindow();
    if (activeWindow != null
        && TraversalTreeCache.continuesAfterScrolledAway(activeWindow.getId())) {
      LogUtils.d(TAG, String.format("%s: Return, the focused item was scrolled ahead.", subTag));
      return false;
    }

    // An app changing its controls as it starts recording, such as a voice message's record button
    // giving way to the recording's controls, would otherwise get focus placed elsewhere and
    // spoken, into the recording. Only then: a call or a recording in the background goes on for
    // long, while focus lost in another app should come back.
    if (LowLatencyAudio.startedRecordingWithin(RECORDING_START_MS)
        || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && TouchInteractionMonitor.isTouchPassedToApp())) {
      LogUtils.d(TAG, String.format("%s: Return, an app records or has the touch.", subTag));
      return false;
    }

    // Typing can take focus off the text field, such as when a new line makes the field taller.
    if (refocusEditField(eventId)) {
      return true;
    }

    if (keepFocusOnSheetsCell(eventId)) {
      return true;
    }

    if (!FocusProcessorForScreenStateChange.focusesNewWindows()) {
      // Only replace focus that was lost on this screen, such as when the focused item is removed.
      return focusedSinceScreenChange(state) && requestA11yFocusFromLastFocusRecord(eventId);
    }

    // On TV, we only try to follow input focus.
    if (FormFactorUtils.isAndroidTv()) {
      return pipeline.returnFeedback(
          Feedback.create(eventId, toFeedbackPart(INITIAL_FOCUS_FOLLOW_INPUT, state)));
    }

    // TODO: Consider to use the information of subtree change event to find the
    // initial focus.

    // Try to generate the focus on the same window with the last focused node. It avoids the focus
    // jumping to another window after a node tree changed. (Especially on IME windows.)
    if (requestA11yFocusFromLastFocusRecord(eventId)) {
      return true;
    }

    ArrayList<Feedback.Part> feedbackFailovers = new ArrayList<>();
    // If couldn't get the last focus record, find the initial focus in the same way with handling
    // window changes.
    feedbackFailovers.add(
        toFeedbackPart(INITIAL_FOCUS_RESTORE, screenState.getStableScreenState()));
    feedbackFailovers.add(
        toFeedbackPart(INITIAL_FOCUS_FOLLOW_INPUT, screenState.getStableScreenState()));
    feedbackFailovers.add(
        toFeedbackPart(INITIAL_FOCUS_FIRST_CONTENT, screenState.getStableScreenState()));

    return pipeline.returnFeedback(Feedback.create(eventId, feedbackFailovers));
  }

  /** Indexes of the search results of {@link #findActivatedItem}, for problem reports. */
  private static final int SEARCH_STOPPED = 0;

  private static final int SEARCH_SAME_KIND = 1;

  private static final int SEARCH_SAME_LABEL = 2;

  private static final int SEARCH_STATS = 3;

  /**
   * Logs {@code format} with {@code args} at Debug level and notes it for problem reports, which
   * get it whatever the log level is. Notes name no texts of items, only their kinds.
   */
  private static void noteBack(String format, Object... args) {
    String text = String.format(Locale.ROOT, format, args);
    LogUtils.d(TAG, "%s", text);
    ReportNotes.focusAfterBack.add(text);
  }

  private static int lineCount(String text) {
    int lines = 1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '\n') {
        lines++;
      }
    }
    return lines;
  }

  /** Most nodes read to find an activated list item again after Back. */
  private static final int MAX_ACTIVATED_ITEM_NODES = 2000;

  /** Finding an activated list item again after Back stops after this long. */
  private static final long MAX_ACTIVATED_ITEM_SEARCH_MS = 60;

  /** Descendants read for an item label, and how deep. */
  private static final int MAX_LABEL_NODES = 16;

  private static final int MAX_LABEL_DEPTH = 3;

  /**
   * Returns {@code item} described to be found again after Back, or null when it is not in a list
   * or has no text.
   */
  @Nullable ActivatedItem describeActivatedItem(
      @Nullable AccessibilityNodeInfoCompat item) {
    if (item == null
        || item.getClassName() == null
        || !AccessibilityNodeInfoUtils.isInCollection(item)) {
      noteBack(
          "Activated item not kept for Back: not in a list, class %s",
          (item == null) ? null : item.getClassName());
      return null;
    }
    String label = itemLabel(item);
    if (label == null) {
      noteBack("Activated item not kept for Back: no text, class %s", item.getClassName());
      return null;
    }
    Rect bounds = new Rect();
    item.getBoundsInScreen(bounds);
    return new ActivatedItem(
        item.getWindowId(),
        item.getClassName().toString(),
        item.getViewIdResourceName(),
        label,
        bounds.centerY());
  }

  /** Keeps {@code item}, a list item just activated, to focus it again after Back. */
  void onItemActivated(@NonNull ActivatedItem item) {
    noteBack(
        "Activated list item kept for Back: window %d, class %s, view ID %s, text of %d lines",
        item.windowId, item.className, item.viewId, lineCount(item.label));
    history.onItemActivated(item);
  }

  /** Notes a Back, before it runs. */
  void onBack() {
    AccessibilityNodeInfoCompat focus = focusFinder.findFocusCompat(FOCUS_ACCESSIBILITY);
    int windowId = -1;
    if (focus != null) {
      windowId = focus.getWindowId();
    } else {
      ScreenState state = screenState.getStableScreenState();
      AccessibilityWindowInfo active = (state == null) ? null : state.getActiveWindow();
      if (active != null) {
        windowId = active.getId();
      }
    }
    List<Integer> windowIds = new ArrayList<>();
    for (AccessibilityWindowInfo window : AccessibilityServiceCompatUtils.getWindows(service)) {
      windowIds.add(window.getId());
    }
    history.onBack(
        SystemClock.uptimeMillis(),
        windowId,
        focus,
        (focus == null) ? null : itemLabel(focus),
        windowIds.isEmpty() ? null : windowIds);
  }

  /**
   * Notes a Back that an app runs itself, such as SandFly's settings showing the screen before,
   * unless a Back that SandFly ran or saw still waits for its list: that Back is the same one.
   * Backs from the navigation bar or the system Back gesture don't reach SandFly otherwise.
   */
  void onBackInApp() {
    if (history.isBackPending(SystemClock.uptimeMillis())) {
      return;
    }
    noteBack("Back run by the app");
    onBack();
  }

  /** Ends the pending Back, given up before it ran, without dropping an activated item. */
  void onBackCancelled() {
    history.cancelBack();
  }

  /**
   * Within {@link AccessibilityFocusActionHistory#RESTORE_AFTER_BACK_MS} of a Back, focuses the
   * list item activated before in the same window, as when an app shows a folder's list again in
   * place of the subfolder's. Focus is left alone when it was moved by the user after the Back,
   * when it is in another window, or when it is still on a list item that shows what it showed at
   * the Back, as the list has not changed yet.
   *
   * @return true if focus was placed on the item
   */
  boolean restoreActivatedItemAfterBack(EventId eventId) {
    long now = SystemClock.uptimeMillis();
    if (pipeline == null
        || !history.isBackPending(now)
        || !FocusProcessorForScreenStateChange.focusesNewWindows()) {
      return false;
    }
    @Nullable FocusActionRecord last = history.getLastFocusActionRecord();
    boolean placedSinceBack = last != null && last.getActionTime() >= history.getBackTime();
    if (placedSinceBack && !isPlacedAutomatically(last.getExtraInfo().sourceAction)) {
      noteBack(
          "After Back: focus moved by the user (source %d), activated item not focused",
          last.getExtraInfo().sourceAction);
      history.endBack();
      return false;
    }
    int windowId = history.getBackWindowId();
    AccessibilityNodeInfoCompat focus = focusFinder.findFocusCompat(FOCUS_ACCESSIBILITY);
    if (focus != null) {
      if (focus.getWindowId() != windowId) {
        noteBack(
            "After Back: focus in window %d, Back in window %d", focus.getWindowId(), windowId);
        return false;
      }
      if (!placedSinceBack
          && focus.equals(history.getFocusAtBack())
          && AccessibilityNodeInfoUtils.isInCollection(focus)
          && TextUtils.equals(itemLabel(focus), history.getFocusLabelAtBack())) {
        noteBack("After Back: list not changed yet");
        return false;
      }
    }
    List<ActivatedItem> items = history.getActivatedItemsAfterBack(now);
    if (items.isEmpty()) {
      noteBack("After Back: no activated items in window %d", windowId);
      return false;
    }
    AccessibilityNodeInfoCompat root = windowRoot(windowId);
    if (root == null) {
      noteBack("After Back: window %d not on screen", windowId);
      return false;
    }
    ActivatedItem[] found = new ActivatedItem[1];
    int[] search = new int[SEARCH_STATS];
    AccessibilityNodeInfoCompat node = findActivatedItem(root, items, found, search);
    if (node == null) {
      noteBack(
          "After Back: none of %d activated items found in window %d; newest item: %d of its class"
              + " and view ID, %d with its text%s",
          items.size(),
          windowId,
          search[SEARCH_SAME_KIND],
          search[SEARCH_SAME_LABEL],
          (search[SEARCH_STOPPED] != 0) ? ", search stopped at its limit" : "");
      return false;
    }
    history.onActivatedItemRestored(found[0]);
    if (node.isAccessibilityFocused()) {
      noteBack("After Back: activated item already focused");
      return true;
    }
    noteBack("Focus returns after Back to the list item activated before");
    FocusActionInfo focusActionInfo =
        FocusActionInfo.builder()
            .setSourceAction(FocusActionInfo.SCREEN_STATE_CHANGE)
            .setInitialFocusType(FocusActionInfo.RESTORED_LAST_FOCUS)
            .build();
    return pipeline.returnFeedback(
        eventId,
        Focus.builder()
            .setAction(Focus.Action.FOCUS)
            .setFocusActionInfo(focusActionInfo)
            .setTarget(node));
  }

  /** Whether focus from {@code sourceAction} was placed by SandFly, not moved by the user. */
  private static boolean isPlacedAutomatically(int sourceAction) {
    return sourceAction == FocusActionInfo.SCREEN_STATE_CHANGE
        || sourceAction == FocusActionInfo.ENSURE_ON_SCREEN
        || sourceAction == FocusActionInfo.FOCUS_SYNCHRONIZATION;
  }

  private @Nullable AccessibilityNodeInfoCompat windowRoot(int windowId) {
    for (AccessibilityWindowInfo window : AccessibilityServiceCompatUtils.getWindows(service)) {
      if (window.getId() == windowId) {
        return AccessibilityWindowInfoUtils.getRootCompat(window);
      }
    }
    return null;
  }

  /**
   * Returns the text of {@code node}, or else of its descendants, one per line, or null if there is
   * none.
   */
  @VisibleForTesting
  static @Nullable String itemLabel(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return null;
    }
    CharSequence own = AccessibilityNodeInfoUtils.getNodeText(node);
    if (own != null) {
      return own.toString();
    }
    StringBuilder label = new StringBuilder();
    int[] read = {0};
    appendDescendantText(node, 1, label, read);
    return (label.length() == 0) ? null : label.toString();
  }

  private static void appendDescendantText(
      AccessibilityNodeInfoCompat node, int depth, StringBuilder label, int[] read) {
    int count = node.getChildCount();
    for (int i = 0; i < count && read[0] < MAX_LABEL_NODES; i++) {
      AccessibilityNodeInfoCompat child = node.getChild(i);
      read[0]++;
      if (child == null || !child.isVisibleToUser()) {
        continue;
      }
      CharSequence text = AccessibilityNodeInfoUtils.getNodeText(child);
      if (text != null) {
        if (label.length() > 0) {
          label.append('\n');
        }
        label.append(text);
      }
      if (depth < MAX_LABEL_DEPTH) {
        appendDescendantText(child, depth + 1, label, read);
      }
    }
  }

  /**
   * Returns the node under {@code root} that is the newest of {@code items}, newest first, found
   * with its class, view ID and label, the one nearest where it was when several are, and sets
   * {@code found[0]} to that item. When the search stops at its limits, only the newest item counts.
   * {@code search} gets, for problem reports, whether the search stopped at its limits and how many
   * nodes had the class and view ID of the newest item, and also its label.
   */
  private static @Nullable AccessibilityNodeInfoCompat findActivatedItem(
      AccessibilityNodeInfoCompat root,
      List<ActivatedItem> items,
      ActivatedItem[] found,
      int[] search) {
    AccessibilityNodeInfoCompat[] best = new AccessibilityNodeInfoCompat[items.size()];
    int[] bestDistance = new int[items.size()];
    Rect bounds = new Rect();
    ArrayDeque<AccessibilityNodeInfoCompat> queue = new ArrayDeque<>();
    queue.add(root);
    int read = 0;
    long deadline = SystemClock.uptimeMillis() + MAX_ACTIVATED_ITEM_SEARCH_MS;
    boolean stopped = false;
    while (!queue.isEmpty()) {
      if (read >= MAX_ACTIVATED_ITEM_NODES || SystemClock.uptimeMillis() > deadline) {
        stopped = true;
        search[SEARCH_STOPPED] = 1;
        break;
      }
      AccessibilityNodeInfoCompat node = queue.poll();
      int count = node.getChildCount();
      for (int i = 0; i < count; i++) {
        AccessibilityNodeInfoCompat child = node.getChild(i);
        read++;
        if (child == null || !child.isVisibleToUser()) {
          continue;
        }
        queue.add(child);
        CharSequence className = child.getClassName();
        if (className == null) {
          continue;
        }
        String label = null;
        for (int k = 0; k < items.size(); k++) {
          ActivatedItem item = items.get(k);
          if (!item.className.contentEquals(className)
              || (item.viewId != null && !item.viewId.equals(child.getViewIdResourceName()))) {
            continue;
          }
          if (k == 0) {
            search[SEARCH_SAME_KIND]++;
          }
          if (label == null) {
            label = itemLabel(child);
            if (label == null) {
              break;
            }
          }
          if (!item.label.equals(label)) {
            continue;
          }
          if (k == 0) {
            search[SEARCH_SAME_LABEL]++;
          }
          if (!AccessibilityNodeInfoUtils.shouldFocusNode(child)) {
            continue;
          }
          child.getBoundsInScreen(bounds);
          int distance = Math.abs(bounds.centerY() - item.centerY);
          if (best[k] == null || distance < bestDistance[k]) {
            best[k] = child;
            bestDistance[k] = distance;
          }
        }
      }
    }
    for (int k = 0; k < items.size(); k++) {
      if (stopped && k > 0) {
        break;
      }
      if (best[k] != null) {
        found[0] = items.get(k);
        return best[k];
      }
    }
    return null;
  }

  /**
   * Keeps focus on a Google Sheets cell Sheets stopped listing for a moment, instead of letting
   * focus fall back to the grid's range label. While a Sheets move is in progress, focus is left to
   * the move.
   *
   * @return true if focus was left alone or placed on the cell, without speaking
   */
  private boolean keepFocusOnSheetsCell(EventId eventId) {
    @Nullable FocusActionRecord record = history.getLastFocusActionRecord();
    if (record == null) {
      return false;
    }
    AccessibilityNodeInfoCompat lastFocus = record.getFocusedNode();
    if (!SheetsGrid.isCell(lastFocus)) {
      return false;
    }
    if (SheetsGrid.isMoving()) {
      LogUtils.d(TAG, "EnsureOnScreen: Return, a Sheets move is in progress.");
      return true;
    }
    AccessibilityNodeInfoCompat cell = SheetsGrid.sameCellListedAgain(lastFocus);
    if (cell == null) {
      return false;
    }
    LogUtils.d(TAG, "EnsureOnScreen: Focus returns to the Sheets cell.");
    FocusActionInfo focusActionInfo =
        FocusActionInfo.builder()
            .setForceMuteFeedback(true)
            .setSourceAction(FocusActionInfo.ENSURE_ON_SCREEN)
            .build();
    return pipeline.returnFeedback(
        eventId,
        Focus.builder()
            .setAction(Focus.Action.FOCUS)
            .setFocusActionInfo(focusActionInfo)
            .setTarget(cell));
  }

  /**
   * Puts focus back on the text field with input focus when focus was last on that text field,
   * without speaking it again.
   *
   * @return true if accessibility focus was placed on the text field
   */
  private boolean refocusEditField(EventId eventId) {
    @Nullable FocusActionRecord record = history.getLastFocusActionRecord();
    if (record == null) {
      return false;
    }
    AccessibilityNodeInfoCompat lastFocus = record.getFocusedNode();
    if (lastFocus == null) {
      return false;
    }
    AccessibilityNodeInfoCompat editField = focusFinder.findFocusCompat(FOCUS_INPUT);
    if (editField == null || !editField.isEditable() || !editField.isVisibleToUser()) {
      return false;
    }
    // The same field can be a new node after a layout change, and then has the same view ID.
    boolean wasOnEditField =
        lastFocus.isEditable()
            && editField.getWindowId() == lastFocus.getWindowId()
            && (editField.equals(lastFocus)
                || (editField.getViewIdResourceName() != null
                    && editField.getViewIdResourceName().equals(lastFocus.getViewIdResourceName())));
    // Gboard moves focus to its suggestions during physical typing, and Enter removes them.
    boolean wasOnKeyboardWhileTyping =
        isKeyboardInput()
            && AccessibilityWindowInfoUtils.isImeWindow(
                AccessibilityNodeInfoUtils.getWindow(lastFocus));
    if (!wasOnEditField && !wasOnKeyboardWhileTyping) {
      return false;
    }
    LogUtils.d(TAG, "EnsureOnScreen: Focus returns to the text field.");
    FocusActionInfo focusActionInfo =
        FocusActionInfo.builder()
            .setForceMuteFeedback(true)
            .setSourceAction(FocusActionInfo.ENSURE_ON_SCREEN)
            .build();
    return pipeline.returnFeedback(
        eventId,
        Focus.builder()
            .setAction(Focus.Action.FOCUS)
            .setFocusActionInfo(focusActionInfo)
            .setTarget(editField));
  }

  /** Returns whether the last input was from a physical keyboard. */
  private boolean isKeyboardInput() {
    return service instanceof TalkBackService talkBack
        && talkBack.getInputModeTracker().getInputMode() == InputModeTracker.INPUT_MODE_KEYBOARD;
  }

  /** Returns whether focus was last placed after the windows changed. */
  private boolean focusedSinceScreenChange(ScreenState state) {
    @Nullable FocusActionRecord record = history.getLastFocusActionRecord();
    return record != null && record.getActionTime() > state.getScreenTransitionStartTime();
  }

  /**
   * Finds a focusable node from the last {@link FocusActionRecord} for accessibility focus. At
   * first it will try to restore the accessibility focus from the record. If restoring fail, then
   * try to report the input focus node. Finally, reports the node with {@link
   * AccessibilityNodeInfoCompat#hasRequestInitialAccessibilityFocus()} or the first non-title node
   * under the same root.
   *
   * @return true if accessibility focus is successfully found and assigned
   */
  private boolean requestA11yFocusFromLastFocusRecord(EventId eventId) {
    @Nullable FocusActionRecord record = history.getLastFocusActionRecord();
    if (record == null) {
      return false;
    }

    AccessibilityNodeInfoCompat lastFocus = record.getFocusedNode();
    if (lastFocus == null) {
      return false;
    }

    final String subTag = "EnsureOnScreen";
    if (skipRefocus
        && record.getExtraInfo().isSourceEnsureOnScreen()
        && sameItemRestores < MAX_SAME_ITEM_RESTORES) {
      AccessibilityNodeInfoCompat sameRoot = AccessibilityNodeInfoUtils.getRoot(lastFocus);
      AccessibilityNodeInfoCompat sameItem =
          (sameRoot != null && sameRoot.refresh()) ? findSameItemNearby(sameRoot, lastFocus) : null;
      if (sameItem != null) {
        sameItemRestores++;
        LogUtils.d(TAG, "%s: Focus returns again to the item drawn again as a new node.", subTag);
        return focusSameItem(sameItem, eventId);
      }
    }
    if (skipRefocus && record.getExtraInfo().isSourceEnsureOnScreen()) {
      // If the last focus record is also from the source ENSURE_ON_SCREEN, doesn't request to
      // ensure the focus repeatedly.
      LogUtils.w(
          TAG,
          String.format(
              "%s: App UI (%s) is not stable so stop searching focus on it.",
              subTag, lastFocus.getPackageName()));
      return false;
    }
    skipRefocus = true;

    AccessibilityNodeInfoCompat root = AccessibilityNodeInfoUtils.getRoot(lastFocus);
    if (root == null || !root.refresh()) {
      return false;
    }

    // Try to restore focus by the focus record.
    ScreenState stableScreenState = screenState.getStableScreenState();
    AtomicInteger type = new AtomicInteger(FocusActionInfo.UNDEFINED);
    AccessibilityNodeInfoCompat nodeToFocus =
        findInitialFocusFromTargetWindowRoot(
            service, root, stableScreenState, actorState.focusHistory, focusFinder, type);
    if (type.get() != FocusActionInfo.RESTORED_LAST_FOCUS) {
      // An app that draws its views again, as Facebook does while its lists scroll, takes focus off
      // a node and shows the same item as a new node, which the record's path doesn't find. The
      // window's first item would send the user back to the top of the screen.
      AccessibilityNodeInfoCompat sameItem = findSameItemNearby(root, lastFocus);
      if (sameItem != null) {
        LogUtils.d(TAG, "%s: Focus returns to the item drawn again as a new node.", subTag);
        sameItemRestores++;
        return focusSameItem(sameItem, eventId);
      }
      if (sameItemUnclear && wasNavigatedTo(record)) {
        // Several items like it are near, as the Add Friend buttons of a list of people. The next
        // swipe goes on from its place; the window's first item would lose the place.
        LogUtils.d(TAG, "%s: Focus left off for the next swipe to go on from its place.", subTag);
        return true;
      }
    }
    if (nodeToFocus == null) {
      return false;
    }

    // The remembered item is gone. Prefer the list that moved it to the first window control.
    if (type.get() == FocusActionInfo.FIRST_FOCUSABLE_NODE
        && service instanceof TalkBackService talkBack) {
      AccessibilityNodeInfoCompat item =
          talkBack.itemInRecentlyScrolledList(TraversalStrategy.SEARCH_FOCUS_UNKNOWN);
      if (item != null
          && setAccessibilityFocus(
              item,
              /* forceRefocusIfAlreadyFocused= */ false,
              FocusActionInfo.builder().setSourceAction(FocusActionInfo.MANUAL_SCROLL).build(),
              eventId)) {
        return true;
      }
    }

    boolean firstTime =
        stableScreenState != null && stableScreenState.isInterpretFirstTimeWhenWakeUp();
    // Cell broadcast is an emergent announcement, so we don't mute it.
    boolean forceMuteFeedback =
        FocusActorHelper.shouldMuteFeedbackForFocusedNode(nodeToFocus, stableScreenState);
    // A key that leaves the keyboard, such as the alternative character just typed from Gboard's
    // popup, would otherwise have the keyboard's first item focused and spoken after the typing.
    if (AccessibilityWindowInfoUtils.isImeWindow(AccessibilityNodeInfoUtils.getWindow(root))) {
      forceMuteFeedback = true;
    }

    FocusActionInfo focusActionInfo =
        FocusActionInfo.builder()
            .setForceMuteFeedback(forceMuteFeedback)
            .setSourceAction(FocusActionInfo.ENSURE_ON_SCREEN)
            .setInitialFocusType(type.get())
            .build();

    boolean success =
        pipeline.returnFeedback(
            eventId,
            Focus.builder()
                .setAction(Focus.Action.FOCUS)
                .setFocusActionInfo(focusActionInfo)
                .setTarget(nodeToFocus));

    if (firstTime && success) {
      stableScreenState.consumeInterpretFirstTimeWhenWakeUp();
    }
    return success;
  }

  /** Focuses {@code item}, the focused item drawn again, without speaking it again. */
  private boolean focusSameItem(AccessibilityNodeInfoCompat item, EventId eventId) {
    FocusActionInfo focusActionInfo =
        FocusActionInfo.builder()
            .setForceMuteFeedback(true)
            .setSourceAction(FocusActionInfo.ENSURE_ON_SCREEN)
            .setInitialFocusType(FocusActionInfo.RESTORED_LAST_FOCUS)
            .build();
    return pipeline.returnFeedback(
        eventId,
        Focus.builder()
            .setAction(Focus.Action.FOCUS)
            .setFocusActionInfo(focusActionInfo)
            .setTarget(item));
  }

  /** Whether the last {@link #findSameItemNearby} found several matches or ran out of time. */
  private static boolean sameItemUnclear;

  /** How recent a navigation must be for its place to stand in for focus that was lost. */
  private static final long NAVIGATED_RECENTLY_MS = 10_000;

  /** Whether {@code record} is of a move to the next or previous item made shortly before. */
  private static boolean wasNavigatedTo(FocusActionRecord record) {
    return record.getExtraInfo().sourceAction == FocusActionInfo.LOGICAL_NAVIGATION
        && SystemClock.uptimeMillis() - record.getActionTime() < NAVIGATED_RECENTLY_MS;
  }

  /** Most nodes read to find the item that lost focus drawn again. */
  private static final int MAX_SAME_ITEM_NODES = 1500;

  /** Finding the item that lost focus drawn again stops after this long. */
  private static final long MAX_SAME_ITEM_SEARCH_MS = 40;

  /**
   * Returns the only node under {@code root} other than {@code lastFocus} with its class and its
   * content description, or text, on screen within a quarter of the window's height of where {@code
   * lastFocus} was when it was focused, or null if there is none or more than one.
   */
  private static @Nullable AccessibilityNodeInfoCompat findSameItemNearby(
      AccessibilityNodeInfoCompat root, AccessibilityNodeInfoCompat lastFocus) {
    CharSequence label = lastFocus.getContentDescription();
    if (TextUtils.isEmpty(label)) {
      label = lastFocus.getText();
    }
    CharSequence className = lastFocus.getClassName();
    if (TextUtils.isEmpty(label) || className == null) {
      return null;
    }
    Rect last = new Rect();
    lastFocus.getBoundsInScreen(last);
    Rect window = new Rect();
    root.getBoundsInScreen(window);
    if (last.isEmpty() || window.isEmpty()) {
      return null;
    }
    int maxDistance = window.height() / 4;
    String wanted = label.toString();
    AccessibilityNodeInfoCompat best = null;
    int bestDistance = Integer.MAX_VALUE;
    int matches = 0;
    Rect bounds = new Rect();
    ArrayDeque<AccessibilityNodeInfoCompat> queue = new ArrayDeque<>();
    queue.add(root);
    int read = 0;
    long deadline = SystemClock.uptimeMillis() + MAX_SAME_ITEM_SEARCH_MS;
    sameItemUnclear = false;
    while (!queue.isEmpty() && read < MAX_SAME_ITEM_NODES) {
      if (SystemClock.uptimeMillis() > deadline) {
        sameItemUnclear = true;
        return null;
      }
      AccessibilityNodeInfoCompat node = queue.poll();
      int count = node.getChildCount();
      for (int i = 0; i < count && read < MAX_SAME_ITEM_NODES; i++) {
        AccessibilityNodeInfoCompat child = node.getChild(i);
        read++;
        if (child == null || !child.isVisibleToUser()) {
          continue;
        }
        queue.add(child);
        if (child.equals(lastFocus) || !className.equals(child.getClassName())) {
          continue;
        }
        CharSequence childLabel = child.getContentDescription();
        if (TextUtils.isEmpty(childLabel)) {
          childLabel = child.getText();
        }
        if (childLabel == null
            || !wanted.equals(childLabel.toString())
            || !child.isVisibleToUser()
            || !AccessibilityNodeInfoUtils.shouldFocusNode(child)) {
          continue;
        }
        child.getBoundsInScreen(bounds);
        int distance = Math.abs(bounds.centerY() - last.centerY());
        if (distance > maxDistance) {
          continue;
        }
        matches++;
        if (distance < bestDistance) {
          best = child;
          bestDistance = distance;
        }
      }
    }
    // Several items with the label nearby, such as the Add Friend buttons of a list of people:
    // which one is the focused item can't be told.
    sameItemUnclear = matches > 1;
    return matches == 1 ? best : null;
  }

  /**
   * Finds the initial focus from the specified window root for accessibility focus. At first it
   * will try to restore the accessibility focus from the record. If restoring fail, then try to
   * report the input focus node. Finally, reports the node with {@link
   * AccessibilityNodeInfoCompat#hasRequestInitialAccessibilityFocus()} or the first focusable node
   * under the same root. It also updates {@code initialFocusType} with the type.
   *
   * @return the node to focus
   */
  public static @Nullable AccessibilityNodeInfoCompat findInitialFocusFromTargetWindowRoot(
      AccessibilityService service,
      AccessibilityNodeInfoCompat windowRoot,
      ScreenState screenState,
      AccessibilityFocusActionHistory.Reader history,
      FocusFinder focusFinder,
      AtomicInteger initialFocusType) {
    AccessibilityNodeInfoCompat nodeToFocus = null;
    if (service == null || windowRoot == null || focusFinder == null || initialFocusType == null) {
      return null;
    }
    if (history != null) {
      final WindowIdentifier windowIdentifier =
          WindowIdentifier.create(windowRoot.getWindowId(), screenState);
      // The previous focus record may come from a different pane in the same window, it needs to
      // get the right record from the WindowIdentifier to restore.
      FocusActionRecord windowIdentifierRecord =
          history.getLastFocusActionRecordInWindow(windowIdentifier);
      if (windowIdentifierRecord != null) {
        initialFocusType.set(FocusActionInfo.RESTORED_LAST_FOCUS);
        nodeToFocus =
            history.getFocusableNodeFromFocusRecord(
                windowRoot, focusFinder, windowIdentifierRecord);
      }
    }

    // If couldn't restore the focus from the record directly, then try to find focus node
    // on the same root. The priority: input focus > requested initial focus > the first non-title
    // node.
    if (nodeToFocus == null) {
      Filter<AccessibilityNodeInfoCompat> nodeFilter =
          Filter.node((node) -> AccessibilityNodeInfoUtils.shouldFocusNode(node));
      if (FormFactorUtils.isAndroidWear()) {
        nodeFilter =
            AccessibilityNodeInfoUtils.getFilterExcludingSmallTopAndBottomBorderNode(service)
                .and(nodeFilter);
      }

      AccessibilityNodeInfoCompat inputFocusNode =
          focusFinder.findFocusCompat(AccessibilityNodeInfo.FOCUS_INPUT);
      if (AccessibilityNodeInfoUtils.hasAncestor(inputFocusNode, windowRoot)
          && nodeFilter.accept(inputFocusNode)) {
        nodeToFocus = inputFocusNode;
        initialFocusType.set(FocusActionInfo.SYNCED_INPUT_FOCUS);
      } else {
        OrderedTraversalStrategy strategy = new OrderedTraversalStrategy(windowRoot);
        nodeToFocus = strategy.focusInitial(windowRoot);
        initialFocusType.set(FocusActionInfo.REQUESTED_INITIAL_NODE);

        if (nodeToFocus == null || !nodeFilter.accept(nodeToFocus)) {
          nodeToFocus =
              TraversalStrategyUtils.findFirstFocusInNodeTree(
                  strategy, windowRoot, TraversalStrategy.SEARCH_FOCUS_FORWARD, nodeFilter);
          initialFocusType.set(FocusActionInfo.FIRST_FOCUSABLE_NODE);
        }
      }
    }
    return nodeToFocus;
  }

  /**
   * Creates {@link Feedback.Part} for focus actions.
   *
   * @param action The focus action
   * @param state Current screen state, it can't be null
   * @return Feedback.Part for the pipeline
   */
  private Feedback.Part toFeedbackPart(Focus.Action action, ScreenState state) {
    return Feedback.part().setFocus(Feedback.focus(action).setScreenState(state).build()).build();
  }

  /**
   * Returns whether the node is accessibility focused.
   *
   * <p><strong>Note:</strong> {@link #setAccessibilityFocus(AccessibilityNodeInfoCompat, boolean,
   * FocusActionInfo, EventId)} should use this method instead of directly invoking {@link
   * AccessibilityNodeInfoCompat#isAccessibilityFocused()}. This is in case that if we want to
   * bypass framework's touch exploration and maintain our own accessibility focus, we can easily
   * override this method.
   */
  private boolean isAccessibilityFocused(@Nullable AccessibilityNodeInfoCompat node) {
    return node != null && node.isAccessibilityFocused();
  }

  /**
   * Performs {@link AccessibilityNodeInfoCompat#ACTION_ACCESSIBILITY_FOCUS} on the node. Saves the
   * record if the action is successfully performed.
   *
   * <p>The focus can be spoken before the action, which waits for the app's main thread, when
   * {@code maySpeakFirst}: the item is spoken while the app sets the focus. If the action then
   * fails, the speech is stopped.
   *
   * @return {@code true} if the action is successfully performed.
   */
  @VisibleForTesting
  protected boolean performAccessibilityFocusActionInternal(
      @NonNull AccessibilityNodeInfoCompat node,
      @NonNull FocusActionInfo focusActionInfo,
      @Nullable EventId eventId,
      boolean maySpeakFirst) {
    long currentTime = SystemClock.uptimeMillis();
    // Focus that the action is about to mute is never spoken first.
    boolean spokenFirst =
        maySpeakFirst
            && focusSetListener != null
            && !muteNextFocus
            && !muteFocus
            && !shouldMuteFeedbackForMicroGranularityNavigation(focusActionInfo, node)
            && focusSetListener.beforeAccessibilityFocusSet(
                node, focusActionInfo, eventId, currentTime);
    boolean result =
        pipeline.returnFeedback(
            eventId,
            Feedback.nodeAction(node, AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS));
    if (!result && spokenFirst) {
      pipeline.returnFeedback(eventId, Feedback.part().setInterruptAllFeedback(true));
      focusSetListener.onAccessibilityFocusNotSet(node);
    }
    if (result) {
      focusActionInfo = updateFocusActionInfoIfNecessary(focusActionInfo, node);
      // Told first, so that speech can start before the history walks the node's ancestors. The
      // history only has to be ready by the time the app's focus event arrives.
      if (focusSetListener != null) {
        focusSetListener.onAccessibilityFocusSet(node, focusActionInfo, eventId, currentTime);
      }
      // AccessibilityFocusActionHistory makes copy of the node, no need to obtain() here.
      history.onAccessibilityFocusAction(
          node, focusActionInfo, currentTime, screenState.getStableScreenState());
    }
    LogUtils.d(
        TAG,
        "Set accessibility focus:result=%s\nNode:%s\nFocusActionInfo:%s",
        result,
        node,
        focusActionInfo);
    return result;
  }

  private FocusActionInfo updateFocusActionInfoIfNecessary(
      @NonNull FocusActionInfo focusActionInfo, @Nullable AccessibilityNodeInfoCompat node) {
    if (shouldMuteFeedbackForMicroGranularityNavigation(focusActionInfo, node)) {
      LogUtils.d(TAG, "Mute node feedback for micro granularity navigation.");
      focusActionInfo =
          new FocusActionInfo.Builder(focusActionInfo).setForceMuteFeedback(true).build();
    }
    if (muteNextFocus) {
      // Reset mute option regardless of sourceAction. Sometimes it doesn't mute because the
      // source action does not come from SCREEN_STATE_CHANGE, so mute focus from next screen
      // state change.
      muteNextFocus = false;

      LogUtils.d(TAG, "FocusActionInfo modified.");
      focusActionInfo =
          new FocusActionInfo.Builder(focusActionInfo).setForceMuteFeedback(true).build();
    } else if (muteFocus) {
      focusActionInfo =
          new FocusActionInfo.Builder(focusActionInfo).setForceMuteFeedback(true).build();
    }

    return focusActionInfo;
  }

  /**
   * Checks whether we should mute node feedback for micro granularity navigation.
   *
   * <p>When navigating with micro granularity(character, word, line, etc) across nodes, we don't
   * announce the entire node description from accessibility focus event. There is an exception: If
   * the next node doesn't support target granularity.
   */
  private boolean shouldMuteFeedbackForMicroGranularityNavigation(
      @NonNull FocusActionInfo info, @Nullable AccessibilityNodeInfoCompat node) {
    if (info.navigationAction == null) {
      return false;
    }
    CursorGranularity originalNavigationGranularity =
        info.navigationAction.originalNavigationGranularity;

    if ((originalNavigationGranularity == null)
        || !originalNavigationGranularity.isMicroGranularity()) {
      return false;
    }

    return CursorGranularityManager.getSupportedGranularities(node)
        .contains(originalNavigationGranularity);
  }

  /**
   * Clears accessibility focus on the given node.
   *
   * @return {@code true} if successfully perform {@link
   *     AccessibilityNodeInfo#ACTION_CLEAR_ACCESSIBILITY_FOCUS} on the given node.
   */
  boolean clearAccessibilityFocus(AccessibilityNodeInfoCompat currentNode, EventId eventId) {
    return pipeline.returnFeedback(
        eventId,
        Feedback.nodeAction(currentNode, AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS));
  }

  void clearAccessibilityFocus(EventId eventId) {
    AccessibilityNodeInfoCompat currentFocus =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (currentFocus != null) {
      clearAccessibilityFocus(currentFocus, eventId);
    }
  }

  void clearFocus(EventId eventId, boolean includeEditable) {
    AccessibilityNodeInfoCompat currentFocus = accessibilityFocusMonitor.getInputFocus();
    if (currentFocus != null && (includeEditable || !currentFocus.isEditable())) {
      pipeline.returnFeedback(
          eventId, Feedback.nodeAction(currentFocus, AccessibilityNodeInfo.ACTION_CLEAR_FOCUS));
    }
  }

  void setFocus(@NonNull EventId eventId, @NonNull AccessibilityNodeInfoCompat node) {
    long currentTime = SystemClock.uptimeMillis();
    boolean result =
        pipeline.returnFeedback(
            eventId, Feedback.nodeAction(node, AccessibilityNodeInfo.ACTION_FOCUS));
    if (result) {
      actorState.setInputFocus(node, currentTime);
    }
    LogUtils.d(
        TAG,
        "Perform input focus action:result=%s\n" + " eventId=%s," + " Node=%s",
        result,
        eventId,
        node);
  }
}
