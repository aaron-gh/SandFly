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

package com.google.android.accessibility.talkback.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeciderTest {
  private val message =
    NotificationInfo(
      packageName = "com.example.chat",
      appName = "Chat",
      category = "msg",
      title = "Ann",
      text = "See you at 5",
    )

  private fun decide(
    settings: NotificationSettings = NotificationSettings(),
    info: NotificationInfo = message,
    state: DeviceState = DeviceState(),
    now: Long = 1_000_000,
    decider: NotificationDecider = NotificationDecider(),
  ) =
    decider.decide(info, settings, state, now, categoryName = "Message", time = "10:30").also {
      if (it.read) decider.markSpoken(info.packageName, it, now)
    }

  @Test
  fun theDefaultReadsTheTypeTitleAndText() {
    val decision = decide()
    assertTrue(decision.read)
    assertEquals("Message, Ann, See you at 5", decision.text)
  }

  @Test
  fun aFormatChoosesWhatIsSaid() {
    val decision = decide(NotificationSettings(format = "{app}: {title} says {text} at {time}"))
    assertEquals("Chat: Ann says See you at 5 at 10:30", decision.text)
  }

  @Test
  fun emptyPartsLeaveNoSeparators() {
    val decision = decide(info = message.copy(title = ""), settings = NotificationSettings(format = "{app}, {title}, {text}"))
    assertEquals("Chat, See you at 5", decision.text)
  }

  @Test
  fun appModes() {
    val ignore = NotificationSettings(apps = mapOf("com.example.chat" to AppSettings(AppMode.IGNORE)))
    assertEquals(Outcome.APP_IGNORED, decide(ignore).outcome)

    val private = NotificationSettings(apps = mapOf("com.example.chat" to AppSettings(AppMode.PRIVATE)))
    val decision = decide(private)
    assertTrue(decision.private)
    assertEquals("Chat", decision.text)

    val onlyListed = NotificationSettings(otherApps = AppMode.IGNORE)
    assertEquals(Outcome.APP_IGNORED, decide(onlyListed).outcome)

    val named = NotificationSettings(
      format = "{app} {text}",
      apps = mapOf("com.example.chat" to AppSettings(spokenName = "Talk")),
    )
    assertEquals("Talk See you at 5", decide(named).text)

    val priority = NotificationSettings(apps = mapOf("com.example.chat" to AppSettings(AppMode.PRIORITY)))
    assertTrue(decide(priority).priority)
  }

  @Test
  fun appsAndRulesChooseTheVoice() {
    assertEquals(null, decide().voiceProfile)

    val dectalk = NotificationSettings(apps = mapOf("com.example.chat" to AppSettings(voiceProfile = "2")))
    assertEquals("2", decide(dectalk).voiceProfile)

    val sandFly = NotificationSettings(apps = mapOf("com.example.chat" to AppSettings(voiceProfile = "")))
    assertEquals("", decide(sandFly).voiceProfile)

    // A rule's voice wins over the app's.
    val rule =
      NotificationRule(
        conditions = listOf(RuleCondition(RuleCondition.Type.CONTAINS, value = "see you")),
        actions = listOf(RuleAction(RuleAction.Type.VOICE, value = "3")),
      )
    assertEquals("3", decide(dectalk.copy(rules = listOf(rule))).voiceProfile)
  }

  @Test
  fun anAppsVoiceIsSaved() {
    val apps = mapOf("a" to AppSettings(voiceProfile = "2"), "b" to AppSettings(voiceProfile = ""), "c" to AppSettings(AppMode.READ))
    val read = NotificationSettings.apps(NotificationSettings.appsToJson(apps))
    assertEquals("2", read["a"]?.voiceProfile)
    assertEquals("", read["b"]?.voiceProfile)
    assertEquals(null, read["c"]?.voiceProfile)
    assertFalse(AppSettings(voiceProfile = "").isEmpty)
  }

  @Test
  fun words() {
    assertEquals(
      Outcome.REQUIRED_WORDS_MISSING,
      decide(NotificationSettings(requiredWords = listOf(TextMatch("urgent")))).outcome,
    )
    assertTrue(decide(NotificationSettings(requiredWords = listOf(TextMatch("SEE YOU")))).read)
    assertEquals(
      Outcome.BLOCKED_WORD,
      decide(NotificationSettings(blockedWords = listOf(TextMatch("at \\d", regex = true)))).outcome,
    )
    val private = decide(NotificationSettings(privateWords = listOf(TextMatch("ann"))))
    assertTrue(private.private)
  }

  @Test
  fun replacementsAndCleanup() {
    val settings =
      NotificationSettings(
        format = "{text}",
        replacements = listOf(Replacement("lol", "laughing")),
        removeEmoji = true,
        separateDigits = true,
        minDigits = 4,
        urls = UrlHandling.DOMAIN,
      )
    val info = message.copy(text = "lol 😀 code 123456 at https://www.example.com/x?y=1. lollipop")
    assertEquals("laughing code 1 2 3 4 5 6 at example.com. lollipop", decide(settings, info).text)
  }

  @Test
  fun lengthLimits() {
    val info = message.copy(text = "One two three. Four five six. Seven.")
    assertEquals("One two…", decide(NotificationSettings(format = "{text}", maxWords = 2), info).text)
    assertEquals("One two three.", decide(NotificationSettings(format = "{text}", maxSentences = 1), info).text)
  }

  @Test
  fun kindsOfNotifications() {
    assertEquals(Outcome.ONGOING, decide(NotificationSettings(skipOngoing = true), message.copy(ongoing = true)).outcome)
    assertEquals(Outcome.MEDIA, decide(info = message.copy(media = true)).outcome)
    assertEquals(Outcome.SYSTEM, decide(NotificationSettings(skipSystem = true), message.copy(packageName = "android")).outcome)
    assertEquals(Outcome.EMPTY, decide(NotificationSettings(skipEmpty = true), message.copy(text = "😀")).outcome)
  }

  @Test
  fun repeatsAndCooldown() {
    val decider = NotificationDecider()
    val settings = NotificationSettings(repeatSeconds = 60)
    assertTrue(decide(settings, now = 0, decider = decider).read)
    assertEquals(Outcome.REPEAT, decide(settings, now = 30_000, decider = decider).outcome)
    assertTrue(decide(settings, now = 100_000, decider = decider).read)

    val numbers = NotificationDecider()
    val alike = NotificationSettings(repeatSeconds = 60, numbersAlike = true)
    assertTrue(decide(alike, message.copy(text = "Downloading 10%"), now = 0, decider = numbers).read)
    assertEquals(
      Outcome.REPEAT,
      decide(alike, message.copy(text = "Downloading 20%"), now = 1000, decider = numbers).outcome,
    )

    val cooling = NotificationDecider()
    val cooldown = NotificationSettings(cooldownSeconds = 10)
    assertTrue(decide(cooldown, now = 0, decider = cooling).read)
    assertEquals(Outcome.COOLDOWN, decide(cooldown, message.copy(text = "Other"), now = 5000, decider = cooling).outcome)

    // One that was decided on but never read doesn't start the cooldown.
    val unread = NotificationDecider()
    assertTrue(unread.decide(message, cooldown, DeviceState(), 0).read)
    assertTrue(decide(cooldown, message.copy(text = "Other"), now = 5000, decider = unread).read)
  }

  @Test
  fun aNotificationPostedTwiceIsReadOnce() {
    val decider = NotificationDecider()
    assertTrue(decide(now = 0, decider = decider).read)
    assertEquals(Outcome.REPEAT, decide(now = 1000, decider = decider).outcome)
    // A group summary with the same text, under the app's name.
    val summary = message.copy(title = "Chat", groupSummary = true)
    assertEquals(Outcome.REPEAT, decide(info = summary, now = 1500, decider = decider).outcome)
    // The same words from someone else are read.
    assertTrue(decide(info = message.copy(title = "Bob"), now = 2000, decider = decider).read)
    // And the same notification again, later.
    assertTrue(decide(now = 10_000, decider = decider).read)

    // A summary first, then the message.
    val other = NotificationDecider()
    assertTrue(decide(info = summary, now = 0, decider = other).read)
    assertEquals(Outcome.REPEAT, decide(now = 500, decider = other).outcome)
  }

  @Test
  fun aRepeatedStartIsLeftOut() {
    val decider = NotificationDecider()
    val settings = NotificationSettings(format = "{title}: {text}", skipRepeatedPrefix = true)
    val group = message.copy(title = "Family")
    assertEquals("Family: hello", decide(settings, group.copy(text = "hello"), now = 0, decider = decider).text)
    assertEquals("bye", decide(settings, group.copy(text = "bye"), now = 1000, decider = decider).text)
  }

  @Test
  fun deviceStates() {
    assertEquals(Outcome.SCREEN_ON, decide(NotificationSettings(whenScreenOn = false)).outcome)
    assertEquals(
      Outcome.HEADPHONES_NOT_CONNECTED,
      decide(NotificationSettings(headphones = HeadphoneMode.ONLY_WITH)).outcome,
    )
    assertTrue(
      decide(
        NotificationSettings(headphones = HeadphoneMode.ONLY_WITH),
        state = DeviceState(wiredHeadphones = true),
      ).read
    )
    val quiet = NotificationSettings(quietHours = true, quietStartMinutes = 22 * 60, quietEndMinutes = 7 * 60)
    assertEquals(Outcome.QUIET_HOURS, decide(quiet, state = DeviceState(minuteOfDay = 23 * 60)).outcome)
    assertEquals(Outcome.QUIET_HOURS, decide(quiet, state = DeviceState(minuteOfDay = 6 * 60)).outcome)
    assertTrue(decide(quiet, state = DeviceState(minuteOfDay = 8 * 60)).read)
    assertEquals(
      Outcome.MEDIA_PLAYING,
      decide(NotificationSettings(media = MediaBehavior.SKIP), state = DeviceState(mediaPlaying = true)).outcome,
    )
  }

  @Test
  fun rules() {
    val headphonesOnly =
      NotificationRule(
        name = "Only with headphones",
        conditions = listOf(RuleCondition(RuleCondition.Type.BLUETOOTH_DEVICE, negate = true, value = "AA:BB")),
        actions = listOf(RuleAction(RuleAction.Type.SKIP)),
      )
    val settings = NotificationSettings(rules = listOf(headphonesOnly))
    assertEquals(Outcome.RULE, decide(settings).outcome)
    assertTrue(decide(settings, state = DeviceState(bluetoothDevices = setOf("aa:bb"))).read)

    val privateAtWork =
      NotificationRule(
        matchAll = false,
        conditions =
          listOf(
            RuleCondition(RuleCondition.Type.WIFI_NETWORK, value = "Office"),
            RuleCondition(RuleCondition.Type.FROM_APP, value = "com.example.chat"),
          ),
        actions = listOf(RuleAction(RuleAction.Type.PRIVATE), RuleAction(RuleAction.Type.PRIORITY)),
      )
    val decision = decide(NotificationSettings(rules = listOf(privateAtWork)))
    assertTrue(decision.private)
    assertTrue(decision.priority)

    val disabled = headphonesOnly.copy(enabled = false)
    assertTrue(decide(NotificationSettings(rules = listOf(disabled))).read)

    val quiet = NotificationSettings(quietHours = true, quietStartMinutes = 0, quietEndMinutes = 23 * 60 + 59,
      rules = listOf(NotificationRule(conditions = listOf(RuleCondition(RuleCondition.Type.CONTAINS, value = "see you")),
        actions = listOf(RuleAction(RuleAction.Type.ALWAYS_READ)))))
    assertTrue(decide(quiet).read)
  }

  @Test
  fun rulesSurviveJson() {
    val rule =
      NotificationRule(
        name = "Evenings",
        matchAll = false,
        conditions =
          listOf(RuleCondition(RuleCondition.Type.TIME, startMinutes = 18 * 60, endMinutes = 23 * 60, days = setOf(1, 5))),
        actions = listOf(RuleAction(RuleAction.Type.MAX_WORDS, number = 10)),
      )
    val back = NotificationRule.listFromJson(NotificationRule.listToJson(listOf(rule)))
    assertEquals(listOf(rule), back)
    assertEquals(emptyList<NotificationRule>(), NotificationRule.listFromJson("not json"))
  }

  @Test
  fun timeRanges() {
    assertTrue(isBetween(30, 22 * 60, 60))
    assertFalse(isBetween(120, 22 * 60, 60))
    assertFalse(isBetween(100, 100, 100))
  }

  @Test
  fun summariesFollowAppsWordsAndRules() {
    val decider = NotificationDecider()
    val format = "{app}, {title}, {text}"
    assertEquals("Chat, Ann, See you at 5", decider.describe(message, NotificationSettings(), format))
    val ignored = NotificationSettings(apps = mapOf("com.example.chat" to AppSettings(AppMode.IGNORE)))
    assertEquals(null, decider.describe(message, ignored, format))
    val privateRule =
      NotificationSettings(
        rules = listOf(NotificationRule(conditions = emptyList(), actions = listOf(RuleAction(RuleAction.Type.PRIVATE))))
      )
    assertEquals("Chat", decider.describe(message, privateRule, format))
    // A summary is not a repeat.
    assertEquals("Chat, Ann, See you at 5", decider.describe(message, NotificationSettings(repeatSeconds = 60), format))
  }
}
