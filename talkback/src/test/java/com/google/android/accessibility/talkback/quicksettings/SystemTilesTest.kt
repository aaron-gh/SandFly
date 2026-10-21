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

package com.google.android.accessibility.talkback.quicksettings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemTilesTest {

  @Test
  fun readsPixelTiles() {
    val dump =
      """
          QSTileHost:
          ----------------------------------------------------------------------------
          CurrentTileInteractorImpl:
          User: 0
          WifiTile:    QSTile${'$'}State[spec=wifi,icon=DrawableIconWithRes[resId=0x7f080bcf],iconSupplier=null,label=Wi-Fi,secondaryLabel=Networks unavailable,contentDescription=Wi-Fi,Networks unavailable,stateDescription=null,dualLabelContentDescription=null,expandedAccessibilityClassName=android.widget.Switch,disabledByPolicy=false,dualTarget=false,isTransient=false,handlesSecondaryClick=true,state=1,sideViewCustomDrawable=null]
          CustomTile:    QSTile${'$'}State[spec=custom(com.google.android.gms/.nearby.sharing.SharingTileService),icon=null,iconSupplier=com.android.systemui.qs.external.CustomTile${'$'}${'$'}ExternalSyntheticLambda0@4980466,label=Quick Share,secondaryLabel=null,contentDescription=Quick Share,stateDescription=null,dualLabelContentDescription=null,expandedAccessibilityClassName=android.widget.Button,disabledByPolicy=false,dualTarget=false,isTransient=false,handlesSecondaryClick=false,state=2,sideViewCustomDrawable=null]
      """
        .trimIndent()
    val tiles = SystemTiles.parse(dump)
    assertEquals(
      TileState("wifi", "Wi-Fi", "Networks unavailable", 1, isSwitch = true),
      tiles["wifi"],
    )
    assertEquals(
      TileState(
        "custom(com.google.android.gms/.nearby.sharing.SharingTileService)",
        "Quick Share",
        null,
        2,
        isSwitch = false,
      ),
      tiles["custom(com.google.android.gms/.nearby.sharing.SharingTileService)"],
    )
  }

  @Test
  fun joinsLabelsWrappedOntoNewLines() {
    // A Galaxy phone prints a label with a line break on two lines.
    val dump =
      """
          AirplaneModeTile:    QSTile${'$'}BooleanState[spec=AirplaneMode,icon=AnimationIcon[resId=0x7e080536],nextIcon=null,iconSupplier=null,label=Flight
          mode,secondaryLabel=null,contentDescription=null,stateDescription=null,dualLabelContentDescription=null,expandedAccessibilityClassName=null,disabledByPolicy=false,dualTarget=true,isTransient=false,handlesSecondaryClick=false,state=2,sideViewCustomDrawable=null,tileClassName=null,value=true,forceExpandIcon=false]
          CustomTile:
              SecTile=true SecActiveTile=true supportDetail=false user=0
              36599003  listeners size : 0  {}
              QSTile${'$'}State[spec=custom(com.samsung.android.lool/com.samsung.android.sm.battery.ui.setting.tile.BatteryProtectionTile),icon=null,nextIcon=null,iconSupplier=null,label=Battery protection,secondaryLabel=Maximum,contentDescription=Battery protection,stateDescription=null,dualLabelContentDescription=null,expandedAccessibilityClassName=android.widget.Button,disabledByPolicy=false,dualTarget=true,isTransient=false,handlesSecondaryClick=false,state=2,sideViewCustomDrawable=null,tileClassName=BatteryProtection]
      """
        .trimIndent()
    val tiles = SystemTiles.parse(dump)
    assertEquals("Flight mode", tiles["AirplaneMode"]?.label)
    assertEquals(2, tiles["AirplaneMode"]?.state)
    val protection =
      tiles["custom(com.samsung.android.lool/com.samsung.android.sm.battery.ui.setting.tile.BatteryProtectionTile)"]
    assertEquals("Maximum", protection?.secondaryLabel)
  }

  @Test
  fun leavesOutDirectionMarksAndEmptyLabels() {
    val dump =
      "WifiTile:    QSTile\$State[spec=Wifi,label=Wi-Fi,secondaryLabel=Speedy‎,contentDescription=Wi-Fi,stateDescription=null,expandedAccessibilityClassName=null,state=2]\n" +
        "BluetoothTile:    QSTile\$BooleanState[spec=bt,label=Bluetooth,secondaryLabel=,contentDescription=Bluetooth.,stateDescription=Not connected.,expandedAccessibilityClassName=null,state=1,value=false]"
    val tiles = SystemTiles.parse(dump)
    assertEquals("Speedy", tiles["Wifi"]?.secondaryLabel)
    assertNull(tiles["bt"]?.secondaryLabel)
  }

  @Test
  fun ignoresTextWithoutTiles() {
    assertTrue(SystemTiles.parse("Can't find service: com.android.systemui").isEmpty())
  }
}
