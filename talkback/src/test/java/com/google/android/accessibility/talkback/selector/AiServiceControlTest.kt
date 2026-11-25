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

package com.google.android.accessibility.talkback.selector

import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.AiProvider
import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import com.google.android.accessibility.utils.ValueSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiServiceControlTest {
  private val context = RuntimeEnvironment.getApplication()
  private val prefs = FakeSharedPreferences()
  private val three = listOf(AiProvider.GEMINI, AiProvider.CLAUDE, AiProvider.DEEPSEEK)

  private fun setUp(provider: AiProvider) {
    provider.setApiKey(prefs, "key")
    provider.setModel(prefs, "model")
  }

  @Test
  fun step_goesToTheNextAndPreviousService() {
    assertEquals(AiProvider.DEEPSEEK, AiServiceControl.step(three, AiProvider.CLAUDE, forward = true))
    assertEquals(AiProvider.GEMINI, AiServiceControl.step(three, AiProvider.CLAUDE, forward = false))
  }

  @Test
  fun step_wrapsAround() {
    assertEquals(AiProvider.GEMINI, AiServiceControl.step(three, AiProvider.DEEPSEEK, forward = true))
    assertEquals(AiProvider.DEEPSEEK, AiServiceControl.step(three, AiProvider.GEMINI, forward = false))
  }

  @Test
  fun step_withWrapAroundOff_staysAtTheLastAndFirstService() {
    var service: AiProvider? = null
    assertTrue(
      ValueSteps.run(false) { service = AiServiceControl.step(three, AiProvider.DEEPSEEK, true) }
    )
    assertEquals(AiProvider.DEEPSEEK, service)
    assertTrue(
      ValueSteps.run(false) { service = AiServiceControl.step(three, AiProvider.GEMINI, false) }
    )
    assertEquals(AiProvider.GEMINI, service)
  }

  @Test
  fun step_fromAServiceNotListed_goesToTheFirstOrLast() {
    assertEquals(AiProvider.GEMINI, AiServiceControl.step(three, AiProvider.NONE, forward = true))
    assertEquals(AiProvider.DEEPSEEK, AiServiceControl.step(three, AiProvider.NONE, forward = false))
  }

  @Test
  fun step_withFewerThanTwoServices_isNull() {
    assertNull(AiServiceControl.step(emptyList(), AiProvider.NONE, forward = true))
    assertNull(AiServiceControl.step(listOf(AiProvider.CLAUDE), AiProvider.CLAUDE, forward = true))
    assertNull(AiServiceControl.step(listOf(AiProvider.CLAUDE), AiProvider.NONE, forward = false))
  }

  @Test
  fun services_areTheServicesSetUpWithoutNone() {
    setUp(AiProvider.DEEPSEEK)
    setUp(AiProvider.CLAUDE)
    AiProvider.MISTRAL.setApiKey(prefs, "key")
    assertEquals(listOf(AiProvider.CLAUDE, AiProvider.DEEPSEEK), AiServiceControl.services(prefs))
  }

  @Test
  fun services_leaveOutSandflyProWithoutALicence() {
    setUp(AiProvider.CLAUDE)
    assertFalse(AiProvider.SANDFLY_PRO in AiServiceControl.services(prefs))
  }

  @Test
  fun change_usesTheNextServiceAndSaysItsName() {
    setUp(AiProvider.CLAUDE)
    setUp(AiProvider.DEEPSEEK)
    AiProvider.setActive(prefs, AiProvider.CLAUDE)
    assertEquals(
      context.getString(R.string.ai_provider_deepseek),
      AiServiceControl.change(context, prefs, next = true),
    )
    assertEquals(AiProvider.DEEPSEEK, AiProvider.active(prefs))
    assertEquals(
      context.getString(R.string.ai_provider_deepseek),
      AiServiceControl.current(context, prefs),
    )
    assertEquals(
      context.getString(R.string.ai_provider_claude),
      AiServiceControl.change(context, prefs, next = false),
    )
    assertEquals(AiProvider.CLAUDE, AiProvider.active(prefs))
  }

  @Test
  fun change_fromNone_usesTheFirstService() {
    setUp(AiProvider.CLAUDE)
    setUp(AiProvider.DEEPSEEK)
    AiProvider.setActive(prefs, AiProvider.NONE)
    AiServiceControl.change(context, prefs, next = true)
    assertEquals(AiProvider.CLAUDE, AiProvider.active(prefs))
  }

  @Test
  fun change_withOneService_changesNothing() {
    setUp(AiProvider.CLAUDE)
    AiProvider.setActive(prefs, AiProvider.CLAUDE)
    assertEquals(
      context.getString(R.string.ai_service_control_one),
      AiServiceControl.change(context, prefs, next = true),
    )
    assertEquals(AiProvider.CLAUDE, AiProvider.active(prefs))
  }

  @Test
  fun change_withNoService_changesNothing() {
    AiProvider.setActive(prefs, AiProvider.NONE)
    assertEquals(
      context.getString(R.string.ai_service_control_none),
      AiServiceControl.change(context, prefs, next = true),
    )
    assertEquals(AiProvider.NONE, AiProvider.active(prefs))
  }
}
