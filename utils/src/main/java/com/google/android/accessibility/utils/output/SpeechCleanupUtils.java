/*
 * Copyright (C) 2011 Google Inc.
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

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.LocaleSpan;
import android.text.style.TtsSpan;
import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;
import com.google.android.accessibility.utils.EmojiUtils;
import com.google.android.accessibility.utils.R;
import com.google.android.accessibility.utils.SpannableUtils;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

/** Utilities for cleaning up speech text. */
public final class SpeechCleanupUtils {
  /** The regular expression used to match consecutive identical characters */
  // Double escaping of regex characters is required. "\\1\\1\\1" refers to the
  // third capturing group between the outer nesting of "[]"s.
  // When the text contains any repeat of special characters, the collapse function applies.
  private static final String CONSECUTIVE_CHARACTER_REGEX =
      "("
          + "[\\-\\\\/|!@#$%^&*\\(\\)=_+\\[\\]\\{\\}.?;'\":<>\\u2022\\u25E6\\u2023]" // punctuation and symbols
          + "|"
          + EmojiUtils.EMOJI_REGEX // Emoji
          + ")"
          + "\\1\\1\\1+"; // Backreference \1 requires the identical character to repeat 3+ times

  /** The Pattern used to match consecutive identical characters */
  private static final Pattern CONSECUTIVE_CHARACTER_PATTERN =
      Pattern.compile(CONSECUTIVE_CHARACTER_REGEX);

  /** Speak punctuation verbosity levels. */
  @IntDef({ALL, MOST, SOME, NONE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface PunctuationVerbosity {}

  /**
   * Verbosity level of the punctuation and symbol that the user always hear. If the level is lower
   * than TalkBack verbosity preference, the punctuation and symbol will not be announced.
   */
  public static final int ALL = 0;

  /**
   * Verbosity level of the punctuation and symbol that user usually hears. If the level is lower
   * than TalkBack verbosity preference, the punctuation and symbol will not be announced.
   */
  public static final int MOST = 1;

  /**
   * Verbosity level of the punctuation and symbol that user sometimes hears. If the level is lower
   * than TalkBack verbosity preference, the punctuation and symbol will not be announced.
   *
   * <p>Note: TalkBack speak punctuation verbosity preference in Some level will let TTS engine
   * handle speaking punctuation in SOME level.
   */
  public static final int SOME = 2;

  /**
   * Speak punctuation verbosity in which no punctuation or symbol is said: {@link
   * #withPunctuationHidden} puts spaces in their place for the speech engine. A character said on
   * its own, such as when moving by character, is still named.
   */
  public static final int NONE = 3;

  /**
   * Map containing string to speech conversions and the verbosity level of symbol and punctuation .
   */
  private static final ImmutableMap<Character, Pair<Integer, Integer>>
      TALKBACK_PUNCTUATION_AND_SYMBOL =
          ImmutableMap.<Character, Pair<Integer, Integer>>builder()
              .put('&', new Pair<>(R.string.symbol_ampersand, MOST))
              .put('<', new Pair<>(R.string.symbol_angle_bracket_left, MOST))
              .put('>', new Pair<>(R.string.symbol_angle_bracket_right, MOST))
              .put('\'', new Pair<>(R.string.symbol_apostrophe, ALL))
              .put('*', new Pair<>(R.string.symbol_asterisk, MOST))
              .put('@', new Pair<>(R.string.symbol_at_sign, MOST))
              .put('\\', new Pair<>(R.string.symbol_backslash, MOST))
              .put('•', new Pair<>(R.string.symbol_bullet, MOST))
              // Other bullets of nested lists, which TTS engines can leave silent.
              .put('◦', new Pair<>(R.string.symbol_bullet, MOST))
              .put('‣', new Pair<>(R.string.symbol_bullet, MOST))
              .put('^', new Pair<>(R.string.symbol_caret, MOST))
              .put('¢', new Pair<>(R.string.symbol_cent, ALL))
              .put(':', new Pair<>(R.string.symbol_colon, MOST))
              .put(',', new Pair<>(R.string.symbol_comma, ALL))
              .put('©', new Pair<>(R.string.symbol_copyright, MOST))
              .put('{', new Pair<>(R.string.symbol_curly_bracket_left, MOST))
              .put('}', new Pair<>(R.string.symbol_curly_bracket_right, MOST))
              .put('°', new Pair<>(R.string.symbol_degree, MOST))
              .put('÷', new Pair<>(R.string.symbol_division, MOST))
              .put('…', new Pair<>(R.string.symbol_ellipsis, ALL))
              .put('—', new Pair<>(R.string.symbol_em_dash, MOST))
              .put('–', new Pair<>(R.string.symbol_en_dash, MOST))
              .put('!', new Pair<>(R.string.symbol_exclamation_mark, ALL))
              .put('`', new Pair<>(R.string.symbol_grave_accent, MOST))
              .put('-', new Pair<>(R.string.symbol_hyphen_minus, ALL))
              .put('„', new Pair<>(R.string.symbol_low_double_quote, MOST))
              .put('×', new Pair<>(R.string.symbol_multiplication, MOST))
              .put('\n', new Pair<>(R.string.symbol_new_line, ALL))
              .put('¶', new Pair<>(R.string.symbol_paragraph_mark, MOST))
              .put('(', new Pair<>(R.string.symbol_parenthesis_left, MOST))
              .put(')', new Pair<>(R.string.symbol_parenthesis_right, MOST))
              .put('%', new Pair<>(R.string.symbol_percent, MOST))
              .put('.', new Pair<>(R.string.symbol_period, ALL))
              .put('π', new Pair<>(R.string.symbol_pi, MOST))
              .put('#', new Pair<>(R.string.symbol_pound, ALL))
              // Currency
              .put('$', new Pair<>(R.string.symbol_dollar_sign, ALL))
              .put('€', new Pair<>(R.string.symbol_euro, ALL))
              .put('£', new Pair<>(R.string.symbol_pound_sterling, ALL))
              .put('¥', new Pair<>(R.string.symbol_yen, ALL))
              .put('₱', new Pair<>(R.string.symbol_currency_peso, ALL))
              .put('₹', new Pair<>(R.string.symbol_rupee, MOST))
              .put('₫', new Pair<>(R.string.symbol_currency_dong, ALL))
              .put('¤', new Pair<>(R.string.symbol_currency_sign, ALL))
              .put('?', new Pair<>(R.string.symbol_question_mark, ALL))
              .put('"', new Pair<>(R.string.symbol_quotation_mark, MOST))
              .put('®', new Pair<>(R.string.symbol_registered_trademark, MOST))
              .put(';', new Pair<>(R.string.symbol_semicolon, MOST))
              .put('/', new Pair<>(R.string.symbol_slash, MOST))
              .put(' ', new Pair<>(R.string.symbol_space, ALL))
              .put('\u00a0', new Pair<>(R.string.symbol_space, ALL))
              .put('[', new Pair<>(R.string.symbol_square_bracket_left, MOST))
              .put(']', new Pair<>(R.string.symbol_square_bracket_right, MOST))
              .put('√', new Pair<>(R.string.symbol_square_root, MOST))
              .put('™', new Pair<>(R.string.symbol_trademark, MOST))
              .put('✓', new Pair<>(R.string.symbol_check_mark, MOST))
              .put('_', new Pair<>(R.string.symbol_underscore, MOST))
              .put('|', new Pair<>(R.string.symbol_vertical_bar, MOST))
              .put('¬', new Pair<>(R.string.symbol_not_sign, MOST))
              .put('¦', new Pair<>(R.string.symbol_broken_bar, MOST))
              .put('µ', new Pair<>(R.string.symbol_micro_sign, MOST))
              .put('≈', new Pair<>(R.string.symbol_almost_equals, MOST))
              .put('≠', new Pair<>(R.string.symbol_not_equals, MOST))
              .put('§', new Pair<>(R.string.symbol_section_sign, MOST))
              .put('↑', new Pair<>(R.string.symbol_upwards_arrow, MOST))
              .put('←', new Pair<>(R.string.symbol_leftwards_arrow, MOST))
              .put('→', new Pair<>(R.string.symbol_rightwards_arrow, MOST))
              .put('↓', new Pair<>(R.string.symbol_downwards_arrow, MOST))
              .put('♥', new Pair<>(R.string.symbol_black_heart, MOST))
              .put('~', new Pair<>(R.string.symbol_tilde, MOST))
              .put('=', new Pair<>(R.string.symbol_equal, MOST))
              .put('￦', new Pair<>(R.string.symbol_won, MOST))
              .put('₩', new Pair<>(R.string.symbol_won_sign, MOST))
              .put('※', new Pair<>(R.string.symbol_reference, MOST))
              .put('☆', new Pair<>(R.string.symbol_white_star, MOST))
              .put('★', new Pair<>(R.string.symbol_black_star, MOST))
              .put('♡', new Pair<>(R.string.symbol_white_heart, MOST))
              .put('○', new Pair<>(R.string.symbol_white_circle, MOST))
              .put('●', new Pair<>(R.string.symbol_black_circle, MOST))
              .put('⊙', new Pair<>(R.string.symbol_solar, MOST))
              .put('◎', new Pair<>(R.string.symbol_bullseye, MOST))
              .put('♧', new Pair<>(R.string.symbol_white_club_suit, MOST))
              .put('♤', new Pair<>(R.string.symbol_white_spade_suit, MOST))
              .put('☜', new Pair<>(R.string.symbol_white_left_pointing_index, MOST))
              .put('☞', new Pair<>(R.string.symbol_white_right_pointing_index, MOST))
              .put('◐', new Pair<>(R.string.symbol_circle_left_half_black, MOST))
              .put('◑', new Pair<>(R.string.symbol_circle_right_half_black, MOST))
              .put('□', new Pair<>(R.string.symbol_white_square, MOST))
              .put('■', new Pair<>(R.string.symbol_black_square, MOST))
              .put('△', new Pair<>(R.string.symbol_white_up_pointing_triangle, MOST))
              .put('▽', new Pair<>(R.string.symbol_white_down_pointing_triangle, MOST))
              .put('◁', new Pair<>(R.string.symbol_white_left_pointing_triangle, MOST))
              .put('▷', new Pair<>(R.string.symbol_white_right_pointing_triangle, MOST))
              .put('◇', new Pair<>(R.string.symbol_white_diamond, MOST))
              .put('♩', new Pair<>(R.string.symbol_quarter_note, MOST))
              .put('♪', new Pair<>(R.string.symbol_eighth_note, MOST))
              .put('♬', new Pair<>(R.string.symbol_beamed_sixteenth_note, MOST))
              .put('♀', new Pair<>(R.string.symbol_female, MOST))
              .put('♂', new Pair<>(R.string.symbol_male, MOST))
              .put('【', new Pair<>(R.string.symbol_left_black_lenticular_bracket, MOST))
              .put('】', new Pair<>(R.string.symbol_right_black_lenticular_bracket, MOST))
              .put('「', new Pair<>(R.string.symbol_left_corner_bracket, MOST))
              .put('」', new Pair<>(R.string.symbol_right_corner_bracket, MOST))
              .put('±', new Pair<>(R.string.symbol_plus_minus_sign, MOST))
              .put('ℓ', new Pair<>(R.string.symbol_liter, MOST))
              .put('℃', new Pair<>(R.string.symbol_celsius_degree, MOST))
              .put('℉', new Pair<>(R.string.symbol_fahrenheit_degree, MOST))
              .put('≒', new Pair<>(R.string.symbol_approximately_equals, MOST))
              .put('∫', new Pair<>(R.string.symbol_integral, MOST))
              .put('⟨', new Pair<>(R.string.symbol_mathematical_left_angle_bracket, MOST))
              .put('⟩', new Pair<>(R.string.symbol_mathematical_right_angle_bracket, MOST))
              .put('〒', new Pair<>(R.string.symbol_postal_mark, MOST))
              .put('▲', new Pair<>(R.string.symbol_black_triangle_pointing_up, MOST))
              .put('▼', new Pair<>(R.string.symbol_black_triangle_pointing_down, MOST))
              .put('♦', new Pair<>(R.string.symbol_black_suit_of_diamonds, MOST))
              .put('◆', new Pair<>(R.string.symbol_black_suit_of_diamonds, MOST))
              .put('･', new Pair<>(R.string.symbol_halfwidth_katakana_middle_dot, MOST))
              .put('▪', new Pair<>(R.string.symbol_black_smallsquare, MOST))
              .put('《', new Pair<>(R.string.symbol_left_angle_bracket, MOST))
              .put('》', new Pair<>(R.string.symbol_right_angle_bracket, MOST))
              .put('¡', new Pair<>(R.string.symbol_inverted_exclamation_mark, MOST))
              .put('¿', new Pair<>(R.string.symbol_inverted_question_mark, ALL))
              // Full-width
              .put('，', new Pair<>(R.string.symbol_full_width_comma, ALL))
              .put('！', new Pair<>(R.string.symbol_full_width_exclamation_mark, ALL))
              .put('。', new Pair<>(R.string.symbol_full_width_ideographic_full_stop, MOST))
              .put('？', new Pair<>(R.string.symbol_full_width_question_mark, ALL))
              .put('·', new Pair<>(R.string.symbol_middle_dot, MOST))
              .put('”', new Pair<>(R.string.symbol_right_double_quotation_mark, MOST))
              .put('、', new Pair<>(R.string.symbol_ideographic_comma, ALL))
              .put('：', new Pair<>(R.string.symbol_full_width_colon, MOST))
              .put('；', new Pair<>(R.string.symbol_full_width_semicolon, MOST))
              .put('＆', new Pair<>(R.string.symbol_full_width_ampersand, MOST))
              .put('＾', new Pair<>(R.string.symbol_full_width_circumflex, MOST))
              .put('～', new Pair<>(R.string.symbol_full_width_tilde, MOST))
              .put('“', new Pair<>(R.string.symbol_left_double_quotation_mark, MOST))
              .put('（', new Pair<>(R.string.symbol_full_width_left_parenthesis, MOST))
              .put('）', new Pair<>(R.string.symbol_full_width_right_parenthesis, MOST))
              .put('＊', new Pair<>(R.string.symbol_full_width_asterisk, MOST))
              .put('＿', new Pair<>(R.string.symbol_full_width_underscore, MOST))
              .put('’', new Pair<>(R.string.symbol_right_single_quotation_mark, MOST))
              .put('｛', new Pair<>(R.string.symbol_full_width_left_curly_bracket, MOST))
              .put('｝', new Pair<>(R.string.symbol_full_width_right_curly_bracket, MOST))
              .put('＜', new Pair<>(R.string.symbol_full_width_less_than_sign, MOST))
              .put('＞', new Pair<>(R.string.symbol_full_width_greater_than_sign, MOST))
              .put('‘', new Pair<>(R.string.symbol_left_single_quotation_mark, MOST))
              // Arabic diacritical marks.
              .put('\u064e', new Pair<>(R.string.symbol_fatha, MOST))
              .put('\u0650', new Pair<>(R.string.symbol_kasra, MOST))
              .put('\u064f', new Pair<>(R.string.symbol_damma, MOST))
              .put('\u064b', new Pair<>(R.string.symbol_fathatan, MOST))
              .put('\u064d', new Pair<>(R.string.symbol_kasratan, MOST))
              .put('\u064c', new Pair<>(R.string.symbol_dammatan, MOST))
              .put('\u0651', new Pair<>(R.string.symbol_shadda, MOST))
              .put('\u0652', new Pair<>(R.string.symbol_sukun, MOST))
              .buildOrThrow();

  // TODO fine tune the parameters later
  private static final int SHORTEN_TEXT_LENGTH_THRESHOLD = 100;
  private static final int SHORTEN_TEXT_OFFSET = 20;

  /**
   * Cleans up text for speech. Converts symbols to their spoken equivalents.
   *
   * @param context The context used to resolve string resources.
   * @param text The text to clean up.
   * @return Cleaned up text, or null if text is null.
   */
  public static @PolyNull CharSequence cleanUp(Context context, @PolyNull CharSequence text) {
    if (text != null) {
      CharSequence textAfterTrim = SpannableUtils.trimText(text);
      int trimmedLength = textAfterTrim.length();
      if (trimmedLength == 1) {
        String textAfterCleanUp = getCleanValueFor(context, textAfterTrim.charAt(0));
        // Return the text as it is if it remains the same after clean up so
        // that any Span information is not lost
        if (TextUtils.equals(textAfterCleanUp, textAfterTrim)) {
          return textAfterTrim;
        }
        // Retaining Spans that might have got stripped during cleanUp
        return retainSpans(context, text, textAfterCleanUp);
      } else if (trimmedLength == 0 && text.length() > 0) {
        // For example, just spaces.
        String textAfterCleanUp = getCleanValueFor(context, text.toString().charAt(0));
        // Retaining Spans that might have got stripped during cleanUp
        return retainSpans(context, text, textAfterCleanUp);
      }
    }
    return text;
  }

  /**
   * Returns {@code textAfterCleanUp}, the name of the character {@code text} is, with the spans of
   * {@code text}. The name is in the language of {@code context}, so a language mark of the
   * character, such as a keyboard key's, is replaced by a mark in that language.
   */
  private static CharSequence retainSpans(
      Context context, CharSequence text, CharSequence textAfterCleanUp) {
    if (text instanceof Spannable) {
      Spannable spannable = (Spannable) text;
      Object[] spans = spannable.getSpans(0, text.length(), Object.class);
      if (spans.length != 0) {
        SpannableString ss = new SpannableString(textAfterCleanUp);
        boolean marked = false;
        for (Object span : spans) {
          if (span instanceof LocaleSpan) {
            marked = true;
          } else {
            ss.setSpan(span, 0, ss.length(), 0);
          }
        }
        if (marked) {
          Locale language = context.getResources().getConfiguration().getLocales().get(0);
          ss.setSpan(new LocaleSpan(language), 0, ss.length(), 0);
        }
        return ss;
      }
    }
    return textAfterCleanUp;
  }

  /**
   * Collapses repeated consecutive characters in a CharSequence by matching against {@link
   * #CONSECUTIVE_CHARACTER_REGEX}.
   *
   * @param context Context for retrieving resources
   * @param text The text to process
   * @return The text with consecutive identical characters collapsed
   */
  @VisibleForTesting
  static @Nullable CharSequence collapseRepeatedCharacters(
      Context context, @Nullable CharSequence text) {
    if (TextUtils.isEmpty(text)) {
      return null;
    }

    // TODO: Add tests
    Matcher matcher = CONSECUTIVE_CHARACTER_PATTERN.matcher(text);
    SpannableString spannable = new SpannableString(text);
    while (matcher.find()) {
      final String replacement;
      int charUnicode = matcher.group().codePointAt(0);
      String unicodeString = new String(Character.toChars(charUnicode));
      if (EmojiUtils.findEmoji(unicodeString).find()) {
        // Divide length by 2 because an Emoji UTF-16 has two-char length.
        int len = matcher.group().length() / 2;
        // TODO: Add emoji postfix feedback for repeated emojis.
        replacement = context.getString(R.string.character_collapse_template, len, unicodeString);
      } else {
        replacement =
            context.getString(
                R.string.character_collapse_template,
                matcher.group().length(),
                getCleanValueFor(context, matcher.group().charAt(0)));
      }
      final int matchFromIndex = matcher.end();

      // Add TtsSpan for the collapsed text. Then "copy last spoken phrase" action would provide the
      // raw text that is useful for URL link.
      TtsSpan ttsSpan = new TtsSpan.TextBuilder(replacement).build();
      spannable.setSpan(ttsSpan, matcher.start(), matcher.end(), SPAN_EXCLUSIVE_EXCLUSIVE);

      matcher = CONSECUTIVE_CHARACTER_PATTERN.matcher(text);
      matcher.region(matchFromIndex, text.length());
    }

    return spannable;
  }

  /**
   * Returns the result of {@code collapseRepeatedCharacters} and {@code cleanUp}.
   *
   * @param context Context for retrieving resources
   * @param text The text to process
   * @param countRepeatedSymbols If the text should count repeated symbols
   */
  public static @Nullable CharSequence collapseRepeatedCharactersAndCleanUp(
      Context context, @Nullable CharSequence text, boolean countRepeatedSymbols) {
    return countRepeatedSymbols
        ? cleanUp(context, collapseRepeatedCharacters(context, text))
        : cleanUp(context, text);
  }

  /** The text the symbol dictionary says a character as, in place of its name. */
  public interface CharacterReplacement {
    /** The text {@code key} is said as, or null if it keeps its name. Never blank. */
    @Nullable String replacementFor(char key);
  }

  private static volatile @Nullable CharacterReplacement characterReplacement;

  /**
   * Sets the text that characters are named with in place of their names, such as when moving by
   * character or deleting one. Null when the symbol dictionary is empty.
   */
  public static void setCharacterReplacement(@Nullable CharacterReplacement replacement) {
    characterReplacement = replacement;
  }

  private static volatile int spokenPunctuationVerbosity = ALL;

  /** Sets the punctuation verbosity that {@link #isHiddenByVerbosity} compares against. */
  public static void setPunctuationVerbosity(@PunctuationVerbosity int verbosity) {
    spokenPunctuationVerbosity = verbosity;
  }

  /**
   * Whether {@code key} is a punctuation or symbol that the punctuation verbosity leaves out of
   * speech, so that nothing should say it, not even a dictionary entry.
   */
  public static boolean isHiddenByVerbosity(char key) {
    Pair<Integer, Integer> pair = TALKBACK_PUNCTUATION_AND_SYMBOL.get(key);
    if (pair != null) {
      return key != ' ' && pair.second < spokenPunctuationVerbosity;
    }
    return spokenPunctuationVerbosity == NONE && isPunctuationOrSymbol(key);
  }

  /** Whether the punctuation verbosity is {@link #NONE}. */
  public static boolean hidesAllPunctuation() {
    return spokenPunctuationVerbosity == NONE;
  }

  /** Punctuation that joins the letters or digits on either side of it into one word. */
  private static final String WORD_JOINERS = "'\u2019\u02bc-\u2010\u2011";

  /** Punctuation that joins the digits on either side of it into one number, date or time. */
  private static final String NUMBER_JOINERS = ".,:/";

  /**
   * Punctuation that engines pause for and don't say when it ends a word, kept for the pause and
   * the intonation of a question.
   */
  private static final String PAUSE_PUNCTUATION =
      ".,!?;:\u2026\u3002\uff0c\uff01\uff1f\u3001\uff1b\uff1a";

  /** Whether {@code c} is a punctuation mark or a symbol, by its Unicode category. */
  public static boolean isPunctuationOrSymbol(char c) {
    switch (Character.getType(c)) {
      case Character.CONNECTOR_PUNCTUATION:
      case Character.DASH_PUNCTUATION:
      case Character.START_PUNCTUATION:
      case Character.END_PUNCTUATION:
      case Character.INITIAL_QUOTE_PUNCTUATION:
      case Character.FINAL_QUOTE_PUNCTUATION:
      case Character.OTHER_PUNCTUATION:
      case Character.MATH_SYMBOL:
      case Character.CURRENCY_SYMBOL:
      case Character.MODIFIER_SYMBOL:
      case Character.OTHER_SYMBOL:
        return true;
      default:
        return false;
    }
  }

  /**
   * Whether the character at {@code i} of {@code text} is left out of speech at {@link #NONE}.
   * Kept: emoji, apostrophes and hyphens inside a word, separators inside a number, and
   * punctuation that ends a word, which engines pause for.
   */
  @VisibleForTesting
  static boolean isHiddenAt(CharSequence text, int i) {
    char c = text.charAt(i);
    if (!isPunctuationOrSymbol(c)) {
      return false;
    }
    int length = text.length();
    char next = i + 1 < length ? text.charAt(i + 1) : 0;
    if (next == '\ufe0f' || next == '\u20e3') {
      // An emoji, or a keycap such as #️⃣.
      return false;
    }
    if (Character.getType(c) == Character.OTHER_SYMBOL
        && !TALKBACK_PUNCTUATION_AND_SYMBOL.containsKey(c)) {
      // Emoji and pictographs without a name here.
      return false;
    }
    char previous = i > 0 ? text.charAt(i - 1) : 0;
    if (WORD_JOINERS.indexOf(c) >= 0
        && Character.isLetterOrDigit(previous)
        && Character.isLetterOrDigit(next)) {
      return false;
    }
    if (NUMBER_JOINERS.indexOf(c) >= 0 && Character.isDigit(previous) && Character.isDigit(next)) {
      return false;
    }
    if (PAUSE_PUNCTUATION.indexOf(c) >= 0
        && i > 0
        && !Character.isWhitespace(previous)
        && (i + 1 == length || Character.isWhitespace(next) || c >= '\u3000')) {
      // Ideographic punctuation is followed by text without a space.
      return false;
    }
    return true;
  }

  /**
   * Returns {@code text} with a space said in place of each punctuation mark and symbol that
   * {@link #isHiddenAt} leaves out, as a {@link TtsSpan}, or null if there are none. The text
   * itself, and so the text copied from speech, keeps them. Characters already covered by a
   * TtsSpan or an identifier span are left as they are.
   */
  public static @Nullable CharSequence withPunctuationHidden(@Nullable CharSequence text) {
    if (TextUtils.isEmpty(text)) {
      return null;
    }
    SpannableStringBuilder builder = null;
    for (int i = 0; i < text.length(); i++) {
      if (!isHiddenAt(text, i) || isCoveredBySpokenSpan(text, i)) {
        continue;
      }
      if (builder == null) {
        builder = new SpannableStringBuilder(text);
      }
      builder.setSpan(
          new TtsSpan.TextBuilder(" ").build(), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }
    return builder;
  }

  private static boolean isCoveredBySpokenSpan(CharSequence text, int i) {
    if (!(text instanceof Spanned spanned)) {
      return false;
    }
    return covers(spanned, i, TtsSpan.class)
        || covers(spanned, i, SpannableUtils.IdentifierSpan.class);
  }

  private static boolean covers(Spanned spanned, int i, Class<?> type) {
    for (Object span : spanned.getSpans(i, i + 1, type)) {
      if (spanned.getSpanStart(span) <= i && spanned.getSpanEnd(span) > i) {
        return true;
      }
    }
    return false;
  }

  /**
   * Whether {@code text} is one character, apart from spaces around it, such as when moving by
   * character or typing. At {@link #NONE} such a character is still said.
   */
  public static boolean isSingleCharacter(@Nullable CharSequence text) {
    if (text == null) {
      return false;
    }
    String trimmed = text.toString().trim();
    return !trimmed.isEmpty() && trimmed.codePointCount(0, trimmed.length()) == 1;
  }

  /** The symbol dictionary's text for {@code key}, or null if it keeps its name. */
  private static @Nullable String replacementFor(char key) {
    @Nullable CharacterReplacement replacement = characterReplacement;
    return replacement == null ? null : replacement.replacementFor(key);
  }

  /** Returns the "clean" value for the specified character. */
  public static String getCleanValueFor(Context context, char key) {
    Pair<Integer, Integer> pair = TALKBACK_PUNCTUATION_AND_SYMBOL.get(key);
    if (pair != null) {
      @Nullable String replacement = replacementFor(key);
      if (replacement != null) {
        return replacement;
      }
      final int resId = pair.first;

      if (resId != 0) {
        return context.getString(resId);
      }
    }
    return Character.toString(key);
  }

  /** Returns the "clean" value for the specified character as punctuation. */
  public static @Nullable String characterToName(Context context, char key) {
    return characterToName(context, key, ALL);
  }

  /**
   * Returns the "clean" value for the specified character as punctuation.
   *
   * @param context the parent context
   * @param key the character to be transformed
   * @param punctuationVerbosity the verbosity of user preference
   * @return the character name defined in this utils
   */
  public static @Nullable String characterToName(
      Context context, char key, int punctuationVerbosity) {
    Pair<Integer, Integer> pair = TALKBACK_PUNCTUATION_AND_SYMBOL.get(key);
    if (pair == null) {
      return null;
    } else if (key != ' ') {
      if (pair.second < punctuationVerbosity) {
        return null;
      }
      @Nullable String replacement = replacementFor(key);
      if (replacement != null) {
        return replacement;
      }
    }
    final int resId = pair.first;

    if (resId != 0 && key != ' ') {
      return context.getString(resId);
    }
    return null;
  }

  /**
   * Returns the shorten-text feedback that would help to reduce speech verbosity. If the text
   * length is lower than the threshold, it returns the source text.
   *
   * <ul>
   *   <li>Text-Selected feedbacks,
   * </ul>
   *
   * @param context the parent context
   * @param srcText the source text
   */
  public static CharSequence shortenLongTextFeedback(Context context, CharSequence srcText) {
    if (TextUtils.isEmpty(srcText)) {
      return "";
    }
    int length = srcText.length();
    if (length < SHORTEN_TEXT_LENGTH_THRESHOLD) {
      return srcText;
    }
    CharSequence toSrcText = srcText.subSequence(length - SHORTEN_TEXT_OFFSET, length);
    CharSequence fromSrcText = srcText.subSequence(0, SHORTEN_TEXT_OFFSET);
    return context.getString(R.string.template_shorten_text, length, fromSrcText, toSrcText);
  }

  private SpeechCleanupUtils() {}
}
