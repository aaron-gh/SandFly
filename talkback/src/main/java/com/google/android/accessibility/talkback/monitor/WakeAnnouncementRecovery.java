/*
 * Copyright 2026 SandFly contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.monitor;

import android.os.Handler;
import android.os.SystemClock;
import com.google.android.accessibility.utils.monitor.RecentUserInput;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.output.SpeechController.UtteranceCompleteRunnable;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.function.BooleanSupplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Retries one wake announcement canceled by the system before the user starts interacting. */
final class WakeAnnouncementRecovery {
  // A late interruption must not bring back an announcement from the start of the wake.
  private static final long MAX_AGE_MS = 2000;

  private final Handler handler;
  private final BooleanSupplier canRetry;
  private final Runnable retry;
  @Nullable private Pending pending;

  WakeAnnouncementRecovery(Handler handler, BooleanSupplier canRetry, Runnable retry) {
    this.handler = handler;
    this.canRetry = canRetry;
    this.retry = retry;
  }

  /** Tracks the original request; the retry itself is deliberately not tracked. */
  UtteranceCompleteRunnable track() {
    Pending request = new Pending();
    pending = request;
    return status -> {
      if (handler.getLooper().isCurrentThread()) {
        onCompleted(request, status);
      } else {
        handler.post(() -> onCompleted(request, status));
      }
    };
  }

  /** Runs AccessibilityService.onInterrupt() normally, then recovers only its wake announcement. */
  void onSystemInterrupt(Runnable interrupt) {
    Pending request = pending;
    if (request != null) {
      request.systemInterrupted = true;
    }
    interrupt.run();
    if (request != null) {
      // Leave the clear operation before enqueueing speech. A queued item can be dropped without
      // a completion callback, and stopping the current one can complete it with STATUS_ERROR.
      handler.post(() -> recover(request));
    }
  }

  /** Invalidates callbacks from a previous screen state or service lifetime. */
  void cancel() {
    pending = null;
  }

  private void onCompleted(Pending request, int status) {
    if (pending == request
        && (status == SpeechController.STATUS_SPOKEN || !request.systemInterrupted)) {
      pending = null;
    }
  }

  private void recover(Pending request) {
    if (pending != request) {
      return;
    }
    pending = null;
    if (RecentUserInput.inputSequence() == request.inputSequence
        && SystemClock.uptimeMillis() - request.requestUptime <= MAX_AGE_MS
        && canRetry.getAsBoolean()) {
      LogUtils.d("WakeAnnouncementRecovery", "Retrying wake announcement after system interrupt");
      retry.run();
    }
  }

  private static final class Pending {
    final long requestUptime = SystemClock.uptimeMillis();
    final long inputSequence = RecentUserInput.inputSequence();
    boolean systemInterrupted;
  }
}
