package com.google.android.accessibility.talkback.braille;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityService.SoftKeyboardController;
import android.annotation.SuppressLint;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.KeyboardUtils;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/** Utility class for TalkBack functions related to braille functions interaction. */
public class TalkBackForBrailleUtils {

  /** TalkBack provides the ability to enable BrailleIme. */
  @CanIgnoreReturnValue
  @SuppressLint("NewApi")
  public static boolean setBrailleKeyboardEnabled(AccessibilityService service) {
    if (FeatureSupport.supportEnableDisableIme()) {
      return service
              .getSoftKeyboardController()
              .setInputMethodEnabled(
                  KeyboardUtils.getImeId(
                      TalkBackService.getInstance(),
                      AccessibilityServiceCompatUtils.Constants.BRAILLE_KEYBOARD,
                      /* enabledOnly= */ false),
                  /* enabled= */ true)
          == SoftKeyboardController.ENABLE_IME_SUCCESS;
    }
    return false;
  }
}
