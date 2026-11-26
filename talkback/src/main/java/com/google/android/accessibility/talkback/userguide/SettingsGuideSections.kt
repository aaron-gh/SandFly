/*
 * Copyright 2026 SandFly contributors
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

package com.google.android.accessibility.talkback.userguide

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.AppGesturesFragment
import com.google.android.accessibility.talkback.sandflyai.SandflyAi
import com.google.android.accessibility.utils.preference.SettingsHelp

/**
 * The Help action of settings screens: opens the section of the user guide about the screen shown.
 * Screens without a section in [SECTIONS] have no Help action.
 */
object SettingsGuideSections : SettingsHelp.Provider {

  private const val TALKBACK = "com.google.android.accessibility.talkback"
  private const val BRAILLE = "com.google.android.accessibility.braille"
  private const val BRAILLE_IME = "com.google.android.accessibility.brailleime"

  /** The SandFly Pro settings screen, whose class is in the SandFly AI module. */
  const val PRO_SETTINGS = "pro-settings"

  /** The app keyboard shortcuts screen, which is the app gestures screen opened from Keyboard shortcuts. */
  const val APP_KEYBOARD_SHORTCUTS = "keyboards-app-shortcuts"

  /**
   * The ID of the guide heading about each settings screen, by the class name of its fragment, or of
   * its activity for screens without a fragment. Nested classes are named Outer$Inner.
   */
  val SECTIONS: Map<String, String> =
    mapOf(
      // Main settings screen.
      "$TALKBACK.preference.base.TalkBackPreferenceFragment" to "getting-started-settings",
      // Speech.
      "$TALKBACK.preference.base.TextToSpeechSettingsFragment" to "speech",
      "$TALKBACK.preference.base.VoiceProfilesFragment" to "speech-voice-profiles",
      "$TALKBACK.preference.base.VoiceProfileFragment" to "speech-voice-profile-settings",
      "$TALKBACK.preference.base.SpeechEqualizerFragment" to "voice-effects",
      "$TALKBACK.preference.base.LanguageVoicesFragment" to "speech-voices-by-language",
      "$TALKBACK.speechdictionary.SpeechDictionaryFragment" to "speech-dictionary",
      "$TALKBACK.speechdictionary.SymbolDictionaryFragment" to "speech-symbol-dictionary",
      "$TALKBACK.speechcontrol.SpeechControlFragment" to "speech-other-apps",
      "$TALKBACK.speechhistory.SpeechHistoryFragment" to "speech-history",
      // Verbosity.
      "$TALKBACK.preference.base.VerbosityPrefFragment" to "verbosity",
      "$TALKBACK.preference.base.ItemDetailsOrderFragment" to "verbosity-item-order",
      "$TALKBACK.preference.base.TextFormattingDetailFragment" to "verbosity-text-formatting",
      "$TALKBACK.preference.base.TextFormattingAnnouncementModeFragment" to "verbosity-text-formatting",
      "$TALKBACK.preference.base.ScreenAnnouncementsFragment" to "verbosity-screen-on-off",
      "$TALKBACK.status.StatusReadoutFragment" to "verbosity-status-readout",
      "$TALKBACK.feedbackpolicy.SpeakUserFocusAppsFragment" to "verbosity-own-speech",
      "$TALKBACK.feedbackpolicy.KeyboardsWithOwnSpeechFragment" to "verbosity-own-speech",
      // Sound and vibration, Audio output.
      "$TALKBACK.preference.base.SoundAndVibrationFragment" to "sound",
      "$TALKBACK.soundthemes.SoundThemesFragment" to "sound-themes",
      "$TALKBACK.soundthemes.ThemeSoundsFragment" to "sound-themes-replace",
      "$TALKBACK.scripting.SoundThemeSourcesFragment" to "sound-themes-install",
      "$TALKBACK.scripting.SoundThemeSourceFragment" to "sound-themes-install",
      "$TALKBACK.individualfeedback.IndividualFeedbackFragment" to "sound-individual",
      "$TALKBACK.preference.base.AudioOutputFragment" to "sound-audio-output",
      // Touch.
      "$TALKBACK.preference.base.TouchActivationFragment" to "touch",
      "$TALKBACK.preference.base.FocusDelayPrefFragment" to "touch-focus-delay",
      "$TALKBACK.directtouch.DirectTouchFragment" to "touch-direct-touch",
      "$TALKBACK.doubletaptaps.DoubleTapTapsFragment" to "touch-activate-with-tap",
      // Gestures.
      "$TALKBACK.preference.base.TalkBackGestureShortcutPreferenceFragment" to "gestures-screen",
      "$TALKBACK.commands.AppGesturesFragment" to "gestures-in-apps",
      "$TALKBACK.commands.AppGestureFragment" to "gestures-in-apps",
      "$TALKBACK.commands.GestureProfilesFragment" to "gestures-profiles",
      "$TALKBACK.commands.GestureProfileFragment" to "gestures-profiles",
      "$TALKBACK.commands.AppShortcutsFragment" to "gestures-app-shortcuts",
      "$TALKBACK.commands.EdgeMenuFragment" to "gestures-edge-menus",
      // Keyboard shortcuts.
      "$TALKBACK.preference.base.TalkBackKeyboardShortcutPreferenceFragment" to "keyboards-settings",
      // Volume keys.
      "$TALKBACK.preference.base.VolumeKeysFragment" to "buttons",
      // Commands and menus.
      "$TALKBACK.commands.CommandsFragment" to "commands-list",
      "$TALKBACK.commands.CommandCategoryFragment" to "commands-categories",
      "$TALKBACK.commands.CommandFragment" to "commands-assign",
      "$TALKBACK.preference.base.SelectorMenuFragment" to "commands-reading-controls",
      "$TALKBACK.selector.ReadingControlLettersFragment" to "commands-handwriting-letters",
      "$TALKBACK.commands.CustomReadingControlsFragment" to "commands-custom-reading-controls",
      "$TALKBACK.selector.ReadingControlListsFragment" to "commands-reading-control-lists",
      "$TALKBACK.selector.ReadingControlListFragment" to "commands-reading-control-lists",
      "$TALKBACK.commands.CommandMenuFragment" to "commands-edit-menu",
      "$TALKBACK.commands.MenuAddCommandFragment" to "commands-edit-menu",
      "$TALKBACK.preference.base.ContextMenuFragment" to "commands-edit-menu",
      "$TALKBACK.preference.base.NavigationMenuFragment" to "commands-edit-menu",
      "$TALKBACK.commands.CustomMenusFragment" to "commands-custom-menus",
      // Macros.
      "$TALKBACK.macros.MacrosFragment" to "macros-screen",
      "$TALKBACK.macros.MacroFragment" to "macros-macro-screen",
      "$TALKBACK.macros.MacroConditionFragment" to "macros-conditions",
      // Voice commands.
      "$TALKBACK.actor.voicecommands.VoiceCommandsFragment" to "voice-commands",
      // Navigation.
      "$TALKBACK.preference.base.NavigationPrefFragment" to "reading-navigation-settings",
      // Typing.
      "$TALKBACK.preference.base.TypingFragment" to "typing-settings",
      "$TALKBACK.keyboardecho.KeyboardEchoFragment" to "typing-keyboard-echo",
      "$TALKBACK.preference.base.TypingMethodPrefFragment" to "typing-method",
      "$TALKBACK.preference.base.TypingFocusDelayPrefFragment" to "typing-focus-delay",
      "$TALKBACK.keyboard.DoublePressCharactersFragment" to "keyboards-double-press",
      "$TALKBACK.dictation.DictationFragment" to "typing-dictation",
      // Braille.
      "$TALKBACK.preference.base.BrailleFragment" to "braille",
      "$TALKBACK.braillecommands.BrailleCommandsFragment" to "braille-commands",
      "$TALKBACK.braillecommands.BrailleCommandSetFragment" to "braille-commands-cells",
      "$TALKBACK.braillecommands.BrailleCommandAppsFragment" to "braille-commands-profiles",
      "$TALKBACK.braillecommands.BrailleCommandProfilesFragment" to "braille-commands-profiles",
      "$TALKBACK.speechdictionary.BrailleDictionaryFragment" to "braille-dictionaries",
      "$TALKBACK.braille.CustomBrailleCharactersFragment" to "braille-keyboard-custom-characters",
      "$BRAILLE_IME.settings.BrailleImePreferencesActivity\$BrailleImePrefFragment" to "braille-keyboard",
      "$BRAILLE_IME.settings.BrailleImeGestureActivity\$BrailleImeGestureFragment" to "braille-keyboard-gestures",
      "$BRAILLE_IME.settings.BrailleImeGestureCommandActivity\$BrailleImeGestureCommandFragment" to "braille-keyboard-gestures",
      "$BRAILLE.common.settings.BrailleLanguagesActivity\$BrailleLanguageFragment" to "braille-keyboard-languages",
      "$BRAILLE.common.settings.AddLanguageActivity\$AddLanguageFragment" to "braille-keyboard-languages",
      "$BRAILLE.common.settings.LocaleLanguageActivity\$PreferredLocaleLanguageFragment" to "braille-keyboard-languages",
      "$BRAILLE.brailledisplay.settings.BrailleDisplaySettingsFragment" to "braille-displays-settings",
      "$BRAILLE.brailledisplay.settings.AdvancedSettingsActivity\$AdvancedSettingsFragment" to "braille-displays-settings",
      "$BRAILLE.brailledisplay.settings.AutoScrollActivity\$AutoScrollFragment" to "braille-displays-settings",
      "$BRAILLE.brailledisplay.settings.BrailleElementsActivity\$BrailleElementsFragment" to "braille-displays-settings",
      "$BRAILLE.brailledisplay.settings.BrailleGradeActivity\$BrailleGradeFragment" to "braille-displays-settings",
      "$BRAILLE.brailledisplay.settings.BrailleLearningModeActivity\$BrailleLearningModeFragment" to "braille-displays-settings",
      "$BRAILLE.brailledisplay.settings.KeyBindingsActivity\$KeyBindingsFragment" to "braille-displays-keys",
      "$BRAILLE.brailledisplay.settings.KeyBindingsCommandActivity\$KeyBindingsCommandFragment" to "braille-displays-keys",
      "$BRAILLE.brailledisplay.settings.ScreenReaderCommandKeysActivity\$ScreenReaderCommandKeysFragment" to "braille-displays-keys",
      "$BRAILLE.brailledisplay.settings.ExternalDisplaysActivity\$ExternalDisplaysFragment" to "braille-displays-other-apps",
      // Display, quick settings, overview.
      "$TALKBACK.preference.base.DisplayFragment" to "display",
      "$TALKBACK.preference.base.FocusIndicatorPrefFragment" to "display-focus-indicator",
      "$TALKBACK.preference.base.QuickSettingsFragment" to "display-quick-settings",
      "$TALKBACK.quicksettings.QuickSettingsItemsFragment" to "display-quick-settings-edit",
      "$TALKBACK.preference.base.OverviewFragment" to "display-overview",
      // Notifications.
      "$TALKBACK.notifications.NotificationsFragment" to "notifications",
      "$TALKBACK.notifications.NotificationReadingFragment" to "notifications-reading",
      "$TALKBACK.notifications.NotificationAppsFragment" to "notifications-apps",
      "$TALKBACK.notifications.NotificationAppFragment" to "notifications-apps",
      "$TALKBACK.notifications.NotificationWordsFragment" to "notifications-words",
      "$TALKBACK.notifications.NotificationKindsFragment" to "notifications-kinds",
      "$TALKBACK.notifications.NotificationWhenFragment" to "notifications-when",
      "$TALKBACK.notifications.NotificationRulesFragment" to "notifications-rules",
      "$TALKBACK.notifications.NotificationRuleFragment" to "notifications-rules",
      "$TALKBACK.notifications.NotificationRepeatsFragment" to "notifications-repeats",
      "$TALKBACK.notifications.NotificationStopFragment" to "notifications-stop",
      "$TALKBACK.notifications.NotificationSchedulesFragment" to "notifications-summary",
      "$TALKBACK.notifications.NotificationHistoryFragment" to "notifications-history",
      "$TALKBACK.notificationlist.NotificationPartsFragment" to "notifications-list",
      // Translation.
      "$TALKBACK.translation.TranslationFragment" to "translation",
      // Image and screen descriptions, AI services.
      "$TALKBACK.preference.base.AutomaticDescriptionsFragment" to "descriptions-settings",
      "$TALKBACK.preference.base.GeminiSettingsFragment" to "descriptions-gemini",
      "$TALKBACK.actor.gemini.GeminiSettingsFragment" to "descriptions-gemini",
      "$TALKBACK.actor.gemini.AiServicesFragment" to "descriptions-ai-services",
      "$TALKBACK.actor.gemini.AiProviderFragment" to "descriptions-set-up-service",
      "$TALKBACK.actor.gemini.local.OnDeviceAiFragment" to "descriptions-on-device",
      "$TALKBACK.actor.gemini.local.ManageModelsFragment" to "descriptions-on-device-settings",
      // Scripts and custom labels.
      "$TALKBACK.scripting.ScriptsFragment" to "scripts",
      "$TALKBACK.scripting.ScriptSourcesFragment" to "scripts-sources",
      "$TALKBACK.scripting.ScriptSourceFragment" to "scripts-sources",
      "$TALKBACK.scripting.ScriptDetailFragment" to "scripts-manage",
      "$TALKBACK.scripting.ScriptSettingsFragment" to "scripts-manage",
      "$TALKBACK.scripting.ScriptCommandFragment" to "scripts-commands",
      "$TALKBACK.scripting.ScriptLogFragment" to "scripts-log",
      "$TALKBACK.scripting.ScriptBuilderFragment" to "scripts-builder",
      "$TALKBACK.scripting.ScriptRuleFragment" to "scripts-builder-rules",
      "$TALKBACK.scripting.ScriptRulePickFragment" to "scripts-builder-rules",
      "$TALKBACK.scripting.ScriptQueryFragment" to "scripts-builder-rules",
      "$TALKBACK.scripting.ScriptReadFragment" to "scripts-builder-rules",
      "$TALKBACK.scripting.ScriptBuiltCommandFragment" to "scripts-builder-commands",
      "$TALKBACK.scripting.ScriptSpeechChangeFragment" to "scripts-builder-speech",
      "$TALKBACK.labeling.LabelManagerSummaryActivity" to "scripts-labels-manage",
      "$TALKBACK.labeling.LabelManagerPackageActivity" to "scripts-labels-manage",
      // Pause, profiles, backup.
      "$TALKBACK.preference.base.PauseFragment" to "pause",
      "$TALKBACK.backup.SettingsProfilesFragment" to "profiles-settings-profiles",
      "$TALKBACK.backup.SettingsProfileFragment" to "profiles-change-settings",
      "$TALKBACK.preference.base.BackupFragment" to "profiles-backup",
      // Updates, Shizuku and root.
      "$TALKBACK.preference.base.UpdatesFragment" to "updates",
      "$TALKBACK.preference.base.ShizukuRootFragment" to "updates-shizuku",
      // Help and feedback.
      "$TALKBACK.preference.base.HelpFragment" to "help",
      "$TALKBACK.preference.base.TutorialAndHelpFragment" to "help-tutorial",
      "$TALKBACK.report.ProblemReportFragment" to "help-feedback-send",
      "$TALKBACK.report.ReportsFragment" to "help-feedback-inbox",
      "$TALKBACK.report.ReportThreadFragment" to "help-feedback-inbox",
      "$TALKBACK.preference.base.DeveloperPrefFragment" to "help-developer",
      "$TALKBACK.preference.base.TalkBackDumpAccessibilityEventFragment" to "help-developer",
    )

  /** Returns the ID of the guide heading about the screen [className], or null. */
  fun section(className: String, arguments: Bundle?): String? {
    if (className == AppGesturesFragment::class.java.name &&
      arguments?.getString(AppGesturesFragment.ARG_SHOWN_FROM) == AppGesturesFragment.FROM_KEYBOARD_SHORTCUTS) {
      return APP_KEYBOARD_SHORTCUTS
    }
    SECTIONS[className]?.let { return it }
    return if (className == SandflyAi.features?.settingsFragment) PRO_SETTINGS else null
  }

  override fun helpIntent(activity: Activity, screen: Fragment?): Intent? {
    if (!UserGuideActivity.isAvailable()) return null
    val section =
      section(screen?.javaClass?.name ?: activity.javaClass.name, screen?.arguments) ?: return null
    return UserGuideActivity.intent(activity, section)
  }

  override fun title(context: Context): CharSequence = context.getString(R.string.title_pref_help)
}
