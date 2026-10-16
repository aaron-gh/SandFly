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

package com.google.android.accessibility.talkback.soundthemes

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.core.content.ContextCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.controlsounds.ControlSounds
import com.google.android.accessibility.talkback.controlsounds.ControlSoundsSettings
import com.google.android.accessibility.talkback.individualfeedback.FeedbackItem
import com.google.android.accessibility.talkback.individualfeedback.IndividualFeedbackSettings
import com.google.android.accessibility.talkback.individualfeedback.SoundVibrations
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.json.JSONException

/** An installed sound theme. */
data class SoundTheme(val id: String, val manifest: SoundThemeManifest, val directory: File) {
  val isBuiltIn: Boolean
    get() = id == SoundThemes.BACKTALK
}

/** A theme unpacked from a theme file, waiting for the person to install it or not. */
data class StagedTheme(
  val id: String,
  val manifest: SoundThemeManifest,
  val directory: File,
  /** The sounds it replaces, in the order of Individual sounds and vibrations. */
  val sounds: List<FeedbackItem>,
  /** Files in the theme that are not used, with why. */
  val skipped: List<String>,
)

/** The sounds and vibrations of the theme in use, for the feedback controller. */
class ThemeFeedback(
  /** Sound files by the resource names of the sounds they replace. */
  val soundPaths: Map<String, String>,
  /**
   * Vibration patterns by the names they play under: the resource names of the sounds they play
   * with, of the patterns they replace, and of vibrations without a sound.
   */
  val vibrations: Map<String, IntArray>,
) {
  /**
   * The vibrations, with the ones whose switch is in [mutedVibrations] made empty, so that they
   * play nothing rather than the vibration they replace. The braille keyboard's vibrations that
   * are turned off are empty too, theme or not, as the keyboard plays its own vibrations only
   * when the theme leaves them alone.
   */
  fun vibrationsPlaying(mutedVibrations: Set<String>): Map<String, IntArray> {
    val playing =
      vibrations.mapValues { (name, pattern) ->
        if (SoundVibrations.switchOfPlayed(name) in mutedVibrations) IntArray(0) else pattern
      }
    val mutedKeyboard =
      SoundVibrations.BRAILLE_KEYBOARD.filter { it in mutedVibrations && it !in playing }
    return if (mutedKeyboard.isEmpty()) playing
    else playing + mutedKeyboard.associateWith { IntArray(0) }
  }

  /** The sounds the theme gives a vibration that can be felt, by resource name. */
  fun felt(): Set<String> = vibrations.filterValues { it.isNotEmpty() }.keys
}

/**
 * Sound themes: sets of sounds, vibrations and settings that replace Backtalk's own. Backtalk's
 * own sounds are the Backtalk theme, which is always there. Any theme can have sounds of the
 * person's own, so the Backtalk theme is where single sounds are replaced without installing
 * anything.
 *
 * Each theme is a folder in the app's device protected storage, so it plays before the first
 * unlock too. It holds theme.json, the sound files, named key.stamp.extension so that a new sound
 * always has a new path, and the license and readme files that came with it. See themes.md at the
 * top of the repository for the theme file format.
 *
 * Control sounds and 3D audio are settings of each theme. The settings screens change the usual
 * preferences, which hold the settings of the theme in use, and they are saved to the theme when
 * another theme is put in use or the theme is saved as a file.
 */
object SoundThemes {
  const val PREF_ACTIVE = "pref_sound_theme"

  /** Changed whenever the theme in use changes, so that the service reloads it. */
  const val PREF_CHANGED = "pref_sound_theme_changed"

  const val BACKTALK = "backtalk"

  /** Sounds are short, so a larger file is a mistake, and a theme cannot fill the storage. */
  const val MAX_SOUND_BYTES = 5L * 1024 * 1024
  const val MAX_THEME_BYTES = 50L * 1024 * 1024
  private const val MAX_DOCUMENT_BYTES = 1L * 1024 * 1024
  private const val MAX_ENTRIES = 1000

  private const val DIRECTORY = "sound_themes"
  private const val STAGING_PREFIX = ".staging-"
  private const val STAGING_MAX_AGE_MS = 60L * 60 * 1000
  private const val TAG = "SoundThemes"

  /** The formats Android can play, as file extensions. */
  val EXTENSIONS = setOf("wav", "ogg", "oga", "opus", "mp3", "flac", "m4a", "aac")

  /** License, readme and credits files, which stay with the theme when it is shared again. */
  private val DOCUMENT = Regex("(license|licence|copying|readme|notice|authors|credits)(\\.(txt|md))?")

  enum class Result {
    OK,
    TOO_LARGE,
    NOT_AUDIO,
    FAILED,
  }

  /** Thrown when a file is not a sound theme, with a message for the person installing it. */
  class NotAThemeException(message: String) : Exception(message)

  /**
   * The braille keyboard's typing sounds, which are Android's keyboard sounds unless the theme
   * replaces them, with the Android sound effect each replaces. They play only with the braille
   * keyboard's typing sounds setting on.
   */
  @JvmField
  val BRAILLE_TYPING_EFFECTS: Map<String, Int> =
    linkedMapOf(
      "braille_keyboard_character" to AudioManager.FX_KEYPRESS_STANDARD,
      "braille_keyboard_space" to AudioManager.FX_KEYPRESS_SPACEBAR,
      "braille_keyboard_delete" to AudioManager.FX_KEYPRESS_DELETE,
      "braille_keyboard_new_line" to AudioManager.FX_KEYPRESS_RETURN,
    )

  private val BRAILLE_TYPING_SOUNDS =
    listOf(
      FeedbackItem("braille_keyboard_character", R.string.theme_sound_braille_character),
      FeedbackItem("braille_keyboard_space", R.string.theme_sound_braille_space),
      FeedbackItem("braille_keyboard_delete", R.string.theme_sound_braille_delete),
      FeedbackItem("braille_keyboard_new_line", R.string.theme_sound_braille_new_line),
    )

  /** The braille keyboard's typing sounds among the theme's sound files by name. */
  @JvmStatic
  fun brailleTypingSounds(soundPaths: Map<String, String>): Map<String, String> =
    soundPaths.filterKeys { it in BRAILLE_TYPING_EFFECTS }

  /** Every sound a theme can replace, in the order the settings show them. */
  @JvmStatic
  val SOUNDS: List<FeedbackItem>
    get() = IndividualFeedbackSettings.SOUNDS + BRAILLE_TYPING_SOUNDS

  val SOUND_KEYS: Set<String>
    get() = SOUNDS.map { it.key }.toSet()

  /** The names of the vibrations a theme can replace. */
  val VIBRATION_NAMES: Set<String>
    get() = SoundVibrations.themeNames(IndividualFeedbackSettings.SOUNDS.map { it.key })

  // ---------------------------------------------------------------------------------------------
  // Installed themes

  /** The installed themes, Backtalk's first and then by name. */
  @JvmStatic
  fun installed(context: Context): List<SoundTheme> {
    val themes =
      themesDirectory(context)
        .listFiles()
        .orEmpty()
        .filter { it.isDirectory && it.name != BACKTALK }
        .mapNotNull { directory ->
          if (directory.name.startsWith(STAGING_PREFIX)) {
            // Left over from an install that never finished, unless one is still going on.
            if (System.currentTimeMillis() - directory.lastModified() > STAGING_MAX_AGE_MS) {
              directory.deleteRecursively()
            }
            null
          } else {
            read(directory)
          }
        }
        .sortedBy { it.manifest.name.lowercase(Locale.getDefault()) }
    return listOf(backtalk(context)) + themes
  }

  @JvmStatic fun activeId(prefs: SharedPreferences): String = prefs.getString(PREF_ACTIVE, BACKTALK)!!

  /** The theme in use, or Backtalk's if the one in use is gone. */
  @JvmStatic
  fun active(context: Context, prefs: SharedPreferences): SoundTheme =
    theme(context, activeId(prefs)) ?: backtalk(context)

  @JvmStatic
  fun theme(context: Context, id: String): SoundTheme? =
    if (id == BACKTALK) backtalk(context) else read(File(themesDirectory(context), id))

  /** The sound files of [theme], by item key. */
  @JvmStatic
  fun soundFiles(theme: SoundTheme): Map<String, File> {
    val keys = SOUND_KEYS
    return theme.directory
      .listFiles()
      .orEmpty()
      .mapNotNull { file -> keyOfFile(file.name)?.takeIf { it in keys }?.let { it to file } }
      .toMap()
  }

  /** The custom sound of [item] in the theme in use, or null if it plays Backtalk's sound. */
  @JvmStatic
  fun soundFile(context: Context, prefs: SharedPreferences, item: FeedbackItem): File? =
    soundFiles(active(context, prefs))[item.key]

  /**
   * The sounds and vibrations of the theme in use. An item with several sounds, like the circle
   * menu, plays its theme sound and vibration for all of them.
   */
  @JvmStatic
  fun feedback(context: Context, prefs: SharedPreferences): ThemeFeedback {
    val theme = active(context, prefs)
    val items = SOUNDS.associateBy { it.key }
    val paths = HashMap<String, String>()
    for ((key, file) in soundFiles(theme)) {
      items[key]?.resourceNames?.forEach { paths[it] = file.path }
    }
    val vibrations =
      SoundVibrations.playedAs(
        theme.manifest.vibrationPatterns(),
        IndividualFeedbackSettings.SOUNDS.associate { it.key to it.resourceNames },
      )
    return ThemeFeedback(paths, vibrations)
  }

  /**
   * Puts the theme [id] in use, with its settings. Unless [saveCurrent] is false, the settings of
   * the theme in use until now are saved to it first.
   */
  @JvmStatic
  @JvmOverloads
  fun activate(
    context: Context,
    prefs: SharedPreferences,
    id: String,
    saveCurrent: Boolean = true,
  ) {
    if (saveCurrent) saveSettings(context, prefs)
    val theme = theme(context, id) ?: return
    val manifest = theme.manifest
    prefs
      .edit()
      .putString(PREF_ACTIVE, theme.id)
      .putBoolean(
        ControlSoundsSettings.PREF_ON,
        manifest.controlSounds
          ?: (soundFiles(theme).keys + manifest.vibrations.keys).any { it in ControlSounds.SOUNDS },
      )
      .putString(
        ControlSoundsSettings.PREF_3D,
        manifest.audio3d ?: ControlSoundsSettings.VALUE_3D_WITH_HEADPHONES,
      )
      .putLong(PREF_CHANGED, System.currentTimeMillis())
      .apply()
  }

  /** Saves the control sounds and 3D audio settings to the theme in use. */
  @JvmStatic
  fun saveSettings(context: Context, prefs: SharedPreferences) {
    val theme = active(context, prefs)
    val manifest =
      theme.manifest.copy(
        controlSounds = ControlSoundsSettings.isOn(prefs),
        audio3d =
          prefs.getString(ControlSoundsSettings.PREF_3D, ControlSoundsSettings.VALUE_3D_WITH_HEADPHONES),
      )
    if (manifest != theme.manifest) write(theme.copy(manifest = manifest))
  }

  /** Removes the theme [id], and puts Backtalk's theme in use if it was in use. */
  @JvmStatic
  fun delete(context: Context, prefs: SharedPreferences, id: String) {
    if (id == BACKTALK) return
    if (activeId(prefs) == id) activate(context, prefs, BACKTALK, saveCurrent = false)
    File(themesDirectory(context), id).deleteRecursively()
  }

  // ---------------------------------------------------------------------------------------------
  // Sounds of the theme in use

  /** Makes [input], a file whose name ends in [extension], the sound of [item]. */
  fun setSound(
    context: Context,
    prefs: SharedPreferences,
    item: FeedbackItem,
    input: InputStream,
    extension: String,
  ): Result {
    val theme = active(context, prefs)
    theme.directory.mkdirs()
    val old = soundFiles(theme)[item.key]
    val file = File(theme.directory, fileName(item.key, extension, System.currentTimeMillis()))
    val result = copySound(input, file, MAX_SOUND_BYTES)
    if (result != Result.OK) return result
    old?.delete()
    changed(prefs)
    return Result.OK
  }

  /** Makes [item] play Backtalk's sound again in the theme in use. */
  fun removeSound(context: Context, prefs: SharedPreferences, item: FeedbackItem) {
    soundFiles(active(context, prefs))[item.key]?.delete()
    changed(prefs)
  }

  /** Makes every item play Backtalk's sound again in the theme in use. */
  fun removeAllSounds(context: Context, prefs: SharedPreferences) {
    soundFiles(active(context, prefs)).values.forEach { it.delete() }
    changed(prefs)
  }

  private fun changed(prefs: SharedPreferences) {
    prefs.edit().putLong(PREF_CHANGED, System.currentTimeMillis()).apply()
  }

  // ---------------------------------------------------------------------------------------------
  // Theme files

  /** Saves the theme [id] as a theme file, and returns how many sounds it has. */
  @Throws(IOException::class)
  fun export(context: Context, prefs: SharedPreferences, id: String, output: OutputStream): Int {
    if (id == activeId(prefs)) saveSettings(context, prefs)
    val theme = theme(context, id) ?: throw IOException("No theme $id")
    var count = 0
    ZipOutputStream(output).use { zip ->
      zip.putNextEntry(ZipEntry(SoundThemeManifest.FILE_NAME))
      zip.write(theme.manifest.toJson().toByteArray())
      zip.closeEntry()
      val sounds = soundFiles(theme)
      for (item in SOUNDS) {
        val file = sounds[item.key] ?: continue
        zip.putNextEntry(ZipEntry("${item.key}.${file.extension}"))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
        count++
      }
      theme.directory.listFiles().orEmpty().filter { isDocument(it.name) }.forEach { file ->
        zip.putNextEntry(ZipEntry(file.name))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
      }
    }
    return count
  }

  /**
   * Unpacks the theme file [zip] so it can be installed. [fallbackName] names a theme without a
   * theme.json, such as a ZIP file of sounds alone.
   */
  @Throws(IOException::class, NotAThemeException::class)
  fun stage(context: Context, zip: File, fallbackName: String): StagedTheme {
    val items = SOUNDS.associateBy { it.key }
    val staging = File(themesDirectory(context), STAGING_PREFIX + System.currentTimeMillis())
    staging.mkdirs()
    try {
      ZipFile(zip).use { file ->
        val entries = file.entries().toList().filter { !it.isDirectory }
        if (entries.size > MAX_ENTRIES) throw NotAThemeException("too many files")
        val layout = ThemeLayout.of(entries.map { it.name })
        val manifest =
          layout.manifest?.let { path ->
            val text =
              file.getInputStream(file.getEntry(path)).use {
                String(readLimited(it, MAX_DOCUMENT_BYTES), Charsets.UTF_8)
              }
            try {
              SoundThemeManifest.parse(text, VIBRATION_NAMES, fallbackName)
            } catch (e: JSONException) {
              throw NotAThemeException("theme.json: ${e.message}")
            }
          } ?: SoundThemeManifest(name = fallbackName)

        val sounds = LinkedHashSet<String>()
        val skipped = ArrayList<String>()
        var total = 0L
        for ((path, role) in layout.files) {
          val name = path.substringAfterLast('/')
          when (role) {
            ThemeLayout.Role.SOUND -> {
              val (key, extension) = parseSoundName(name)!!
              if (key !in items) {
                skipped += "$name: there is no sound called $key"
                continue
              }
              val target = File(staging, fileName(key, extension, System.currentTimeMillis()))
              // Every byte read counts toward the theme's total, even of sounds that are refused,
              // so that a small ZIP of many large, refused sounds cannot write and check gigabytes.
              val counted = CountingInputStream(file.getInputStream(file.getEntry(path)))
              val result = counted.use { copySound(it, target, MAX_SOUND_BYTES) }
              total += counted.count
              when (result) {
                Result.OK -> {
                  // A theme with two files for one sound keeps the last.
                  if (key in sounds) {
                    staging.listFiles().orEmpty()
                      .filter { keyOfFile(it.name) == key && it != target }
                      .forEach { it.delete() }
                  }
                  sounds += key
                }
                Result.TOO_LARGE -> skipped += "$name: larger than 5 MB"
                else -> skipped += "$name: Android can't play it"
              }
            }
            ThemeLayout.Role.DOCUMENT -> {
              val target = File(staging, name.lowercase(Locale.ROOT))
              file.getInputStream(file.getEntry(path)).use { input ->
                target.writeBytes(readLimited(input, MAX_DOCUMENT_BYTES))
              }
              total += target.length()
            }
          }
          if (total > MAX_THEME_BYTES) throw NotAThemeException("larger than 50 MB")
        }
        if (layout.manifest == null && sounds.isEmpty()) {
          throw NotAThemeException(
            "it has no theme.json and no sounds named after Backtalk's, such as focus.wav"
          )
        }
        File(staging, SoundThemeManifest.FILE_NAME).writeText(manifest.toJson())
        return StagedTheme(
          id = idFor(manifest.name),
          manifest = manifest,
          directory = staging,
          sounds = SOUNDS.filter { it.key in sounds },
          skipped = skipped + manifest.warnings,
        )
      }
    } catch (e: Exception) {
      staging.deleteRecursively()
      throw e
    }
  }

  /** Throws away a theme that was not installed. */
  fun discard(staged: StagedTheme) {
    staged.directory.deleteRecursively()
  }

  /** Whether installing [staged] replaces an installed theme of the same name. */
  fun replacesInstalled(context: Context, staged: StagedTheme): Boolean =
    File(themesDirectory(context), staged.id).isDirectory

  /** Installs [staged], replacing a theme of the same name, and puts it in use if [use]. */
  @Throws(IOException::class)
  fun install(context: Context, prefs: SharedPreferences, staged: StagedTheme, use: Boolean) {
    val target = File(themesDirectory(context), staged.id)
    val wasActive = activeId(prefs) == staged.id
    if (use && !wasActive) saveSettings(context, prefs)
    target.deleteRecursively()
    if (!staged.directory.renameTo(target)) {
      staged.directory.deleteRecursively()
      throw IOException("Cannot install theme ${staged.id}")
    }
    if (use || wasActive) {
      // The theme's settings replace those of the theme it replaces.
      activate(context, prefs, staged.id, saveCurrent = false)
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Names

  /** Returns the item key and extension of a sound file called [name], or null if it is not one. */
  @JvmStatic
  fun parseSoundName(name: String): Pair<String, String>? {
    if (name.startsWith(".") || !name.contains('.')) return null
    val extension = name.substringAfterLast('.').lowercase(Locale.ROOT)
    if (extension !in EXTENSIONS) return null
    return name.substringBeforeLast('.').lowercase(Locale.ROOT) to extension
  }

  @JvmStatic
  fun isDocument(name: String): Boolean = DOCUMENT.matches(name.lowercase(Locale.ROOT))

  /** Returns the extension of a sound file called [name], or null if it is not a known format. */
  @JvmStatic
  fun extensionOf(name: String?): String? =
    name?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)?.takeIf { it in EXTENSIONS }

  @JvmStatic
  fun fileName(key: String, extension: String, stamp: Long): String = "$key.$stamp.$extension"

  /** Returns the item key of a stored sound file, or null if the name is not one. */
  @JvmStatic
  fun keyOfFile(name: String): String? {
    val parts = name.split('.')
    return if (parts.size == 3 && parts[0].isNotEmpty() && parts[2] in EXTENSIONS) parts[0]
    else null
  }

  /**
   * The folder name of a theme, from its name, so that a theme of the same name replaces it. It is
   * never Backtalk's own.
   */
  @JvmStatic
  fun idFor(name: String): String {
    val slug =
      name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-').take(60).ifEmpty {
        "theme"
      }
    return if (slug == BACKTALK) "$slug-theme" else slug
  }

  // ---------------------------------------------------------------------------------------------
  // Storage

  private fun backtalk(context: Context): SoundTheme {
    val directory = File(themesDirectory(context), BACKTALK)
    val stored = read(directory)
    return SoundTheme(
      BACKTALK,
      stored?.manifest ?: SoundThemeManifest(name = BACKTALK_NAME),
      directory,
    )
  }

  private fun read(directory: File): SoundTheme? {
    val file = File(directory, SoundThemeManifest.FILE_NAME)
    if (!file.isFile) return null
    return try {
      SoundTheme(
        directory.name,
        SoundThemeManifest.parse(file.readText(), VIBRATION_NAMES, directory.name),
        directory,
      )
    } catch (e: JSONException) {
      LogUtils.w(TAG, "Cannot read theme %s: %s", directory.name, e)
      null
    } catch (e: IOException) {
      LogUtils.w(TAG, "Cannot read theme %s: %s", directory.name, e)
      null
    }
  }

  private fun write(theme: SoundTheme) {
    theme.directory.mkdirs()
    File(theme.directory, SoundThemeManifest.FILE_NAME).writeText(theme.manifest.toJson())
  }

  /** An input stream that counts the bytes read from it. */
  private class CountingInputStream(input: InputStream) : FilterInputStream(input) {
    var count = 0L
      private set

    override fun read(): Int = super.read().also { if (it >= 0) count++ }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
      super.read(buffer, offset, length).also { if (it > 0) count += it }
  }

  /** Copies a sound into [file], keeping it only if it is small enough and Android can play it. */
  private fun copySound(input: InputStream, file: File, maxBytes: Long): Result {
    try {
      file.outputStream().use { output ->
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
          val count = input.read(buffer)
          if (count < 0) break
          total += count
          if (total > maxBytes) {
            output.close()
            file.delete()
            return Result.TOO_LARGE
          }
          output.write(buffer, 0, count)
        }
      }
    } catch (e: IOException) {
      LogUtils.w(TAG, "Cannot copy sound: %s", e)
      file.delete()
      return Result.FAILED
    }
    if (!isAudio(file)) {
      file.delete()
      return Result.NOT_AUDIO
    }
    return Result.OK
  }

  private fun readLimited(input: InputStream, maxBytes: Long): ByteArray {
    val bytes = input.readNBytesCompat(maxBytes + 1)
    if (bytes.size > maxBytes) throw NotAThemeException("a text file is larger than 1 MB")
    return bytes
  }

  private fun InputStream.readNBytesCompat(limit: Long): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (out.size() < limit) {
      val count = read(buffer, 0, minOf(buffer.size.toLong(), limit - out.size()).toInt())
      if (count < 0) break
      out.write(buffer, 0, count)
    }
    return out.toByteArray()
  }

  private fun isAudio(file: File): Boolean {
    val extractor = MediaExtractor()
    return try {
      extractor.setDataSource(file.path)
      (0 until extractor.trackCount).any {
        extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
      }
    } catch (e: Exception) {
      false
    } finally {
      extractor.release()
    }
  }

  private fun themesDirectory(context: Context): File {
    val storage = ContextCompat.createDeviceProtectedStorageContext(context) ?: context
    return File(storage.filesDir, DIRECTORY).apply { mkdirs() }
  }

  /** The name of Backtalk's theme in theme files, which the settings show translated. */
  private const val BACKTALK_NAME = "Backtalk"
}

/**
 * Which files of a theme file are used. The theme is the folder that holds theme.json, so that a
 * ZIP file of a whole repository, which puts everything in one folder, works too. Without a
 * theme.json, sounds from any folder are used.
 */
data class ThemeLayout(val manifest: String?, val files: Map<String, Role>) {
  enum class Role {
    SOUND,
    DOCUMENT,
  }

  companion object {
    @JvmStatic
    fun of(paths: List<String>): ThemeLayout {
      val usable = paths.filter { path -> path.split('/').none { it == "__MACOSX" } }
      val manifest =
        usable
          .filter { it.substringAfterLast('/') == SoundThemeManifest.FILE_NAME }
          .minByOrNull { it.count { c -> c == '/' } }
      val base = manifest?.substringBeforeLast('/', "")
      val files = LinkedHashMap<String, Role>()
      for (path in usable) {
        val folder = path.substringBeforeLast('/', "")
        if (base != null && folder != base) continue
        val name = path.substringAfterLast('/')
        when {
          SoundThemes.parseSoundName(name) != null -> files[path] = Role.SOUND
          (base != null || !folder.contains('/')) && SoundThemes.isDocument(name) ->
            files[path] = Role.DOCUMENT
        }
      }
      return ThemeLayout(manifest, files)
    }
  }
}
