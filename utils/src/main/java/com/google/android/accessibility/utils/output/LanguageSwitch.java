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

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.LocaleSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Decides whether speech follows the language an app marks text as. Switching to another
 * language, such as German for a British English voice, and switching to another country's form
 * of the voice's own language, such as US English, are turned off separately. While a voice
 * profile is in use, all speech uses its voice, so speech never switches.
 */
public final class LanguageSwitch {

  /** A stretch of text, from {@code start} to {@code end}, in one language or none. */
  record Run(int start, int end, @Nullable Locale locale) {}

  private static volatile boolean switchLanguages = true;
  private static volatile boolean switchDialects = true;

  /** The language chosen from the language menu, or null to use the speech engine's own. */
  private static volatile @Nullable Locale chosenLanguage;

  /** The language unmarked text is spoken in. */
  private static volatile Locale voiceLanguage = Locale.getDefault();

  private LanguageSwitch() {}

  /** Sets whether speech switches to other languages, and to other forms of its own language. */
  static void setSwitches(boolean languages, boolean dialects) {
    switchLanguages = languages;
    switchDialects = dialects;
  }

  /** Sets the language chosen from the language menu, or null when it is reset. */
  public static void setChosenLanguage(@Nullable Locale language) {
    boolean changed = !Objects.equals(chosenLanguage, language);
    chosenLanguage = language;
    if (changed) {
      EmojiSpeech.onLanguageChanged();
    }
  }

  /** Sets the language the speech engine speaks unmarked text in. */
  static void setVoiceLanguage(Locale language) {
    boolean changed = !language.equals(voiceLanguage);
    voiceLanguage = language;
    if (changed) {
      EmojiSpeech.onLanguageChanged();
    }
  }

  /** Returns the language to speak text marked as {@code marked} in, by the switches. */
  static @Nullable Locale localeToSpeak(@Nullable Locale marked) {
    if (VoiceProfiles.isProfileActive()) {
      return null;
    }
    Locale chosen = chosenLanguage;
    return localeToSpeak(
        marked, chosen, chosen != null ? chosen : voiceLanguage, switchLanguages, switchDialects);
  }

  /**
   * Returns the language to speak text marked as {@code marked} in.
   *
   * @param marked the language the text is marked as, or null if it isn't marked
   * @param chosen the language chosen from the language menu, or null if none is
   * @param own the voice's language: {@code chosen}, or else the one unmarked text is spoken in
   * @return {@code marked} if speech should switch to it, otherwise {@code chosen}, which is null
   *     to speak in the usual voice
   */
  static @Nullable Locale localeToSpeak(
      @Nullable Locale marked,
      @Nullable Locale chosen,
      Locale own,
      boolean switchLanguages,
      boolean switchDialects) {
    if (marked == null || (switchLanguages && switchDialects) || marked.equals(chosen)) {
      return marked;
    }
    boolean sameLanguage = marked.getLanguage().equals(own.getLanguage());
    return (sameLanguage ? switchDialects : switchLanguages) ? marked : chosen;
  }

  /** Returns the language text marked as {@code marked} is spoken in, which may be the voice's. */
  static Locale spokenLanguage(@Nullable Locale marked) {
    Locale toSpeak = localeToSpeak(marked);
    if (toSpeak != null) {
      return toSpeak;
    }
    Locale chosen = chosenLanguage;
    return chosen != null ? chosen : voiceLanguage;
  }

  /** Splits {@code text} where its language marks change, into runs in the language marked. */
  static List<Run> runs(Spanned text) {
    int length = text.length();
    List<Run> runs = new ArrayList<>();
    for (int start = 0, end; start < length; start = end) {
      end = text.nextSpanTransition(start, length, LocaleSpan.class);
      // The first mark is the one the text splitter has always used where marks overlap.
      LocaleSpan[] here = text.getSpans(start, end, LocaleSpan.class);
      runs.add(new Run(start, end, here.length == 0 ? null : here[0].getLocale()));
    }
    return runs;
  }

  /**
   * Returns {@code text} marked with the languages speech will use. Text is split into separate
   * utterances wherever its language marks change, and each split adds a pause, so a change that
   * the switches turn off must not leave a mark behind, and neither must a mark in the voice's own
   * language, which unmarked text is spoken in anyway. Returns {@code text} itself when it has no
   * such marks, otherwise a copy with its other spans kept. With a voice profile in use, the copy
   * has no language marks at all.
   */
  static Spannable markSpokenLanguages(Spannable text) {
    int length = text.length();
    LocaleSpan[] marks = text.getSpans(0, length, LocaleSpan.class);
    if (marks.length == 0) {
      return text;
    }
    if (VoiceProfiles.isProfileActive()) {
      Spannable unmarked = new SpannableString(text);
      for (LocaleSpan mark : marks) {
        unmarked.removeSpan(mark);
      }
      return unmarked;
    }
    Locale voice = voiceLanguage;
    if (switchLanguages && switchDialects && !anyMarkIn(marks, voice)) {
      return text;
    }
    List<Run> marked = new ArrayList<>();
    for (Run run : runs(text)) {
      marked.add(
          new Run(run.start(), run.end(), unlessVoiceLanguage(localeToSpeak(run.locale()), voice)));
    }
    Spannable spoken = new SpannableString(text);
    for (LocaleSpan mark : marks) {
      spoken.removeSpan(mark);
    }
    for (Run run : spokenRuns(marked)) {
      spoken.setSpan(
          new LocaleSpan(run.locale()), run.start(), run.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    return spoken;
  }

  /**
   * Returns {@code locale}, or null when it is {@code voice}, the language that unmarked text is
   * spoken in, so that the text needs no mark. Unicode extensions, such as the units chosen in
   * regional preferences, don't change the voice and are not compared.
   */
  static @Nullable Locale unlessVoiceLanguage(@Nullable Locale locale, Locale voice) {
    return (locale != null && locale.stripExtensions().equals(voice.stripExtensions()))
        ? null
        : locale;
  }

  private static boolean anyMarkIn(LocaleSpan[] marks, Locale voice) {
    for (LocaleSpan mark : marks) {
      if (mark.getLocale() != null && unlessVoiceLanguage(mark.getLocale(), voice) == null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Joins neighbouring runs in the same language, and leaves out the runs in no language, which
   * are spoken in the voice's own.
   */
  static List<Run> spokenRuns(List<Run> runs) {
    List<Run> joined = new ArrayList<>();
    for (Run run : runs) {
      Run last = joined.isEmpty() ? null : joined.get(joined.size() - 1);
      boolean sameLanguage = last != null && Objects.equals(last.locale(), run.locale());
      if (sameLanguage && last.end() == run.start()) {
        joined.set(joined.size() - 1, new Run(last.start(), run.end(), run.locale()));
      } else {
        joined.add(run);
      }
    }
    joined.removeIf(run -> run.locale() == null);
    return joined;
  }
}
