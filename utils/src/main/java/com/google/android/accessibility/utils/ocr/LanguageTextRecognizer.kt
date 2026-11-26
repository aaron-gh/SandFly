package com.google.android.accessibility.utils.ocr

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.android.accessibility.utils.mlkit.MlKitHelper
import com.google.android.accessibility.utils.output.LanguageSwitch
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.text.Text
import java.io.Closeable
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Recognizes text with the ML Kit recognizer for the script of the languages in use: the speech
 * language, then the system languages. The recognizers other than the Latin one also read Latin
 * letters. While Google Play services has no model for the script, the Latin recognizer reads the
 * image and the model download is requested.
 *
 * ML Kit has no Cyrillic recognizer: Cyrillic text is read with Tesseract, with the model of the
 * language that the user downloads ([CyrillicOcr]). Until it is downloaded, the Latin recognizer
 * reads the image, and [MissingModelListener] is told once.
 *
 * The recognizers run in the `:mlkit` process, through [MlKitHelper], since they load Google Play
 * services code, and an update of Google Play services stops every process with its code loaded.
 * Images are taken as bitmaps, not as ML Kit InputImages: InputImage.fromBitmap needs ML Kit's
 * context, which ML Kit's content provider sets up in SandFly's process only after the user unlocks
 * the phone once it has started.
 */
class LanguageTextRecognizer
@JvmOverloads
constructor(
  context: Context,
  private val languages: () -> List<Locale> = ::languagesInUse,
  private val clock: () -> Long = SystemClock::elapsedRealtime,
  private val helper: MlKitHelper = MlKitHelper.get(context),
  /** Whether a Tesseract model, such as "rus", is downloaded. */
  private val modelInstalled: (String) -> Boolean = { CyrillicOcr.isInstalled(context, it) },
) : Closeable {

  /** The scripts there is a text recognizer for: ML Kit's, and Tesseract's for Cyrillic. */
  enum class Script {
    LATIN,
    CHINESE,
    DEVANAGARI,
    JAPANESE,
    KOREAN,
    CYRILLIC,
  }

  /** Told when Cyrillic text is read with the Latin recognizer, since no model is downloaded. */
  fun interface MissingModelListener {
    /** [language] is the language written in Cyrillic whose model is missing. */
    fun onMissingModel(language: Locale)
  }

  /** The recognizer chosen, the Tesseract models for Cyrillic, and the language without a model. */
  private class Choice(val script: Script, val models: String? = null, val missing: Locale? = null)

  private val downloadRequested = HashSet<Script>()
  private var latinUntil = 0L
  private var closed = false

  /** The script recognized next, which is Latin while the model of the wanted one is missing. */
  @Synchronized fun script(): Script = choose().script

  @Synchronized
  private fun choose(): Choice {
    val languages = languages()
    val wanted = scriptFor(languages)
    if (wanted != Script.LATIN && clock() < latinUntil) return Choice(Script.LATIN)
    if (wanted != Script.CYRILLIC) return Choice(wanted)
    val language = languages.first { scriptOf(it) == Script.CYRILLIC }
    val model = CyrillicOcr.modelFor(language)
    if (!TesseractRecognizer.IS_AVAILABLE || model == null || !modelInstalled(model)) {
      return Choice(Script.LATIN, missing = language.takeIf { TesseractRecognizer.IS_AVAILABLE })
    }
    return Choice(
      Script.CYRILLIC,
      CyrillicOcr.languages(model, modelInstalled(CyrillicOcr.LATIN_MODEL)),
    )
  }

  /**
   * Recognizes text in [bitmap], turned by [rotation] degrees. The task fails if no recognizer can
   * be made or used.
   */
  fun process(bitmap: Bitmap, rotation: Int): Task<Text> {
    val choice: Choice
    synchronized(this) {
      if (closed) return Tasks.forException(IllegalStateException("Text recognizer closed"))
      choice = choose()
    }
    choice.missing?.let { reportMissingModel(it) }
    val script = choice.script
    val task = helper.recognizeText(bitmap, rotation, script, choice.models)
    if (script == Script.LATIN) return task
    return task.continueWithTask { result ->
      if (result.isSuccessful || !isUnavailable(result.exception)) return@continueWithTask result
      synchronized(this) {
        if (closed) return@continueWithTask result
        LogUtils.w(TAG, "No %s text recognizer, Latin is used", script)
        latinUntil = clock() + SCRIPT_RETRY_MS
        // Google Play services has no Tesseract models; the user downloads them in settings.
        if (script != Script.CYRILLIC) requestDownload(script)
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
    val script = script()
    // Tesseract models are downloaded in settings, not by Google Play services.
    if (script == Script.CYRILLIC) return Tasks.forResult(null)
    return helper.installTextRecognizer(script)
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

    @Volatile private var missingModelListener: MissingModelListener? = null
    private val missingModelReported = AtomicBoolean(false)

    /**
     * Sets what is told, once, the first time Cyrillic text is read without its model, such as
     * the screen reader, which says so. Each call allows one report again; null stops them.
     */
    @JvmStatic
    fun setMissingModelListener(listener: MissingModelListener?) {
      missingModelListener = listener
      missingModelReported.set(false)
    }

    private fun reportMissingModel(language: Locale) {
      val listener = missingModelListener ?: return
      if (!missingModelReported.compareAndSet(false, true)) return
      try {
        listener.onMissingModel(language)
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "Missing model not reported: %s", e)
      }
    }

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

    /** The script [language] is written in, or Latin when there is no recognizer for it. */
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
        "Cyrl" -> Script.CYRILLIC
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
        // Languages written in Cyrillic, unless their locale names another script. Uzbek and
        // Azerbaijani are written in Latin letters unless the locale says Cyrl.
        "ru",
        "uk",
        "be",
        "bg",
        "sr",
        "mk",
        "kk",
        "ky",
        "tg",
        "mn",
        "tt",
        "ba",
        "cv",
        "ce",
        "os",
        "sah" -> Script.CYRILLIC
        else -> Script.LATIN
      }

    /** Whether [e] says Google Play services has no model for the recognizer yet. */
    @JvmStatic
    fun isUnavailable(e: Exception?): Boolean =
      e is MlKitException && e.errorCode == MlKitException.UNAVAILABLE
  }
}
