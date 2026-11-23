package com.google.android.accessibility.talkback.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnOffLogTest {
  private val head = "11-22 22:08:26.931 pid 13351\nStill an enabled accessibility service: no\n\n"

  @Test
  fun keepsTheHeadWhenTheLogIsLongerThanTheLimit() {
    val line = "11-22 22:07:41.857 13351 13351 E Vendor: repeated line\n"
    val log = line.repeat(TurnOffLog.MAX_CHARS / line.length * 2) + "last line\n"
    val kept = TurnOffLog.compose(head, log)
    assertTrue(kept.startsWith(head))
    assertTrue(kept.endsWith("last line\n"))
    assertTrue(kept.length <= TurnOffLog.MAX_CHARS)
    // Only whole lines follow the head.
    assertTrue(kept.removePrefix(head).startsWith("11-22 "))
  }

  @Test
  fun keepsAShortLogWhole() {
    assertEquals(head + "a\nb\n", TurnOffLog.compose(head, "a\nb\n"))
  }
}
