package com.google.android.accessibility.utils.ocr

import android.content.Context
import android.content.res.Resources
import android.os.SystemClock
import com.google.android.accessibility.utils.mlkit.MlKitHelper
import com.google.android.accessibility.utils.output.LanguageSwitch
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import java.io.Closeable
import java.util.Locale

/**
 * Recognizes text with the ML Kit recognizer for the script of the languages in use: the speech
 * language, then the system languages. The recognizers other than the Latin one also read Latin
 * letters. While Google Play services has no model for the script, the Latin recognizer reads the
 * image and the model download is requested.
 *
 * The recognizers run in the `:mlkit` process, through [MlKitHelper], since they load Google Play
 * services code, and an update of Google Play services stops every process with its code loaded.
 * Only images made with [InputImage.fromBitmap] are recognized.
 */
class LanguageTextRecognizer
@JvmOverloads
constructor(
  context: Context,
  private val languages: () -> List<Locale> = ::languagesInUse,
  private val clock: () -> Long = SystemClock::elapsedRealtime,
  private val helper: MlKitHelper = MlKitHelper.get(context),
) : Closeable {

  /** The scripts ML Kit has a text recognizer for. */
  enum class Script {
    LATIN,
    CHINESE,
    DEVANAGARI,
    JAPANESE,
    KOREAN,
  }

  private val downloadRequested = HashSet<Script>()
  private var latinUntil = 0L
  private var closed = false

  /** The script recognized next, which is Latin while the model of the wanted one is missing. */
  @Synchronized
  fun script(): Script {
    val wanted = scriptFor(languages())
    return if (wanted != Script.LATIN && clock() < latinUntil) Script.LATIN else wanted
  }

  /** Recognizes text in [image]. The task fails if no recognizer can be made or used. */
  fun process(image: InputImage): Task<Text> {
    val script: Script
    synchronized(this) {
      if (closed) return Tasks.forException(IllegalStateException("Text recognizer closed"))
      script = script()
    }
    val bitmap =
      image.bitmapInternal
        ?: return Tasks.forException(
          MlKitException("Only bitmaps are recognized", MlKitException.INVALID_ARGUMENT)
        )
    val rotation = image.rotationDegrees
    val task = helper.recognizeText(bitmap, rotation, script)
    if (script == Script.LATIN) return task
    return task.continueWithTask { result ->
      if (result.isSuccessful || !isUnavailable(result.exception)) return@continueWithTask result
      synchronized(this) {
        if (closed) return@continueWithTask result
        LogUtils.w(TAG, "No %s text recognizer, Latin is used", script)
        latinUntil = clock() + SCRIPT_RETRY_MS
        requestDownload(script)
      }
      helper.recognizeText(bitmap, rotation, Script.LATIN)
    }
  }

  /**
   * Asks Google Play services to download the model of the recognizer last used, such as after
   * [process] failed with [MlKitException.UNAVAILABLE].
   */
  @Synchronized
  fun download(): Task<Void?> {
    if (closed) return Tasks.forException(IllegalStateException("Text recognizer closed"))
    return helper.installTextRecognizer(script())
  }

  /** Stops new requests. The recognizers in the other process stay until it is unbound. */
  @Synchronized
  override fun close() {
    closed = true
  }

  private fun requestDownload(script: Script) {
    if (!downloadRequested.add(script)) return
    helper.installTextRecognizer(script).addOnFailureListener { e ->
      LogUtils.w(TAG, "%s text recognizer not downloaded: %s", script, e)
    }
  }

  companion object {
    private const val TAG = "LanguageTextRecognizer"

    /** How long the Latin recognizer stands in after the wanted one's model was missing. */
    private const val SCRIPT_RETRY_MS = 60_000L

    /** The speech language, then the system languages. */
    @JvmStatic
    fun languagesInUse(): List<Locale> {
      val languages = ArrayList<Locale>()
      languages.add(LanguageSwitch.voiceLanguage())
      val system = Resources.getSystem().configuration.locales
      for (i in 0 until system.size()) {
        languages.add(system.get(i))
      }
      return languages
    }

    /**
     * The script of the first language that isn't written in Latin letters, or Latin. The other
     * recognizers also read Latin letters, so a second language in another script is read too.
     */
    @JvmStatic
    fun scriptFor(languages: List<Locale>): Script =
      languages.firstNotNullOfOrNull { scriptOf(it).takeIf { script -> script != Script.LATIN } }
        ?: Script.LATIN

    /** The script [language] is written in, or Latin when ML Kit has no recognizer for it. */
    @JvmStatic
    fun scriptOf(language: Locale): Script =
      when (language.script) {
        "Hans",
        "Hant",
        "Hani" -> Script.CHINESE
        "Jpan",
        "Hira",
        "Kana",
        "Hrkt" -> Script.JAPANESE
        "Kore",
        "Hang" -> Script.KOREAN
        "Deva" -> Script.DEVANAGARI
        "" -> scriptOfLanguage(language.language)
        else -> Script.LATIN
      }

    private fun scriptOfLanguage(language: String): Script =
      when (language) {
        "zh",
        "yue",
        "wuu",
        "nan",
        "hak",
        "gan",
        "hsn",
        "cmn" -> Script.CHINESE
        "ja" -> Script.JAPANESE
        "ko" -> Script.KOREAN
        // Languages written in Devanagari.
        "hi",
        "mr",
        "ne",
        "sa",
        "kok",
        "mai",
        "bho",
        "doi",
        "brx",
        "mag",
        "awa",
        "new" -> Script.DEVANAGARI
        else -> Script.LATIN
      }

    /** Whether [e] says Google Play services has no model for the recognizer yet. */
    @JvmStatic
    fun isUnavailable(e: Exception?): Boolean =
      e is MlKitException && e.errorCode == MlKitException.UNAVAILABLE
  }
}
