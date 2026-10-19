package com.google.android.accessibility.talkback.soundthemes

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ThemeCreationTest {
  private val context: Context get() = RuntimeEnvironment.getApplication()

  @Test fun draftIsHiddenAndDoesNotChangeActiveThemeUntilSaved() {
    val prefs = context.getSharedPreferences("creation-test", Context.MODE_PRIVATE)
    val draft = SoundThemes.createDraft(context, SoundThemeManifest("My sounds", author = "Tester"))
    assertFalse(SoundThemes.installed(context).any { it.id == draft.id })
    assertEquals(SoundThemes.BACKTALK, SoundThemes.activeId(prefs))
    val saved = SoundThemes.saveDraft(context, draft.id)
    assertTrue(SoundThemes.installed(context).any { it.id == saved.id })
    SoundThemes.activate(context, prefs, saved.id)
    assertEquals(saved.id, SoundThemes.activeId(prefs))
    assertEquals("Tester", SoundThemes.active(context, prefs).manifest.author)
  }

  @Test fun duplicateNameDoesNotOverwriteAnExistingTheme() {
    val first = SoundThemes.saveDraft(context,
      SoundThemes.createDraft(context, SoundThemeManifest("Same", author = "First")).id)
    val second = SoundThemes.createDraft(context, SoundThemeManifest("Same", author = "Second"))
    assertThrows(IOException::class.java) { SoundThemes.saveDraft(context, second.id) }
    assertEquals("First", SoundThemes.theme(context, first.id)!!.manifest.author)
    assertNotNull(SoundThemes.theme(context, second.id))
  }

  @Test fun draftExportsMetadataAndChosenSoundsAsImportableZip() {
    val draft = SoundThemes.createDraft(context, SoundThemeManifest(
      "Exported", author = "Tester", description = "My theme", website = "https://example.com"))
    // Storage/export should preserve a selected sound byte-for-byte.
    val sound = byteArrayOf(1, 2, 3, 4)
    java.io.File(draft.directory, SoundThemes.fileName("focus", "wav", 1)).writeBytes(sound)
    val prefs = context.getSharedPreferences("export-test", Context.MODE_PRIVATE)
    val output = ByteArrayOutputStream()
    assertEquals(1, SoundThemes.export(context, prefs, draft.id, output))
    val entries = mutableMapOf<String, ByteArray>()
    ZipInputStream(output.toByteArray().inputStream()).use { zip ->
      while (true) {
        val entry = zip.nextEntry ?: break
        entries[entry.name] = zip.readBytes()
      }
    }
    val manifest = SoundThemeManifest.parse(String(entries.getValue("theme.json")), emptySet(), "")
    assertEquals("My theme", manifest.description)
    assertEquals("Tester", manifest.author)
    assertNull(manifest.license)
    assertArrayEquals(sound, entries.getValue("focus.wav"))
    assertFalse(SoundThemes.installed(context).any { it.id == draft.id })
  }

  @Test fun untouchedSettingsRemainUnsetAndControlSoundsEnableOnActivation() {
    val draft = SoundThemes.createDraft(context, SoundThemeManifest("Controls"))
    java.io.File(draft.directory, SoundThemes.fileName("control_button", "wav", 1)).writeBytes(byteArrayOf(1))
    val storage = context.createDeviceProtectedStorageContext()
    val prefs = storage.getSharedPreferences(SoundThemes.draftPreferencesName(draft.id), Context.MODE_PRIVATE)
    prefs.edit().putString(SoundThemes.PREF_ACTIVE, draft.id).commit()
    SoundThemes.saveSettings(context, prefs)
    val manifest = SoundThemes.theme(context, draft.id)!!.manifest
    assertNull(manifest.controlSounds)
    assertNull(manifest.audio3d)
    val saved = SoundThemes.saveDraft(context, draft.id)
    assertFalse(java.io.File(storage.dataDir, "shared_prefs/${SoundThemes.draftPreferencesName(draft.id)}.xml").exists())
    val activePrefs = context.getSharedPreferences("activate-controls", Context.MODE_PRIVATE)
    SoundThemes.activate(context, activePrefs, saved.id)
    assertTrue(com.google.android.accessibility.talkback.controlsounds.ControlSoundsSettings.isOn(activePrefs))
  }

  @Test fun explicitControlSoundChoiceIsSaved() {
    val draft = SoundThemes.createDraft(context, SoundThemeManifest("Explicit"))
    val prefs = context.getSharedPreferences("explicit-controls", Context.MODE_PRIVATE)
    prefs.edit().putString(SoundThemes.PREF_ACTIVE, draft.id)
      .putBoolean(com.google.android.accessibility.talkback.controlsounds.ControlSoundsSettings.PREF_ON, false).commit()
    SoundThemes.saveSettings(context, prefs)
    assertEquals(false, SoundThemes.theme(context, draft.id)!!.manifest.controlSounds)
  }

  @Test fun discardRemovesDraftAndPreferences() {
    val draft = SoundThemes.createDraft(context, SoundThemeManifest("Discard"))
    val storage = context.createDeviceProtectedStorageContext()
    val name = SoundThemes.draftPreferencesName(draft.id)
    storage.getSharedPreferences(name, Context.MODE_PRIVATE).edit().putString(SoundThemes.PREF_ACTIVE, draft.id).commit()
    SoundThemes.discardDraft(context, draft.id)
    assertFalse(draft.directory.exists())
    assertFalse(java.io.File(storage.dataDir, "shared_prefs/$name.xml").exists())
  }

  @Test fun authoredVibrationSurvivesFeedbackExportAndSoundRemoval() {
    val draft = SoundThemes.createDraft(context, SoundThemeManifest("Authored", vibrations = mapOf("focus" to "none")))
    java.io.File(draft.directory, SoundThemes.fileName("focus", "wav", 1)).writeBytes(byteArrayOf(1))
    val prefs = context.getSharedPreferences("authored", Context.MODE_PRIVATE)
    prefs.edit().putString(SoundThemes.PREF_ACTIVE, draft.id).commit()
    SoundThemes.feedback(context, prefs)
    SoundThemes.export(context, prefs, draft.id, ByteArrayOutputStream())
    assertEquals("none", SoundThemes.theme(context, draft.id)!!.manifest.vibrations["focus"])
    SoundThemes.removeSound(context, prefs, SoundThemes.SOUNDS.first { it.key == "focus" })
    assertEquals("none", SoundThemes.theme(context, draft.id)!!.manifest.vibrations["focus"])
  }

  @Test fun missingDraftNeverFallsBackToBacktalk() {
    val draft = SoundThemes.createDraft(context, SoundThemeManifest("Missing"))
    val prefs = context.getSharedPreferences("missing-draft", Context.MODE_PRIVATE)
    prefs.edit().putString(SoundThemes.PREF_ACTIVE, draft.id).commit()
    draft.directory.deleteRecursively()
    assertEquals(SoundThemes.BACKTALK, SoundThemes.active(context, prefs).id)
    assertThrows(IOException::class.java) { SoundThemes.removeAllSounds(context, prefs) }
    assertThrows(IOException::class.java) { SoundThemes.saveSettings(context, prefs) }
  }

  @Test fun duplicateDraftCanBeRenamedWithoutLosingSounds() {
    SoundThemes.saveDraft(context, SoundThemes.createDraft(context, SoundThemeManifest("Taken")).id)
    val draft = SoundThemes.createDraft(context, SoundThemeManifest("Taken"))
    val file = java.io.File(draft.directory, SoundThemes.fileName("focus", "wav", 1))
    file.writeBytes(byteArrayOf(1, 2))
    assertThrows(SoundThemes.DuplicateThemeException::class.java) { SoundThemes.saveDraft(context, draft.id) }
    SoundThemes.renameDraft(context, draft.id, "Available")
    val saved = SoundThemes.saveDraft(context, draft.id)
    assertArrayEquals(byteArrayOf(1, 2), SoundThemes.soundFiles(saved).getValue("focus").readBytes())
  }

  @Test fun orphanPreferencesAreRemovedWhenListingThemes() {
    val draft = SoundThemes.createDraft(context, SoundThemeManifest("Orphan"))
    val storage = context.createDeviceProtectedStorageContext()
    val name = SoundThemes.draftPreferencesName(draft.id)
    storage.getSharedPreferences(name, Context.MODE_PRIVATE).edit().putString("test", "value").commit()
    draft.directory.deleteRecursively()
    SoundThemes.installed(context)
    assertFalse(java.io.File(storage.dataDir, "shared_prefs/$name.xml").exists())
  }

  @Test fun draftsBeingEditedOutliveInstallLeftovers() {
    val storage = context.createDeviceProtectedStorageContext()
    val twoHoursAgo = System.currentTimeMillis() - 2 * 60 * 60 * 1000
    val draft = SoundThemes.createDraft(context, SoundThemeManifest("Editing"))
    storage.getSharedPreferences(SoundThemes.draftPreferencesName(draft.id), Context.MODE_PRIVATE)
      .edit().putString(SoundThemes.PREF_ACTIVE, draft.id).commit()
    draft.directory.setLastModified(twoHoursAgo)
    val leftover = SoundThemes.createDraft(context, SoundThemeManifest("Leftover"))
    leftover.directory.setLastModified(twoHoursAgo)
    SoundThemes.installed(context)
    assertTrue(draft.directory.isDirectory)
    assertFalse(leftover.directory.exists())
    val leftoverPrefs = SoundThemes.draftPreferencesName(leftover.id)
    assertFalse(java.io.File(storage.dataDir, "shared_prefs/$leftoverPrefs.xml").exists())

    draft.directory.setLastModified(System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000)
    SoundThemes.installed(context)
    assertFalse(draft.directory.exists())
  }

  @Test fun touchingADraftKeepsIt() {
    val draft = SoundThemes.createDraft(context, SoundThemeManifest("Touched"))
    draft.directory.setLastModified(0)
    assertTrue(SoundThemes.touchDraft(context, draft.id))
    assertTrue(draft.directory.lastModified() > 0)
    draft.directory.deleteRecursively()
    assertFalse(SoundThemes.touchDraft(context, draft.id))
  }
}
