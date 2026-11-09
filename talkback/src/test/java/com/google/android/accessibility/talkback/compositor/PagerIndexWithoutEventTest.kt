package com.google.android.accessibility.talkback.compositor

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PagerIndexWithoutEventTest {
  @Test
  fun describingAFocusedPagerWithoutAnEventHasNoIndex() {
    assertEquals(
      "",
      AccessibilityEventFeedbackUtils.getPagerIndexCount(
          null,
          RuntimeEnvironment.getApplication(),
          null,
        )
        .toString(),
    )
  }
}
