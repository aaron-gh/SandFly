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

package com.google.android.accessibility.talkback.commands

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandSearchTest {
  private fun command(value: String, label: String) = Commands.Command(value, label, null)

  private val reading =
    Commands.Category(
      1,
      listOf(
        command("top", "Read from top"),
        command("current", "Read from focused item"),
        command("spell", "Spell last spoken phrase"),
      ),
    )
  private val speech =
    Commands.Category(
      2,
      listOf(command("rate", "Increase speech rate"), command("top", "Read from top")),
    )
  private val other =
    Commands.Category(3, listOf(command("app", "Café Menü"), command("history", "Speech history")))
  private val categories = listOf(reading, speech, other)

  private fun values(query: String?) =
    CommandSearch.find(categories, query).map { it.command.value }

  @Test
  fun emptyQueryFindsNothing() {
    assertTrue(values(null).isEmpty())
    assertTrue(values("").isEmpty())
    assertTrue(values("   ").isEmpty())
  }

  @Test
  fun everyWordMustBeInTheName() {
    assertEquals(listOf("top"), values("read top"))
    assertEquals(listOf("spell", "history", "rate"), values("sp"))
    assertTrue(values("read rate").isEmpty())
  }

  @Test
  fun namesStartingWithTheQueryComeFirst() {
    assertEquals(listOf("history", "rate"), values("speech"))
    assertEquals(listOf("rate"), values("speech rate"))
    assertEquals(listOf("rate"), values("increase"))
  }

  @Test
  fun caseAndAccentsAreIgnored() {
    assertEquals(listOf("app"), values("CAFE menu"))
  }

  @Test
  fun aCommandInSeveralCategoriesIsListedOnceWithTheFirst() {
    val found = CommandSearch.find(categories, "top")
    assertEquals(1, found.size)
    assertEquals(reading, found.single().category)
  }
}
