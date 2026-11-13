/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.google.android.accessibility.talkback.eventprocessor;

import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.compositor.Compositor;
import com.google.android.accessibility.talkback.compositor.EventFilter;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.ConcurrentNodeQueries;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.WeakReferenceHandler;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.accessibility.utils.input.TextEventInterpreter;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Manages the event feedback queue. Queued events are run through the {@link Compositor} to
 * generate spoken, haptic, and audible feedback.
 */
public class ProcessorEventQueue implements AccessibilityEventListener {

  private static final String TAG = "ProcessorEventQueue";
  private static final Predicate<AccessibilityEvent> DEFAULT_ASSERTIVE_CLASSIFIER =
      ProcessorEventQueue::isAssertiveLiveRegionChange;

  /** Manages pending speech events. */
  private final ProcessorEventHandler handler = new ProcessorEventHandler(this);

  /** Event types that are handled by ProcessorEventQueue. */
  private static final int MASK_EVENTS_HANDLED_BY_PROCESSOR_EVENT_QUEUE =
      AccessibilityEvent.TYPES_ALL_MASK;

  /**
   * Types of queued events that are handled before a web focus event, as their speech, if it came
   * after the focus, could interrupt it.
   */
  private static final int MASK_SPOKEN_BEFORE_WEB_FOCUS =
      AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
          | AccessibilityEvent.TYPE_ANNOUNCEMENT
          | AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
          | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;

  /**
   * The most queued content changes checked for a live region change before a web focus event, the
   * latest first. Each check reads the change's source, which handling the change reads anyway.
   */
  @VisibleForTesting static final int MAX_CONTENT_CHANGES_CHECKED = 8;

  /** The most ancestors of a content change's source checked for a live region. */
  private static final int MAX_LIVE_REGION_DEPTH = 8;

  /**
   * We keep the accessibility events to be processed. If a received event is the same type as the
   * previous one it replaces the latter, otherwise it is added to the queue. All events in this
   * queue are processed while we speak and this occurs after a certain timeout since the last
   * received event.
   */
  private final EventQueue eventQueue = new EventQueue();
  /** Answers belong to queued event copies, not to nodes or future content changes. */
  private final Map<AccessibilityEvent, Boolean> assertiveClassifications = new IdentityHashMap<>();
  private final Predicate<AccessibilityEvent> assertiveClassifier;
  private final Function<List<AccessibilityEvent>, ConcurrentNodeQueries.LiveRegionRead> liveRegionPreparer;
  private long contentGeneration;
  private record PreparedRegions(AccessibilityEvent focus, long generation,
      List<AccessibilityEvent> changes, ConcurrentNodeQueries.LiveRegionRead read) {}
  private PreparedRegions preparedRegions;


  /** Speaks an event through the {@link Compositor} and interprets its text changes. */
  private final BiConsumer<AccessibilityEvent, EventId> eventProcessor;

  public ProcessorEventQueue(EventFilter eventFilter, TextEventInterpreter textEventInterpreter) {
    this(
        (event, eventId) -> {
          eventFilter.sendEvent(event, eventId);
          textEventInterpreter.interpret(event, eventId);
        });
    textEventInterpreter.addListener(eventFilter::accept);
  }

  @VisibleForTesting
  ProcessorEventQueue(BiConsumer<AccessibilityEvent, EventId> eventProcessor) {
    this(eventProcessor, DEFAULT_ASSERTIVE_CLASSIFIER);
  }

  @VisibleForTesting
  ProcessorEventQueue(BiConsumer<AccessibilityEvent, EventId> eventProcessor,
      Predicate<AccessibilityEvent> assertiveClassifier) {
    this(eventProcessor, assertiveClassifier, assertiveClassifier == DEFAULT_ASSERTIVE_CLASSIFIER
        ? changes -> ConcurrentNodeQueries.beginLiveRegions(changes, MAX_LIVE_REGION_DEPTH) : null);
  }

  @VisibleForTesting
  ProcessorEventQueue(BiConsumer<AccessibilityEvent, EventId> eventProcessor,
      Predicate<AccessibilityEvent> assertiveClassifier,
      Function<List<AccessibilityEvent>, ConcurrentNodeQueries.LiveRegionRead> liveRegionPreparer) {
    this.eventProcessor = eventProcessor;
    this.assertiveClassifier = assertiveClassifier;
    this.liveRegionPreparer = liveRegionPreparer;
  }

  private void cancelPreparedRegions() {
    if (preparedRegions != null) preparedRegions.read.cancel();
    preparedRegions = null;
  }

  /** Starts this focus event's alert reads while the earlier listeners handle collection data. */
  public void prepareWebFocus(AccessibilityEvent event) {
    cancelPreparedRegions();
    if (liveRegionPreparer == null || !isWebFocus(event)) return;
    List<AccessibilityEvent> changes;
    synchronized (eventQueue) {
      changes = eventQueue.latestOfType(
          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, MAX_CONTENT_CHANGES_CHECKED);
    }
    changes.removeIf(assertiveClassifications::containsKey);
    if (!changes.isEmpty()) {
      preparedRegions = new PreparedRegions(event, contentGeneration,
          new java.util.ArrayList<>(changes), liveRegionPreparer.apply(changes));
    }
  }

  @Override
  public int getEventTypes() {
    return MASK_EVENTS_HANDLED_BY_PROCESSOR_EVENT_QUEUE;
  }

  private static boolean changesWindowStructure(AccessibilityEvent event) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return true;
    int focusOnly = AccessibilityEvent.WINDOWS_CHANGE_ACTIVE
        | AccessibilityEvent.WINDOWS_CHANGE_FOCUSED
        | AccessibilityEvent.WINDOWS_CHANGE_ACCESSIBILITY_FOCUSED;
    int changes = event.getWindowChanges();
    // Moving focus does not change aria-live or a queued change's ancestor chain. Unknown and
    // structural window changes still invalidate it, as does every content change below.
    return changes == 0 || (changes & ~focusOnly) != 0;
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        || (event.getEventType() == AccessibilityEvent.TYPE_WINDOWS_CHANGED
            && changesWindowStructure(event))) {
      // A new change can alter aria-live on an ancestor of an already queued change.
      assertiveClassifications.clear();
      contentGeneration++;
      cancelPreparedRegions();
    }
    if (isWebFocus(event)) {
      processWebFocus(event, eventId);
      return;
    }

    synchronized (eventQueue) {
      eventQueue.enqueue(event);
      handler.postSpeak();
    }
  }

  /**
   * Whether {@code event} is accessibility focus moving to an item of web content. Only a focus
   * event asks for its source, which the focus listeners before this one have already read.
   */
  private static boolean isWebFocus(AccessibilityEvent event) {
    return event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
        && WebInterfaceUtils.supportsWebActions(AccessibilityEventUtils.sourceCompat(event));
  }

  /**
   * Speaks focus that a web page moved at once. Native focus is spoken before its event arrives,
   * but a web page chooses the item itself, so only its event can say which item it is. While a
   * page changes, it sends many content change events, each of which waits for the app when it is
   * handled. Queued behind them, focus was spoken late, and in a fast run of swipes the queue's
   * limit of two focus events dropped most of them. The queued events of {@link
   * #MASK_SPOKEN_BEFORE_WEB_FOCUS}, and changes to assertive live regions, are handled first, so
   * that the speech of an earlier window change, announcement or alert does not come after the
   * focus and cut it off. The other events stay queued, in their order, and are handled in time
   * slices after the focus.
   */
  private void processWebFocus(AccessibilityEvent event, EventId eventId) {
    List<AccessibilityEvent> latestContentChanges;
    synchronized (eventQueue) {
      latestContentChanges =
          eventQueue.latestOfType(
              AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, MAX_CONTENT_CHANGES_CHECKED);
    }
    Set<AccessibilityEvent> assertiveChanges = Collections.newSetFromMap(new IdentityHashMap<>());
    // A polite change can remain queued across many focus events. Read it once while it is
    // queued, rather than walking its parents again before each item can be spoken.
    assertiveClassifications.keySet().retainAll(latestContentChanges);
    List<AccessibilityEvent> unchecked = new java.util.ArrayList<>();
    for (AccessibilityEvent change : latestContentChanges) {
      if (!assertiveClassifications.containsKey(change)) unchecked.add(change);
    }
    // Classification still precedes focus speech. Only independent app reads are batched;
    // any timed-out or own-window result is checked by the original synchronous path.
    PreparedRegions prepared = preparedRegions;
    preparedRegions = null;
    if (prepared != null && prepared.focus == event && prepared.generation == contentGeneration
        && prepared.changes.equals(unchecked)) {
      assertiveClassifications.putAll(prepared.read.finish());
    } else {
      if (prepared != null) prepared.read.cancel();
    }
    for (AccessibilityEvent contentChange : latestContentChanges) {
      if (assertiveClassifications.computeIfAbsent(contentChange, assertiveClassifier::test)) {
        assertiveChanges.add(contentChange);
      }
    }
    List<AccessibilityEvent> earlier;
    synchronized (eventQueue) {
      earlier =
          eventQueue.removeMatching(
              queued ->
                  AccessibilityEventUtils.eventMatchesAnyType(queued, MASK_SPOKEN_BEFORE_WEB_FOCUS)
                      || assertiveChanges.contains(queued));
    }
    for (AccessibilityEvent earlierEvent : earlier) {
      processEvent(earlierEvent, Performance.getInstance().toEventId(earlierEvent));
    }
    // A copy, as a queued event is: on Android 12 and older the framework recycles the event after
    // dispatch, and the feedback for it can outlast that.
    processEvent(AccessibilityEvent.obtain(event), eventId);
  }

  /**
   * Whether {@code event} changes an assertive live region, whose speech interrupts other speech.
   * Chrome marks only the root of a live region, and sends the change from the node in it that
   * changed, so the nearest live region among the source and its ancestors decides.
   */
  private static boolean isAssertiveLiveRegionChange(AccessibilityEvent event) {
    AccessibilityNodeInfoCompat node = AccessibilityEventUtils.sourceCompat(event);
    for (int depth = 0; node != null && depth <= MAX_LIVE_REGION_DEPTH; depth++) {
      int liveRegion = node.getLiveRegion();
      if (liveRegion != View.ACCESSIBILITY_LIVE_REGION_NONE) {
        return liveRegion == View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE;
      }
      if (depth < MAX_LIVE_REGION_DEPTH) node = node.getParent();
    }
    return false;
  }

  public void clearQueue() {
    handler.removeMessages(WHAT_SPEAK);
    assertiveClassifications.clear();
    contentGeneration++;
    cancelPreparedRegions();
  }

  /**
   * Processes an <code>event</code> by asking the {@link Compositor} to match it against its rules
   * and in case an utterance is generated it is spoken.
   *
   * @param event The event to process.
   */
  private void processEvent(AccessibilityEvent event, EventId eventId) {
    if (event == null) {
      return;
    }
    LogUtils.d(TAG, "Processing event: %s", event);
    assertiveClassifications.remove(event);

    eventProcessor.accept(event, eventId);
  }

  private static final int WHAT_SPEAK = 1;
  private static final long TIME_SLICE_MS = 16;

  private static class ProcessorEventHandler extends WeakReferenceHandler<ProcessorEventQueue> {
    /** Speak action. */

    public ProcessorEventHandler(ProcessorEventQueue parent) {
      super(parent);
    }

    @Override
    public void handleMessage(Message message, ProcessorEventQueue parent) {
      switch (message.what) {
        case WHAT_SPEAK -> processAllEvents(parent);
        default -> {}
      }
    }

    /**
     * Processes the events in the queue, in order, for about {@link #TIME_SLICE_MS}. A busy app can
     * queue events faster than they are handled, and each takes a round trip to the app to read its
     * source, so handling them all in one message kept gestures waiting behind the whole backlog.
     * The rest of the queue is handled in a later message, after the ones already waiting.
     */
    private void processAllEvents(ProcessorEventQueue parent) {
      long sliceEnd = SystemClock.uptimeMillis() + TIME_SLICE_MS;
      while (true) {
        final AccessibilityEvent event;

        synchronized (parent.eventQueue) {
          if (parent.eventQueue.isEmpty()) {
            return;
          }

          event = parent.eventQueue.dequeue();
        }

        // Re-generate event id -- slower than passing event id, but avoids modifying
        // the EventQueue to hold event ids.
        EventId eventId = Performance.getInstance().toEventId(event);

        parent.processEvent(event, eventId);

        if (SystemClock.uptimeMillis() >= sliceEnd) {
          synchronized (parent.eventQueue) {
            if (!parent.eventQueue.isEmpty()) {
              postSpeak();
            }
          }
          return;
        }
      }
    }

    /**
     * Sends {@link #WHAT_SPEAK} to the speech handler. This method cancels the old message (if such
     * exists) since it is no longer relevant.
     */
    public void postSpeak() {
      if (!hasMessages(WHAT_SPEAK)) {
        sendEmptyMessage(WHAT_SPEAK);
      }
    }
  }
}
