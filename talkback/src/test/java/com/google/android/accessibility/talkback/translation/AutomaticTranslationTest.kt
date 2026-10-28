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

package com.google.android.accessibility.talkback.translation

import android.os.Looper
import android.text.Spanned
import android.text.style.LocaleSpan
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.SpeechController
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AutomaticTranslationTest {
  private val context = RuntimeEnvironment.getApplication()
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)
  private val engine = FakeEngine()
  private val spoken = mutableListOf<String?>()
  private lateinit var auto: AutomaticTranslation

  private class FakeEngine : AutomaticTranslation.Engine {
    override val onDeviceAvailable = true
    val languages = mutableMapOf<String, String>()
    val translations = mutableMapOf<String, String>()
    /** Translations that are answered only when the test says so. */
    val waiting = mutableMapOf<String, (Translator.Result) -> Unit>()
    var slow = false
    var translated = 0
    val downloads = mutableListOf<Translator.Downloads>()
    /** Identification confidence by text; others are identified surely. */
    val confidences = mutableMapOf<String, Float>()
    /** Whether the language models are on the phone; when not, downloads wait for [finishDownload]. */
    var modelsDownloaded = true
    private val downloadsDone = mutableListOf<() -> Unit>()
    val said = mutableListOf<String>()
    var closed = false

    override fun detect(text: String, done: (AutomaticTranslation.Detection?) -> Unit) =
      done(
        AutomaticTranslation.Detection(languages[text] ?: AutomaticTranslation.NO_LANGUAGE, confidences[text] ?: 0.95f)
      )

    override fun translate(
      text: String,
      service: TranslationService,
      from: String,
      to: String,
      downloads: Translator.Downloads,
      onDownloaded: () -> Unit,
      done: (Translator.Result) -> Unit,
    ) {
      translated++
      this.downloads += downloads
      if (!modelsDownloaded) {
        val failed = Translator.Result.Failed(TranslationFailure(TranslationFailure.Kind.DOWNLOADING), service)
        when (downloads) {
          Translator.Downloads.NONE -> done(failed)
          Translator.Downloads.START -> {
            downloadsDone += onDownloaded
            done(Translator.Result.Downloading(listOf(from)))
            done(failed)
          }
          Translator.Downloads.WAIT -> {
            done(Translator.Result.Downloading(listOf(from)))
            downloadsDone += { translate(text, service, from, to, Translator.Downloads.NONE, {}, done) }
          }
        }
        return
      }
      val result: Translator.Result =
        translations[text]?.let { Translator.Result.Done(Translation(it, from, to)) }
          ?: Translator.Result.Failed(TranslationFailure(TranslationFailure.Kind.OTHER), service)
      if (slow) waiting[text] = { done(result) } else done(result)
    }

    fun answer(text: String) = waiting.remove(text)!!.invoke(Translator.Result.Done(Translation(translations[text]!!, "de", "en")))

    fun finishDownload() {
      modelsDownloaded = true
      val done = downloadsDone.toList()
      downloadsDone.clear()
      done.forEach { it() }
    }

    override fun speak(text: CharSequence, history: Boolean) {
      said += text.toString()
    }

    override fun close() {
      closed = true
    }
  }

  @Before
  fun setUp() {
    TranslationSettings.setTarget(prefs, "en")
    TranslationSettings.setService(prefs, TranslationService.ON_DEVICE)
    engine.languages["Guten Morgen"] = "de"
    engine.languages["Bonjour tout le monde"] = "fr"
    engine.languages["Good morning"] = "en"
    engine.translations["Guten Morgen"] = "Good morning"
    engine.translations["Bonjour tout le monde"] = "Hello everyone"
    auto = AutomaticTranslation(context, engine)
  }

  private fun say(text: String, queueMode: Int = SpeechController.QUEUE_MODE_QUEUE): Boolean =
    auto.hold(text, queueMode, 0) { spoken += it?.toString() }

  @Test
  fun textInAnotherLanguage_isSpokenTranslatedWithItsLanguage() {
    val texts = mutableListOf<CharSequence?>()
    assertTrue(auto.hold("Guten Morgen", SpeechController.QUEUE_MODE_QUEUE, 0) { texts += it })
    assertEquals("Good morning", texts.single().toString())
    val span = (texts.single() as Spanned).getSpans(0, 12, LocaleSpan::class.java).single()
    assertEquals("en", span.locale?.language)
  }

  @Test
  fun textInTheTargetLanguage_isSpokenAtOnceOnceItsLanguageIsKnown() {
    assertTrue(say("Good morning"))
    assertEquals(listOf("Good morning"), spoken)
    // The language is remembered, so the second time it isn't held at all.
    assertFalse(say("Good morning"))
    assertEquals(0, engine.translated)
  }

  @Test
  fun shortText_isNeverHeld() {
    assertFalse(say("OK"))
    assertTrue(spoken.isEmpty())
  }

  @Test
  fun untranslatedLanguages_andOtherSourceLanguages_areLeft() {
    TranslationSettings.setUntranslated(prefs, setOf("de"))
    say("Guten Morgen")
    TranslationSettings.setSource(prefs, "de")
    TranslationSettings.setUntranslated(prefs, emptySet())
    say("Bonjour tout le monde")
    assertEquals(listOf("Guten Morgen", "Bonjour tout le monde"), spoken)
    assertEquals(0, engine.translated)
  }

  @Test
  fun speechAfterAHeldUtterance_waitsForIt() {
    engine.slow = true
    say("Good morning")
    assertTrue(say("Guten Morgen"))
    assertTrue(say("Good morning"))
    assertEquals(listOf("Good morning"), spoken)
    engine.answer("Guten Morgen")
    assertEquals(listOf("Good morning", "Good morning", "Good morning"), spoken)
  }

  @Test
  fun interruptingSpeech_dropsHeldUtterances() {
    engine.slow = true
    say("Guten Morgen")
    say("Bonjour tout le monde", SpeechController.QUEUE_MODE_INTERRUPT)
    assertEquals(listOf<String?>(null), spoken)
    engine.answer("Guten Morgen")
    engine.answer("Bonjour tout le monde")
    assertEquals(listOf(null, "Hello everyone"), spoken)
  }

  @Test
  fun interruptingSpeech_dropsWhatWaitsBehindAHeldUtterance() {
    engine.slow = true
    say("Guten Morgen")
    say("Good morning")
    say("Bonjour tout le monde", SpeechController.QUEUE_MODE_INTERRUPT)
    assertEquals(listOf<String?>(null, null), spoken)
  }

  @Test
  fun notifications_areTranslatedAndNeverDropped() {
    engine.slow = true
    say("Guten Morgen", SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH)
    say("Good morning", SpeechController.QUEUE_MODE_INTERRUPT)
    assertTrue(spoken.isEmpty())
    engine.answer("Guten Morgen")
    assertEquals(listOf("Good morning", "Good morning"), spoken)
  }

  @Test
  fun failedAndSlowTranslations_speakTheOriginal() {
    engine.languages["Wie geht es dir"] = "de"
    say("Wie geht es dir")
    assertEquals(listOf("Wie geht es dir"), spoken)
    engine.slow = true
    say("Guten Morgen")
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
    assertEquals(listOf("Wie geht es dir", "Guten Morgen"), spoken)
  }

  @Test
  fun translations_areRemembered() {
    say("Guten Morgen")
    say("Guten Morgen")
    assertEquals(listOf("Good morning", "Good morning"), spoken)
    assertEquals(1, engine.translated)
  }

  @Test
  fun close_speaksWhatIsHeld() {
    engine.slow = true
    say("Guten Morgen")
    auto.close()
    assertEquals(listOf("Guten Morgen"), spoken)
    assertTrue(engine.closed)
  }

  @Test
  fun translateNotifications_translatesNotificationsWithoutAutomaticTranslation() {
    prefs.edit().putBoolean(TranslationSettings.PREF_NOTIFICATIONS, true).commit()
    AutomaticTranslation.update(context)
    assertTrue(AutomaticTranslation.translatesNotifications())
    prefs.edit().putBoolean(TranslationSettings.PREF_NOTIFICATIONS, false).commit()
    AutomaticTranslation.update(context)
    assertFalse(AutomaticTranslation.translatesNotifications())
    val texts = mutableListOf<CharSequence>()
    AutomaticTranslation.translate("Guten Morgen") { texts += it }
    assertEquals(listOf("Guten Morgen"), texts.map { it.toString() })
  }

  @Test
  fun languageModels_downloadAtOnceForSureText_andForLessSureTextTheSecondTime() {
    engine.languages["Wie geht es dir"] = "de"
    engine.languages["Bis morgen am Bahnhof"] = "de"
    engine.confidences["Guten Morgen"] = 0.75f
    engine.confidences["Wie geht es dir"] = 0.75f
    say("Bis morgen am Bahnhof")
    say("Guten Morgen")
    say("Wie geht es dir")
    assertEquals(
      listOf(Translator.Downloads.START, Translator.Downloads.NONE, Translator.Downloads.START),
      engine.downloads,
    )
  }

  @Test
  fun afterADownload_theUtteranceSaidUntranslatedIsSaidAgainTranslated() {
    engine.modelsDownloaded = false
    say("Bonjour tout le monde")
    assertEquals(listOf("Bonjour tout le monde"), spoken)
    engine.finishDownload()
    assertEquals(listOf("Downloading French for on-device translation", "Hello everyone"), engine.said)
  }

  @Test
  fun afterADownload_aHintAfterTheUtteranceDoesNotStopItBeingSaidAgain() {
    engine.modelsDownloaded = false
    say("Bonjour tout le monde", SpeechController.QUEUE_MODE_INTERRUPT)
    say("Double-tap to activate")
    engine.finishDownload()
    assertEquals("Hello everyone", engine.said.last())
  }

  @Test
  fun afterADownload_nothingIsSaidAgainOnceTheUserMovedOn() {
    engine.modelsDownloaded = false
    say("Bonjour tout le monde", SpeechController.QUEUE_MODE_INTERRUPT)
    say("Good morning", SpeechController.QUEUE_MODE_INTERRUPT)
    engine.finishDownload()
    assertEquals(listOf("Downloading French for on-device translation"), engine.said)
  }

  @Test
  fun notifications_waitForTheirLanguageModel() {
    engine.modelsDownloaded = false
    val texts = mutableListOf<String>()
    auto.translateNotification("Bonjour tout le monde") { texts += it.toString() }
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(10))
    assertTrue(texts.isEmpty())
    engine.finishDownload()
    assertEquals(listOf("Hello everyone"), texts)
  }

  @Test
  fun autoService_neverUsesTheAiService() {
    TranslationSettings.setService(prefs, TranslationService.AI)
    assertEquals(TranslationService.ON_DEVICE, TranslationSettings.autoService(prefs, onDeviceAvailable = true))
    assertEquals(TranslationService.GOOGLE_CLOUD, TranslationSettings.autoService(prefs, onDeviceAvailable = false))
    TranslationSettings.setAutoService(prefs, TranslationService.DEEPL)
    assertEquals(TranslationService.DEEPL, TranslationSettings.autoService(prefs, onDeviceAvailable = true))
  }
}
