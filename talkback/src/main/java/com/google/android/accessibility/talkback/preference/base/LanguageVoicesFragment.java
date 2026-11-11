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

package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.material.preference.AccessibilitySuiteListPreference;
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.speech.VoiceProfileNames;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.FailoverTextToSpeech;
import com.google.android.accessibility.utils.output.LanguageDetector;
import com.google.android.accessibility.utils.output.LanguageSwitch;
import com.google.android.accessibility.utils.output.LanguageVoices;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Voices by language: the languages the user reads, each said with SandFly default's voice for it
 * or a voice profile, and the language of each script that several of them are written in.
 */
public class LanguageVoicesFragment extends TalkbackBaseFragment {

  /** How long the installed engines have to list their languages. */
  private static final long ENGINE_LANGUAGES_WAIT_MS = 4000;

  private SharedPreferences prefs;
  private final Handler handler = new Handler(Looper.getMainLooper());

  /** The languages of the installed engines, by language code, once listed. */
  private @Nullable Set<String> engineLanguages;

  /** Engines still listing their languages. */
  private final List<TextToSpeech> listing = new ArrayList<>();

  /** Whether the language list opens once the engines have listed their languages. */
  private boolean addWhenListed;

  private final Runnable endListing = this::endListing;

  private final SharedPreferences.OnSharedPreferenceChangeListener listener =
      (sharedPrefs, key) -> {
        if (LanguageVoices.isKey(key)) {
          handler.post(this::updateRows);
        }
      };

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_language_voices);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    Context context = requireContext();
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
    // SandFly's settings are in device protected storage; see VoiceProfilesFragment.
    getPreferenceManager().setStorageDeviceProtected();
    setPreferenceScreen(getPreferenceManager().createPreferenceScreen(context));
    updateRows();
    listEngineLanguages();
  }

  @Override
  public void onResume() {
    super.onResume();
    updateRows();
    prefs.registerOnSharedPreferenceChangeListener(listener);
  }

  @Override
  public void onPause() {
    super.onPause();
    prefs.unregisterOnSharedPreferenceChangeListener(listener);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    handler.removeCallbacks(endListing);
    addWhenListed = false;
    for (TextToSpeech engine : listing) {
      engine.shutdown();
    }
    listing.clear();
  }

  /** Lists the add row, a row for each language, and the language of each shared script. */
  private void updateRows() {
    if (!isAdded()) {
      return;
    }
    Context context = requireContext();
    PreferenceScreen screen = getPreferenceScreen();
    screen.removeAll();
    screen.addPreference(row(context, getString(R.string.title_pref_add_language), null, this::add));
    Map<String, String> names = VoiceProfileNames.names(context, prefs);
    List<Locale> languages = new ArrayList<>();
    languages.add(LanguageSwitch.voiceLanguage());
    for (String tag : LanguageVoices.languages(prefs)) {
      Locale language = Locale.forLanguageTag(tag);
      languages.add(language);
      String id = LanguageVoices.profileId(prefs, tag);
      String name = names.get(id);
      screen.addPreference(
          row(
              context,
              language.getDisplayName(),
              name != null ? name : getString(R.string.voice_profile_default),
              () -> chooseVoice(tag)));
    }
    for (Map.Entry<String, List<Locale>> shared :
        LanguageDetector.sharedScripts(languages).entrySet()) {
      screen.addPreference(scriptRow(context, shared.getKey(), shared.getValue()));
    }
  }

  private Preference scriptRow(Context context, String script, List<Locale> languages) {
    AccessibilitySuiteListPreference list = new AccessibilitySuiteListPreference(context);
    String title = getString(R.string.title_pref_script_language, scriptName(script));
    list.setKey(LanguageVoices.scriptKey(script));
    list.setTitle(title);
    list.setDialogTitle(title);
    CharSequence[] entries = new CharSequence[languages.size()];
    CharSequence[] values = new CharSequence[languages.size()];
    for (int i = 0; i < languages.size(); i++) {
      entries[i] = languages.get(i).getDisplayName();
      values[i] = languages.get(i).toLanguageTag();
    }
    list.setEntries(entries);
    list.setEntryValues(values);
    list.setIconSpaceReserved(false);
    // Saved only when chosen. Unset, text in the script is in the voice's language, listed first.
    list.setPersistent(false);
    String value = prefs.getString(LanguageVoices.scriptKey(script), null);
    list.setValue(value != null && list.findIndexOfValue(value) >= 0 ? value : (String) values[0]);
    list.setSummary("%s");
    list.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          prefs.edit().putString(LanguageVoices.scriptKey(script), (String) newValue).apply();
          return true;
        });
    return list;
  }

  /** The name of the script with ISO 15924 code {@code script}, such as "Latin". */
  private static String scriptName(String script) {
    String code =
        switch (script) {
          case "Kana" -> "Jpan";
          case "Hang" -> "Kore";
          default -> script;
        };
    try {
      String name = new Locale.Builder().setScript(code).build().getDisplayScript();
      return name.isEmpty() ? script : name;
    } catch (RuntimeException e) {
      return script;
    }
  }

  private void chooseVoice(String tag) {
    Context context = requireContext();
    Map<String, String> names = VoiceProfileNames.names(context, prefs);
    List<String> ids = new ArrayList<>();
    List<CharSequence> entries = new ArrayList<>();
    ids.add("");
    entries.add(getString(R.string.voice_profile_default));
    for (Map.Entry<String, String> name : names.entrySet()) {
      ids.add(name.getKey());
      entries.add(name.getValue());
    }
    int checked = Math.max(0, ids.indexOf(LanguageVoices.profileId(prefs, tag)));
    new AlertDialog.Builder(context)
        .setTitle(Locale.forLanguageTag(tag).getDisplayName())
        .setSingleChoiceItems(
            entries.toArray(new CharSequence[0]),
            checked,
            (dialog, which) -> {
              prefs.edit().putString(LanguageVoices.voiceKey(tag), ids.get(which)).apply();
              dialog.dismiss();
              updateRows();
            })
        .setNeutralButton(
            R.string.title_pref_remove_language,
            (dialog, which) -> {
              LanguageVoices.remove(prefs, tag);
              updateRows();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  /** Lists the languages of the installed engines not added yet, once the engines have said. */
  private void add() {
    if (engineLanguages == null) {
      addWhenListed = true;
      return;
    }
    Set<String> added = new HashSet<>();
    for (String tag : LanguageVoices.languages(prefs)) {
      added.add(Locale.forLanguageTag(tag).getLanguage());
    }
    Collator collator = Collator.getInstance();
    Map<String, String> byName = new TreeMap<>(collator);
    for (String language : engineLanguages) {
      if (!added.contains(language)) {
        byName.put(Locale.forLanguageTag(language).getDisplayName(), language);
      }
    }
    if (byName.isEmpty()) {
      return;
    }
    List<String> tags = new ArrayList<>(byName.values());
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.title_pref_add_language)
        .setItems(
            byName.keySet().toArray(new CharSequence[0]),
            (dialog, which) -> {
              LanguageVoices.add(prefs, tags.get(which));
              updateRows();
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  /** Asks every installed engine for its languages, for {@link #add}. */
  private void listEngineLanguages() {
    Context context = requireContext().getApplicationContext();
    List<String> engines = FailoverTextToSpeech.getInstalledTtsEngines(context.getPackageManager());
    Set<String> languages = new HashSet<>();
    Set<TextToSpeech> waiting = new HashSet<>();
    for (String engine : engines) {
      TextToSpeech[] connection = new TextToSpeech[1];
      try {
        connection[0] =
            new TextToSpeech(
                context,
                status ->
                    handler.post(
                        () -> {
                          TextToSpeech started = connection[0];
                          if (started == null || !waiting.remove(started)) {
                            return;
                          }
                          if (status == TextToSpeech.SUCCESS) {
                            try {
                              Set<Locale> available = started.getAvailableLanguages();
                              if (available != null) {
                                for (Locale locale : available) {
                                  if (!locale.getLanguage().isEmpty()) {
                                    languages.add(locale.getLanguage());
                                  }
                                }
                              }
                            } catch (RuntimeException e) {
                              // The engine stopped.
                            }
                          }
                          listing.remove(started);
                          started.shutdown();
                          if (waiting.isEmpty()) {
                            finishListing(languages);
                          }
                        }),
                engine);
      } catch (RuntimeException e) {
        continue;
      }
      waiting.add(connection[0]);
      listing.add(connection[0]);
    }
    if (waiting.isEmpty()) {
      finishListing(languages);
    } else {
      pendingLanguages = languages;
      handler.postDelayed(endListing, ENGINE_LANGUAGES_WAIT_MS);
    }
  }

  /** The languages listed so far, while engines are still listing them. */
  private @Nullable Set<String> pendingLanguages;

  /** Gives up on engines that haven't listed their languages in time. */
  private void endListing() {
    for (TextToSpeech engine : listing) {
      engine.shutdown();
    }
    listing.clear();
    Set<String> languages = pendingLanguages;
    finishListing(languages != null ? languages : new HashSet<>());
  }

  private void finishListing(Set<String> languages) {
    if (engineLanguages != null) {
      return;
    }
    handler.removeCallbacks(endListing);
    pendingLanguages = null;
    engineLanguages = languages;
    if (addWhenListed && isAdded()) {
      addWhenListed = false;
      add();
    }
  }

  private static Preference row(
      Context context, CharSequence title, @Nullable CharSequence summary, Runnable onClick) {
    Preference row = new AccessibilitySuitePreference(context);
    row.setTitle(title);
    row.setSummary(summary);
    row.setPersistent(false);
    row.setIconSpaceReserved(false);
    row.setOnPreferenceClickListener(
        preference -> {
          onClick.run();
          return true;
        });
    return row;
  }
}
