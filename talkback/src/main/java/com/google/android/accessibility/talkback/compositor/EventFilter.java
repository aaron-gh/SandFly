/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.android.accessibility.talkback.compositor;

import static com.google.android.accessibility.talkback.compositor.CompositorConfigs.shouldEarlyAnnounceForLiftToType;

import android.app.Notification;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.compositor.rule.EventTypeViewAccessibilityFocusedFeedbackRule;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.focusmanagement.ColumnWrap;
import com.google.android.accessibility.talkback.focusmanagement.TraversalTreeCache;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo;
import com.google.android.accessibility.talkback.notifications.NotificationReader;
import com.google.android.accessibility.talkback.notifications.Outcome;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.TelegramChatCell;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.accessibility.utils.input.TextEventInterpreter;
import com.google.android.accessibility.utils.monitor.TouchMonitor;
import com.google.android.accessibility.utils.monitor.VoiceActionDelegate;
import com.google.android.accessibility.utils.output.SpeechHistory;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SearchDirection;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Determines whether events should be passed on to the compositor. Also interprets events into more
 * specific event types, and extracts data from events.
 */
public class EventFilter {

  private static final String TAG = "EventFilter";

  ///////////////////////////////////////////////////////////////////////////////////
  // Member variables
  private final Context context;
  private final Compositor compositor;
  private VoiceActionDelegate voiceActionDelegate;

  private AccessibilityFocusEventInterpreter accessibilityFocusEventInterpreter;
  private final GlobalVariables globalVariables;

  private final @NonNull TouchMonitor touchMonitor;
  private AccessibilityNodeInfoCompat lastHoverEnteredNode = null;
  private final EarlyFocusSpeech earlyFocusSpeech;

  /** Works out which node a swipe from the focused node would most likely reach. */
  public interface TargetPredictor {
    @Nullable AccessibilityNodeInfoCompat predictTarget(
        AccessibilityNodeInfoCompat pivot, @SearchDirection int searchDirection);
  }

  private @Nullable TargetPredictor targetPredictor;
  // The direction of the last move to the next or previous item, which the next swipe most likely
  // takes too.
  private IntSupplier lastDirection = () -> TraversalStrategy.SEARCH_FOCUS_FORWARD;
  // Whether a finger is on the screen now.
  private BooleanSupplier fingerDown = () -> false;
  // The node that accessibility focus last moved to, as far as this filter knows.
  private @Nullable AccessibilityNodeInfoCompat lastFocusedNode;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable prepareSwipeTargets = this::prepareSwipeTargets;

  /**
   * How long after focus moves the announcements of the next and previous nodes are prepared: after
   * the app's focus event has been handled, and usually before the next swipe starts.
   */
  private static final long PREPARE_DELAY_MS = 30;

  /**
   * Whether a swipe's target that is only partly on screen is spoken before the swipe has scrolled
   * it into view, rather than from the app's focus event after it has, as TalkBack does.
   */
  private static volatile boolean speakItemsBeforeScroll = false;

  /** Focus spoken before its focus action, while the action runs. */
  private static final class SpokenFirst {
    /** The node that the action focuses. */
    final AccessibilityNodeInfoCompat focusedNode;

    /** The node that was described, read again from the app. */
    final AccessibilityNodeInfoCompat node;

    /** The state that follows focus as it was before, to put back if the action fails. */
    final GlobalVariables.SavedFocusState saved;

    /** Whether it was spoken, or only the state that follows focus updated. */
    final boolean spoken;

    SpokenFirst(
        AccessibilityNodeInfoCompat focusedNode,
        AccessibilityNodeInfoCompat node,
        GlobalVariables.SavedFocusState saved,
        boolean spoken) {
      this.focusedNode = focusedNode;
      this.node = node;
      this.saved = saved;
      this.spoken = spoken;
    }
  }

  private @Nullable SpokenFirst spokenFirst;

  /**
   * How long a node whose focus action failed after it was spoken is spoken only after its action.
   * A failed focus is tried again at once, and would be cut off a second time.
   */
  private static final long FAILED_FOCUS_MS = 2_000;

  // The last node whose focus action failed after it was spoken, and when.
  private @Nullable AccessibilityNodeInfoCompat failedFocusNode;
  private long failedFocusTime;

  // /////////////////////////////////////////////////////////////////////////////////
  // Construction

  public EventFilter(
      Context context,
      Compositor compositor,
      @NonNull TouchMonitor touchMonitor,
      GlobalVariables globalVariables,
      EarlyFocusSpeech earlyFocusSpeech) {
    this.context = context;
    this.compositor = compositor;
    this.touchMonitor = touchMonitor;
    this.globalVariables = globalVariables;
    this.earlyFocusSpeech = earlyFocusSpeech;
  }

  ///////////////////////////////////////////////////////////////////////////////////
  // Methods

  public void setVoiceActionDelegate(VoiceActionDelegate delegate) {
    voiceActionDelegate = delegate;
  }

  /**
   * Works out again what a swipe from the focused node would say, such as after a scroll has
   * brought the next item on screen.
   */
  public void prepareSwipeTargetsNow() {
    handler.removeCallbacks(prepareSwipeTargets);
    if (targetPredictor != null && lastFocusedNode != null) {
      handler.post(prepareSwipeTargets);
    }
  }

  public void setTargetPredictor(@Nullable TargetPredictor predictor) {
    targetPredictor = predictor;
  }

  /** Sets how to get the direction of the last move to the next or previous item. */
  public void setLastDirection(IntSupplier lastDirection) {
    this.lastDirection = lastDirection;
  }

  /** Sets how to tell whether a finger is on the screen, so that a swipe never waits for this. */
  public void setFingerDownSupplier(BooleanSupplier supplier) {
    fingerDown = supplier;
  }

  public void setAccessibilityFocusEventInterpreter(
      AccessibilityFocusEventInterpreter interpreter) {
    accessibilityFocusEventInterpreter = interpreter;
  }

  public void sendEvent(AccessibilityEvent event, @Nullable EventId eventId) {
    EarlyFocusMatch earlyFocus = earlyFocusSpeech.consume(event);
    if (earlyFocus == EarlyFocusMatch.SPOKEN) {
      // Spoken as soon as the focus was set, so this event only goes to the interpreter, which
      // passes it on for other uses, such as image captions.
      lastHoverEnteredNode = null;
      if (accessibilityFocusEventInterpreter != null) {
        accessibilityFocusEventInterpreter.interpret(event);
      }
      return;
    }
    if (earlyFocus == EarlyFocusMatch.STALE) {
      // Focus moved on, and was spoken, after this event was sent. Speaking it now would cut off
      // the newer focus, and its node's captions and state no longer matter.
      LogUtils.d(TAG, "Drop focus event older than the focus spoken early: %s", event);
      return;
    }
    if (earlyFocusSpeech.isDescriptionChangeFromFocus(event)) {
      // The app changed the description of the node because it was focused, such as the One UI
      // launcher adding the column to a folder item. The focus was spoken already.
      LogUtils.d(TAG, "Drop description change made for the focus spoken early: %s", event);
      return;
    }

    if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
      // The focus moved without being spoken early, so the prepared announcements are of no use.
      PreparedFocusSpeech.clear();
      @Nullable AccessibilityNodeInfoCompat focused = AccessibilityEventUtils.sourceCompat(event);
      if (focused != null) {
        onFocusMoved(focused);
      }
    }

    // Update persistent state, unless the focus spoken early already did, but failed to speak.
    if (earlyFocus != EarlyFocusMatch.STATE_UPDATED) {
      globalVariables.updateStateFromEvent(event);
    }

    // Interpret event more specifically, and extract data from event.
    EventInterpretation eventInterpreted =
        new EventInterpretation(Compositor.toCompositorEvent(event));
    AccessibilityFocusEventInterpretation a11yFocusEventInterpreted =
        (accessibilityFocusEventInterpreter == null)
            ? null
            : accessibilityFocusEventInterpreter.interpret(event);
    if (a11yFocusEventInterpreted != null) {
      eventInterpreted.setEvent(a11yFocusEventInterpreted.getEvent());
      a11yFocusEventInterpreted.setIsEqualsToLastHoverEnterKeyboardEventNode(
          isEqualsToLastHoverEnteredKeyboardNode(event));
      eventInterpreted.setAccessibilityFocusInterpretation(a11yFocusEventInterpreted);
    }

    eventInterpreted.setReadOnly();

    int eventType = eventInterpreted.getEvent();
    if (eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
      // Drop accessibility-focus events based on EventState.
      // TODO: Remove this when focus management is done.
      {
        lastHoverEnteredNode = null;
        if (globalVariables.resettingSkipFocusProcessing()) {
          return;
        }
        if ((a11yFocusEventInterpreted != null)
            && a11yFocusEventInterpreted.getShouldMuteFeedback()) {
          return;
        }
        if (a11yFocusEventInterpreted != null) {
          accessibilityFocusEventInterpreter.beforeFocusSpoken(a11yFocusEventInterpreted);
        }
      }
    } else if (eventType == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER) {
      if (globalVariables.isTapToType()
          && AccessibilityEventFeedbackUtils.isEventRelatedToKeyboardKey(event)) {
        // The key is typed without being spoken.
        return;
      }
      AccessibilityNodeInfoCompat node = AccessibilityEventUtils.sourceCompat(event);
      if (shouldEarlyAnnounceForLiftToType(context)
          && AccessibilityEventFeedbackUtils.isEventRelatedToKeyboardKey(event)
          && !Objects.equals(node, lastHoverEnteredNode)) {
        lastHoverEnteredNode = node;
      } else if (node != null) {
        lastHoverEnteredNode = node;
        // For focus fallback events, drop events with a source node.
        return;
      }
    } else if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
      // Event notification
      // REFERTO. If the user is touching on screen, skip event.
      // For toast events, the notification parcel is null. (Use event text instead.)
      Notification notification = AccessibilityEventUtils.extractNotification(event);
      // While the device is locked, a private notification's event has only its public version,
      // or none, and the notification listener reads the whole notification instead.
      if (notification == null
          && Notification.class.getName().contentEquals(String.valueOf(event.getClassName()))
          && NotificationReader.readsFromListener()) {
        LogUtils.d(TAG, "Notification without content while locked: read from the listener");
        return;
      }
      boolean isCall =
          (notification != null) && Notification.CATEGORY_CALL.equals(notification.category);
      // Read now: the state kept in GlobalVariables changes only when a setting does.
      boolean dnd = SettingsUtils.getDoNotDisturbState(context) > 0;
      boolean touching = touchMonitor.isUserTouchingScreen();
      // Do Not Disturb silences calls it doesn't let through. Toasts still answer what the user
      // just did.
      if (isCall && dnd && !NotificationReader.callAllowedDuringDnd(event, notification)) {
        LogUtils.d(TAG, "Do not announce call: DND is enabled");
        return;
      }
      if (isCall) {
        NotificationReader.noteCall(event, notification);
      } else if (notification != null) {
        NotificationReader.playOwnNotificationSound(event, notification, dnd);
      }
      // Incoming calls are still announced, so that the user hears who is calling.
      if ((notification != null) && !isCall && !globalVariables.getSpeakNotifications()) {
        LogUtils.d(TAG, "Do not announce notification: disabled in settings");
        NotificationReader.noteUnread(event, notification, Outcome.SPEECH_OFF);
        return;
      }
      // Notifications are read as the notification settings say, Do Not Disturb included, and
      // those that arrive while the screen is touched are read after the touch.
      if ((notification != null) && NotificationReader.handle(event, notification, touching, dnd)) {
        return;
      }
      if ((notification != null) && (touching || (dnd && !isCall))) {
        LogUtils.d(TAG, "Do not announce notification: DND is enabled or the screen is touched");
        return;
      }
      if ((voiceActionDelegate != null)
          && voiceActionDelegate.isVoiceRecognitionActive()
          && (Role.getSourceRole(event) == Role.ROLE_TOAST)) {
        LogUtils.d(TAG, "Do not announce the toast: Voice recognition is active.");
        return;
      }
    } else if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        || eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        || eventType == AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY) {
      return; // Text-events only travel through TextEventInterpreter.
    } else if (eventType == AccessibilityEvent.TYPE_VIEW_SELECTED) {
      LogUtils.d(
          TAG,
          "Do not handle TYPE_VIEW_SELECTED by ProcessEventQueue. "
              + "Handled in SelectionEventInterpreter.");
      return;
    } else {
      // Let event go to compositor.
    }

    compositor.handleEvent(event, eventId, eventInterpreted);
  }

  /**
   * Told when Backtalk has set accessibility focus on {@code node}, before the app's focus event
   * for it arrives. Speaks the focus at once if it can, see {@link #speakFocusEarly}; otherwise
   * makes sure the app's event for it is spoken.
   *
   * @param actionTime when the focus action started, in {@link SystemClock#uptimeMillis()} time
   * @param continuousReading whether continuous reading moved the focus. Its focus is not spoken
   *     early: continuous reading moves on when the speech for a node ends, and gains little from
   *     speaking sooner, so it keeps waiting for the app's event.
   */
  public void onAccessibilityFocusSet(
      AccessibilityNodeInfoCompat node,
      FocusActionInfo info,
      @Nullable EventId eventId,
      long actionTime,
      boolean continuousReading) {
    // The speech history must know whether the node is a password before its focus is spoken,
    // without waiting for the app's focus event.
    SpeechHistory.onAccessibilityFocusSet(node.isPassword(), actionTime);
    @Nullable SpokenFirst first = spokenFirst;
    spokenFirst = null;
    if (first != null && first.focusedNode.equals(node)) {
      if (first.spoken) {
        earlyFocusSpeech.addSpoken(first.node, actionTime);
      } else {
        earlyFocusSpeech.addStateUpdated(first.node, actionTime);
      }
      onFocusMoved(first.node);
      return;
    }
    if (continuousReading || !speakFocusEarly(node, info, eventId, actionTime)) {
      // A node spoken early before, whose event never came here, must not hide this focus.
      earlyFocusSpeech.forget(node);
    }
  }

  /**
   * Told just before SandFly performs the action that sets accessibility focus on {@code node},
   * which waits for the app's main thread. Speaks the focus then, as {@link #speakFocusEarly} would
   * after the action, when it can work out what to say without the action, so that the speech
   * starts while the app sets the focus. Otherwise the focus is handled as usual after the action.
   *
   * <p>Items of web content are spoken here too, from their nodes, as their focus events carry
   * nothing more than their nodes.
   *
   * @return whether the focus was spoken, or its state updated, in which case {@link
   *     #onAccessibilityFocusSet} or {@link #onAccessibilityFocusNotSet} follows
   */
  public boolean speakBeforeFocusSet(
      AccessibilityNodeInfoCompat focusedNode,
      FocusActionInfo info,
      @Nullable EventId eventId,
      boolean continuousReading) {
    spokenFirst = null;
    if (continuousReading || !shouldSpeakFocusEarly(info)) {
      return false;
    }
    if (focusedNode.equals(failedFocusNode)
        && SystemClock.uptimeMillis() - failedFocusTime < FAILED_FOCUS_MS) {
      return false;
    }
    // Only looked at: if nothing is spoken here, it is used after the action.
    PreparedFocusSpeech.@Nullable Prepared prepared =
        (info.columnWrap == ColumnWrap.NONE) ? PreparedFocusSpeech.peek(focusedNode) : null;
    AccessibilityNodeInfoCompat node;
    if (prepared != null) {
      node = prepared.getNode();
    } else if (TraversalTreeCache.holdsCurrent(focusedNode)) {
      node = focusedNode;
    } else {
      // Read the node again from the app, as after the action. Before the action, the app has no
      // new focus to draw first.
      @SuppressWarnings("deprecation") // obtain(): copy, so that the saved node is left as it was.
      AccessibilityNodeInfoCompat copy = AccessibilityNodeInfoCompat.obtain(focusedNode);
      if (!copy.refresh()) {
        return false;
      }
      node = copy;
    }
    if (!shouldSpeakFocusBeforeAction(node)) {
      return false;
    }
    AccessibilityEvent event = focusEventFor(node);
    EventInterpretation eventInterpreted = focusInterpretation(info);
    GlobalVariables.SavedFocusState saved = globalVariables.saveFocusState();
    EventFeedback feedback;
    try {
      globalVariables.updateStateFromFocusedNode(node);
      globalVariables.updateCollectionStateFromFocusedNode(node, event);
      if (prepared != null) {
        feedback = prepared.getFeedback();
        EventTypeViewAccessibilityFocusedFeedbackRule.currentContainerTitle =
            prepared.getContainerTitle();
      } else {
        feedback = getSwipeTargetFeedback(event, node, eventInterpreted);
      }
    } catch (RuntimeException e) {
      LogUtils.e(TAG, "Cannot speak focus before setting it: %s", e);
      globalVariables.restoreFocusState(saved);
      return false;
    }
    if (!hasSpeech(feedback)) {
      // Some nodes show their content only once focused, so they are left to the action.
      globalVariables.restoreFocusState(saved);
      return false;
    }
    // The focus is moving, so the prepared announcements are used now or not at all.
    PreparedFocusSpeech.clear();
    lastHoverEnteredNode = null;
    // The speech history must know whether the node is a password before the speech starts; the
    // action, and the app's focus event, come after.
    SpeechHistory.onAccessibilityFocusSet(node.isPassword(), SystemClock.uptimeMillis());
    boolean spoken = true;
    try {
      compositor.handleEvent(event, node, eventId, eventInterpreted, feedback);
    } catch (RuntimeException e) {
      // As after the action: the state was updated, so the app's event is spoken without updating
      // it again.
      LogUtils.e(TAG, "Cannot speak focus before setting it: %s", e);
      spoken = false;
    }
    spokenFirst = new SpokenFirst(focusedNode, node, saved, spoken);
    return true;
  }

  /**
   * Told when the focus action that {@link #speakBeforeFocusSet} spoke failed. Puts back the state
   * that follows focus. The caller stops the speech.
   */
  public void onAccessibilityFocusNotSet(AccessibilityNodeInfoCompat focusedNode) {
    @Nullable SpokenFirst first = spokenFirst;
    spokenFirst = null;
    if (first != null && first.focusedNode.equals(focusedNode)) {
      // The focus stays where it was, which may be a password field.
      SpeechHistory.onFocusUnknown();
      globalVariables.restoreFocusState(first.saved);
      failedFocusNode = focusedNode;
      failedFocusTime = SystemClock.uptimeMillis();
    }
  }

  /**
   * Speaks the accessibility focus that a swipe or other user navigation just set on {@code node},
   * without waiting for the app's focus event, which takes a round trip to the app and every event
   * listener before it would be spoken. The focus event, when it arrives, is then not spoken again.
   *
   * <p>Only focus that stays in the same window is spoken early, so that window changes are still
   * announced from the event. Nodes whose descriptions need details only the event has, such as
   * keyboard keys, sliders, pages and web content, wait for their event.
   *
   * @return whether its node is now remembered, so that the app's focus event for it is handled
   *     by what was done here
   */
  private boolean speakFocusEarly(
      AccessibilityNodeInfoCompat focusedNode,
      FocusActionInfo info,
      @Nullable EventId eventId,
      long actionTime) {
    // The focus is moving, so the prepared announcements are used now or not at all.
    PreparedFocusSpeech.@Nullable Prepared prepared = PreparedFocusSpeech.take(focusedNode);
    if (!shouldSpeakFocusEarly(info)) {
      return false;
    }
    AccessibilityNodeInfoCompat node;
    @Nullable EventFeedback preparedFeedback;
    @Nullable CharSequence preparedContainerTitle = null;
    if (prepared != null && info.columnWrap == ColumnWrap.NONE) {
      // Prepared from the node read again from the app, and thrown away had anything in its
      // window changed since, even its text or state. So it needs no second read, which would
      // wait for the app to finish drawing the focus it was just given.
      node = prepared.getNode();
      preparedFeedback = prepared.getFeedback();
      preparedContainerTitle = prepared.getContainerTitle();
    } else if (TraversalTreeCache.holdsCurrent(focusedNode)) {
      // Read from the app with the saved order, and nothing in its window has changed since, so a
      // second read would only wait for the app, which on a watch is often busy scrolling.
      node = focusedNode;
      preparedFeedback = null;
    } else {
      // The node may come from the saved reading order, which keeps nodes for a while after their
      // text or state changes, such as a progress label or a switch the app turned on. Read it
      // again from the app, as its focus event would. If it is gone, leave it to the event.
      @SuppressWarnings("deprecation") // obtain(): copy, so that the saved node is left as it was.
      AccessibilityNodeInfoCompat copy = AccessibilityNodeInfoCompat.obtain(focusedNode);
      if (!copy.refresh()) {
        return false;
      }
      node = copy;
      preparedFeedback = null;
    }
    if (!shouldSpeakFocusEarly(node)) {
      return false;
    }
    AccessibilityEvent event = focusEventFor(node);
    EventInterpretation eventInterpreted = focusInterpretation(info);

    // The speech is composed from the state that follows focus, so it is updated first, and the
    // app's event must then not update it a second time, whether or not the speech succeeds:
    // moving the collection state twice would lose "list, N items" or "in table".
    globalVariables.updateStateFromFocusedNode(node);
    globalVariables.updateCollectionStateFromFocusedNode(node, event);
    lastHoverEnteredNode = null;
    try {
      EventFeedback feedback;
      if (preparedFeedback != null) {
        feedback = preparedFeedback;
        // Working the feedback out would have moved the container title to the node's, as
        // preparing it did before putting it back.
        EventTypeViewAccessibilityFocusedFeedbackRule.currentContainerTitle =
            preparedContainerTitle;
      } else {
        feedback = getSwipeTargetFeedback(event, node, eventInterpreted);
      }
      if (!hasSpeech(feedback)) {
        // Some nodes only show their content once focused, such as the second notification of a
        // collapsed group, whose text is hidden until then. Nothing to say yet means the app's
        // event, which comes after it shows the content, should say it.
        earlyFocusSpeech.addStateUpdated(node, actionTime);
        onFocusMoved(node);
        return true;
      }
      compositor.handleEvent(event, node, eventId, eventInterpreted, feedback);
    } catch (RuntimeException e) {
      // Something on the way to speech needed more than a Backtalk-made event has. Leave the
      // focus to be spoken from the app's event rather than stop Backtalk.
      LogUtils.e(TAG, "Cannot speak focus early: %s", e);
      earlyFocusSpeech.addStateUpdated(node, actionTime);
      onFocusMoved(node);
      return true;
    }
    earlyFocusSpeech.addSpoken(node, actionTime);
    onFocusMoved(node);
    return true;
  }

  /** Sets whether items are spoken before a swipe scrolls them into view; see the field. */
  public static void setSpeakItemsBeforeScroll(boolean speak) {
    speakItemsBeforeScroll = speak;
  }

  /**
   * Works out what focusing {@code node} by a swipe says. This comes before the swipe has scrolled
   * the node into view, so its children still off screen are described as they will be once it has.
   */
  private EventFeedback getSwipeTargetFeedback(
      AccessibilityEvent event, AccessibilityNodeInfoCompat node, EventInterpretation interpreted) {
    globalVariables.setDescribingSwipeTarget(speakItemsBeforeScroll);
    try {
      return compositor.getFeedback(event, node, interpreted);
    } finally {
      globalVariables.setDescribingSwipeTarget(false);
    }
  }

  /** The interpretation of the focus event for focus that user navigation set with {@code info}. */
  private static EventInterpretation focusInterpretation(FocusActionInfo info) {
    AccessibilityFocusEventInterpretation focusInterpretation =
        new AccessibilityFocusEventInterpretation(
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
    focusInterpretation.setForceFeedbackEvenIfAudioPlaybackActive(
        info.forceFeedbackEvenIfAudioPlaybackActive());
    focusInterpretation.setForceFeedbackEvenIfMicrophoneActive(
        info.forceFeedbackEvenIfMicrophoneActive());
    focusInterpretation.setForceFeedbackEvenIfSsbActive(info.forceFeedbackEvenIfSsbActive());
    focusInterpretation.setIsNavigateByUser(true);
    focusInterpretation.setColumnWrap(info.columnWrap);
    EventInterpretation eventInterpreted =
        new EventInterpretation(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
    eventInterpreted.setAccessibilityFocusInterpretation(focusInterpretation);
    eventInterpreted.setReadOnly();
    return eventInterpreted;
  }

  /** Whether focus set with {@code info} may be spoken early, before looking at its node. */
  private boolean shouldSpeakFocusEarly(FocusActionInfo info) {
    return info.sourceAction == FocusActionInfo.LOGICAL_NAVIGATION
        && info.navigationAction != null
        && !info.forceMuteFeedback
        && !globalVariables.hasSkipFocusProcessing();
  }

  /** Whether focus on {@code node} may be spoken early. */
  private boolean shouldSpeakFocusEarly(AccessibilityNodeInfoCompat node) {
    return !WebInterfaceUtils.supportsWebActions(node)
        && shouldSpeakFocusEarly(node, Role.getRole(node));
  }

  /**
   * Whether focus on {@code node} may be spoken before the action that sets it. Unlike after the
   * action, items of web content may: a page sends nothing more for focus that SandFly set than
   * the item's node. The page itself is still spoken from its event.
   */
  private boolean shouldSpeakFocusBeforeAction(AccessibilityNodeInfoCompat node) {
    int role = Role.getRole(node);
    // The kind of a selectable image is said only while it has focus.
    if (role == Role.ROLE_WEB_VIEW
        || (role == Role.ROLE_IMAGE
            && AccessibilityNodeInfoUtils.supportsAction(
                node, AccessibilityNodeInfoCompat.ACTION_SELECT))) {
      return false;
    }
    return shouldSpeakFocusEarly(node, role);
  }

  /** Whether focus on {@code node}, which has {@code role}, may be spoken early, web or not. */
  private boolean shouldSpeakFocusEarly(AccessibilityNodeInfoCompat node, int role) {
    if (node.getWindowId() != globalVariables.getCurrentWindowId()) {
      return false;
    }
    if (role == Role.ROLE_TEXT_ENTRY_KEY
        || role == Role.ROLE_KEYBOARD_KEY
        || role == Role.ROLE_SEEK_CONTROL
        || role == Role.ROLE_PROGRESS_BAR
        || AccessibilityNodeInfoUtils.isPage(node)) {
      return false;
    }
    // A Telegram chat list row may still describe the conversation it showed before it was reused,
    // until the app fills in its focus event.
    if (TelegramChatCell.isDescribedOnFocus(node)) {
      return false;
    }
    AccessibilityWindowInfo window = AccessibilityNodeInfoUtils.getWindow(node.unwrap());
    if (AccessibilityWindowInfoUtils.getType(window)
        == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
      return false;
    }
    // An item only partly on screen, which the swipe is about to scroll into view, can have all
    // its text in children still off screen, which descriptions leave out unless they are spoken
    // as they will be after the scroll. Otherwise the app's event, after the scroll, says it.
    return speakItemsBeforeScroll || !hasChildOffScreen(node);
  }

  /** Whether a child of {@code node} is off screen. */
  private static boolean hasChildOffScreen(AccessibilityNodeInfoCompat node) {
    for (int i = 0; i < node.getChildCount(); i++) {
      AccessibilityNodeInfoCompat child = node.getChild(i);
      if (child != null && !child.isVisibleToUser()) {
        return true;
      }
    }
    return false;
  }

  /** Prepares the announcement of the node after {@code node}, which focus just moved to. */
  private void onFocusMoved(AccessibilityNodeInfoCompat node) {
    lastFocusedNode = node;
    handler.removeCallbacks(prepareSwipeTargets);
    if (targetPredictor != null) {
      handler.postDelayed(prepareSwipeTargets, PREPARE_DELAY_MS);
    }
  }

  /**
   * Works out the announcement of the node that a swipe from the focused node, in the direction of
   * the last one, would most likely reach, while the user listens to the focused node, so that the swipe can
   * speak one at once. Finding them also fetches them from the app, so that the swipe's own search
   * finds them already fetched.
   */
  private void prepareSwipeTargets() {
    @Nullable AccessibilityNodeInfoCompat pivot = lastFocusedNode;
    if (pivot == null || targetPredictor == null || fingerDown.getAsBoolean()) {
      // A swipe may be starting, and must not wait for this.
      return;
    }
    // Only in the direction of the last move: each target is read from the app again and its
    // announcement worked out, and the next swipe mostly goes the same way.
    @Nullable AccessibilityNodeInfoCompat target;
    try {
      target = targetPredictor.predictTarget(pivot, lastDirection.getAsInt());
    } catch (RuntimeException e) {
      LogUtils.e(TAG, "Cannot predict swipe target: %s", e);
      return;
    }
    if (target != null && !PreparedFocusSpeech.has(target)) {
      prepareFocusSpeech(target);
    }
  }

  /** Works out and keeps what focusing {@code focusedNode} by a swipe would say. */
  private void prepareFocusSpeech(AccessibilityNodeInfoCompat focusedNode) {
    // Read the node again from the app, as a swipe to it would, rather than prepare from a copy
    // that the saved reading order may have kept since before its text or state changed.
    @SuppressWarnings("deprecation") // obtain(): copy, so that the saved node is left as it was.
    AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain(focusedNode);
    if (!node.refresh()) {
      return;
    }
    if (globalVariables.hasSkipFocusProcessing() || !shouldSpeakFocusEarly(node)) {
      return;
    }
    AccessibilityEvent event = focusEventFor(node);
    FocusActionInfo info =
        FocusActionInfo.builder().setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION).build();
    // Working it out moves the focus state to the node, as a swipe would, so put it back after.
    GlobalVariables.SavedFocusState saved = globalVariables.saveFocusState();
    EventFeedback feedback;
    CharSequence containerTitle;
    try {
      globalVariables.updateStateFromFocusedNode(node);
      globalVariables.updateCollectionStateFromFocusedNode(node, event);
      feedback = getSwipeTargetFeedback(event, node, focusInterpretation(info));
      containerTitle = EventTypeViewAccessibilityFocusedFeedbackRule.currentContainerTitle;
    } catch (RuntimeException e) {
      LogUtils.e(TAG, "Cannot prepare focus speech: %s", e);
      return;
    } finally {
      globalVariables.restoreFocusState(saved);
    }
    if (hasSpeech(feedback)) {
      PreparedFocusSpeech.put(node, feedback, containerTitle);
    }
  }

  /** Whether {@code feedback} says anything. */
  private static boolean hasSpeech(EventFeedback feedback) {
    return !TextUtils.isEmpty(feedback.ttsOutput().orElse(null));
  }

  /** Makes the focus event the app would send for {@code node}, without its source. */
  @SuppressWarnings("deprecation") // AccessibilityEvent(int) needs Android 11.
  private static AccessibilityEvent focusEventFor(AccessibilityNodeInfoCompat node) {
    AccessibilityEvent event =
        AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
    event.setEventTime(SystemClock.uptimeMillis());
    event.setClassName(node.getClassName());
    event.setPackageName(node.getPackageName());
    event.setContentDescription(node.getContentDescription());
    if (node.getText() != null) {
      event.getText().add(node.getText());
    }
    event.setEnabled(node.isEnabled());
    event.setChecked(node.isChecked());
    event.setPassword(node.isPassword());
    event.setScrollable(node.isScrollable());
    return event;
  }

  /** Passes text-event-interpretation to compositor. */
  public void accept(TextEventInterpreter.Interpretation textEventInterpreted) {
    AccessibilityEvent event = textEventInterpreted.event;
    globalVariables.setLastTextEditIsPassword(event.isPassword());

    int eventType =
        (textEventInterpreted.interpretation == null)
            ? Compositor.toCompositorEvent(event)
            : Compositor.toCompositorEvent(textEventInterpreted.interpretation.getEvent());
    EventInterpretation eventInterpreted = new EventInterpretation(eventType);
    eventInterpreted.setTextEventInterpretation(textEventInterpreted.interpretation);
    eventInterpreted.setPackageName(event.getPackageName());
    eventInterpreted.setReadOnly();

    compositor.handleEvent(event, textEventInterpreted.eventId, eventInterpreted);
  }

  private boolean isEqualsToLastHoverEnteredKeyboardNode(@Nullable AccessibilityEvent event) {
    if (!FeatureFlagReader.enableEarlyAnnounceForLiftToType(context)
        || !AccessibilityEventFeedbackUtils.isEventRelatedToKeyboardKey(event)) {
      return false;
    }
    return Objects.equals(lastHoverEnteredNode, AccessibilityEventUtils.sourceCompat(event));
  }
}
