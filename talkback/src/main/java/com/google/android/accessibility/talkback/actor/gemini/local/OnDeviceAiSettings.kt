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

package com.google.android.accessibility.talkback.actor.gemini.local

import android.content.SharedPreferences
import com.google.android.accessibility.talkback.acceleration.Accelerators

/** The settings for running Describe image and Describe screen on the phone instead of the cloud. */
object OnDeviceAiSettings {
  const val PREF_PROVIDER = "pref_on_device_ai_provider"
  const val PREF_MODEL = "pref_on_device_ai_model"
  /** The GPU switch of earlier versions, read once to set [PREF_PROCESSOR]. */
  private const val PREF_GPU = "pref_on_device_ai_gpu"
  const val PREF_PROCESSOR = "pref_on_device_ai_processor"
  const val PREF_SHORT_PROMPTS = "pref_on_device_ai_short_prompts"
  const val PREF_TIMEOUT = "pref_on_device_ai_timeout_seconds"
  const val PREF_IGNORE_MEMORY_LIMITS = "pref_on_device_ai_ignore_memory_limits"

  const val PROVIDER_CLOUD = "cloud"
  const val PROVIDER_DEVICE = "device"

  /** Android reports a little less RAM than the phone's label says, so allow for that. */
  private const val RAM_SLACK = 0.85

  fun isOnDevice(prefs: SharedPreferences): Boolean =
    prefs.getString(PREF_PROVIDER, PROVIDER_CLOUD) == PROVIDER_DEVICE

  fun setOnDevice(prefs: SharedPreferences, onDevice: Boolean) {
    prefs.edit().putString(PREF_PROVIDER, if (onDevice) PROVIDER_DEVICE else PROVIDER_CLOUD).apply()
  }

  /**
   * The model the user chose. When they have not chosen one, this is [defaultModel] for a phone
   * with [totalRamBytes] of RAM, or Gemma 4 E2B when the RAM is not known.
   */
  fun preferredModel(prefs: SharedPreferences, totalRamBytes: Long? = null): LocalModel =
    npuModel().takeIf { processor(prefs) == Processor.NPU }
      ?: LocalModel.fromId(prefs.getString(PREF_MODEL, null))?.takeIf { it.npuChip == null }
      ?: totalRamBytes?.let { defaultModel(it) }
      ?: LocalModel.E2B

  /** Whether a phone with [totalRamBytes] of RAM has enough memory for [model]. */
  fun fits(model: LocalModel, totalRamBytes: Long): Boolean =
    totalRamBytes >= model.minTotalRamBytes * RAM_SLACK

  /**
   * Whether a model file of [modelBytes] can load now, with [availableBytes] of memory free, as
   * Android counts it. A model that loads into less memory than its own size pushes the phone so low
   * that Android stops apps, and in the end the model itself.
   */
  fun hasFreeMemoryFor(modelBytes: Long, availableBytes: Long, lowMemory: Boolean): Boolean =
    !lowMemory && availableBytes >= modelBytes

  /**
   * The models to offer on a phone with [totalRamBytes] of RAM: the ones that fit, and any that
   * are already on the phone, so that they can still be chosen or deleted.
   */
  fun modelsFor(totalRamBytes: Long, isInstalled: (LocalModel) -> Boolean): List<LocalModel> =
    LocalModel.choices.filter { fits(it, totalRamBytes) || isInstalled(it) }

  /**
   * The model to use when the user has not chosen one: Gemma 4 E2B when it fits, otherwise the
   * largest model that fits, preferring ones that are not experimental.
   */
  fun defaultModel(totalRamBytes: Long): LocalModel {
    if (fits(LocalModel.E2B, totalRamBytes)) {
      return LocalModel.E2B
    }
    return LocalModel.choices
      .filter { fits(it, totalRamBytes) }
      .sortedWith(compareBy<LocalModel> { it.experimental }.thenByDescending { it.sizeBytes })
      .firstOrNull() ?: LocalModel.E2B
  }

  fun setPreferredModel(prefs: SharedPreferences, model: LocalModel) {
    prefs.edit().putString(PREF_MODEL, model.id).apply()
  }

  /** The processors a model can run on. */
  enum class Processor(val id: String) {
    CPU("cpu"),
    /** The GPU, through OpenCL. */
    GPU("gpu"),
    /** The NPU of a Google Tensor or Snapdragon chip, with the model built for it. */
    NPU("npu"),
  }

  /** The processors this phone has: the CPU, and the GPU and NPU when models can run on them. */
  fun availableProcessors(): List<Processor> =
    listOfNotNull(
      Processor.CPU,
      Processor.GPU.takeIf { Accelerators.hasOpenCl },
      Processor.NPU.takeIf { npuModel() != null },
    )

  /** The processor the user chose, or the CPU when this phone doesn't have it. */
  fun processor(prefs: SharedPreferences): Processor {
    val chosen =
      Processor.entries.firstOrNull { it.id == prefs.getString(PREF_PROCESSOR, null) }
        ?: if (prefs.getBoolean(PREF_GPU, false)) Processor.GPU else Processor.CPU
    return chosen.takeIf { it in availableProcessors() } ?: Processor.CPU
  }

  fun setProcessor(prefs: SharedPreferences, processor: Processor) {
    prefs.edit().putString(PREF_PROCESSOR, processor.id).remove(PREF_GPU).apply()
  }

  /** The model built for this phone's NPU, or null when it has none that a model is built for. */
  fun npuModel(): LocalModel? =
    Accelerators.npuChip?.let { chip -> LocalModel.entries.firstOrNull { it.npuChip == chip } }

  /** Whether Describe screen asks for less detail, to answer sooner. Off keeps the full prompt. */
  fun shortPrompts(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_SHORT_PROMPTS, false)

  fun setShortPrompts(prefs: SharedPreferences, short: Boolean) {
    prefs.edit().putBoolean(PREF_SHORT_PROMPTS, short).apply()
  }

  /**
   * Whether to let the user choose a model that needs more memory than the phone has, and load it
   * without checking for free memory. A developer setting, to test what happens when Android stops
   * the model.
   */
  fun ignoreMemoryLimits(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(PREF_IGNORE_MEMORY_LIMITS, false)

  /** How long to wait for an answer, in seconds, that the user can pick from. */
  val TIMEOUT_CHOICES_SECONDS = listOf(60, 120, 180, 300, 600)

  /** The cloud gives up after 35 seconds, which a model on the phone often cannot meet. */
  const val DEFAULT_TIMEOUT_SECONDS = 180

  fun timeoutSeconds(prefs: SharedPreferences): Int =
    prefs.getInt(PREF_TIMEOUT, DEFAULT_TIMEOUT_SECONDS).takeIf { it in TIMEOUT_CHOICES_SECONDS }
      ?: DEFAULT_TIMEOUT_SECONDS

  fun setTimeoutSeconds(prefs: SharedPreferences, seconds: Int) {
    prefs.edit().putInt(PREF_TIMEOUT, seconds).apply()
  }

  /** Why a phone can or cannot run on-device AI. */
  enum class Support {
    OK,
    /** The model library only ships for 64-bit ARM. */
    UNSUPPORTED_CPU,
    NOT_ENOUGH_RAM,
  }

  fun support(model: LocalModel, totalRamBytes: Long, supportedAbis: List<String>): Support =
    when {
      "arm64-v8a" !in supportedAbis -> Support.UNSUPPORTED_CPU
      !fits(model, totalRamBytes) -> Support.NOT_ENOUGH_RAM
      else -> Support.OK
    }
}
