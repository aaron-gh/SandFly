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
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.compositor.Compositor;
import com.google.android.accessibility.talkback.compositor.EventFilter;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.WeakReferenceHandler;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.accessibility.utils.input.TextEventInterpreter;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Manages the event feedback queue. Queued events are run through the {@link Compositor} to
 * generate spoken, haptic, and audible feedback.
 */
public class ProcessorEventQueue implements AccessibilityEventListener {

  private static final String TAG = "ProcessorEventQueue";

  /** Manages pending speech events. */
  private final ProcessorEventHandler handler = new ProcessorEventHandler(this);

  /** Event types that are handled by ProcessorEventQueue. */
  private static final int MASK_EVENTS_HANDLED_BY_PROCESSOR_EVENT_QUEUE =
      AccessibilityEvent.TYPES_ALL_MASK;

  /**
   * We keep the accessibility events to be processed. If a received event is the same type as the
   * previous one it replaces the latter, otherwise it is added to the queue. All events in this
   * queue are processed while we speak and this occurs after a certain timeout since the last
   * received event.
   */
  private final EventQueue eventQueue = new EventQueue();

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
    this.eventProcessor = eventProcessor;
  }

  @Override
  public int getEventTypes() {
    return MASK_EVENTS_HANDLED_BY_PROCESSOR_EVENT_QUEUE;
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
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
   * limit of two focus events dropped most of them. The events queued before it, other than content
   * changes, are handled first, so that the speech of an earlier window change or announcement does
   * not come after the focus and cut it off. Content changes stay queued, in their order.
   */
  private void processWebFocus(AccessibilityEvent event, EventId eventId) {
    List<AccessibilityEvent> earlier;
    synchronized (eventQueue) {
      earlier = eventQueue.removeAllExcept(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }
    for (AccessibilityEvent earlierEvent : earlier) {
      processEvent(earlierEvent, Performance.getInstance().toEventId(earlierEvent));
    }
    // A copy, as a queued event is: on Android 12 and older the framework recycles the event after
    // dispatch, and the feedback for it can outlast that.
    processEvent(AccessibilityEvent.obtain(event), eventId);
  }

  public void clearQueue() {
    handler.removeMessages(WHAT_SPEAK);
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
