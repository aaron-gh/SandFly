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

import com.google.android.accessibility.utils.Role
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionInfoSwitchesTest {
  @Test
  fun grid_followsTheGridSwitch() {
    assertTrue(GlobalVariables.speaksCollectionInfo(Role.ROLE_GRID, 3, 4, false, true))
    assertFalse(GlobalVariables.speaksCollectionInfo(Role.ROLE_GRID, 3, 4, true, false))
  }

  @Test
  fun staggeredGrid_followsTheGridSwitch() {
    assertTrue(GlobalVariables.speaksCollectionInfo(Role.ROLE_STAGGERED_GRID, -1, 2, false, true))
    assertFalse(GlobalVariables.speaksCollectionInfo(Role.ROLE_STAGGERED_GRID, -1, 2, true, false))
  }

  @Test
  fun pagerInRowsAndColumns_followsTheGridSwitch() {
    assertTrue(GlobalVariables.speaksCollectionInfo(Role.ROLE_PAGER, 2, 3, false, true))
    assertFalse(GlobalVariables.speaksCollectionInfo(Role.ROLE_PAGER, 2, 3, true, false))
  }

  @Test
  fun pagerInOneRow_followsTheListSwitch() {
    assertTrue(GlobalVariables.speaksCollectionInfo(Role.ROLE_PAGER, 1, 3, true, false))
    assertFalse(GlobalVariables.speaksCollectionInfo(Role.ROLE_PAGER, 1, 3, false, true))
  }

  @Test
  fun list_followsTheListSwitch() {
    assertTrue(GlobalVariables.speaksCollectionInfo(Role.ROLE_LIST, 5, 1, true, false))
    assertFalse(GlobalVariables.speaksCollectionInfo(Role.ROLE_LIST, 5, 1, false, true))
  }
}
