/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.interpreters

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.google.android.accessibility.talkback.Pipeline
import java.time.Duration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** General restoration must not steal focus from a manual web-scroll search between slices. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SubtreeWebScrollRecoveryTest {
  @Test fun theGeneralFallbackWaitsForWebScrollRecoveryAndStillRunsAfterIt() {
    val interpreter = SubtreeChangeEventInterpreter(null, null)
    interpreter.setActorState { null }
    var pending = true
    interpreter.setWebScrollFocusPending { pending }
    var calls = 0
    interpreter.setPipeline(Pipeline.InterpretationReceiver { _, _, _, _ -> calls++; true })
    val handler = ReflectionHelpers.getField<Handler>(interpreter, "subtreeChangedHandler")
    handler.sendMessage(Message.obtain(handler, 1))
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
    assertEquals("The WebView fallback must not cancel the web search", 0, calls)
    pending = false
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(150))
    assertEquals("Failure or completion must leave the general fallback available", 1, calls)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
    assertEquals(1, calls)
  }

  @Test fun withoutManualWebRecoveryTheGeneralFallbackRunsAsBefore() {
    val interpreter = SubtreeChangeEventInterpreter(null, null)
    interpreter.setActorState { null }
    var calls = 0
    interpreter.setPipeline(Pipeline.InterpretationReceiver { _, _, _, _ -> calls++; true })
    val handler = ReflectionHelpers.getField<Handler>(interpreter, "subtreeChangedHandler")
    handler.sendMessage(Message.obtain(handler, 1))
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(1, calls)
  }
}
