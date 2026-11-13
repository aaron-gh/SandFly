package com.google.android.accessibility.utils.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HiddenFieldEchoTest {
  @Test
  fun lastDigitRemoved() {
    assertEquals("0", HiddenFieldEcho.removedCharacter("zł500", "zł50"))
  }

  @Test
  fun groupingSpacesAreIgnored() {
    assertEquals("1", HiddenFieldEcho.removedCharacter("zł5 001", "zł500"))
    assertEquals("5", HiddenFieldEcho.removedCharacter("zł5", "zł"))
  }

  @Test
  fun digitRemovedInTheMiddle() {
    assertEquals("2", HiddenFieldEcho.removedCharacter("zł123", "zł13"))
  }

  @Test
  fun otherChangesAreNotADeletion() {
    assertNull(HiddenFieldEcho.removedCharacter("zł500", "zł5"))
    assertNull(HiddenFieldEcho.removedCharacter("zł500", "zł501"))
    assertNull(HiddenFieldEcho.removedCharacter("Konto", "zł50"))
  }
}
