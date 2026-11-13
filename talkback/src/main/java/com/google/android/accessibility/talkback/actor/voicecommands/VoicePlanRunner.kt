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

package com.google.android.accessibility.talkback.actor.voicecommands

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.actor.gemini.AiResultOutput
import com.google.android.accessibility.talkback.dictation.SandflyDictation
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import com.google.android.accessibility.talkback.macros.MacroRunner
import com.google.android.accessibility.talkback.macros.Macros
import com.google.android.accessibility.talkback.permission.PermissionUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Locale

/**
 * Runs the steps of a voice command, one after another, on the main thread. Starting a command
 * stops the one running. A step that fails says why and stops the command.
 *
 * Each step can leave a result, the text of a description, that the next step can copy or send.
 */
object VoicePlanRunner {
  private const val TAG = "VoicePlanRunner"

  /** The time after a step that changes the screen, for the change to start. */
  private const val STEP_GAP_MS = 300L

  /** The time a messaging app has to open before its Send button is looked for. */
  private const val COMPOSE_GAP_MS = 1_500L

  /** How long a description can take. */
  private const val DESCRIBE_TIMEOUT_MS = 60_000L

  private const val POLL_MS = 100L

  /** How long speech can take to start, and to finish, before a step goes on anyway. */
  private const val SPEECH_START_MS = 1_000L
  private const val SPEECH_MAX_MS = 60_000L

  /** How long the Send button of a messaging app is looked for. */
  private const val SEND_TIMEOUT_MS = 5_000L

  /** Anything between words of an item's text: not a letter or digit. */
  private const val GAP = "[^\\p{L}\\p{N}]"

  private const val PREF_CALL_PERMISSION_ASKED = "pref_voice_commands_call_permission_asked"

  private const val WHATSAPP = "com.whatsapp"
  private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"

  private val mainHandler = Handler(Looper.getMainLooper())
  private val executor = VoiceCommandEngine.newExecutor()

  private var current: Run? = null

  /**
   * Runs [steps]. [legacy] runs the text of a [VoiceStep.Legacy] step as one of TalkBack's voice
   * commands, and returns whether it is one.
   */
  @JvmStatic
  fun run(service: TalkBackService, steps: List<VoiceStep>, locale: Locale, legacy: (String) -> Boolean) {
    stop()
    if (steps.isEmpty()) return
    LogUtils.i(TAG, "Running %d steps: %s", steps.size, steps.joinToString { it::class.java.simpleName })
    val run = Run(service, steps, locale, legacy)
    current = run
    run.later(0) { run.runStep(0) }
  }

  /** Stops the command running, if one is. */
  @JvmStatic
  fun stop() {
    current?.finish()
    current = null
  }

  private class Run(
    val service: TalkBackService,
    val steps: List<VoiceStep>,
    val locale: Locale,
    val legacy: (String) -> Boolean,
  ) {
    var finished = false

    /** The result of the step before: the text of a description. */
    private var previous: String? = null

    // Whether a macro run of this command is going on, which stopping this stops.
    private var macroRunning = false

    // The receiver of a description this run waits for.
    private var describeReceiver: ((String?) -> Unit)? = null

    private val active: Boolean
      get() = !finished && current === this && TalkBackService.getInstance() === service

    fun later(delayMs: Long, block: () -> Unit) {
      mainHandler.postAtTime({ if (active) block() else done() }, this, SystemClock.uptimeMillis() + delayMs)
    }

    /** Runs [block] on the executor, then [then] with its result on the main thread while active. */
    fun <T> inBackground(block: () -> T, then: (T) -> Unit) {
      executor.execute {
        val result =
          try {
            block()
          } catch (e: Throwable) {
            LogUtils.w(TAG, "Voice command step failed: %s", e)
            null
          }
        mainHandler.post {
          if (!active) return@post
          @Suppress("UNCHECKED_CAST")
          if (result == null) fail("background step failed") else then(result as T)
        }
      }
    }

    fun runStep(index: Int) {
      if (!active) return
      val step = steps.getOrNull(index)
      if (step == null) {
        done()
        return
      }
      val next = { later(STEP_GAP_MS) { runStep(index + 1) } }
      try {
        when (step) {
          is VoiceStep.Command -> command(step, index, next)
          is VoiceStep.Tap -> tap(step.label, next)
          is VoiceStep.OpenApp -> openApp(step, next)
          is VoiceStep.TypeText ->
            macro(Macros.TypeText(step.text), next) { stop(service.getString(R.string.not_editable)) }
          is VoiceStep.Wait -> later(step.millis) { runStep(index + 1) }
          is VoiceStep.Call -> call(step)
          is VoiceStep.Message -> message(step, next)
          is VoiceStep.Describe -> describe(step.screen) { later(0) { afterSpeech { runStep(index + 1) } } }
          VoiceStep.Copy -> copy(next)
          is VoiceStep.Say -> {
            say(step.text)
            next()
          }
          is VoiceStep.Legacy -> {
            if (legacy(VoiceCommandActor.commandText(step.text))) {
              next()
            } else {
              say(service.getString(R.string.voice_commands_not_understood, step.text))
              done()
            }
          }
        }
      } catch (e: RuntimeException) {
        fail(e.toString())
      }
    }

    private fun command(step: VoiceStep.Command, index: Int, next: () -> Unit) {
      val value = step.value
      when {
        value == service.getString(R.string.shortcut_value_speak_screen_description) ->
          describe(screen = true) { afterSpeech { runStep(index + 1) } }
        value == service.getString(R.string.shortcut_value_speak_image_description) ->
          describe(screen = false) { afterSpeech { runStep(index + 1) } }
        value == service.getString(R.string.shortcut_value_copy_last_spoken_phrase) -> copy(next)
        value == Macros.STOP_COMMAND -> {
          MacroRunner.stop()
          next()
        }
        Macros.isCommand(value) -> {
          val id = Macros.idOf(value)
          macroRunning = true
          val started =
            id != null &&
              MacroRunner.run(id, sayNotMet = true) { ok ->
                macroRunning = false
                if (!active) return@run
                if (ok) next() else done()
              }
          if (!started) {
            macroRunning = false
            say(service.getString(R.string.voice_commands_not_available, step.label))
            done()
          }
        }
        service.performCommand(value) -> next()
        else -> {
          say(service.getString(R.string.voice_commands_not_available, step.label))
          done()
        }
      }
    }

    /**
     * Runs [step] as a macro, then [next]. When the step fails, [failed] runs, and goes on or
     * calls [stop].
     */
    private fun macro(step: Macros.Step, next: () -> Unit, failed: () -> Unit) {
      macroRunning = true
      val started =
        MacroRunner.runSteps(listOf(step), quiet = true) { ok ->
          macroRunning = false
          if (!active) return@runSteps
          if (ok) next() else failed()
        }
      if (!started) {
        macroRunning = false
        done()
      }
    }

    /**
     * Activates the item [label]: first one whose whole text is it, with any punctuation and "&"
     * for "and", then one whose text contains it.
     */
    private fun tap(label: String, next: () -> Unit) {
      val notFound = { stop(service.getString(R.string.voice_commands_not_found, label)) }
      val whole = wholeLabelPattern(label)
      if (whole == null) {
        macro(Macros.Tap(label), next, notFound)
        return
      }
      macro(Macros.Tap(whole, Macros.DEFAULT_TAP_TIMEOUT_MS, regex = true), next) {
        macro(Macros.Tap(label, Macros.MIN_TAP_TIMEOUT_MS), next, notFound)
      }
    }

    /** A pattern for the whole of [label], or null when [Macros.compilePattern] refuses it. */
    private fun wholeLabelPattern(label: String): String? {
      val and = service.resources.getStringArray(R.array.voice_commands_and_words)
      val words = VoiceGrammar.normalize(label, locale).split(' ').filter { it.isNotEmpty() }
      if (words.isEmpty()) return null
      val pattern =
        words.joinToString("$GAP+", prefix = "^$GAP*", postfix = "$GAP*$") { word ->
          if (and.any { VoiceGrammar.normalize(it, locale) == word }) "(?:" + Regex.escape(word) + "|&)"
          else Regex.escape(word)
        }
      return pattern.takeIf { Macros.compilePattern(it) != null }
    }

    private fun openApp(step: VoiceStep.OpenApp, next: () -> Unit) {
      if (step.value != null) {
        macro(Macros.OpenApp(step.value), next) { stop(service.getString(R.string.voice_commands_no_app, step.name)) }
        return
      }
      inBackground({ listOf(VoiceTargets.findApp(OpenAppActions.apps(service), step.name, locale)) }) { found ->
        val app = found.firstOrNull()
        if (app?.value == null) {
          say(service.getString(R.string.voice_commands_no_app, step.name))
          done()
        } else {
          macro(Macros.OpenApp(app.value), next) { stop(service.getString(R.string.voice_commands_no_app, step.name)) }
        }
      }
    }

    private fun call(step: VoiceStep.Call) {
      if (VoiceContacts.isNumber(step.who)) {
        placeCall(step.who, step.who)
        return
      }
      if (!VoiceContacts.canRead(service)) {
        askContacts()
        return
      }
      inBackground({ VoiceContacts.find(VoiceContacts.numbers(service), step.who, locale) }) { found ->
        val number = found.number
        if (number == null) {
          sayNotFound(step.who, found)
          done()
        } else {
          placeCall(number.name, number.number)
        }
      }
    }

    private fun placeCall(name: String, number: String) {
      val prefs = SharedPreferencesUtils.getSharedPreferences(service)
      val canCall =
        service.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
      if (!canCall && !prefs.getBoolean(PREF_CALL_PERMISSION_ASKED, false)) {
        prefs.edit().putBoolean(PREF_CALL_PERMISSION_ASKED, true).apply()
        PermissionUtils.requestPermissions(service, Manifest.permission.CALL_PHONE)
        say(service.getString(R.string.voice_commands_call_permission))
        done()
        return
      }
      val uri = Uri.fromParts("tel", number, null)
      // Without the permission to call, the phone app opens with the number for a call to be made.
      val intent =
        Intent(if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      say(service.getString(R.string.voice_commands_calling, name))
      start(intent)
      done()
    }

    private fun message(step: VoiceStep.Message, next: () -> Unit) {
      val who = step.who
      if (who != null && VoiceContacts.isNumber(who)) {
        messageText(step, who, who, step.text, next)
        return
      }
      val numberAtStart = if (who == null) VoiceContacts.numberAtStart(step.whoAndText.orEmpty()) else null
      if (numberAtStart != null) {
        val (number, rest) = numberAtStart
        messageText(step, number, number, step.text ?: VoiceGrammar.trimText(rest).ifEmpty { null }, next)
        return
      }
      if (!VoiceContacts.canRead(service)) {
        askContacts()
        return
      }
      inBackground({
        val numbers = VoiceContacts.numbers(service)
        if (who != null) {
          VoiceContacts.find(numbers, who, locale) to step.text
        } else {
          val split = VoiceContacts.findAtStart(numbers, step.whoAndText.orEmpty(), locale)
          (split?.first ?: VoiceContacts.Found(null, emptyList())) to
            (step.text ?: split?.second?.let(VoiceGrammar::trimText)?.ifEmpty { null })
        }
      }) { (found, text) ->
        val number = found.number
        if (number == null) {
          sayNotFound(who ?: step.whoAndText.orEmpty(), found)
          done()
        } else {
          messageText(step, number.name, number.number, text, next)
        }
      }
    }

    /** Finds the text of the message to [name], asking for it when there is none, and writes it. */
    private fun messageText(step: VoiceStep.Message, name: String, number: String, text: String?, next: () -> Unit) {
      val known =
        text ?: if (step.usePrevious) previous ?: service.getLastSpokenText()?.ifEmpty { null } else null
      if (known != null) {
        compose(step, name, number, known, fromResult = text == null, next)
        return
      }
      askByVoice(service.getString(R.string.voice_commands_message_text, name)) { said ->
        if (said.isNullOrBlank()) {
          say(service.getString(R.string.voice_commands_message_not_sent))
          done()
        } else {
          compose(step, name, number, said.trim(), fromResult = false, next)
        }
      }
    }

    /**
     * Opens the messaging app with [text] to [number], then asks whether to send it and presses
     * Send when the answer is yes. Apps other than SMS and WhatsApp get the text only.
     */
    private fun compose(
      step: VoiceStep.Message,
      name: String,
      number: String,
      text: String,
      fromResult: Boolean,
      next: () -> Unit,
    ) {
      val appName = step.app?.let { VoiceGrammar.normalize(it, locale) }
      val sms = service.resources.getStringArray(R.array.voice_commands_sms_names).map { VoiceGrammar.normalize(it, locale) }
      val intent: Intent
      var confirm = true
      when {
        appName == null || appName in sms ->
          intent =
            Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)).putExtra("sms_body", text)
        appName.replace(" ", "").contains("whatsapp") -> {
          val pkg = installed(WHATSAPP) ?: installed(WHATSAPP_BUSINESS)
          if (pkg == null) {
            say(service.getString(R.string.voice_commands_no_app, step.app))
            done()
            return
          }
          val phone = internationalDigits(number)
          intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=" + Uri.encode(text)),
              )
              .setPackage(pkg)
        }
        else -> {
          val app = VoiceTargets.findApp(OpenAppActions.apps(service), step.app.orEmpty(), locale)
          val pkg = OpenAppActions.packageName(app?.value)
          if (pkg == null) {
            say(service.getString(R.string.voice_commands_no_app, step.app))
            done()
            return
          }
          // Other apps choose the chat themselves.
          intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text).setPackage(pkg)
          confirm = false
        }
      }
      if (!start(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))) {
        done()
        return
      }
      if (!confirm || !SandflyDictation.isAvailable) {
        next()
        return
      }
      val question =
        if (fromResult) service.getString(R.string.voice_commands_message_confirm_result, name)
        else service.getString(R.string.voice_commands_message_confirm, name, text)
      later(COMPOSE_GAP_MS) {
        afterSpeech {
          askByVoice(question) { answer ->
            if (answer != null && isYes(answer)) {
              pressSend(next)
            } else {
              say(service.getString(R.string.voice_commands_message_not_sent))
              done()
            }
          }
        }
      }
    }

    private fun pressSend(next: () -> Unit) {
      val label = service.getString(R.string.voice_commands_send_button)
      macroRunning = true
      val started =
        MacroRunner.runSteps(
          listOf(Macros.Tap(label, SEND_TIMEOUT_MS, field = Macros.Field.DESCRIPTION)),
          quiet = true,
        ) { ok ->
          macroRunning = false
          if (!active) return@runSteps
          if (ok) {
            say(service.getString(R.string.voice_commands_send_pressed))
            next()
          } else {
            say(service.getString(R.string.voice_commands_not_found, label))
            done()
          }
        }
      if (!started) {
        macroRunning = false
        done()
      }
    }

    private fun isYes(answer: String): Boolean {
      val said = VoiceGrammar.normalize(answer, locale)
      return service.resources.getStringArray(R.array.voice_commands_yes).any {
        val yes = VoiceGrammar.normalize(it, locale)
        said == yes || said.startsWith("$yes ")
      }
    }

    /** Speaks [question], then records the answer and gives it to [then], or null. */
    private fun askByVoice(question: String, then: (String?) -> Unit) {
      say(question)
      afterSpeech {
        var answered = false
        val started =
          SandflyDictation.listen(false) { text ->
            if (answered) return@listen
            answered = true
            mainHandler.post { if (active) then(text) }
          }
        if (!started && !answered) {
          answered = true
          then(null)
        }
      }
    }

    /** Speaks a description, keeps it as the result, then runs [then]. */
    private fun describe(screen: Boolean, then: () -> Unit) {
      lateinit var receiver: (String?) -> Unit
      val timeout = Runnable {
        AiResultOutput.forget(receiver)
        if (active) {
          say(service.getString(R.string.voice_commands_no_description))
          done()
        }
      }
      receiver = { text ->
        mainHandler.post {
          mainHandler.removeCallbacks(timeout)
          describeReceiver = null
          if (!active) return@post
          if (text.isNullOrBlank()) {
            // The failure was spoken.
            done()
          } else {
            previous = text
            then()
          }
        }
      }
      describeReceiver = receiver
      AiResultOutput.expect(receiver)
      val value =
        service.getString(
          if (screen) R.string.shortcut_value_speak_screen_description
          else R.string.shortcut_value_speak_image_description
        )
      // A command that didn't request a spoken description, as with no image focused or before
      // the user agreed to send screenshots, said why or opened a window.
      if (!service.performCommand(value) || !AiResultOutput.isWaiting(receiver)) {
        AiResultOutput.forget(receiver)
        describeReceiver = null
        done()
        return
      }
      mainHandler.postAtTime(timeout, this, SystemClock.uptimeMillis() + DESCRIBE_TIMEOUT_MS)
    }

    private fun copy(next: () -> Unit) {
      val text = previous
      if (text == null) {
        service.performCommand(service.getString(R.string.shortcut_value_copy_last_spoken_phrase))
        next()
        return
      }
      val clipboard = service.getSystemService(ClipboardManager::class.java)
      if (clipboard == null) {
        fail("no clipboard")
        return
      }
      clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
      say(service.getString(R.string.voice_commands_copied))
      next()
    }

    /** Runs [then] once SandFly has stopped speaking, or after a time limit. */
    private fun afterSpeech(then: () -> Unit) {
      val start = SystemClock.uptimeMillis()
      var started = false
      fun check() {
        val speaking = service.speechController?.isSpeaking == true
        started = started || speaking
        val elapsed = SystemClock.uptimeMillis() - start
        when {
          speaking && elapsed < SPEECH_MAX_MS -> later(POLL_MS) { check() }
          !started && elapsed < SPEECH_START_MS -> later(POLL_MS) { check() }
          else -> then()
        }
      }
      later(POLL_MS) { check() }
    }

    private fun askContacts() {
      PermissionUtils.requestPermissions(service, Manifest.permission.READ_CONTACTS)
      say(service.getString(R.string.voice_commands_contacts_permission))
      done()
    }

    private fun sayNotFound(who: String, found: VoiceContacts.Found) {
      say(
        if (found.names.isNotEmpty()) {
          service.getString(R.string.voice_commands_contacts_several, who, found.names.take(5).joinToString(", "))
        } else {
          service.getString(R.string.voice_commands_no_contact, who)
        }
      )
    }

    private fun installed(pkg: String): String? =
      try {
        service.packageManager.getPackageInfo(pkg, 0)
        pkg
      } catch (e: PackageManager.NameNotFoundException) {
        null
      }

    /** [number] in international form, digits only, as WhatsApp links take it. */
    private fun internationalDigits(number: String): String {
      val telephony = service.getSystemService(TelephonyManager::class.java)
      val country =
        (telephony?.networkCountryIso?.ifEmpty { null } ?: telephony?.simCountryIso?.ifEmpty { null })
          ?.uppercase(Locale.ROOT)
      val e164 = country?.let { PhoneNumberUtils.formatNumberToE164(number, it) } ?: number
      return e164.filter { it.isDigit() }
    }

    private fun start(intent: Intent): Boolean =
      try {
        service.startActivity(intent)
        true
      } catch (e: ActivityNotFoundException) {
        LogUtils.w(TAG, "No app for %s", intent)
        say(service.getString(R.string.voice_commands_no_app_for_action))
        false
      } catch (e: SecurityException) {
        LogUtils.w(TAG, "Cannot start %s: %s", intent, e)
        say(service.getString(R.string.voice_commands_no_app_for_action))
        false
      }

    private fun say(text: String) {
      service.feedbackReturner?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.requestedSpeech(text))
    }

    /** Says [text] and stops. */
    private fun stop(text: String) {
      say(text)
      done()
    }

    private fun fail(reason: String) {
      LogUtils.w(TAG, "Voice command stopped: %s", reason)
      say(service.getString(R.string.voice_commands_error))
      done()
    }

    fun done() {
      finish()
      if (current === this) current = null
    }

    fun finish() {
      if (finished) return
      finished = true
      mainHandler.removeCallbacksAndMessages(this)
      describeReceiver?.let { AiResultOutput.forget(it) }
      describeReceiver = null
      if (macroRunning) {
        macroRunning = false
        MacroRunner.stop()
      }
    }
  }
}
