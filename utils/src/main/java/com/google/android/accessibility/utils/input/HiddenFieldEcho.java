package com.google.android.accessibility.utils.input;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tells the keyboard what a field that doesn't hand its text back lost to a deletion.
 *
 * <p>Revolut's top-up amount takes digits from the keyboard but returns no text and sends no text
 * change events: only its container's content description changes, such as from "zł500" to "zł50".
 * The screen reader passes every description change here; after the braille keyboard deletes a
 * character it can't name, it calls {@link #expect}, and the next description change from the
 * field's app is compared with the one before it in the same window to find the deleted character.
 */
public final class HiddenFieldEcho {
  public interface Listener {
    void onDeleted(String deleted);

    void onChanged(CharSequence now);
  }

  private static final long ANSWER_WINDOW_MS = 1000;
  private static final int REMEMBERED_WINDOWS = 8;

  private static final class Description {
    final String packageName;
    final String text;

    Description(String packageName, String text) {
      this.packageName = packageName;
      this.text = text;
    }
  }

  private static @Nullable Listener listener;
  private static @Nullable String expectedPackage;
  private static @Nullable Handler handler;
  private static final Runnable stopExpecting = HiddenFieldEcho::stopExpecting;

  private static final Map<Integer, Description> lastDescriptionByWindow =
      new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Description> eldest) {
          return size() > REMEMBERED_WINDOWS;
        }
      };

  private HiddenFieldEcho() {}

  /**
   * Waits up to {@link #ANSWER_WINDOW_MS} for the next description change in a window of {@code
   * fieldPackage} that had a description before.
   */
  public static void expect(String fieldPackage, Listener deletionListener) {
    listener = deletionListener;
    expectedPackage = fieldPackage;
    Handler mainHandler = mainHandler();
    mainHandler.removeCallbacks(stopExpecting);
    mainHandler.postDelayed(stopExpecting, ANSWER_WINDOW_MS);
  }

  public static void onAccessibilityEvent(AccessibilityEvent event) {
    if (!isContentDescriptionChange(event)) {
      return;
    }
    CharSequence description = event.getContentDescription();
    if (TextUtils.isEmpty(description)) {
      return;
    }
    String packageName = String.valueOf(event.getPackageName());
    Description before =
        lastDescriptionByWindow.put(
            event.getWindowId(), new Description(packageName, description.toString()));
    Listener waiting = listener;
    if (waiting == null
        || !packageName.equals(expectedPackage)
        || before == null
        || !before.packageName.equals(packageName)) {
      return;
    }
    stopExpecting();
    String deleted = removedCharacter(before.text, description.toString());
    if (deleted != null) {
      waiting.onDeleted(deleted);
    } else {
      waiting.onChanged(description);
    }
  }

  private static void stopExpecting() {
    listener = null;
    expectedPackage = null;
    if (handler != null) {
      handler.removeCallbacks(stopExpecting);
    }
  }

  private static Handler mainHandler() {
    if (handler == null) {
      handler = new Handler(Looper.getMainLooper());
    }
    return handler;
  }

  private static boolean isContentDescriptionChange(AccessibilityEvent event) {
    return event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        && (event.getContentChangeTypes()
                & AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION)
            != 0;
  }

  /** Ignores the spaces that group digits; null when more than one character differs. */
  static @Nullable String removedCharacter(String before, String after) {
    String longer = withoutSpaces(before);
    String shorter = withoutSpaces(after);
    if (longer.length() != shorter.length() + 1) {
      return null;
    }
    int i = 0;
    while (i < shorter.length() && longer.charAt(i) == shorter.charAt(i)) {
      i++;
    }
    if (!longer.substring(i + 1).equals(shorter.substring(i))) {
      return null;
    }
    return longer.substring(i, i + 1);
  }

  private static String withoutSpaces(String text) {
    StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (!Character.isWhitespace(c) && !Character.isSpaceChar(c)) {
        out.append(c);
      }
    }
    return out.toString();
  }
}
