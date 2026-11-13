package com.google.android.accessibility.utils.ocr

import android.content.Context
import android.content.res.Resources
import android.os.SystemClock
import com.google.android.accessibility.utils.output.LanguageSwitch
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallResponse
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable
import java.util.Locale

/**
 * Recognizes text with the ML Kit recognizer for the script of the languages in use: the speech
 * language, then the system languages. The recognizers other than the Latin one also read Latin
 * letters. While Google Play services has no model for the script, the Latin recognizer reads the
 * image and the model download is requested.
 */
class LanguageTextRecognizer
@JvmOverloads
constructor(
  context: Context,
  private val languages: () -> List<Locale> = ::languagesInUse,
  private val clock: () -> Long = SystemClock::elapsedRealtime,
) : Closeable {

  /** The scripts ML Kit has a text recognizer for. */
  enum class Script {
    LATIN,
    CHINESE,
    DEVANAGARI,
    JAPANESE,
    KOREAN,
  }

  private val context: Context = context.applicationContext ?: context
  private val recognizers = HashMap<Script, TextRecognizer>()
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
    val recognizer: TextRecognizer
    synchronized(this) {
      if (closed) return Tasks.forException(IllegalStateException("Text recognizer closed"))
      script = script()
      recognizer =
        try {
          recognizer(script)
        } catch (e: RuntimeException) {
          // Such as ML Kit not being set up yet just after the phone starts.
          return Tasks.forException(e)
        }
    }
    val task = processWith(recognizer, image)
    if (script == Script.LATIN) return task
    return task.continueWithTask { result ->
      if (result.isSuccessful || !isUnavailable(result.exception)) return@continueWithTask result
      val latin: TextRecognizer
      synchronized(this) {
        if (closed) return@continueWithTask result
        LogUtils.w(TAG, "No %s text recognizer, Latin is used", script)
        latinUntil = clock() + SCRIPT_RETRY_MS
        requestDownload(script)
        latin =
          try {
            recognizer(Script.LATIN)
          } catch (e: RuntimeException) {
            return@continueWithTask result
          }
      }
      processWith(latin, image)
    }
  }

  private fun processWith(recognizer: TextRecognizer, image: InputImage): Task<Text> =
    try {
      recognizer.process(image)
    } catch (e: RuntimeException) {
      // Such as a recognizer closed after a language change.
      Tasks.forException(e)
    }

  /**
   * Asks Google Play services to download the model of the recognizer last used, such as after
   * [process] failed with [MlKitException.UNAVAILABLE].
   */
  @Synchronized
  fun download(): Task<ModuleInstallResponse> {
    if (closed) return Tasks.forException(IllegalStateException("Text recognizer closed"))
    return try {
      install(recognizer(script()))
    } catch (e: RuntimeException) {
      Tasks.forException(e)
    }
  }

  /** Closes the recognizers. They are made again only by a new instance. */
  @Synchronized
  override fun close() {
    closed = true
    for (recognizer in recognizers.values) {
      closeQuietly(recognizer)
    }
    recognizers.clear()
  }

  private fun recognizer(script: Script): TextRecognizer {
    recognizers[script]?.let {
      return it
    }
    // Another script's model stays loaded only as long as the language that wanted it is in use.
    if (script != Script.LATIN) {
      val others = recognizers.keys.filter { it != Script.LATIN }
      for (other in others) {
        closeQuietly(recognizers.remove(other))
      }
    }
    return TextRecognition.getClient(optionsFor(script)).also { recognizers[script] = it }
  }

  private fun closeQuietly(recognizer: TextRecognizer?) {
    try {
      recognizer?.close()
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Text recognizer not closed: %s", e)
    }
  }

  private fun requestDownload(script: Script) {
    if (!downloadRequested.add(script)) return
    val recognizer = recognizers[script] ?: return
    try {
      install(recognizer).addOnFailureListener { e ->
        LogUtils.w(TAG, "%s text recognizer not downloaded: %s", script, e)
      }
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "%s text recognizer not downloaded: %s", script, e)
    }
  }

  private fun install(recognizer: TextRecognizer): Task<ModuleInstallResponse> =
    ModuleInstall.getClient(context)
      .installModules(ModuleInstallRequest.newBuilder().addApi(recognizer).build())

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

    private fun optionsFor(script: Script): TextRecognizerOptionsInterface =
      when (script) {
        Script.LATIN -> TextRecognizerOptions.DEFAULT_OPTIONS
        Script.CHINESE -> ChineseTextRecognizerOptions.Builder().build()
        Script.DEVANAGARI -> DevanagariTextRecognizerOptions.Builder().build()
        Script.JAPANESE -> JapaneseTextRecognizerOptions.Builder().build()
        Script.KOREAN -> KoreanTextRecognizerOptions.Builder().build()
      }

    /** Whether [e] says Google Play services has no model for the recognizer yet. */
    @JvmStatic
    fun isUnavailable(e: Exception?): Boolean =
      e is MlKitException && e.errorCode == MlKitException.UNAVAILABLE
  }
}
