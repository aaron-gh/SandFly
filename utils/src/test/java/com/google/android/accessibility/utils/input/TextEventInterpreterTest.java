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

package com.google.android.accessibility.utils.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TextEventInterpreterTest {

  @Test
  public void wordTypedAloneEndsAtItsLastCharacter() {
    assertEquals(5, TextEventInterpreter.getWordEchoEnd("hello "));
    assertEquals(5, TextEventInterpreter.getWordEchoEnd("hello,"));
  }

  @Test
  public void wordAddedWithPunctuationAndSpaceEndsAtThePunctuation() {
    assertEquals(5, TextEventInterpreter.getWordEchoEnd("hello, "));
    assertEquals(5, TextEventInterpreter.getWordEchoEnd("there. "));
  }

  @Test
  public void wordAddedWithHyphenAndSpaceEndsAtTheSpace() {
    assertEquals(5, TextEventInterpreter.getWordEchoEnd("well- "));
  }

  @Test
  public void spaceTypedAloneEndsAtTheSpace() {
    assertEquals(0, TextEventInterpreter.getWordEchoEnd(" "));
  }

  @Test
  public void spaceAfterALetterIsNotTreatedAsPunctuation() {
    assertEquals(2, TextEventInterpreter.getWordEchoEnd("ab "));
  }

  @Test
  public void wordTypedCharacterByCharacterIncludesItsPunctuation() {
    assertEquals("non-normal ", echoWord("non-normal ", 10, " "));
    assertEquals("don't ", echoWord("hi don't ", 8, " "));
    assertEquals("hello,world ", echoWord("hello,world ", 11, " "));
  }

  @Test
  public void punctuationEndsTheWholeWord() {
    assertEquals("non-normal.", echoWord("non-normal.", 10, "."));
    assertEquals("non-normal)", echoWord("non-normal)", 10, ")"));
  }

  @Test
  public void hyphenAndApostropheEndNoWord() {
    assertNull(echoWord("non-", 3, "-"));
    assertNull(echoWord("don'", 3, "'"));
    assertEquals("well- ", echoWord("well- ", 5, " "));
  }

  @Test
  public void spaceAfterPunctuationTypedEarlierReadsNothing() {
    assertNull(echoWord("non-normal. ", 11, " "));
  }

  @Test
  public void wordAddedAllAtOnceIsReadWhole() {
    assertEquals("non-normal ", echoWord("non-normal ", 0, "non-normal "));
    assertEquals("non-normal.", echoWord("Hi non-normal. ", 3, "non-normal. "));
  }

  @Test
  public void correctedWordIsReadWhole() {
    // "non-normla" corrected as the space is typed, replacing only the letters that changed.
    assertEquals("non-normal ", echoWord("non-normal ", 8, "al "));
    // The same correction, replacing the part after the hyphen.
    assertEquals("non-normal ", echoWord("non-normal ", 4, "normal "));
  }

  @Test
  public void openingBracketsAndDoubleQuotesAreLeftOut() {
    assertEquals("non-normal)", echoWord("(non-normal)", 11, ")"));
    assertEquals("hi ", echoWord("\"hi ", 3, " "));
    assertEquals("'n ", echoWord("rock 'n ", 7, " "));
    assertEquals("a)", echoWord("(a)", 2, ")"));
  }

  @Test
  public void severalWordsAddedAtOnceReadNothing() {
    assertNull(echoWord("hello world ", 0, "hello world "));
  }

  @Test
  public void singleCharacterBeforeASymbolReadsNothing() {
    assertNull(echoWord("a@", 1, "@"));
    assertEquals("I ", echoWord("I ", 1, " "));
  }

  @Test
  public void spaceAfterASpaceReadsNothing() {
    assertNull(echoWord("hi  ", 3, " "));
  }

  @Test
  public void addedLetterEndsNoWord() {
    assertNull(echoWord("non-norm", 7, "m"));
  }

  @Test
  public void webAddressTypedCharacterByCharacterIsOneWord() {
    assertNull(echoWord("https:", 5, ":"));
    assertNull(echoWord("https:/", 6, "/"));
    assertNull(echoWord("https://", 7, "/"));
    assertNull(echoWord("https://github.", 14, "."));
    assertNull(echoWord("https://github.com/", 18, "/"));
    assertNull(echoWord("https://github.com/author/", 25, "/"));
    assertEquals(
        "https://github.com/author/repo ",
        echoWord("see https://github.com/author/repo ", 34, " "));
  }

  @Test
  public void webAddressWithoutAScheme() {
    assertNull(echoWord("www.", 3, "."));
    assertNull(echoWord("www.example.", 11, "."));
    assertNull(echoWord("github.com/", 10, "/"));
    assertEquals("github.com/author ", echoWord("github.com/author ", 17, " "));
    // A domain alone isn't known to be an address until a path follows it.
    assertEquals("github.", echoWord("github.", 6, "."));
  }

  @Test
  public void punctuationAfterAWebAddressIsReadWithItAtTheSpace() {
    assertNull(echoWord("https://x.com.", 13, "."));
    assertEquals("https://x.com. ", echoWord("https://x.com. ", 14, " "));
    assertNull(echoWord("(https://x.com)", 14, ")"));
    assertEquals("https://x.com) ", echoWord("(https://x.com) ", 15, " "));
  }

  @Test
  public void webAddressAddedWithPunctuationAndSpaceAtOnceIsReadWhole() {
    assertEquals("https://x.com. ", echoWord("https://x.com. ", 0, "https://x.com. "));
    assertEquals("github.com/a ", echoWord("github.com/a ", 0, "github.com/a "));
  }

  @Test
  public void emailAddressIsOneWord() {
    assertNull(echoWord("me@", 2, "@"));
    assertNull(echoWord("me@example.", 10, "."));
    assertEquals("me@example.com ", echoWord("me@example.com ", 14, " "));
  }

  @Test
  public void ordinaryPunctuationStillEndsTheWord() {
    assertEquals("Hello.", echoWord("Hello.", 5, "."));
    assertEquals("e.g.", echoWord("e.g.", 3, "."));
    assertEquals("time:", echoWord("time:", 4, ":"));
  }

  /** Returns the word and the character that ends it, or null if the change ends no word. */
  private static String echoWord(String text, int fromIndex, String addedText) {
    int[] range = TextEventInterpreter.getEchoWordRange(text, fromIndex, addedText);
    return range == null ? null : text.substring(range[0], range[1] + 1);
  }
}
