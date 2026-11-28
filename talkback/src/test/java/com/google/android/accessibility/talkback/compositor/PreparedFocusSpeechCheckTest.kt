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

package com.google.android.accessibility.talkback.compositor

import android.graphics.Rect
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Prepared swipe speech is kept through events that leave its node saying the same, and never used
 * once the node says something else.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreparedFocusSpeechCheckTest {
  private val list = node("androidx.recyclerview.widget.RecyclerView")
  private val row = node("android.widget.LinearLayout").apply { isFocusable = true }
  private val title = node("android.widget.TextView").apply { text = "Wi-Fi" }
  private val summary = node("android.widget.TextView").apply { text = "Connected" }
  private val feedback = EventFeedback.builder().setTtsOutput(java.util.Optional.of<CharSequence>("Wi-Fi")).build()

  init {
    shadowOf(list).addChild(row)
    shadowOf(row).addChild(title)
    shadowOf(row).addChild(summary)
    place(row, 0, 100, 500, 200)
    place(title, 10, 110, 400, 150)
    place(summary, 10, 150, 400, 190)
  }

  @After
  fun tearDown() {
    PreparedFocusSpeech.clear("test")
  }

  private fun node(className: String): AccessibilityNodeInfo =
    AccessibilityNodeInfo.obtain().apply {
      this.className = className
      isVisibleToUser = true
      isEnabled = true
    }

  private fun place(node: AccessibilityNodeInfo, left: Int, top: Int, right: Int, bottom: Int) {
    node.setBoundsInScreen(Rect(left, top, right, bottom))
  }

  private fun compat(node: AccessibilityNodeInfo) = AccessibilityNodeInfoCompat.wrap(node)

  private fun prepare() {
    val target = compat(row)
    PreparedFocusSpeech.put(target, feedback, "", NodeFingerprint.of(target))
    // Old enough to be used.
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PreparedFocusSpeech.MIN_AGE_MS))
  }

  private fun contentChange() {
    PreparedFocusSpeech.onAccessibilityEvent(
      AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
    )
  }

  @Test
  fun withoutChange_preparedIsUsedAsIs() {
    prepare()
    assertTrue(PreparedFocusSpeech.has(compat(row)))
    val found = PreparedFocusSpeech.peek(compat(row))
    assertNotNull(found)
    assertFalse(found!!.isCheckedAfterChange)
    assertSame(feedback, found.feedback)
  }

  @Test
  fun changeThatLeavesTheNodeAsItWas_preparedIsUsedAfterACheck() {
    prepare()
    contentChange()
    // Prepared again before a swipe rather than checked during it, when there is time.
    assertFalse(PreparedFocusSpeech.has(compat(row)))
    val found = PreparedFocusSpeech.peek(compat(row))
    assertNotNull(found)
    assertTrue(found!!.isCheckedAfterChange)
    assertSame(feedback, found.feedback)
  }

  @Test
  fun scrollThatOnlyMovesTheNode_preparedIsUsed() {
    prepare()
    place(row, 0, 40, 500, 140)
    place(title, 10, 50, 400, 90)
    place(summary, 10, 90, 400, 130)
    PreparedFocusSpeech.onAccessibilityEvent(
      AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED)
    )
    assertNotNull(PreparedFocusSpeech.peek(compat(row)))
  }

  @Test
  fun textOfAChildChanged_preparedIsNotUsed() {
    prepare()
    summary.text = "Disconnected"
    contentChange()
    assertNull(PreparedFocusSpeech.peek(compat(row)))
  }

  @Test
  fun stateChanged_preparedIsNotUsed() {
    prepare()
    row.isCheckable = true
    row.isChecked = true
    contentChange()
    assertNull(PreparedFocusSpeech.peek(compat(row)))
  }

  @Test
  fun childMovedInsideTheNode_preparedIsNotUsed() {
    prepare()
    place(summary, 10, 110, 400, 150)
    place(title, 10, 150, 400, 190)
    contentChange()
    assertNull(PreparedFocusSpeech.peek(compat(row)))
  }

  @Test
  fun listDetailsChanged_preparedIsNotUsed() {
    prepare()
    list.collectionInfo = AccessibilityNodeInfo.CollectionInfo.obtain(12, 1, false)
    contentChange()
    assertNull(PreparedFocusSpeech.peek(compat(row)))
  }

  @Test
  fun changeWithoutAnEvent_isNotLookedFor() {
    prepare()
    summary.text = "Disconnected"
    // Until an event says so, nothing in the window is taken to have changed, as before.
    assertNotNull(PreparedFocusSpeech.peek(compat(row)))
  }

  @Test
  fun nodeThatCannotBeReadAgain_preparedIsNotUsed() {
    prepare()
    contentChange()
    shadowOf(row).setRefreshReturnValue(false)
    assertNull(PreparedFocusSpeech.peek(compat(row)))
  }

  @Test
  fun nodeWithoutFingerprint_isNotUsedAfterAChange() {
    PreparedFocusSpeech.put(compat(row), feedback, "", /* fingerprint= */ null)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PreparedFocusSpeech.MIN_AGE_MS))
    assertNotNull(PreparedFocusSpeech.peek(compat(row)))
    contentChange()
    assertNull(PreparedFocusSpeech.peek(compat(row)))
  }

  @Test
  fun anotherScreen_throwsThemAway() {
    prepare()
    PreparedFocusSpeech.onAccessibilityEvent(
      AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    )
    assertFalse(PreparedFocusSpeech.has(compat(row)))
    assertNull(PreparedFocusSpeech.peek(compat(row)))
  }

  @Test
  fun focusAndTouchEvents_changeNothing() {
    prepare()
    for (type in
      listOf(
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED,
        AccessibilityEvent.TYPE_TOUCH_INTERACTION_START,
        AccessibilityEvent.TYPE_GESTURE_DETECTION_START,
      )) {
      PreparedFocusSpeech.onAccessibilityEvent(AccessibilityEvent.obtain(type))
    }
    assertTrue(PreparedFocusSpeech.has(compat(row)))
  }

  @Test
  fun preparedAgainAfterAChange_needsNoCheck() {
    prepare()
    contentChange()
    prepare()
    assertTrue(PreparedFocusSpeech.has(compat(row)))
    assertFalse(PreparedFocusSpeech.peek(compat(row))!!.isCheckedAfterChange)
  }

  @Test
  fun preparedJustBefore_isNotUsed() {
    val target = compat(row)
    PreparedFocusSpeech.put(target, feedback, "", NodeFingerprint.of(target))
    assertNull(PreparedFocusSpeech.peek(target))
  }

  @Test
  fun fingerprint_sameNodes_areEqual() {
    assertEquals(NodeFingerprint.of(compat(row)), NodeFingerprint.of(compat(row)))
  }

  @Test
  fun fingerprint_linkTarget_counts() {
    val before = NodeFingerprint.of(compat(row))
    title.text = SpannableString("Wi-Fi").apply { setSpan(URLSpan("https://a"), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
    val withLink = NodeFingerprint.of(compat(row))
    title.text = SpannableString("Wi-Fi").apply { setSpan(URLSpan("https://b"), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
    assertNotEquals(before, withLink)
    assertNotEquals(withLink, NodeFingerprint.of(compat(row)))
  }

  @Test
  fun fingerprint_tooManyNodes_isNull() {
    for (i in 0 until NodeFingerprint.MAX_NODES) {
      shadowOf(summary).addChild(node("android.view.View"))
    }
    assertNull(NodeFingerprint.of(compat(row)))
  }
}
