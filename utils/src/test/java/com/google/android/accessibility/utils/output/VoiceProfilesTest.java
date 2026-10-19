/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.utils.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import com.google.android.accessibility.utils.output.VoiceProfiles.VoiceProfile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import org.junit.After;
import org.junit.Test;

public class VoiceProfilesTest {
  private static final String DEFAULT = "Backtalk default";
  private static final IntFunction<String> NUMBERED = number -> "Profile " + number;

  private final FakePrefs prefs = new FakePrefs();

  @After
  public void noProfileInUse() {
    VoiceProfiles.setActive(null);
  }

  private void moveReadingControl() {
    VoiceProfiles.moveReadingControl(
        prefs, "done", "shared", "voice", "selected", "OLD_VALUE", "NEW_VALUE");
  }

  @Test
  public void readingControlKeepsBothForAnyoneWithAProfile() {
    create("Reading", "com.example.eloquence");
    prefs.values.put("shared", true);
    prefs.values.put("selected", "OLD_VALUE");

    moveReadingControl();

    assertEquals(true, prefs.values.get("voice"));
    assertEquals(true, prefs.values.get("shared"));
    assertEquals("NEW_VALUE", prefs.values.get("selected"));
    assertEquals(true, prefs.values.get("done"));
  }

  @Test
  public void readingControlKeepsAnotherSelectedControl() {
    create("Reading", "com.example.eloquence");
    prefs.values.put("shared", true);
    prefs.values.put("selected", "SPEECH_RATE");

    moveReadingControl();

    assertEquals(true, prefs.values.get("voice"));
    assertEquals("SPEECH_RATE", prefs.values.get("selected"));
  }

  @Test
  public void readingControlStaysSpeechEngineWithNoProfile() {
    prefs.values.put("shared", true);
    prefs.values.put("selected", "OLD_VALUE");

    moveReadingControl();

    assertEquals(true, prefs.values.get("shared"));
    assertNull(prefs.values.get("voice"));
    assertEquals("OLD_VALUE", prefs.values.get("selected"));
    assertEquals(true, prefs.values.get("done"));
  }

  @Test
  public void readingControlOffStaysOff() {
    create("Reading", "com.example.eloquence");
    prefs.values.put("shared", false);

    moveReadingControl();

    assertNull(prefs.values.get("voice"));
    assertEquals(false, prefs.values.get("shared"));
  }

  @Test
  public void readingControlMovesOnlyOnce() {
    prefs.values.put("shared", true);
    moveReadingControl();
    create("Reading", "com.example.eloquence");

    moveReadingControl();

    assertEquals(true, prefs.values.get("shared"));
    assertNull(prefs.values.get("voice"));
  }

  @Test
  public void readingControlSurvivesDamagedSettings() {
    create("Reading", "com.example.eloquence");
    prefs.values.put("shared", "yes");

    moveReadingControl();

    assertEquals("yes", prefs.values.get("shared"));
    assertNull(prefs.values.get("voice"));
    assertEquals(true, prefs.values.get("done"));
  }

  @Test
  public void defaultIsInUseWithNoProfiles() {
    assertTrue(VoiceProfiles.ids(prefs).isEmpty());
    assertEquals("", VoiceProfiles.activeId(prefs));
    assertNull(VoiceProfiles.readActive(prefs));
  }

  @Test
  public void createdProfilesKeepTheirSettingsAndOrder() {
    String first = create("Reading", "com.example.eloquence");
    String second = create("Fast", "com.google.android.tts");
    assertEquals(Arrays.asList(first, second), VoiceProfiles.ids(prefs));

    VoiceProfile profile = VoiceProfiles.read(prefs, second);
    assertEquals("Fast", profile.name());
    assertEquals("com.google.android.tts", profile.engine());
    assertEquals("", profile.language());
    assertEquals("", profile.voice());
    assertEquals(80, profile.volume());
    assertEquals(1.5f, profile.rate(), 0f);
    assertEquals(0.9f, profile.pitch(), 0f);
    assertTrue(profile.phrases());
  }

  @Test
  public void newIdsAreNeverReused() {
    String first = create("One", "e");
    String second = create("Two", "e");
    VoiceProfiles.delete(prefs, second);
    String third = create("Three", "e");
    assertFalse(third.equals(second) || third.equals(first));
  }

  @Test
  public void deletingTheProfileInUseGoesBackToTheDefault() {
    String id = create("Reading", "e");
    prefs.edit().putString(VoiceProfiles.PREF_ACTIVE, id).apply();
    assertEquals(id, VoiceProfiles.readActive(prefs).id());

    VoiceProfiles.delete(prefs, id);
    assertTrue(VoiceProfiles.ids(prefs).isEmpty());
    assertNull(VoiceProfiles.readActive(prefs));
    assertNull(prefs.values.get(VoiceProfiles.key(id, VoiceProfiles.NAME)));
  }

  @Test
  public void adjacentProfilesWrapAroundThroughTheDefault() {
    assertEquals("", VoiceProfiles.adjacentId(prefs, /* isNext= */ true));

    String one = create("One", "e");
    String two = create("Two", "e");
    assertEquals(one, VoiceProfiles.adjacentId(prefs, /* isNext= */ true));
    assertEquals(two, VoiceProfiles.adjacentId(prefs, /* isNext= */ false));

    prefs.edit().putString(VoiceProfiles.PREF_ACTIVE, two).apply();
    assertEquals("", VoiceProfiles.adjacentId(prefs, /* isNext= */ true));
    assertEquals(one, VoiceProfiles.adjacentId(prefs, /* isNext= */ false));
  }

  @Test
  public void orderKeepsEveryProfileOnce() {
    String one = create("One", "e");
    String two = create("Two", "e");
    String three = create("Three", "e");
    VoiceProfiles.setOrder(prefs, Arrays.asList(three, "99", one, three));
    assertEquals(Arrays.asList(three, one, two), VoiceProfiles.ids(prefs));
  }

  @Test
  public void damagedSettingsGiveDefaultsInsteadOfCrashing() {
    String id = create("Reading", "e");
    prefs.values.put(VoiceProfiles.key(id, VoiceProfiles.NAME), Boolean.TRUE);
    prefs.values.put(VoiceProfiles.key(id, VoiceProfiles.PHRASES), "yes");
    prefs.values.put(VoiceProfiles.key(id, VoiceProfiles.RATE), "NaN");
    prefs.values.put(VoiceProfiles.key(id, VoiceProfiles.PITCH), "0");
    prefs.values.put(VoiceProfiles.key(id, VoiceProfiles.VOLUME), "5000");
    prefs.values.put(VoiceProfiles.PREF_ACTIVE, 7);

    VoiceProfile profile = VoiceProfiles.read(prefs, id);
    assertEquals("", profile.name());
    assertFalse(profile.phrases());
    assertEquals(1f, profile.rate(), 0f);
    assertEquals(0.2f, profile.pitch(), 0f);
    assertEquals(100, profile.volume());
    assertEquals("", VoiceProfiles.activeId(prefs));
  }

  @Test
  public void blankNamesAreNeverSaved() {
    String id = create("Reading", "e");
    assertFalse(VoiceProfiles.rename(prefs, id, "", DEFAULT));
    assertFalse(VoiceProfiles.rename(prefs, id, "   ", DEFAULT));
    assertEquals("Reading", VoiceProfiles.read(prefs, id).name());
    assertTrue(VoiceProfiles.rename(prefs, id, "  Books ", DEFAULT));
    assertEquals("Books", VoiceProfiles.read(prefs, id).name());
  }

  @Test
  public void blankNamesGetNumberedNamesNoOtherProfileHas() {
    String blank = create("Reading", "e");
    create("Profile 1", "e");
    String otherBlank = create("Fast", "e");
    create("Profile 3", "e");
    prefs.values.put(VoiceProfiles.key(blank, VoiceProfiles.NAME), "");
    prefs.values.put(VoiceProfiles.key(otherBlank, VoiceProfiles.NAME), " ");

    Map<String, String> names = VoiceProfiles.names(prefs, DEFAULT, NUMBERED);
    assertEquals("Profile 2", names.get(blank));
    assertEquals("Profile 4", names.get(otherBlank));
    assertEquals("Profile 5", VoiceProfiles.unusedName(prefs, DEFAULT, NUMBERED));

    // Reading names doesn't save them; saveNames does.
    assertEquals("", VoiceProfiles.read(prefs, blank).name());
    VoiceProfiles.saveNames(prefs, DEFAULT, NUMBERED);
    assertEquals("Profile 2", VoiceProfiles.read(prefs, blank).name());
    assertEquals("Profile 4", VoiceProfiles.read(prefs, otherBlank).name());
  }

  @Test
  public void namesTakenByAnotherProfileOrTheDefaultAreRefused() {
    String reading = create("Reading", "e");
    String fast = create("Fast", "e");
    assertTrue(VoiceProfiles.isNameTaken(prefs, fast, " reading ", DEFAULT));
    assertTrue(VoiceProfiles.isNameTaken(prefs, "", "READING", DEFAULT));
    assertTrue(VoiceProfiles.isNameTaken(prefs, fast, "backtalk default", DEFAULT));
    assertFalse(VoiceProfiles.isNameTaken(prefs, reading, "Reading", DEFAULT));
    assertFalse(VoiceProfiles.isNameTaken(prefs, "", "Books", DEFAULT));

    assertFalse(VoiceProfiles.rename(prefs, fast, "Reading", DEFAULT));
    assertFalse(VoiceProfiles.rename(prefs, fast, "Backtalk Default", DEFAULT));
    assertEquals("Fast", VoiceProfiles.read(prefs, fast).name());
    assertTrue(VoiceProfiles.rename(prefs, reading, "READING", DEFAULT));
    assertEquals("READING", VoiceProfiles.read(prefs, reading).name());
  }

  @Test
  public void duplicateNamesGiveTheLowerProfileANameOfItsOwn() {
    String first = create("Reading", "e");
    String second = create("Fast", "e");
    String third = create("Calm", "e");
    prefs.values.put(VoiceProfiles.key(second, VoiceProfiles.NAME), "reading ");
    prefs.values.put(VoiceProfiles.key(third, VoiceProfiles.NAME), DEFAULT);

    Map<String, String> names = VoiceProfiles.names(prefs, DEFAULT, NUMBERED);
    assertEquals("Reading", names.get(first));
    assertEquals("Profile 1", names.get(second));
    assertEquals("Profile 2", names.get(third));
    VoiceProfiles.saveNames(prefs, DEFAULT, NUMBERED);
    assertEquals("Reading", VoiceProfiles.read(prefs, first).name());
    assertEquals("Profile 1", VoiceProfiles.read(prefs, second).name());
    assertEquals("Profile 2", VoiceProfiles.read(prefs, third).name());
  }

  @Test
  public void duplicateIdsCountOnce() {
    String first = create("Reading", "e");
    String second = create("Fast", "e");
    prefs.values.put(VoiceProfiles.PREF_IDS, first + "," + second + "," + first);
    assertEquals(Arrays.asList(first, second), VoiceProfiles.ids(prefs));

    VoiceProfiles.delete(prefs, first);
    assertEquals(Arrays.asList(second), VoiceProfiles.ids(prefs));
    assertEquals(second, prefs.values.get(VoiceProfiles.PREF_IDS));
  }

  @Test
  public void longNamesAreCut() {
    String id = create("x".repeat(500), "e");
    assertEquals(VoiceProfiles.MAX_NAME_LENGTH, VoiceProfiles.read(prefs, id).name().length());
  }

  @Test
  public void aMissingProfileInUseMeansTheDefault() {
    prefs.edit().putString(VoiceProfiles.PREF_ACTIVE, "7").apply();
    assertEquals("", VoiceProfiles.activeId(prefs));
  }

  @Test
  public void allProfileSettingsAreProfileKeys() {
    assertTrue(VoiceProfiles.isProfileKey(VoiceProfiles.PREF_ACTIVE));
    assertTrue(VoiceProfiles.isProfileKey(VoiceProfiles.PREF_IDS));
    assertTrue(VoiceProfiles.isProfileKey(VoiceProfiles.key("1", VoiceProfiles.RATE)));
    assertFalse(VoiceProfiles.isProfileKey("pref_tts_engine"));
    assertFalse(VoiceProfiles.isProfileKey("pref_voice_profile_menu_setting_key"));
    assertFalse(VoiceProfiles.isProfileKey(null));
  }

  @Test
  public void aProfileInUseNeverSwitchesLanguage() {
    LanguageSwitch.setSwitches(true, true);
    assertEquals(Locale.GERMANY, LanguageSwitch.localeToSpeak(Locale.GERMANY));
    VoiceProfiles.setActive(VoiceProfiles.read(prefs, create("Reading", "e")));
    assertNull(LanguageSwitch.localeToSpeak(Locale.GERMANY));
    LanguageSwitch.setChosenLanguage(Locale.FRANCE);
    assertNull(LanguageSwitch.localeToSpeak(null));
    LanguageSwitch.setChosenLanguage(null);
  }

  @Test
  public void sameVoiceComparesEngineLanguageAndVoice() {
    VoiceProfile profile = new VoiceProfile("1", "A", "e", "en-GB", "v", 100, 1f, 1f, false);
    assertTrue(
        profile.sameVoiceAs(new VoiceProfile("2", "B", "e", "en-GB", "v", 50, 2f, 2f, true)));
    assertFalse(
        profile.sameVoiceAs(new VoiceProfile("1", "A", "e", "en-GB", "w", 100, 1f, 1f, false)));
    assertFalse(profile.sameVoiceAs(null));
  }

  private String create(String name, String engine) {
    return VoiceProfiles.create(prefs, name, engine, "80", "1.5", "0.9", true);
  }

  /** Settings in memory, saved at once. */
  private static final class FakePrefs implements SharedPreferences {
    final Map<String, Object> values = new HashMap<>();

    @Override
    public Map<String, ?> getAll() {
      return values;
    }

    @Override
    public String getString(String key, String defValue) {
      Object value = values.get(key);
      return value == null ? defValue : (String) value;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getInt(String key, int defValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getLong(String key, long defValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public float getFloat(String key, float defValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
      Object value = values.get(key);
      return value == null ? defValue : (Boolean) value;
    }

    @Override
    public boolean contains(String key) {
      return values.containsKey(key);
    }

    @Override
    public Editor edit() {
      return new Editor() {
        @Override
        public Editor putString(String key, String value) {
          values.put(key, value);
          return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> value) {
          throw new UnsupportedOperationException();
        }

        @Override
        public Editor putInt(String key, int value) {
          throw new UnsupportedOperationException();
        }

        @Override
        public Editor putLong(String key, long value) {
          throw new UnsupportedOperationException();
        }

        @Override
        public Editor putFloat(String key, float value) {
          throw new UnsupportedOperationException();
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
          values.put(key, value);
          return this;
        }

        @Override
        public Editor remove(String key) {
          values.remove(key);
          return this;
        }

        @Override
        public Editor clear() {
          values.clear();
          return this;
        }

        @Override
        public boolean commit() {
          return true;
        }

        @Override
        public void apply() {}
      };
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {}

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {}
  }
}
