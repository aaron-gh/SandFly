package com.google.android.accessibility.talkback.utils;

import org.checkerframework.checker.nullness.qual.Nullable;

/** Utility methods for interacting with {@link SharedPreferences} objects in TalkBack. */
public final class TalkBackSharedPreferencesUtils {
  private static final String TAG = "TalkBackSharedPreferencesUtils";

  private static final String PREFIX_CONCATENATOR = "|";

  /**
   * Whether a preference key is one that keyboard shortcuts are saved under. Choosing a keymap
   * saves every shortcut, so listeners that only follow other settings skip these.
   */
  public static boolean isKeyComboPreference(@Nullable String prefKey) {
    return prefKey != null
        && (prefKey.endsWith(PREFIX_CONCATENATOR + "key_combo_code")
            || prefKey.endsWith(PREFIX_CONCATENATOR + "trigger_modifier_used")
            || prefKey.endsWith(PREFIX_CONCATENATOR + "always_use_trigger_modifier"));
  }

  public static String getKeyForKeyComboCode(@Nullable String keyPrefix, String key) {
    return getPrefixedKey(keyPrefix, key) + PREFIX_CONCATENATOR + "key_combo_code";
  }

  // TODO: b/370662049 - Remove `isBrowseModeFlagElabned` when deprecating the classic keymap.
  public static String getKeyForTriggerModifierUsed(
      @Nullable String keyPrefix, String key, boolean isBrowseModeFlagElabned) {
    return isBrowseModeFlagElabned
        ? getPrefixedKey(keyPrefix, key) + PREFIX_CONCATENATOR + "trigger_modifier_used"
        : getPrefixedKey(keyPrefix, key) + PREFIX_CONCATENATOR + "always_use_trigger_modifier";
  }

  private static String getPrefixedKey(@Nullable String keyPrefix, String key) {
    if (keyPrefix == null) {
      return key;
    } else {
      return keyPrefix + PREFIX_CONCATENATOR + key;
    }
  }

  private TalkBackSharedPreferencesUtils() {}
}
