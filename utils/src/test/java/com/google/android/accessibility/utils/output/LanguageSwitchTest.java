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
import static org.junit.Assert.assertNull;

import com.google.android.accessibility.utils.output.LanguageSwitch.Run;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.Test;

public class LanguageSwitchTest {
  private static final Locale GERMAN = Locale.GERMANY;

  @Test
  public void bothOnFollowsTheText() {
    assertEquals(GERMAN, LanguageSwitch.localeToSpeak(GERMAN, null, Locale.UK, true, true));
    assertEquals(Locale.US, LanguageSwitch.localeToSpeak(Locale.US, null, Locale.UK, true, true));
  }

  @Test
  public void unmarkedTextStaysUnmarked() {
    assertNull(LanguageSwitch.localeToSpeak(null, null, Locale.UK, false, false));
  }

  @Test
  public void dialectsOffKeepsTheVoiceButSwitchesLanguage() {
    assertNull(LanguageSwitch.localeToSpeak(Locale.US, null, Locale.UK, true, false));
    assertEquals(GERMAN, LanguageSwitch.localeToSpeak(GERMAN, null, Locale.UK, true, false));
  }

  @Test
  public void languagesOffKeepsTheVoiceButSwitchesDialect() {
    assertNull(LanguageSwitch.localeToSpeak(GERMAN, null, Locale.UK, false, true));
    assertEquals(Locale.US, LanguageSwitch.localeToSpeak(Locale.US, null, Locale.UK, false, true));
  }

  @Test
  public void bothOffKeepsTheVoice() {
    assertNull(LanguageSwitch.localeToSpeak(GERMAN, null, Locale.UK, false, false));
    assertNull(LanguageSwitch.localeToSpeak(Locale.US, null, Locale.UK, false, false));
  }

  @Test
  public void languageWithoutCountryIsTheSameLanguage() {
    assertNull(LanguageSwitch.localeToSpeak(Locale.ENGLISH, null, Locale.UK, true, false));
  }

  @Test
  public void chosenLanguageIsAlwaysSpokenAndTakesOverFromTheVoice() {
    Locale us = Locale.US;
    assertEquals(us, LanguageSwitch.localeToSpeak(us, us, us, false, false));
    // Australian English is a dialect of the chosen US English, so stays in US English.
    Locale australian = Locale.forLanguageTag("en-AU");
    assertEquals(us, LanguageSwitch.localeToSpeak(australian, us, us, true, false));
    assertEquals(us, LanguageSwitch.localeToSpeak(GERMAN, us, us, false, true));
  }

  @Test
  public void markInTheVoicesOwnLanguageIsNotNeeded() {
    // Unmarked text is spoken in the voice's language, so a mark for it would only split the text.
    assertNull(LanguageSwitch.unlessVoiceLanguage(Locale.US, Locale.US));
    assertNull(
        LanguageSwitch.unlessVoiceLanguage(Locale.forLanguageTag("en-US-u-mu-celsius"), Locale.US));
    assertNull(LanguageSwitch.unlessVoiceLanguage(null, Locale.US));
  }

  @Test
  public void markInAnotherDialectOrLanguageIsKept() {
    // These still switch the voice, or not, by the switches.
    assertEquals(Locale.UK, LanguageSwitch.unlessVoiceLanguage(Locale.UK, Locale.US));
    assertEquals(Locale.ENGLISH, LanguageSwitch.unlessVoiceLanguage(Locale.ENGLISH, Locale.US));
    assertEquals(GERMAN, LanguageSwitch.unlessVoiceLanguage(GERMAN, Locale.US));
  }

  @Test
  public void runsSpokenInTheVoicesOwnLanguageLeaveNoMarks() {
    // British text, unmarked text and US text, with dialects off: all are spoken in the voice's
    // own language, so no marks are left to split the text at.
    List<Run> runs =
        Arrays.asList(new Run(0, 5, null), new Run(5, 12, null), new Run(12, 19, null));
    assertEquals(List.of(), LanguageSwitch.spokenRuns(runs));
  }

  @Test
  public void neighbouringRunsInOneLanguageJoin() {
    Locale us = Locale.US;
    List<Run> runs =
        Arrays.asList(
            new Run(0, 4, us), new Run(4, 9, us), new Run(9, 15, null), new Run(15, 20, GERMAN));
    assertEquals(
        List.of(new Run(0, 9, us), new Run(15, 20, GERMAN)), LanguageSwitch.spokenRuns(runs));
  }
}
