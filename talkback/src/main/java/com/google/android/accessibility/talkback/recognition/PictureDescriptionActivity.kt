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
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaActionSound
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.Executors

/**
 * Takes one picture with the rear camera and shows it, then runs Describe image on it, so that the
 * description uses the AI service in use and offers follow-up questions as Describe image does.
 */
class PictureDescriptionActivity : ComponentActivity() {
  private val handler = Handler(Looper.getMainLooper())
  private val worker = Executors.newSingleThreadExecutor()
  private val shutter = MediaActionSound()
  private lateinit var picture: ImageView
  private lateinit var retake: Button
  private var cameraProvider: ProcessCameraProvider? = null
  private var capture: ImageCapture? = null
  private var taken = false
  private var capturing = false
  private val cameraPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) start() else stop(R.string.live_recognition_camera_permission)
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    title = getString(R.string.title_take_picture_and_describe)
    picture =
      ImageView(this).apply {
        contentDescription = getString(R.string.picture_description_image)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        isFocusable = true
        scaleType = ImageView.ScaleType.FIT_CENTER
      }
    retake =
      Button(this).apply {
        setText(R.string.picture_description_retake)
        isEnabled = false
        setOnClickListener { start() }
      }
    setContentView(
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        fitsSystemWindows = true
        addView(picture, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        addView(retake)
      }
    )
  }

  override fun onResume() {
    super.onResume()
    if (taken || capturing) return
    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      start()
    } else {
      cameraPermission.launch(Manifest.permission.CAMERA)
    }
  }

  override fun onPause() {
    handler.removeCallbacksAndMessages(null)
    if (capturing) {
      capturing = false
      retake.isEnabled = true
    }
    release()
    super.onPause()
  }

  override fun onDestroy() {
    worker.shutdown()
    shutter.release()
    super.onDestroy()
  }

  private fun start() {
    if (capturing) return
    capturing = true
    taken = false
    retake.isEnabled = false
    LiveRecognition.speak(getString(R.string.picture_description_hold_steady), true)
    val future = ProcessCameraProvider.getInstance(this)
    future.addListener(
      {
        if (!capturing) return@addListener
        try {
          val provider = future.get()
          cameraProvider = provider
          val newCapture = ImageCapture.Builder().build()
          provider.unbindAll()
          provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, newCapture)
          capture = newCapture
          handler.postDelayed({ takePicture(newCapture) }, EXPOSURE_SETTLE_MS)
        } catch (e: Exception) {
          LogUtils.w(TAG, "Camera not opened: %s", e)
          stop(R.string.live_recognition_camera_error)
        }
      },
      ContextCompat.getMainExecutor(this),
    )
  }

  private fun takePicture(use: ImageCapture) {
    shutter.play(MediaActionSound.SHUTTER_CLICK)
    use.takePicture(
      worker,
      object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
          val bitmap =
            try {
              upright(image)
            } catch (e: RuntimeException) {
              LogUtils.w(TAG, "Picture not read: %s", e)
              null
            } finally {
              image.close()
            }
          handler.post {
            if (bitmap == null) stop(R.string.live_recognition_camera_error) else show(bitmap)
          }
        }

        override fun onError(exception: ImageCaptureException) {
          LogUtils.w(TAG, "Picture not taken: %s", exception)
          handler.post { stop(R.string.live_recognition_camera_error) }
        }
      },
    )
  }

  private fun upright(image: ImageProxy): Bitmap {
    val source = image.toBitmap()
    val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(source.width, source.height))
    val matrix =
      Matrix().apply {
        postScale(scale, scale)
        postRotate(image.imageInfo.rotationDegrees.toFloat())
      }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
  }

  private fun show(bitmap: Bitmap) {
    if (!capturing) return
    capturing = false
    taken = true
    release()
    picture.setImageBitmap(bitmap)
    retake.isEnabled = true
    // Describe image describes the focused item, once it is on screen and has focus.
    handler.postDelayed(
      {
        picture.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
        handler.postDelayed(
          {
            TalkBackService.getInstance()
              ?.performCommand(getString(R.string.shortcut_value_describe_image))
          },
          DESCRIBE_DELAY_MS,
        )
      },
      DESCRIBE_DELAY_MS,
    )
  }

  private fun stop(message: Int) {
    capturing = false
    release()
    retake.isEnabled = true
    LiveRecognition.speak(getString(message), true)
  }

  private fun release() {
    capture?.let { cameraProvider?.unbind(it) }
    capture = null
  }

  private companion object {
    const val TAG = "PictureDescription"
    const val EXPOSURE_SETTLE_MS = 1500L
    const val DESCRIBE_DELAY_MS = 400L
    const val MAX_SIDE = 1600
  }
}
