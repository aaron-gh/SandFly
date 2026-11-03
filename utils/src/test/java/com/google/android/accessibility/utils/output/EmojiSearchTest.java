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

package com.google.android.accessibility.utils.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.android.accessibility.utils.output.EmojiSearch.Emoji;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EmojiSearchTest {
  private static final EmojiSearch SEARCH =
      EmojiSearch.of(
          List.of(
              new Emoji("😀", "grinning face"),
              new Emoji("😃", "grinning face with big eyes"),
              new Emoji("😺", "grinning cat"),
              new Emoji("😹", "cat with tears of joy"),
              new Emoji("🐱", "cat face"),
              new Emoji("🐈", "cat"),
              new Emoji("❤️", "red heart"),
              new Emoji("☕", "Hot Beverage"),
              new Emoji("🧉", "maté")),
          Locale.ENGLISH);

  private static List<String> found(String query, int max) {
    List<String> emoji = new ArrayList<>();
    for (Emoji each : SEARCH.find(query, max)) {
      emoji.add(each.emoji());
    }
    return emoji;
  }

  @Test
  public void find_matchesWordsAnywhereInTheName() {
    assertEquals(List.of("😀", "😃", "🐱"), found("face", 10));
    assertEquals(List.of("😹"), found("joy", 10));
  }

  @Test
  public void find_ranksTheWholeNameThenItsStartThenStartsOfWords() {
    // "cat" is the name of 🐈; 😹 and 🐱 start with it; 😺 has it as a word; none is part-word.
    assertEquals(List.of("🐈", "😹", "🐱", "😺"), found("cat", 10));
    // "rin" starts no word.
    assertEquals(List.of("😀", "😃", "😺"), found("rin", 10));
  }

  @Test
  public void find_needsEveryWordInAnyOrder() {
    assertEquals(List.of("😃"), found("eyes grinning", 10));
    assertEquals(List.of(), found("cat heart", 10));
  }

  @Test
  public void find_ignoresCaseAccentsAndPunctuation() {
    assertEquals(List.of("☕"), found("HOT bev", 10));
    assertEquals(List.of("🧉"), found("mate", 10));
    assertEquals(List.of("❤️"), found("  red,  heart! ", 10));
  }

  @Test
  public void find_isBounded() {
    assertEquals(2, found("a", 2).size());
    assertEquals(List.of(), found("a", 0));
  }

  @Test
  public void find_nothingForAnEmptyQuery() {
    assertEquals(List.of(), found("", 10));
    assertEquals(List.of(), found(" ,. ", 10));
  }

  @Test
  public void isOffered_leavesOutSkinTonesAndHairStyles() {
    assertTrue(EmojiSearch.isOffered("👍"));
    assertFalse(EmojiSearch.isOffered("👍🏽"));
    assertFalse(EmojiSearch.isOffered("🏽"));
    assertFalse(EmojiSearch.isOffered("🦰"));
    assertTrue(EmojiSearch.isOffered("👩‍🦰"));
    assertFalse(EmojiSearch.isOffered(""));
  }

  @Test
  public void read_findsEmojiByTheirCldrNames() {
    EmojiSearch search = EmojiSearch.read(RuntimeEnvironment.getApplication(), Locale.US);
    List<Emoji> found = search.find("grinning face", 100);
    assertEquals("😀", found.get(0).emoji());
    assertEquals("grinning face", found.get(0).name());
    for (Emoji each : search.find("thumbs up", 100)) {
      assertTrue(EmojiSearch.isOffered(each.emoji()));
    }
  }

  @Test
  public void read_namesInTheLanguage() {
    EmojiSearch search = EmojiSearch.read(RuntimeEnvironment.getApplication(), Locale.GERMAN);
    // CLDR's German name for 😀 is "grinsendes Gesicht".
    assertEquals("😀", search.find("grinsendes gesicht", 10).get(0).emoji());
  }
}
