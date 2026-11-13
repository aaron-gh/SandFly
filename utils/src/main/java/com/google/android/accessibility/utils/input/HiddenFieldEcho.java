package com.google.android.accessibility.utils.input;

import android.os.SystemClock;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.Nullable;

/**
 * Tells the keyboard what a field that doesn't hand its text back lost to a deletion.
 *
 * <p>Revolut's top-up amount takes digits from the keyboard but returns no text and sends no text
 * change events: only its container's content description changes, such as from "zł500" to "zł50".
 * The screen reader passes every description change here; after the braille keyboard deletes a
 * character it can't name, it calls {@link #expect}, and the next description change is compared
 * with the one before to find the deleted character.
 */
public final class HiddenFieldEcho {
  public interface Listener {
    void onDeleted(String deleted);

    void onChanged(CharSequence now);
  }

  private static final long ANSWER_WINDOW_MS = 1000;

  private static volatile long expectedUntil;
  private static volatile @Nullable Listener listener;
  private static @Nullable String lastDescription;

  private HiddenFieldEcho() {}

  public static void expect(Listener deletionListener) {
    listener = deletionListener;
    expectedUntil = SystemClock.uptimeMillis() + ANSWER_WINDOW_MS;
  }

  public static void onAccessibilityEvent(AccessibilityEvent event) {
    if (!isContentDescriptionChange(event)) {
      return;
    }
    CharSequence description = event.getContentDescription();
    if (TextUtils.isEmpty(description)) {
      return;
    }
    String before = lastDescription;
    lastDescription = description.toString();
    Listener waiting = listener;
    if (waiting == null) {
      return;
    }
    boolean inTime = SystemClock.uptimeMillis() <= expectedUntil;
    expectedUntil = 0;
    listener = null;
    if (!inTime) {
      return;
    }
    String deleted = before == null ? null : removedCharacter(before, lastDescription);
    if (deleted != null) {
      waiting.onDeleted(deleted);
    } else {
      waiting.onChanged(description);
    }
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
