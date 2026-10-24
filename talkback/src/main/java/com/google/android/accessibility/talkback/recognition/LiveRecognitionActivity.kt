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

package com.google.android.accessibility.talkback.recognition

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Base64
import android.util.Size
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.actor.gemini.AiService
import com.google.android.accessibility.talkback.actor.gemini.GeminiApiKey
import com.google.android.accessibility.talkback.pause.PauseController
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.monitor.RecentUserInput
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** The Live recognition screen. The camera runs only while this screen is in the foreground. */
class LiveRecognitionActivity : ComponentActivity() {
  private val enabled = mutableSetOf<RecognitionMode>()
  private val switches = mutableMapOf<RecognitionMode, CheckBox>()
  private val handler = Handler(Looper.getMainLooper())
  private val worker = Executors.newSingleThreadExecutor()
  private val processing = AtomicBoolean(false)
  private val announcements = RecognitionAnnouncements()
  private val pendingResults = linkedMapOf<String, Pair<String, Long>>()
  private val prefs by lazy { SharedPreferencesUtils.getSharedPreferences(this) }
  private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
  private var recognizerUsed = false
  // Read by the camera thread, to encode a frame only when it can be described.
  @Volatile private var describer: FrameDescriber? = null
  private var cameraProvider: ProcessCameraProvider? = null
  private var analysis: ImageAnalysis? = null
  private var preview: Preview? = null
  private var cameraState: LiveData<CameraState>? = null
  private lateinit var previewView: PreviewView
  private lateinit var status: TextView
  private var resumed = false
  private var dialogOpen = false
  private var pendingPermission: RecognitionMode? = null
  private var initialMode: RecognitionMode? = null
  private var pointAbove = true
  @Volatile private var generation = 0
  private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    val mode = pendingPermission
    pendingPermission = null
    if (granted && mode != null) {
      if (resumed) toggle(mode) else initialMode = mode
    } else report(R.string.live_recognition_camera_permission)
  }

  /** While a mode is on: speaks a result that waited for speech or navigation to end. */
  private val runningCheck = object : Runnable {
    override fun run() {
      if (!resumed || enabled.isEmpty()) return
      if (!TalkBackService.isServiceActive() || PauseController.isPaused()) {
        stopRecognition()
        return
      }
      speakPendingResult()
      handler.postDelayed(this, CHECK_INTERVAL_MS)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    title = getString(R.string.live_recognition_title)
    initialMode = if (savedInstanceState == null) parseMode(intent) else null
    val column = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      val pad = (16 * resources.displayMetrics.density).toInt()
      setPadding(pad, pad, pad, pad)
    }
    column.addView(TextView(this).apply {
      text = getString(R.string.live_recognition_instructions)
      textSize = 18f
    })
    previewView = PreviewView(this).apply {
      implementationMode = PreviewView.ImplementationMode.COMPATIBLE
      importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    column.addView(previewView, LinearLayout.LayoutParams(-1, (180 * resources.displayMetrics.density).toInt()))
    status = TextView(this).apply { text = getString(R.string.live_recognition_camera_off); textSize = 20f }
    column.addView(status)
    RecognitionMode.entries.forEach { mode ->
      val button = CheckBox(this).apply {
        text = getString(mode.label)
        textSize = 20f
        setOnClickListener { toggle(mode) }
      }
      switches[mode] = button
      column.addView(button)
    }
    val above = RadioButton(this).apply {
      id = View.generateViewId()
      setText(R.string.live_recognition_point_above)
      isChecked = true
    }
    column.addView(RadioGroup(this).apply {
      addView(above)
      addView(RadioButton(this@LiveRecognitionActivity).apply {
        id = View.generateViewId()
        setText(R.string.live_recognition_point_below)
      })
      // Sent with each request, so recognition keeps running.
      setOnCheckedChangeListener { _, checked -> pointAbove = checked == above.id }
    })
    setContentView(ScrollView(this).apply { addView(column); fitsSystemWindows = true })
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    val mode = parseMode(intent) ?: return
    if (resumed) toggle(mode) else initialMode = mode
  }

  private fun parseMode(intent: Intent): RecognitionMode? =
    RecognitionMode.entries.find { it.name == intent.getStringExtra(EXTRA_MODE) }

  override fun onResume() {
    super.onResume()
    resumed = true
    LiveRecognition.attach(this)
    initialMode?.let { initialMode = null; toggle(it) }
  }

  override fun onPause() {
    resumed = false
    stopRecognition()
    LiveRecognition.detach(this)
    super.onPause()
  }

  override fun onDestroy() {
    describer?.close()
    if (recognizerUsed) recognizer.close()
    worker.shutdown()
    super.onDestroy()
  }

  internal fun canUseRotor(): Boolean = resumed && !dialogOpen && pendingPermission == null
  internal fun isEnabled(mode: RecognitionMode): Boolean = mode in enabled

  internal fun toggle(mode: RecognitionMode) {
    if (!canUseRotor()) { updateSwitches(); return }
    if (mode in enabled) {
      enabled.remove(mode)
      reconcile()
      speakMode(mode)
      return
    }
    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      pendingPermission = mode
      updateSwitches()
      cameraPermission.launch(Manifest.permission.CAMERA)
      return
    }
    if (mode.usesAi && !canUseAi(mode)) {
      updateSwitches()
      return
    }
    enabled.add(mode)
    reconcile()
    speakMode(mode)
  }

  /**
   * Whether the modes that use AI can be turned on: an AI service is in use, and the user agreed to
   * images being sent to it, which this asks once. When it asks, [mode] is turned on after Send.
   */
  private fun canUseAi(mode: RecognitionMode): Boolean {
    val backend = AiBackend.of(this, prefs)
    if (backend == AiBackend.NONE) {
      report(R.string.ai_service_not_set_up)
      return false
    }
    if (backend == AiBackend.GEMINI && GeminiApiKey.effectiveKey(prefs, BuildConfig.GEMINI_API_KEY).isBlank()) {
      report(R.string.gemini_model_needs_key)
      return false
    }
    // Nothing leaves the phone with on-device AI.
    if (backend == AiBackend.ON_DEVICE) return true
    val host = if (backend == AiBackend.SERVICE) AiService.host(prefs) else null
    val consent = if (host == null) CLOUD_CONSENT_PREF else SERVICE_CONSENT_PREF + host
    if (prefs.getBoolean(consent, false)) return true
    dialogOpen = true
    AlertDialog.Builder(this)
      .setTitle(if (host == null) getString(R.string.live_recognition_cloud_title)
        else getString(R.string.live_recognition_service_title, host))
      .setMessage(if (host == null) getString(R.string.live_recognition_cloud_message)
        else getString(R.string.live_recognition_service_message, host))
      .setNegativeButton(android.R.string.cancel, null)
      .setPositiveButton(R.string.live_recognition_send) { _, _ ->
        prefs.edit { putBoolean(consent, true) }
        dialogOpen = false
        if (resumed) toggle(mode)
      }.setOnDismissListener { dialogOpen = false }.show()
    return false
  }

  private fun updateSwitches() { switches.forEach { (mode, view) -> view.isChecked = mode in enabled } }
  private fun speakMode(mode: RecognitionMode) = speak(getString(R.string.live_recognition_mode_state,
    getString(mode.label), getString(if (mode in enabled) R.string.value_on else R.string.value_off)), true)

  internal fun stopAndFinish() { stopRecognition(); finish() }

  internal fun skipResult() {
    TalkBackService.getInstance()?.speechController?.interrupt(false)
    speakPendingResult(ignoreRecentInput = true)
  }

  private fun stopRecognition() {
    enabled.clear()
    reconcile()
  }

  /** Invalidates old OCR/network callbacks before replacing the enabled modes or camera. */
  private fun reconcile() {
    val token = ++generation
    LogUtils.i(TAG, "Modes: %s", enabled)
    TalkBackService.getInstance()?.speechController?.stopUtteranceGroup(SpeechController.UTTERANCE_GROUP_LIVE_RECOGNITION)
    describer?.close()
    describer = null
    cameraState?.removeObservers(this)
    cameraState = null
    analysis?.clearAnalyzer()
    val bound = listOfNotNull(preview, analysis).toTypedArray()
    if (bound.isNotEmpty()) cameraProvider?.unbind(*bound)
    analysis = null
    preview = null
    announcements.clear()
    pendingResults.clear()
    updateSwitches()
    handler.removeCallbacks(runningCheck)
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    if (!resumed || enabled.isEmpty()) {
      status.setText(R.string.live_recognition_camera_off)
      return
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    handler.post(runningCheck)
    status.setText(R.string.live_recognition_camera_on)
    if (enabled.any { it.usesAi }) describer = createDescriber(token)
    startCamera(token)
  }

  /** Describes frames with what Describe image uses: see [AiBackend]. */
  private fun createDescriber(token: Int): FrameDescriber? {
    val language = Locale.getDefault().toLanguageTag()
    val onResult = { text: String -> if (generation == token) announceResult(SOURCE_AI, text) }
    fun stopAi(message: String?) {
      if (generation != token) return
      enabled.removeAll { it.usesAi }
      reconcile()
      if (message != null) report(message)
    }
    return when (val backend = AiBackend.of(this, prefs)) {
      AiBackend.GEMINI -> {
        status.setText(R.string.live_recognition_connecting)
        GeminiLiveSession(
          GeminiApiKey.effectiveKey(prefs, BuildConfig.GEMINI_API_KEY),
          language,
          onReady = { if (generation == token) status.setText(R.string.live_recognition_camera_on) },
          onResult = onResult,
          onFailure = { stopAi(getString(it)) },
        )
      }
      AiBackend.ON_DEVICE, AiBackend.SERVICE -> {
        val questions = LiveRecognition.imageQuestions(this)
        val plain = backend == AiBackend.ON_DEVICE && AiBackend.usesPlainPrompts(this)
        RequestDescriber(
          prompt = { modes, above ->
            if (plain) GeminiLiveProtocol.plainPrompt(modes, above, language)
            else GeminiLiveProtocol.imagePrompt(modes, above, language)
          },
          ask = questions::ask,
          cancel = questions::cancel,
          // The on-device model takes the next frame as soon as it is free; a service is paid for.
          minIntervalMs = if (backend == AiBackend.ON_DEVICE) 0 else GeminiLiveSession.REQUEST_INTERVAL_MS,
          maxResultAgeMs = REQUEST_MAX_RESULT_AGE_MS,
          onResult = onResult,
          // A cancelled request said why itself, such as the phone being low on memory.
          onFailure = { stopAi(it?.userMessage(this)) },
        )
      }
      AiBackend.NONE -> {
        enabled.removeAll { it.usesAi }
        updateSwitches()
        report(R.string.ai_service_not_set_up)
        null
      }
    }
  }

  private fun startCamera(token: Int) {
    val future = ProcessCameraProvider.getInstance(this)
    future.addListener({
      if (generation != token || !resumed) return@addListener
      try {
        val provider = future.get()
        cameraProvider = provider
        val newPreview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val newAnalysis = ImageAnalysis.Builder()
          .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build())
          .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        val text = RecognitionMode.TEXT in enabled
        var lastFrame = -FRAME_INTERVAL_MS
        newAnalysis.setAnalyzer(worker) { frame ->
          val now = SystemClock.elapsedRealtime()
          // Gemini takes a frame every few seconds at most, so most frames are only read for text.
          val ai = describer?.canRequest() == true
          if (generation != token || now - lastFrame < FRAME_INTERVAL_MS || !(text || ai) ||
              !processing.compareAndSet(false, true)) {
            frame.close()
            return@setAnalyzer
          }
          lastFrame = now
          try {
            val raw = frame.toBitmap()
            val rotation = frame.imageInfo.rotationDegrees
            val bitmap = if (rotation == 0) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height,
              Matrix().apply { postRotate(rotation.toFloat()) }, true).also { raw.recycle() }
            val jpeg = if (!ai) null else ByteArrayOutputStream().use {
              bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
              Base64.encodeToString(it.toByteArray(), Base64.NO_WRAP)
            }
            handler.post { processFrame(bitmap, jpeg, token, now) }
          } catch (e: Throwable) {
            LogUtils.e(TAG, "Frame failed: %s", e.javaClass.simpleName)
            // Includes OutOfMemoryError, which would otherwise stop the screen reader.
            processing.set(false)
            handler.post { if (generation == token) cameraFailed() }
          } finally { frame.close() }
        }
        preview = newPreview
        analysis = newAnalysis
        val camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, newPreview, newAnalysis)
        cameraState = camera.cameraInfo.cameraState
        cameraState?.observe(this) { state ->
          if (generation == token && state.error != null) {
            LogUtils.e(TAG, "Camera error %d", state.error!!.code)
            cameraFailed()
          }
        }
      } catch (e: Exception) {
        LogUtils.e(TAG, "Camera not started: %s", e.javaClass.simpleName)
        if (generation == token) cameraFailed()
      }
    }, ContextCompat.getMainExecutor(this))
  }

  private fun cameraFailed() { stopRecognition(); report(R.string.live_recognition_camera_error) }

  private fun processFrame(bitmap: Bitmap, jpeg: String?, token: Int, capturedAt: Long) {
    fun release() { bitmap.recycle(); processing.set(false) }
    if (generation != token || !resumed) { release(); return }
    if (jpeg != null) describer?.describe(jpeg, enabled.toSet(), pointAbove)
    if (RecognitionMode.TEXT !in enabled) { release(); return }
    try {
      recognizerUsed = true
      recognizer.process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener { result ->
          LogUtils.v(TAG, "Text, %d characters, %d ms after the frame", result.text.length,
            SystemClock.elapsedRealtime() - capturedAt)
          if (generation == token &&
              SystemClock.elapsedRealtime() - capturedAt <= GeminiLiveSession.MAX_RESULT_AGE_MS) {
            announceResult(SOURCE_TEXT, result.text.take(MAX_TEXT_LENGTH))
          }
        }
        .addOnFailureListener { e ->
          LogUtils.e(TAG, "Text recognition failed: %s", e.javaClass.simpleName)
          if (generation == token) {
            enabled.remove(RecognitionMode.TEXT)
            reconcile()
            report(R.string.live_recognition_ocr_error)
          }
        }.addOnCompleteListener { release() }
    } catch (_: Exception) {
      release()
      enabled.remove(RecognitionMode.TEXT)
      reconcile()
      report(R.string.live_recognition_ocr_error)
    }
  }

  private fun announceResult(source: String, text: String) {
    if (!resumed || enabled.isEmpty() || PauseController.isPaused()) return
    pendingResults[source] = text to SystemClock.elapsedRealtime()
    speakPendingResult()
  }

  private fun speakPendingResult(ignoreRecentInput: Boolean = false) {
    if (!resumed || enabled.isEmpty() || PauseController.isPaused() || pendingResults.isEmpty()) return
    val speech = TalkBackService.getInstance()?.speechController ?: return
    // Keep at most the latest result per source while the user navigates. Never build a speech backlog.
    if (speech.isSpeaking || (!ignoreRecentInput && RecentUserInput.isRecent())) {
      LogUtils.v(TAG, "Result waits: speaking=%s", speech.isSpeaking)
      return
    }
    val now = SystemClock.elapsedRealtime()
    val iterator = pendingResults.iterator()
    while (iterator.hasNext()) {
      val (source, result) = iterator.next()
      iterator.remove()
      val age = now - result.second
      if (age <= GeminiLiveSession.MAX_RESULT_AGE_MS &&
          announcements.shouldSpeak(source, result.first, now)) {
        LogUtils.d(TAG, "Speaking %s result, %d ms after it came", source, age)
        status.text = result.first
        speak(result.first, false)
        return
      }
      LogUtils.d(TAG, "Dropped %s result, %d characters, %d ms old", source, result.first.length, age)
    }
  }

  private fun report(message: Int) = report(getString(message))
  private fun report(message: String) { status.text = message; speak(message, true) }
  private fun speak(text: String, interrupt: Boolean) = LiveRecognition.speak(text, interrupt)


  companion object {
    const val EXTRA_MODE = "recognition_mode"
    /** Whether the user agreed to camera images being sent to Gemini. */
    private const val CLOUD_CONSENT_PREF = "pref_live_recognition_cloud_consent"
    private const val SOURCE_TEXT = "text"
    private const val SOURCE_AI = "ai"
    /** The first part of the key that says the user agreed to images going to an AI service. */
    private const val SERVICE_CONSENT_PREF = "pref_live_recognition_consent_"
    /** How old an answer from the on-device model or an AI service can be when it comes. */
    private const val REQUEST_MAX_RESULT_AGE_MS = 30_000L
    private const val FRAME_INTERVAL_MS = 1_000L
    private const val CHECK_INTERVAL_MS = 500L
    private const val JPEG_QUALITY = 70
    private const val MAX_TEXT_LENGTH = 1_200
    private const val TAG = "LiveRecognition"
  }
}
