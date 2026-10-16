/*
 * Copyright (C) 2021 Google Inc.
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

package com.google.android.accessibility.talkback.imagecaption;

import static java.lang.Math.max;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.time.Duration;
import java.util.ArrayDeque;

/**
 * A list of requests. Adds and starts a request to the list via {@link
 * RequestList#addRequest(Request)}. After a request is finished, invoke {@link
 * RequestList#performNextRequest(Request)} with it to perform the next one. Use it on the looper
 * that created it.
 */
public class RequestList<T extends Request> {

  private static final String TAG = "RequestsForCaption";
  @VisibleForTesting static final int MSG_RETRY_TO_PERFORM = 1;
  private final SynchronizedArrayDeque<T> requests = new SynchronizedArrayDeque<>();
  private final int capacity;

  /** The interval time of performing requests. */
  private final Duration minIntervalTime;

  private final Handler handler =
      new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
          super.handleMessage(msg);
          if (msg.what == MSG_RETRY_TO_PERFORM) {
            LogUtils.v(TAG, "Retry to perform request");
            performFirstRequest();
          }
        }
      };

  /**
   * When the last request was performed or finished, in {@link SystemClock#uptimeMillis()}, which
   * does not jump when the wall clock is set.
   */
  private long lastRequestExecutionTimeMs = Request.INVALID_TIME;

  public RequestList(int capacity) {
    this(capacity, /* minIntervalTime= */ Duration.ZERO);
  }

  public RequestList(int capacity, Duration minIntervalTime) {
    this.capacity = capacity;
    this.minIntervalTime = minIntervalTime;
  }

  /**
   * Adds the request to the list. Then, the request is started if there is no other request.
   * Otherwise, the request has to wait for the previous requests to be finished.
   */
  public void addRequest(T request) {
    requests.add(request);

    if (requests.size() > 1) {
      LogUtils.v(
          TAG,
          "addRequest() waiting... %d %s",
          requests.size() - 1,
          request.getClass().getSimpleName());
    } else {
      performFirstRequest();
    }
  }

  /**
   * Removes the finished request and performs the first pending request. Does nothing if the
   * request is not the one being performed, such as when it was removed by {@link #clear()}, so
   * that a late result cannot remove another request.
   *
   * <p>Discards the older requests if there are too many requests waiting to be executed.
   */
  public void performNextRequest(Request finishedRequest) {
    if (requests.isEmpty() || requests.getFirst() != finishedRequest) {
      LogUtils.v(TAG, "%s is not being performed", finishedRequest.getClass().getSimpleName());
      return;
    }

    requests.removeFirst();
    // Updates lastRequestExecutionTime by the end timestamp which is more accurate than start
    // timestamp.
    long endTimeMs = finishedRequest.getEndTimeMillis();
    if (endTimeMs != Request.INVALID_TIME) {
      lastRequestExecutionTimeMs = endTimeMs;
    }
    performFirstRequest();
  }

  /**
   * Performs the first pending request or waits until the take-screenshot function is ready.
   *
   * <p>Discards the older requests if there are too many requests waiting to be executed.
   */
  private void performFirstRequest() {
    if (requests.isEmpty()) {
      return;
    }

    while (requests.size() > capacity) {
      T request = requests.removeFirst();
      request.cancel();
      LogUtils.v(TAG, "cancel %s size=%d ", request.getClass().getSimpleName(), requests.size());
    }

    T request = requests.getFirst();
    long now = SystemClock.uptimeMillis();
    if (lastRequestExecutionTimeMs != Request.INVALID_TIME) {
      Duration intervalTime = Duration.ofMillis(now - lastRequestExecutionTimeMs);
      long waitingTime = minIntervalTime.minus(intervalTime).toMillis();
      if (waitingTime > 0) {
        LogUtils.v(TAG, "waiting... %d ms", waitingTime);
        Message message = new Message();
        message.what = MSG_RETRY_TO_PERFORM;
        if (handler.sendMessageDelayed(message, waitingTime)) {
          request.onPending(true, intervalTime);
        } else {
          LogUtils.e(TAG, "Fail to send message to the handler.");
          request.onPending(false, intervalTime);
          // The looper is quitting, so no request can be performed.
          clear();
        }
        return;
      }
    }

    lastRequestExecutionTimeMs = now;
    request.perform();
    LogUtils.v(TAG, "perform %s", request.getClass().getSimpleName());
  }

  public int getWaitingRequestSize() {
    return max(0, requests.size() - 1);
  }

  @VisibleForTesting
  Handler getHandler() {
    return handler;
  }

  /** Removes and cancels all requests, so that their results are ignored if they come later. */
  public void clear() {
    handler.removeMessages(MSG_RETRY_TO_PERFORM);
    while (!requests.isEmpty()) {
      requests.removeFirst().cancel();
    }
  }

  /** A synchronized ArrayDeque which prohibits null elements. */
  private static final class SynchronizedArrayDeque<E> {
    final ArrayDeque<E> arrayDeque = new ArrayDeque<>();
    final Object mutex = new Object();

    private SynchronizedArrayDeque() {}

    public boolean add(@NonNull E request) {
      synchronized (mutex) {
        return arrayDeque.add(request);
      }
    }

    @NonNull
    public E getFirst() {
      synchronized (mutex) {
        return arrayDeque.getFirst();
      }
    }

    @NonNull
    public E removeFirst() {
      synchronized (mutex) {
        return arrayDeque.removeFirst();
      }
    }

    public int size() {
      return arrayDeque.size();
    }

    public boolean isEmpty() {
      return arrayDeque.isEmpty();
    }
  }
}
