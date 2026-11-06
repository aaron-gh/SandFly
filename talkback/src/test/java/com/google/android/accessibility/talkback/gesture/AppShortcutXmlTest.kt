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

package com.google.android.accessibility.talkback.gesture

import android.content.ComponentName
import android.content.Intent
import android.util.Xml
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppShortcutXmlTest {

  /** The texts of the string resources the files refer to, as a compiled file gives references. */
  private val strings = mapOf("@1001" to "Cart", "@1002" to "Your orders", "@1003" to "Open your cart")

  private fun parse(body: String): List<AppShortcutXml.Shortcut> {
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
    parser.setInput(
      StringReader(
        """<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">$body</shortcuts>"""
      )
    )
    return AppShortcutXml.parse(
      parser,
      { value -> if (AppShortcutXml.resourceId(value) != null) strings[value] else value },
      this::readIntent,
    )
  }

  /** Reads an intent's action and target, as Intent.parseIntent does for these files. */
  private fun readIntent(parser: XmlPullParser): Intent? {
    val action = parser.getAttributeValue(ANDROID, "action")
    val targetPackage = parser.getAttributeValue(ANDROID, "targetPackage")
    val targetClass = parser.getAttributeValue(ANDROID, "targetClass")
    AppShortcutXml.skip(parser)
    if (action == null) return null
    val intent = Intent(action)
    if (targetPackage != null && targetClass != null) {
      intent.setClassName(targetPackage, targetClass)
    }
    return intent
  }

  @Test
  fun readsEachShortcutWithItsNameAndIntentInOrder() {
    val shortcuts =
      parse(
        """
        <shortcut android:shortcutId="cart" android:shortcutShortLabel="@1001"
            android:shortcutLongLabel="@1003">
          <intent android:action="android.intent.action.VIEW"
              android:targetPackage="com.example.shop" android:targetClass="com.example.shop.Cart" />
          <categories android:name="android.shortcut.conversation" />
        </shortcut>
        <shortcut android:shortcutId="orders" android:shortcutShortLabel="@1002">
          <intent android:action="com.example.shop.ORDERS" />
        </shortcut>
        """
      )

    assertEquals(listOf("cart", "orders"), shortcuts.map { it.id })
    assertEquals(listOf("Cart", "Your orders"), shortcuts.map { it.label })
    assertEquals(
      ComponentName("com.example.shop", "com.example.shop.Cart"),
      shortcuts[0].intents.single().component,
    )
    assertEquals("com.example.shop.ORDERS", shortcuts[1].intents.single().action)
  }

  @Test
  fun keepsEveryIntentOfAShortcutInOrder() {
    val shortcut =
      parse(
        """
        <shortcut android:shortcutId="compose" android:shortcutShortLabel="Compose">
          <intent android:action="com.example.MAIN" />
          <intent android:action="com.example.COMPOSE" />
        </shortcut>
        """
      )
        .single()

    assertEquals(listOf("com.example.MAIN", "com.example.COMPOSE"), shortcut.intents.map { it.action })
  }

  @Test
  fun usesTheLongLabelWithoutAShortLabel() {
    val shortcut =
      parse(
        """
        <shortcut android:shortcutId="cart" android:shortcutLongLabel="@1003">
          <intent android:action="com.example.CART" />
        </shortcut>
        """
      )
        .single()

    assertEquals("Open your cart", shortcut.label)
  }

  @Test
  fun leavesOutDisabledShortcutsAndThoseMissingAnIdANameOrAnIntent() {
    val shortcuts =
      parse(
        """
        <shortcut android:shortcutId="off" android:enabled="false" android:shortcutShortLabel="Off">
          <intent android:action="com.example.OFF" />
        </shortcut>
        <shortcut android:shortcutShortLabel="No ID">
          <intent android:action="com.example.NO_ID" />
        </shortcut>
        <shortcut android:shortcutId="unnamed" android:shortcutShortLabel="@9999">
          <intent android:action="com.example.UNNAMED" />
        </shortcut>
        <shortcut android:shortcutId="no_intent" android:shortcutShortLabel="No intent" />
        <shortcut android:shortcutId="no_action" android:shortcutShortLabel="No action">
          <intent android:targetPackage="com.example" android:targetClass="com.example.Main" />
        </shortcut>
        <shortcut android:shortcutId="on" android:enabled="true" android:shortcutShortLabel="On">
          <intent android:action="com.example.ON" />
        </shortcut>
        """
      )

    assertEquals(listOf("on"), shortcuts.map { it.id })
  }

  @Test
  fun ignoresCapabilitiesAndOtherElements() {
    val shortcuts =
      parse(
        """
        <capability android:name="actions.intent.OPEN_APP_FEATURE">
          <intent android:action="com.example.FEATURE" />
        </capability>
        <shortcut android:shortcutId="cart" android:shortcutShortLabel="Cart">
          <capability-binding android:key="actions.intent.OPEN_APP_FEATURE">
            <parameter-binding android:key="feature" android:value="cart" />
          </capability-binding>
          <intent android:action="com.example.CART" />
        </shortcut>
        """
      )

    assertEquals(listOf("cart"), shortcuts.map { it.id })
    assertEquals("com.example.CART", shortcuts.single().intents.single().action)
  }

  @Test
  fun keepsTheFirstOfShortcutsWithTheSameId() {
    val shortcuts =
      parse(
        """
        <shortcut android:shortcutId="cart" android:shortcutShortLabel="First">
          <intent android:action="com.example.FIRST" />
        </shortcut>
        <shortcut android:shortcutId="cart" android:shortcutShortLabel="Second">
          <intent android:action="com.example.SECOND" />
        </shortcut>
        """
      )

    assertEquals(listOf("First"), shortcuts.map { it.label })
  }

  @Test
  fun resourceIdReadsOnlyResourceReferences() {
    assertEquals(2131886080, AppShortcutXml.resourceId("@2131886080"))
    assertNull(AppShortcutXml.resourceId("Cart"))
    assertNull(AppShortcutXml.resourceId("@string/cart"))
    assertNull(AppShortcutXml.resourceId("@0"))
    assertNull(AppShortcutXml.resourceId(null))
  }

  @Test
  fun aFileWithoutShortcutsHasNone() {
    assertTrue(parse("").isEmpty())
  }

  private companion object {
    const val ANDROID = "http://schemas.android.com/apk/res/android"
  }
}
