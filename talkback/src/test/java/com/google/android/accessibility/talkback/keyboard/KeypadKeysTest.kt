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

package com.google.android.accessibility.talkback.keyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which buttons letter keys press on keypads. */
class KeypadKeysTest {
  @Test
  fun digitButtonsMayShowTheirLetters() {
    for (label in listOf("2", " 2 ", "2ABC", "2 ABC", "2,ABC", "7 PQRS", "0 +", "0+")) {
      assertTrue(label, KeypadKeys.isLabelFor(label, label.trim()[0]))
    }
  }

  @Test
  fun otherTextStartingWithADigitIsNoButton() {
    for (label in listOf("12", "21 November", "3 new messages", "01:23", "2 missed calls")) {
      assertFalse(label, KeypadKeys.isLabelFor(label, label[0]))
    }
  }

  @Test
  fun calculatorOperatorsMatchTheirSigns() {
    assertTrue(KeypadKeys.isLabelFor("×", '*'))
    assertTrue(KeypadKeys.isLabelFor("÷", '/'))
    assertTrue(KeypadKeys.isLabelFor("−", '-'))
    assertTrue(KeypadKeys.isLabelFor("+", '+'))
    assertTrue(KeypadKeys.isLabelFor("#", '#'))
    assertFalse(KeypadKeys.isLabelFor("+", '-'))
  }

  @Test
  fun calculatorsByPackage() {
    assertTrue(KeypadKeys.isCalculator("com.google.android.calculator"))
    assertTrue(KeypadKeys.isCalculator("com.sec.android.app.popupcalculator"))
    assertFalse(KeypadKeys.isCalculator("com.whatsapp"))
  }
}
