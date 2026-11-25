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

import android.content.Context
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScriptCodeTest {
  private val context: Context = RuntimeEnvironment.getApplication()

  private fun source(
    id: String,
    version: String = "1.0",
    permissions: String = "'input', 'speech'",
    code: String = "sf.log('one')",
  ): String =
    """
    local manifest = {
      id = '$id',
      name = 'Code test',
      version = '$version',
      author = 'Me',
      minApiVersion = 1,
      testedApiVersion = 1,
      permissions = { $permissions },
      settings = { { key = 'sayApp', type = 'switch', title = 'Say app', default = true } },
      commands = { { id = 'go', title = 'Go', gesture = 'swipeUp' } },
    }
    $code
    """
      .trimIndent()

  private fun installed(
    id: String,
    granted: Set<ScriptPermission>,
    permissions: Set<ScriptPermission>,
  ): InstalledScript {
    val keys = permissions.joinToString(",") { "\"${it.key}\"" }
    return InstalledScript(
      ScriptManifest.parse(
        """{"id":"$id","name":"$id","author":"Me","minApiVersion":1,"testedApiVersion":1,"permissions":[$keys]}"""
      ),
      enabled = true,
      granted = granted,
      revision = 1,
    )
  }

  @Test
  fun withMain_replacesMainAndKeepsTheOtherFiles() {
    val sound = byteArrayOf(1, 2, 3)
    val files =
      mapOf(
        ScriptPackage.MAIN to source("zip-script").encodeToByteArray(),
        "sounds/ding.ogg" to sound,
      )

    val pkg = ScriptPackage.withMain(files, source("zip-script", version = "2.0"))

    assertEquals("zip-script", pkg.manifest.id)
    assertEquals("2.0", pkg.manifest.version)
    assertArrayEquals(sound, pkg.files["sounds/ding.ogg"])
    assertEquals(source("zip-script", version = "2.0"), pkg.files[ScriptPackage.MAIN]!!.decodeToString())
  }

  @Test
  fun withMain_refusesWhatAnImportRefuses() {
    assertThrows(ManifestException::class.java) {
      ScriptPackage.withMain(emptyMap(), "sf.log('no manifest')")
    }
    // The manifest needs author.
    assertThrows(ManifestException::class.java) {
      ScriptPackage.withMain(
        emptyMap(),
        "local manifest = { id = 'x', name = 'X', minApiVersion = 1, testedApiVersion = 1 }",
      )
    }
    val large = source("large") + "\n--" + "x".repeat(ScriptPackage.MAX_LUA_BYTES)
    assertThrows(ManifestException::class.java) { ScriptPackage.withMain(emptyMap(), large) }
  }

  @Test
  fun withMain_readsCodeAsReadReadsALuaFile() {
    val text = source("same-as-import")

    val typed = ScriptPackage.withMain(emptyMap(), text)
    val imported = ScriptPackage.read(text.encodeToByteArray())

    assertEquals(imported.manifest.json, typed.manifest.json)
    assertEquals(imported.files.keys, typed.files.keys)
  }

  @Test
  fun idChanged_onlyForAnotherId() {
    val pkg = ScriptPackage.withMain(emptyMap(), source("first"))

    assertFalse(ScriptCode.idChanged("first", pkg))
    assertTrue(ScriptCode.idChanged("second", pkg))
  }

  @Test
  fun grantedAfter_keepsChoicesAndAllowsNewPermissions() {
    val existing =
      installed(
        "perm",
        granted = setOf(ScriptPermission.SCREEN),
        permissions = setOf(ScriptPermission.SCREEN, ScriptPermission.SPEECH),
      )
    val asked = setOf(ScriptPermission.SPEECH, ScriptPermission.SCREEN, ScriptPermission.INPUT)

    assertEquals(setOf(ScriptPermission.INPUT), ScriptInstallQuestion.addedPermissions(existing, asked))
    // Speech stays turned off, as the user left it; input is new and listed in the install dialog.
    assertEquals(
      setOf(ScriptPermission.SCREEN, ScriptPermission.INPUT),
      ScriptInstallQuestion.grantedAfter(existing, asked),
    )
    // A permission the code no longer asks for is dropped.
    assertEquals(
      emptySet<ScriptPermission>(),
      ScriptInstallQuestion.grantedAfter(existing, setOf(ScriptPermission.SPEECH)),
    )
    assertTrue(ScriptInstallQuestion.addedPermissions(existing, setOf(ScriptPermission.SPEECH)).isEmpty())
    assertEquals(asked, ScriptInstallQuestion.grantedAfter(null, asked))
  }

  @Test
  fun edited_replacesTheCodeAndKeepsTheScriptsStateAndChoices() {
    val store = ScriptStore.get(context)
    val id = "code-edit-${System.nanoTime()}"
    store.install(
      ScriptPackage.read(source(id).encodeToByteArray()),
      setOf(ScriptPermission.INPUT),
      origin = "github.com/owner/scripts\n$id.lua",
    )
    val before = store.find(id)!!
    val command = before.manifest.commands.single()
    val gesture = command.bindings.single()
    store.setEnabled(id, false)
    store.setKeepRunning(id, true)
    store.setSetting(id, "sayApp", "false")
    store.setBindingOn(id, command.id, gesture, false)
    store.setBinding(id, command, CommandBinding.MenuItem, true)
    assertTrue(store.storageSet(id, "count", "3"))
    val script = store.find(id)!!

    val pkg = ScriptCode.edited(store, id, source(id, version = "1.1", code = "sf.log('two')"))
    assertFalse(ScriptCode.idChanged(id, pkg))
    assertTrue(ScriptInstallQuestion.addedPermissions(script, pkg.manifest.permissions).isEmpty())
    store.install(pkg, ScriptInstallQuestion.grantedAfter(script, pkg.manifest.permissions))

    val after = store.find(id)!!
    assertEquals(id, after.id)
    assertEquals("1.1", after.manifest.version)
    assertFalse(after.enabled)
    assertTrue(after.keepRunning)
    assertEquals(script.origin, after.origin)
    assertEquals(setOf(ScriptPermission.INPUT), after.granted)
    assertEquals("false", store.settingJson(after, "sayApp"))
    assertFalse(store.isBindingOn(id, command.id, gesture))
    assertTrue(
      store.bindingsOf(id, after.manifest.commands.single()).any { it.key == CommandBinding.MenuItem.key }
    )
    assertEquals("3", store.storageGet(id, "count"))
    assertTrue(store.readFile(id, ScriptPackage.MAIN)!!.decodeToString().endsWith("sf.log('two')"))
    assertEquals(1, store.all().count { it.id == id })
    store.remove(id)
  }
}
