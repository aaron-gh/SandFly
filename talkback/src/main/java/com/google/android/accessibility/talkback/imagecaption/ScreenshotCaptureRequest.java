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

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityService.ScreenshotResult;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.HandlerCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.utils.FocusIndicatorUtils;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.screencapture.ScreenshotCapture;
import com.google.android.accessibility.utils.screencapture.ScreenshotCapture.WindowOrFullScreenScreenshotCallback;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.time.Duration;

/** A request to take screenshot. */
public class ScreenshotCaptureRequest extends Request {

  /** A listener to be invoked when taking screenshot is finished. */
  public interface OnFinishListener {
    /**
     * Called when taking screenshot is finished.
     *
     * @param request the request, which stays first in its {@link RequestList} until it is passed
     *     to {@link RequestList#performNextRequest(Request)}
     */
    void onFinish(
        ScreenshotCaptureRequest request,
        AccessibilityNodeInfoCompat node,
        Bitmap bitmap,
        boolean isUserRequested,
        boolean capturedByWindow);
  }

  private static final String TAG = "ScreenshotRequestForCaption";

  /** Maximal caption request execution time. */
  public static final Duration SCREENSHOT_CAPTURE_TIMEOUT_MS = Duration.ofMillis(3000);

  /** How long the app on screen gets to redraw without the focus indicator. */
  @VisibleForTesting static long hiddenFocusRedrawMs = 150;

  private final AccessibilityService service;
  private final AccessibilityNodeInfoCompat node;
  @NonNull private final OnFinishListener onFinishListener;
  private final boolean isUserRequested;
  private final Handler handler = new Handler(Looper.getMainLooper());
  // Whether the focus indicator is hidden for this screenshot and must be shown again.
  private boolean focusHidden;

  public ScreenshotCaptureRequest(
      AccessibilityService service,
      AccessibilityNodeInfoCompat node,
      @NonNull OnPendingListener onPendingListener,
      @NonNull OnFinishListener onFinishListener,
      boolean isUserRequested) {
    super(onPendingListener, SCREENSHOT_CAPTURE_TIMEOUT_MS);
    this.service = service;
    this.node = node;
    this.onFinishListener = onFinishListener;
    this.isUserRequested = isUserRequested;
  }

  @Override
  @RequiresApi(api = 30)
  public void perform() {
    setStartTimestamp();
    // A screenshot the user asked for is described by AI, which takes the focus indicator for a
    // selection.
    if (isUserRequested && FocusIndicatorUtils.hideForScreenshot(service)) {
      focusHidden = true;
      runTimeoutRunnable();
      // This request is the token, so that the capture can be removed.
      HandlerCompat.postDelayed(handler, this::capture, this, hiddenFocusRedrawMs);
    } else {
      capture();
      runTimeoutRunnable();
    }
  }

  @Override
  void cancel() {
    super.cancel();
    handler.removeCallbacksAndMessages(this);
    restoreFocus();
  }

  @RequiresApi(api = 30)
  private void capture() {
    ScreenshotCapture.takeScreenshotByNode(
        service,
        node,
        new WindowOrFullScreenScreenshotCallback() {
          @Override
          public void onFailure(int errorCode) {
            super.onFailure(errorCode);
            onFinished(/* screenCapture= */ null, false);
          }

          @Override
          public void onSuccess(ScreenshotResult screenshot) {
            super.onSuccess(screenshot);
            onFinished(getBitmap(), isCaptureByWindow());
          }
        });
  }

  private void restoreFocus() {
    if (focusHidden) {
      focusHidden = false;
      FocusIndicatorUtils.restoreAfterScreenshot(service);
    }
  }

  @Override
  protected void onError(int errorCode) {
    onFinished(/* screenCapture= */ null, false);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "= "
        + StringBuilderUtils.joinFields(StringBuilderUtils.optionalSubObj("node", node));
  }

  public OnFinishListener getOnFinishListener() {
    return onFinishListener;
  }

  private void onFinished(@Nullable Bitmap screenCapture, boolean capturedByWindow) {
    handler.removeCallbacksAndMessages(this);
    restoreFocus();
    if (!finish()) {
      // It timed out or was cancelled, and the next request may have started.
      if (screenCapture != null) {
        screenCapture.recycle();
      }
      return;
    }
    setEndTimestamp();
    LogUtils.v(
        TAG,
        "onFinish() "
            + StringBuilderUtils.joinFields(
                StringBuilderUtils.optionalText("name", getClass().getSimpleName()),
                StringBuilderUtils.optionalInt("time", getDurationMillis(), /* defaultValue= */ 0),
                StringBuilderUtils.optionalSubObj("screenCapture", screenCapture),
                StringBuilderUtils.optionalTag("capturedByWindow", capturedByWindow),
                StringBuilderUtils.optionalSubObj("node", node)));
    onFinishListener.onFinish(this, node, screenCapture, isUserRequested, capturedByWindow);
  }
}
