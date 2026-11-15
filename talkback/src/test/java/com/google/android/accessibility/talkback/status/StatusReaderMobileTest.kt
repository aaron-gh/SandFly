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

package com.google.android.accessibility.talkback.status

import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.google.android.accessibility.talkback.monitor.BatteryMonitor
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSubscriptionManager.SubscriptionInfoBuilder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StatusReaderMobileTest {
  private val context = RuntimeEnvironment.getApplication()
  private val telephony = context.getSystemService(TelephonyManager::class.java)
  private val subscriptions = context.getSystemService(SubscriptionManager::class.java)
  private lateinit var reader: StatusReader

  @Before
  fun setUp() {
    shadowOf(telephony).setSimState(TelephonyManager.SIM_STATE_READY)
    shadowOf(telephony).setNetworkOperatorName("Vodafone")
    shadowOf(telephony).setDataNetworkType(TelephonyManager.NETWORK_TYPE_LTE)
    reader = StatusReader(context, BatteryMonitor(context))
  }

  @Test
  fun oneSimIsReadAsBefore() {
    shadowOf(subscriptions).setActiveSubscriptionInfos(sim(1, 0, "SIM 1"))
    assertEquals("Vodafone, LTE", reader.describe(listOf(StatusItem.MOBILE)))
  }

  @Test
  fun noSubscriptionListReadsTheDefaultNetwork() {
    assertEquals("Vodafone, LTE", reader.describe(listOf(StatusItem.MOBILE)))
  }

  @Test
  fun eachActiveSimIsReadInSlotOrder() {
    simManager(1, "Vodafone", TelephonyManager.NETWORK_TYPE_NR)
    simManager(2, "", TelephonyManager.NETWORK_TYPE_UMTS)
    shadowOf(subscriptions).setActiveSubscriptionInfos(sim(1, 0, "SIM 1"), sim(2, 1, "Work"))
    // The second SIM has no carrier name, so its own name is said.
    assertEquals("Vodafone, 5G, Work, 3G", reader.describe(listOf(StatusItem.MOBILE)))
  }

  private fun sim(id: Int, slot: Int, name: String): SubscriptionInfo =
    SubscriptionInfoBuilder.newBuilder()
      .setId(id)
      .setSimSlotIndex(slot)
      .setDisplayName(name)
      .buildSubscriptionInfo()

  private fun simManager(
    id: Int,
    operator: String,
    networkType: Int,
  ) {
    // Robolectric keeps one SIM state for every TelephonyManager: setUp's.
    val manager = Shadow.newInstanceOf(TelephonyManager::class.java)
    shadowOf(manager).setNetworkOperatorName(operator)
    shadowOf(manager).setDataNetworkType(networkType)
    shadowOf(telephony).setTelephonyManagerForSubscriptionId(id, manager)
  }
}
