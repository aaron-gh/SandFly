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

package com.google.android.accessibility.talkback.compositor.roledescription

import com.google.android.accessibility.talkback.compositor.CompositorUtils
import com.google.android.accessibility.talkback.compositor.roledescription.RoleDescriptionExtractor.DESC_ORDER_NAME_ROLE_STATE_POSITION
import com.google.android.accessibility.talkback.compositor.roledescription.RoleDescriptionExtractor.DESC_ORDER_NAME_STATE_ROLE_POSITION
import com.google.android.accessibility.talkback.compositor.roledescription.RoleDescriptionExtractor.DESC_ORDER_STATE_NAME_ROLE_POSITION
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoleWithoutPauseTest {
  @After
  fun tearDown() {
    CompositorUtils.setRoleWithoutPause(false)
  }

  private fun join(order: Int) =
    RoleDescriptionExtractor.joinInOrder(order, "Back", "Button", "Selected").toString()

  @Test
  fun off_keepsTheSeparator() {
    assertEquals("Back, Button, Selected", join(DESC_ORDER_NAME_ROLE_STATE_POSITION))
  }

  @Test
  fun on_joinsAdjacentNameAndRoleWithASpace() {
    CompositorUtils.setRoleWithoutPause(true)
    assertEquals("Back Button, Selected", join(DESC_ORDER_NAME_ROLE_STATE_POSITION))
    assertEquals("Selected, Back Button", join(DESC_ORDER_STATE_NAME_ROLE_POSITION))
  }

  @Test
  fun on_leavesOrdersWithTheStateBetweenThem() {
    CompositorUtils.setRoleWithoutPause(true)
    assertEquals("Back, Selected, Button", join(DESC_ORDER_NAME_STATE_ROLE_POSITION))
  }
}
