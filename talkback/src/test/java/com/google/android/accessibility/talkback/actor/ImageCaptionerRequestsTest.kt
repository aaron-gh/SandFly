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

package com.google.android.accessibility.talkback.actor

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.ActorStateWritable
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.PrimesController
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackAnalyticsImpl
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.actor.gemini.AiCoreEndpoint
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason
import com.google.android.accessibility.talkback.dynamicfeature.DownloaderFactory
import com.google.android.accessibility.talkback.dynamicfeature.ModuleStateManager
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.imagecaption.CaptionRequest
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionStorage
import com.google.android.accessibility.talkback.imagecaption.RequestList
import com.google.android.accessibility.talkback.imagecaption.ScreenshotCaptureRequest
import com.google.android.accessibility.talkback.imagedescription.ImageDescriptionProcessor
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.time.Duration
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** How ImageCaptioner ends its requests: each one once, and on time. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageCaptionerRequestsTest {

  /** Records the screenshot requests, which the tests then end. */
  class FakeScreenshotService : AccessibilityService() {
    val callbacks = mutableListOf<TakeScreenshotCallback>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun takeScreenshot(displayId: Int, executor: Executor, callback: TakeScreenshotCallback) {
      callbacks += callback
    }

    override fun takeScreenshotOfWindow(
      accessibilityWindowId: Int,
      executor: Executor,
      callback: TakeScreenshotCallback,
    ) {
      callbacks += callback
    }
  }

  private class FocusMonitor(service: AccessibilityService) :
    AccessibilityFocusMonitor(service, null, null) {
    var focus: AccessibilityNodeInfoCompat? = null

    override fun getAccessibilityFocus(useInputFocusIfEmpty: Boolean): AccessibilityNodeInfoCompat? =
      focus?.let { AccessibilityNodeInfoCompat.obtain(it) }
  }

  private val context: Context
    get() = RuntimeEnvironment.getApplication()

  private val spoken = mutableListOf<CharSequence?>()
  private val geminiRequests = mutableListOf<Feedback.GeminiRequest>()
  private val resultDialogs = mutableListOf<CharSequence?>()
  private val pipeline =
    object : Pipeline.FeedbackReturner {
      override fun returnFeedback(feedback: Feedback): Boolean {
        val part = feedback.failovers().first()
        part.speech()?.let { spoken += it.text() }
        part.geminiRequest()?.let { geminiRequests += it }
        part.geminiResultDialog()?.let { resultDialogs += it.imageDescriptionResult()?.text() }
        return true
      }
    }
  private lateinit var service: FakeScreenshotService
  private lateinit var focusMonitor: FocusMonitor
  private lateinit var captioner: ImageCaptioner
  private lateinit var node: AccessibilityNodeInfoCompat

  @Before
  fun setUp() {
    // TalkBackService does this when it starts.
    FormFactorUtils.initialize(context)
    service = Robolectric.setupService(FakeScreenshotService::class.java)
    focusMonitor = FocusMonitor(service)
    captioner =
      ImageCaptioner(
        service,
        // No time between screenshots, so that only the requests themselves decide what runs.
        RequestList(ImageCaptioner.SCREENSHOT_REQUEST_CAPACITY, Duration.ZERO),
        ImageCaptionStorage(),
        ModuleStateManager(
          service,
          DownloaderFactory.create(service.application),
          DownloaderFactory.legacy(service.application),
        ),
        focusMonitor,
        TalkBackAnalyticsImpl(context),
        PrimesController(),
      )
    captioner.setPipeline(pipeline)
    captioner.setActorState(actorStateWithoutAiCore())

    // Robolectric compares nodes by their view.
    val info = AccessibilityNodeInfo(View(context))
    info.setBoundsInScreen(Rect(10, 10, 90, 90))
    info.className = "android.widget.ImageView"
    info.isVisibleToUser = true
    node = AccessibilityNodeInfoCompat.wrap(info)
  }

  /** ImageCaptioner asks the actor state whether the phone has AICore, which it does not. */
  private fun actorStateWithoutAiCore(): ActorState {
    val talkBack = TalkBackService()
    ContextWrapper::class
      .java
      .getDeclaredMethod("attachBaseContext", Context::class.java)
      .apply { isAccessible = true }
      .invoke(talkBack, context)
    val aiCore = AiCoreEndpoint(context)
    val gemini = GeminiActor(talkBack, TalkBackAnalyticsImpl(context), PrimesController(), aiCore, aiCore)
    return ActorState(
      ActorStateWritable(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        gemini.state,
        null,
        null,
      )
    )
  }

  private fun idle() {
    shadowOf(Looper.getMainLooper()).idle()
  }

  private fun advance(duration: Duration) {
    shadowOf(Looper.getMainLooper()).idleFor(duration)
  }

  /** Ends a screenshot request with a picture, as the system callback would. */
  private fun takeScreenshot(callback: TakeScreenshotCallback) {
    val requestField =
      callback.javaClass.declaredFields.first {
        ScreenshotCaptureRequest::class.java.isAssignableFrom(it.type)
      }
    requestField.isAccessible = true
    val request = requestField.get(callback) as ScreenshotCaptureRequest
    ScreenshotCaptureRequest::class
      .java
      .getDeclaredMethod("onFinished", Bitmap::class.java, Boolean::class.javaPrimitiveType)
      .apply { isAccessible = true }
      .invoke(request, Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888), false)
    idle()
  }

  private fun failScreenshot(callback: TakeScreenshotCallback) {
    callback.onFailure(AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR)
    idle()
  }

  @Test
  fun aScreenshotThatEndsAfterItsTimeoutDoesNotEndTheNextRequest() {
    captioner.caption(node, /* isUserRequested= */ true) // A
    captioner.caption(node, /* isUserRequested= */ true) // B, which waits for A
    idle()
    assertEquals(1, service.callbacks.size)

    // A's screenshot takes too long, so A ends and B starts.
    advance(ScreenshotCaptureRequest.SCREENSHOT_CAPTURE_TIMEOUT_MS)
    assertEquals(2, service.callbacks.size)

    captioner.caption(node, /* isUserRequested= */ true) // C, which waits for B
    idle()
    failScreenshot(service.callbacks[0]) // A's screenshot fails, late
    assertEquals("C started before B ended", 2, service.callbacks.size)

    failScreenshot(service.callbacks[1])
    assertEquals(3, service.callbacks.size)
  }

  @Test
  fun aSlowCaptionIsSaidAfterFiveSeconds_andItsLateTimeoutEndsNothingElse() {
    focusMonitor.focus = node
    // Its image description never answers, so the caption waits for it.
    captioner.imageDescriptionProcessor =
      ImageDescriptionProcessor(context, TalkBackAnalyticsImpl(context))
    captioner.caption(node, /* isUserRequested= */ true) // A
    idle()
    takeScreenshot(service.callbacks[0])
    captioner.caption(node, /* isUserRequested= */ true) // B, which waits for A's caption
    idle()
    val spokenForA = spoken.size

    advance(Duration.ofMillis(4900))
    assertEquals(spokenForA, spoken.size)
    assertEquals(1, service.callbacks.size)

    // After 5 seconds, A says what it has, and B starts.
    advance(Duration.ofMillis(200))
    assertEquals("A was not said after 5 seconds", spokenForA + 1, spoken.size)
    assertEquals("B did not start when A gave up", 2, service.callbacks.size)

    takeScreenshot(service.callbacks[1])
    captioner.caption(node, /* isUserRequested= */ true) // C, which waits for B's caption
    idle()
    val spokenForB = spoken.size

    // A's own requests would time out now, 10 seconds after they started. They ended with A.
    advance(CaptionRequest.CAPTION_TIMEOUT_MS.minusMillis(5100).plusMillis(50))
    assertEquals("A was said again", spokenForB, spoken.size)
    assertEquals("C started before B ended", 2, service.callbacks.size)

    // B gives up 5 seconds after its screenshot, and C starts.
    advance(Duration.ofMillis(200))
    assertEquals(spokenForB + 1, spoken.size)
    assertEquals(3, service.callbacks.size)
  }

  /** Describe image with Gemini, as the menu does, up to the request to the model. */
  private fun describeImageWithGemini(): Feedback.GeminiRequest {
    val screenshots = service.callbacks.size
    captioner.captionWithGemini(node)
    idle()
    takeScreenshot(service.callbacks[screenshots])
    return geminiRequests.last()
  }

  private fun answer(request: Feedback.GeminiRequest, text: String) {
    captioner.handleResultFromGemini(
      request.requestId(),
      text,
      /* isSuccess= */ true,
      FinishReason.STOP,
      /* errorReason= */ null,
      /* manualTrigger= */ true,
    )
    idle()
  }

  @Test
  fun aModelThatAnswersAfter30Seconds_isNotCutOffAfter5() {
    val request = describeImageWithGemini()

    advance(Duration.ofSeconds(30))
    assertTrue(resultDialogs.isEmpty())
    answer(request, "A cat on a sofa.")
    assertEquals(listOf<CharSequence?>("A cat on a sofa."), resultDialogs)
  }

  @Test
  fun anAutomaticCaptionWithNothingToDo_leavesNoTimeoutForDescribeImage() {
    // Automatic text recognition is off, and there is no icon detector, so the caption has nothing
    // to do.
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putBoolean(context.getString(R.string.pref_auto_text_recognition_key), false)
      .commit()
    captioner.caption(node, /* isUserRequested= */ false)
    idle()
    takeScreenshot(service.callbacks[0])

    val request = describeImageWithGemini()
    advance(Duration.ofSeconds(30))
    answer(request, "A cat on a sofa.")
    assertEquals(spoken.toString(), listOf<CharSequence?>("A cat on a sofa."), resultDialogs)
  }
}
