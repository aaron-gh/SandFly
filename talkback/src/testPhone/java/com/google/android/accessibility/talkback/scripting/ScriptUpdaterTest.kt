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

package com.google.android.accessibility.talkback.scripting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptUpdaterTest {
  @Test
  fun versionsCompareByTheirNumbers() {
    assertTrue(ScriptUpdater.compareVersions("3.10", "3.9") > 0)
    assertTrue(ScriptUpdater.compareVersions("3.3", "3.2.9") > 0)
    assertTrue(ScriptUpdater.compareVersions("1.0", "1.0.1") < 0)
    assertEquals(0, ScriptUpdater.compareVersions("2.0", "2"))
    assertEquals(0, ScriptUpdater.compareVersions("beta", "alpha"))
    assertTrue(ScriptUpdater.compareVersions("v2", "1.9") > 0)
  }

  @Test
  fun anOriginIsTheSourceAndFile() {
    val source = ScriptSource.parse(ScriptSources.OFFICIAL_SCRIPTS)!!
    assertEquals(
      "https://github.com/aaron-gh/sandfly-scripts\nwhatsapp-messages.lua",
      ScriptUpdater.originOf(source, "whatsapp-messages.lua"),
    )
  }
}
