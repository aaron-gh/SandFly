package com.google.android.accessibility.utils.input

import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HiddenFieldEchoTest {
  private val heard = mutableListOf<String>()
  private val listener =
    object : HiddenFieldEcho.Listener {
      override fun onDeleted(deleted: String) {
        heard += "deleted $deleted"
      }

      override fun onChanged(now: CharSequence) {
        heard += "changed $now"
      }
    }

  @After
  fun stopExpecting() {
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
  }

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

  @Test
  fun deletionInTheFieldsWindowIsSpoken() {
    HiddenFieldEcho.onAccessibilityEvent(description(APP, window = 1, "zł500"))
    HiddenFieldEcho.expect(APP, listener)
    HiddenFieldEcho.onAccessibilityEvent(description(APP, window = 1, "zł50"))
    assertEquals(listOf("deleted 0"), heard)
  }

  @Test
  fun changesFromAnotherAppAreNotSpoken() {
    HiddenFieldEcho.onAccessibilityEvent(description(OTHER_APP, window = 2, "12:00"))
    HiddenFieldEcho.expect(APP, listener)
    HiddenFieldEcho.onAccessibilityEvent(description(OTHER_APP, window = 2, "12:0"))
    assertEquals(emptyList<String>(), heard)
  }

  @Test
  fun firstDescriptionOfAWindowIsNotSpoken() {
    HiddenFieldEcho.expect(APP, listener)
    HiddenFieldEcho.onAccessibilityEvent(description(APP, window = 3, "Konto"))
    assertEquals(emptyList<String>(), heard)
  }

  @Test
  fun nothingIsSpokenAfterTheAnswerWindow() {
    HiddenFieldEcho.onAccessibilityEvent(description(APP, window = 4, "zł500"))
    HiddenFieldEcho.expect(APP, listener)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
    HiddenFieldEcho.onAccessibilityEvent(description(APP, window = 4, "zł50"))
    assertEquals(emptyList<String>(), heard)
  }

  private fun description(packageName: String, window: Int, text: String): AccessibilityEvent {
    val event = AccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
    event.contentChangeTypes = AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION
    event.packageName = packageName
    event.contentDescription = text
    ReflectionHelpers.callInstanceMethod<Unit>(
      event,
      "setWindowId",
      ClassParameter.from(Int::class.javaPrimitiveType, window),
    )
    return event
  }

  private companion object {
    const val APP = "com.revolut.revolut"
    const val OTHER_APP = "com.android.systemui"
  }
}
