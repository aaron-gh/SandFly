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

package com.google.android.accessibility.talkback.soundthemes

import com.google.android.accessibility.talkback.backup.SettingsBackup
import com.google.android.accessibility.talkback.soundthemes.SoundFolder.Entry
import java.text.Collator
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SoundFolderTest {
  @Test
  fun soundFilesAreFoundByExtensionOrType() {
    assertEquals(Entry("1", "Click.WAV", false), SoundFolder.entryOf("1", "Click.WAV", null))
    assertEquals(Entry("2", "tick", false), SoundFolder.entryOf("2", "tick", "audio/x-wav"))
    assertEquals(Entry("3", "tock", false), SoundFolder.entryOf("3", "tock", "application/ogg"))
    assertEquals(Entry("4", "song.mp3", false), SoundFolder.entryOf("4", "song.mp3", "application/octet-stream"))
    assertNull(SoundFolder.entryOf("5", "notes.txt", "text/plain"))
    assertNull(SoundFolder.entryOf("6", "cover.jpg", "image/jpeg"))
  }

  @Test
  fun foldersAreListedAndHiddenFilesAreNot() {
    assertEquals(
      Entry("7", "Clicks", true),
      SoundFolder.entryOf("7", "Clicks", "vnd.android.document/directory"),
    )
    assertNull(SoundFolder.entryOf("8", ".trashed-click.wav", "audio/x-wav"))
    assertNull(SoundFolder.entryOf("9", ".thumbnails", "vnd.android.document/directory"))
  }

  @Test
  fun foldersComeFirstThenFilesByName() {
    val entries =
      listOf(
        Entry("a", "beep.wav", false),
        Entry("b", "Zebra", true),
        Entry("c", "Alarm.ogg", false),
        Entry("d", "clicks", true),
      )
    assertEquals(
      listOf("clicks", "Zebra", "Alarm.ogg", "beep.wav"),
      SoundFolder.sorted(entries, Collator.getInstance(Locale.ENGLISH)).map { it.name },
    )
  }

  @Test
  fun theFolderStaysOnThePhoneWhenAProfileIsApplied() {
    assertTrue(SettingsBackup.isKeptOutOfProfiles(SoundFolder.PREF_FOLDER))
  }
}
