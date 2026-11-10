package com.google.android.accessibility.talkback.scripting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptCommandsTest {
  @Test
  fun aSavedValueNamesTheScriptAndTheCommand() {
    assertEquals("SCRIPT:wiki/search", ScriptCommands.valueOf("wiki", "search"))
  }

  @Test
  fun onlyScriptValuesAreScriptCommands() {
    assertTrue(ScriptCommands.isCommand(ScriptCommands.valueOf("wiki", "search")))
    assertFalse(ScriptCommands.isCommand("MENU:menu-1"))
    assertFalse(ScriptCommands.isCommand(null))
  }
}
