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
  public void spaceTypedAloneEndsAtTheSpace() {
    assertEquals(0, TextEventInterpreter.getWordEchoEnd(" "));
  }

  @Test
  public void spaceAfterALetterIsNotTreatedAsPunctuation() {
    assertEquals(2, TextEventInterpreter.getWordEchoEnd("ab "));
  }

  @Test
  public void apostropheInsideAWordIsPartOfTheWord() {
    assertEquals(3, TextEventInterpreter.getPrecedingWhitespaceOrPunctuation("hi don't ", 8));
    assertEquals(0, TextEventInterpreter.getPrecedingWhitespaceOrPunctuation("don't ", 5));
  }

  @Test
  public void apostropheAtTheEdgeOfAWordEndsTheWord() {
    assertEquals(6, TextEventInterpreter.getPrecedingWhitespaceOrPunctuation("rock 'n ", 7));
    assertEquals(4, TextEventInterpreter.getPrecedingWhitespaceOrPunctuation("don'", 4));
  }

  @Test
  public void otherPunctuationInsideAWordStillEndsTheWord() {
    assertEquals(6, TextEventInterpreter.getPrecedingWhitespaceOrPunctuation("hello,world ", 11));
  }
}
