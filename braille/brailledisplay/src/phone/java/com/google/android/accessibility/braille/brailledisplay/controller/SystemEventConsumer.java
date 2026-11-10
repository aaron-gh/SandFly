package com.google.android.accessibility.braille.brailledisplay.controller;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorDisplayer;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.DefaultParser;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.NotificationParser;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.Parser;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.PopUpHistory;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.PopUpMessageInfo;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.SnackbarParser;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.ToastParser;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.Role;

/**
 * An event-consumer handles events from the phone.
 *
 * <p>Devices generates events such as notifications, toasts, and phone calls. This consumer would
 * handle those actions that are automatically triggered by specific events , and also ensuring that
 * users are reminded.
 */
public class SystemEventConsumer implements EventConsumer {
  private static final String TAG = "SystemEventConsumer";
  private static final int SKIP_EVENT_MS = 200;
  private static final long LIVE_REGION_SETTLE_MS = 600;
  private static final long LIVE_REGION_REPEAT_MS = 5000;
  private static final int MAX_LIVE_REGION_NODES = 100;
  private static final int MAX_LIVE_REGION_CHARS = 2000;
  private static final int MAX_LIVE_REGION_DEPTH = 5;
  private final Context context;
  private final BehaviorDisplayer behaviorDisplayer;
  private final CellsContentConsumer cellsContentConsumer;
  private final DefaultParser defaultParser;
  private final ToastParser toastParser;
  private final NotificationParser notificationParser;
  private final SnackbarParser snackbarParser;
  private final PopUpHistory popUpHistory;
  private PopUpMessageInfo cachePopupMessageInfo;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private @Nullable AccessibilityNodeInfoCompat liveRegion;
  private @Nullable String lastLiveRegionText;
  private long lastLiveRegionTime;

  public SystemEventConsumer(
      Context context,
      BehaviorDisplayer behaviorDisplayer,
      CellsContentConsumer cellsContentConsumer,
      PopUpHistory popUpHistory) {
    this.context = context;
    this.behaviorDisplayer = behaviorDisplayer;
    this.cellsContentConsumer = cellsContentConsumer;
    this.defaultParser = new DefaultParser(context);
    this.toastParser = new ToastParser(context);
    this.snackbarParser = new SnackbarParser(context);
    this.notificationParser = new NotificationParser(context);
    this.popUpHistory = popUpHistory;
  }

  @Override
  public void onActivate() {}

  @Override
  public void onDeactivate() {}

  @Override
  public boolean onMappedInputEvent(BrailleInputEvent event) {
    BrailleDisplayLog.v(TAG, "onMappedInputEvent: " + event.getCommand());
    return false;
  }

  @Override
  @SuppressLint("SwitchIntDef") // pre-existing logic
  public void onAccessibilityEvent(AccessibilityEvent event) {
    BrailleDisplayLog.v(TAG, "onAccessibilityEvent: " + event.getEventType());
    Parser parser;
    switch (event.getEventType()) {
      case TYPE_WINDOW_CONTENT_CHANGED -> {
        AccessibilityNodeInfoCompat source = AccessibilityEventUtils.sourceCompat(event);
        if (SnackbarParser.isAlert(source)) {
          parser = snackbarParser;
        } else {
          showLiveRegionWhenSettled(liveRegionOf(event, source));
          return;
        }
      }
      case TYPE_NOTIFICATION_STATE_CHANGED ->
          parser = isToast(event) ? toastParser : notificationParser;
      case TYPE_ANNOUNCEMENT -> parser = defaultParser;
      default -> {
        // do not parse and display
        return;
      }
    }
    PopUpMessageInfo info = parser.parse(event);
    if (!TextUtils.isEmpty(info.output()) && !skip(info)) {
      cachePopupMessageInfo = info;
      // show the notification text output on braille display
      displayPopMessage(info.output());
      // Add the new notificationNode to notificationHistoryList
      if (BrailleUserPreferences.readPopupMessageHistoryEnabled(context)) {
        popUpHistory.addMessage(info);
      }
    }
  }

  /**
   * Shows the text of a live region once it stops changing, since a streamed reply changes many
   * times a second.
   */
  private void showLiveRegionWhenSettled(@Nullable AccessibilityNodeInfoCompat region) {
    if (region == null) {
      return;
    }
    handler.removeCallbacks(showLiveRegion);
    liveRegion = region;
    handler.postDelayed(showLiveRegion, LIVE_REGION_SETTLE_MS);
  }

  private final Runnable showLiveRegion =
      () -> {
        AccessibilityNodeInfoCompat region = liveRegion;
        liveRegion = null;
        if (region == null || !region.refresh()) {
          return;
        }
        StringBuilder text = new StringBuilder();
        appendText(region, text, new int[] {MAX_LIVE_REGION_NODES});
        if (text.length() == 0) {
          // The region was cleared, so the same text next time is a new message.
          lastLiveRegionText = null;
          return;
        }
        String output = text.toString();
        long now = SystemClock.uptimeMillis();
        if (!output.equals(lastLiveRegionText) || now - lastLiveRegionTime >= LIVE_REGION_REPEAT_MS) {
          lastLiveRegionText = output;
          lastLiveRegionTime = now;
          displayPopMessage(output);
        }
      };

  /** Appends the text of {@code node}, or else of its descendants, visiting at most some nodes. */
  private static void appendText(
      AccessibilityNodeInfoCompat node, StringBuilder text, int[] nodesLeft) {
    if (nodesLeft[0]-- <= 0 || text.length() >= MAX_LIVE_REGION_CHARS) {
      return;
    }
    CharSequence own = node.getContentDescription();
    if (TextUtils.isEmpty(own)) {
      own = node.getText();
    }
    if (!TextUtils.isEmpty(own)) {
      if (text.length() > 0) {
        text.append(' ');
      }
      text.append(own);
      return;
    }
    for (int index = 0; index < node.getChildCount(); index++) {
      AccessibilityNodeInfoCompat child = node.getChild(index);
      if (child != null) {
        appendText(child, text, nodesLeft);
      }
    }
  }

  /**
   * The live region that a content change came from, or null. Only a text change looks at ancestors,
   * since content changes are frequent and each ancestor is a call to the app.
   */
  static @Nullable AccessibilityNodeInfoCompat liveRegionOf(
      AccessibilityEvent event, @Nullable AccessibilityNodeInfoCompat source) {
    if (source == null) {
      return null;
    }
    if (source.getLiveRegion() != ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE) {
      return source;
    }
    if ((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) == 0) {
      return null;
    }
    AccessibilityNodeInfoCompat node = source.getParent();
    for (int depth = 0; node != null && depth < MAX_LIVE_REGION_DEPTH; depth++) {
      if (node.getLiveRegion() != ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE) {
        return node;
      }
      node = node.getParent();
    }
    return null;
  }

  private boolean skip(PopUpMessageInfo info) {
    if (cachePopupMessageInfo == null || !cachePopupMessageInfo.output().equals(info.output())) {
      return false;
    }
    return info.eventTime() - cachePopupMessageInfo.eventTime() < SKIP_EVENT_MS;
  }

  private boolean isToast(AccessibilityEvent event) {
    return (Role.getSourceRole(event) == Role.ROLE_TOAST);
  }

  private void displayPopMessage(String popMessage) {
    if (behaviorDisplayer.isBrailleDisplayConnected()) {
      cellsContentConsumer.setTimedContent(
          TimedMessager.Type.ANNOUNCEMENT,
          new CellsContent(popMessage),
          TimedMessager.UNLIMITED_DURATION);
    }
  }
}
