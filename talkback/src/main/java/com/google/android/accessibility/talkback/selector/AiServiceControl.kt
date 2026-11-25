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

package com.google.android.accessibility.talkback.selector

import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.AiProvider
import com.google.android.accessibility.utils.ValueSteps

/**
 * Switches the AI service in use, as AI services > AI service does, for the AI service reading
 * control.
 */
internal object AiServiceControl {

  /**
   * The services the reading control goes through: those set up, in the order of AI services,
   * without None. SandFly Pro only with a licence, which SandFly Pro saves as its API key.
   */
  @JvmStatic
  fun services(prefs: SharedPreferences): List<AiProvider> =
    AiProvider.choices(prefs).filter {
      it != AiProvider.NONE && (it != AiProvider.SANDFLY_PRO || it.apiKey(prefs).isNotEmpty())
    }

  /**
   * The service after, or before, [current] in [services], as [ValueSteps] goes, or null with
   * fewer than two services. From a service not among them, such as None, the first service, or
   * the last going back.
   */
  @JvmStatic
  fun step(services: List<AiProvider>, current: AiProvider, forward: Boolean): AiProvider? {
    if (services.size < 2) return null
    val index = services.indexOf(current)
    if (index < 0) return if (forward) services.first() else services.last()
    return services[ValueSteps.step(index, services.size, forward)]
  }

  /** The name of the service in use. */
  @JvmStatic
  fun current(context: Context, prefs: SharedPreferences): String =
    context.getString(AiProvider.active(prefs).label)

  /**
   * Uses the next or previous service and returns its name to speak. With fewer than two services
   * set up, changes nothing and returns what to say instead.
   */
  @JvmStatic
  fun change(context: Context, prefs: SharedPreferences, next: Boolean): String {
    val services = services(prefs)
    val service =
      step(services, AiProvider.active(prefs), next)
        ?: return context.getString(
          if (services.isEmpty()) R.string.ai_service_control_none else R.string.ai_service_control_one
        )
    if (service != AiProvider.active(prefs)) {
      AiProvider.setActive(prefs, service)
    }
    return context.getString(service.label)
  }
}
