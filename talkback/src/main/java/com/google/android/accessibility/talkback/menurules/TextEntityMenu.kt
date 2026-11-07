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

package com.google.android.accessibility.talkback.menurules

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.os.SystemClock
import android.telephony.PhoneNumberUtils
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.Menu
import android.view.MenuItem
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextLinks
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.contextmenu.ContextMenu
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem
import com.google.android.accessibility.talkback.contextmenu.OnContextMenuItemClickListener
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

/**
 * Links, email addresses, phone numbers and codes, such as one-time codes in a text message, that
 * are written in an item's text without being links, offered with the item's links in the
 * Actions menu: links and addresses open, numbers open in the phone app, and codes are copied.
 *
 * Phone numbers, dates, times, addresses and flight numbers come from Android's text
 * classifier, the one behind Smart Linkify. Codes are runs of 4 to 8 digits that stand on their
 * own and aren't part of anything the classifier found.
 */
class TextEntityMenu(private val pipeline: Pipeline.FeedbackReturner) : NodeMenu {
  /** Something found in the text, and what choosing it does. */
  sealed interface Entity {
    val text: String

    data class Web(override val text: String, val url: String) : Entity

    data class Email(override val text: String, val address: String) : Entity

    data class Phone(override val text: String, val number: String) : Entity

    data class Code(override val text: String) : Entity
  }

  /** Text the classifier found, from [start] to [end], of the classifier's entity [type]. */
  data class Classified(val start: Int, val end: Int, val type: String)

  /**
   * Whether the text of [node] or its children may hold something to offer. It runs as each item
   * is focused, for the hint that actions are available, so it only looks for digits or the marks
   * of an address.
   */
  override fun accept(context: Context, node: AccessibilityNodeInfoCompat): Boolean {
    if (mayHold(node.text) || mayHold(node.contentDescription)) return true
    for (i in 0 until minOf(node.childCount, MAX_CHILDREN_CHECKED)) {
      val child = node.getChild(i) ?: continue
      if (mayHold(child.text) || mayHold(child.contentDescription)) return true
    }
    return false
  }

  /**
   * The items for the Actions menu and the Actions reading control. None for the hint that actions
   * are available ([includeAncestors] false). Inside [withoutWaiting], phone numbers and codes
   * only when the text was classified before.
   */
  override fun getMenuItemsForNode(
    context: Context,
    node: AccessibilityNodeInfoCompat,
    includeAncestors: Boolean,
  ): List<ContextMenuItem> {
    if (!includeAncestors) return emptyList()
    val text = textOf(node)
    val classified = classify(context, text, wait = noWait.get() != true)
    return find(text, linkedUrls(node), classified).mapIndexed { index, entity ->
      val label =
        when (entity) {
          is Entity.Web,
          is Entity.Email -> entity.text
          is Entity.Phone -> context.getString(R.string.menu_item_call_number, entity.text)
          is Entity.Code -> context.getString(R.string.menu_item_copy_code, entity.text)
        }
      ContextMenu.createMenuItem(context, R.id.group_links, FIRST_ITEM_ID + index, Menu.NONE, label)
        .apply {
          setOnMenuItemClickListener(
            object : OnContextMenuItemClickListener {
              override fun onMenuItemClick(item: MenuItem): Boolean = choose(context, entity)
            }
          )
        }
    }
  }

  private fun choose(context: Context, entity: Entity): Boolean {
    val intent =
      when (entity) {
        is Entity.Web -> Intent(Intent.ACTION_VIEW, Uri.parse(entity.url))
        is Entity.Email -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + entity.address))
        is Entity.Phone -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(entity.number)))
        is Entity.Code -> {
          val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
          clipboard.setPrimaryClip(ClipData.newPlainText(entity.text, entity.text))
          pipeline.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.speech(context.getString(R.string.template_text_copied, entity.text)),
          )
          return true
        }
      }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
      context.startActivity(intent)
      true
    } catch (e: ActivityNotFoundException) {
      LogUtils.w(TAG, "Nothing opens %s", entity)
      false
    }
  }

  companion object {
    private const val TAG = "TextEntityMenu"
    // After the link menu's items, which are numbered from 0 in the same group.
    private const val FIRST_ITEM_ID = 1000
    private const val MAX_NODES = 40
    private const val MAX_CHARACTERS = 4_000
    private const val MAX_ENTITIES = 20
    private const val MAX_CHILDREN_CHECKED = 10
    // The classifier answers in 20 to 250 ms on a Galaxy Z Fold7, and the Actions menu and the
    // Actions reading control's swipes wait for it.
    private const val CLASSIFY_TIMEOUT_MS = 1_000L
    private val MAY_HOLD = Regex("\\p{Nd}{4}|@|://|[a-zA-Z0-9]\\.[a-zA-Z]{2,}")

    private fun mayHold(text: CharSequence?): Boolean =
      text != null && text.length >= 4 && MAY_HOLD.containsMatchIn(text)

    private val CODE = Regex("(?<!\\p{Nd})\\p{Nd}{4,8}(?!\\p{Nd})")

    // Types whose digits are no code.
    private val CLASSIFIED_TYPES =
      listOf(
        TextClassifier.TYPE_PHONE,
        TextClassifier.TYPE_DATE,
        TextClassifier.TYPE_DATE_TIME,
        TextClassifier.TYPE_ADDRESS,
        TextClassifier.TYPE_FLIGHT_NUMBER,
        TextClassifier.TYPE_URL,
        TextClassifier.TYPE_EMAIL,
      )

    // One thread, ended when idle, with room for one waiting request: a newer one replaces it,
    // so that a classifier that doesn't answer holds two requests at most.
    private val classifierThread =
      ThreadPoolExecutor(
          1,
          1,
          30,
          TimeUnit.SECONDS,
          ArrayBlockingQueue(1),
          RejectedExecutionHandler { task, executor ->
            if (executor.isShutdown) return@RejectedExecutionHandler
            (executor.queue.poll() as? Future<*>)?.cancel(false)
            executor.queue.offer(task)
          },
        )
        .apply { allowCoreThreadTimeOut(true) }
    private val lock = Any()
    private var pendingText: String? = null
    private var pending: Future<List<Classified>?>? = null
    private var pendingSince = 0L
    private var lastText: String? = null
    private var lastClassified: List<Classified>? = null
    private val noWait = ThreadLocal<Boolean>()
    @Volatile private var classifiedListener: Runnable? = null

    /**
     * Sets [listener] to run, on the classifier's thread, when a classification ends, so that
     * checks made [withoutWaiting] can be made again. Null removes it.
     */
    @JvmStatic
    fun setClassifiedListener(listener: Runnable?) {
      classifiedListener = listener
    }

    /**
     * Whether the latest classification still runs, for at most [CLASSIFY_TIMEOUT_MS], so that a
     * classifier that doesn't answer holds nothing up.
     */
    @JvmStatic
    fun isClassifying(): Boolean =
      synchronized(lock) {
        pending?.isDone == false &&
          SystemClock.uptimeMillis() - pendingSince < CLASSIFY_TIMEOUT_MS
      }

    /**
     * Runs [block] with the text classifier's result taken only when it is already there. A
     * classification that isn't starts in the background, for a later call. For checks that run
     * as items are focused, such as whether the Actions reading control is available.
     */
    @JvmStatic
    fun <T> withoutWaiting(block: Supplier<T>): T {
      val previous = noWait.get()
      noWait.set(true)
      try {
        return block.get()
      } finally {
        noWait.set(previous)
      }
    }

    /**
     * What Android's text classifier finds in [text], or null when there is no classifier
     * (Android 8), it failed, or it takes longer than [CLASSIFY_TIMEOUT_MS]. Without [wait], null
     * also while the classification runs.
     */
    @JvmStatic
    fun classify(context: Context, text: String, wait: Boolean): List<Classified>? {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || text.isBlank()) return null
      val future =
        synchronized(lock) {
          // A failure is asked again only by calls that wait, so that checks made again on
          // focus don't keep asking.
          if (text == lastText && (lastClassified != null || !wait)) return lastClassified
          val current = pending
          if (text == pendingText && current != null && !current.isDone) {
            current
          } else {
            classifierThread
              .submit<List<Classified>?> { classifyNow(context, text) }
              .also {
                pending = it
                pendingText = text
                pendingSince = SystemClock.uptimeMillis()
              }
          }
        }
      if (!wait) return null
      return try {
        future.get(CLASSIFY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
      } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        null
      } catch (e: Exception) {
        // Left running, so that its result is there for the next call.
        LogUtils.w(TAG, "No text classification: %s", e)
        null
      }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun classifyNow(context: Context, text: String): List<Classified>? {
      val result =
        try {
          generateLinks(context, text)
        } catch (e: Throwable) {
          LogUtils.w(TAG, "Text classification failed: %s", e)
          null
        }
      // A failure is kept too: see classify.
      synchronized(lock) {
        lastText = text
        lastClassified = result
      }
      classifiedListener?.run()
      return result
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun generateLinks(context: Context, text: String): List<Classified>? {
      val classifier =
        context.getSystemService(TextClassificationManager::class.java)?.textClassifier
          ?: return null
      if (classifier === TextClassifier.NO_OP) return null
      val request =
        TextLinks.Request.Builder(text)
          .setEntityConfig(TextClassifier.EntityConfig.createWithExplicitEntityList(CLASSIFIED_TYPES))
          .setDefaultLocales(LocaleList.getDefault())
          .build()
      return classifier.generateLinks(request).links.mapNotNull { link ->
        if (link.entityCount == 0) return@mapNotNull null
        // Samsung's classifier lists phone_e164 before phone for the same number.
        val types = (0 until link.entityCount).map(link::getEntity)
        val type = if (TextClassifier.TYPE_PHONE in types) TextClassifier.TYPE_PHONE else types[0]
        Classified(link.start, link.end, type)
      }
    }

    /** The text of [node] and its descendants, as much as is looked through. */
    @JvmStatic
    fun textOf(node: AccessibilityNodeInfoCompat): String {
      val text = StringBuilder()
      val queue = ArrayDeque<AccessibilityNodeInfoCompat>()
      queue.add(node)
      var visited = 0
      while (queue.isNotEmpty() && visited < MAX_NODES && text.length < MAX_CHARACTERS) {
        val current = queue.removeFirst()
        visited++
        current.text?.let { text.append(it).append('\n') }
        current.contentDescription?.let { text.append(it).append('\n') }
        for (i in 0 until current.childCount) {
          current.getChild(i)?.let(queue::add)
        }
      }
      return text.take(MAX_CHARACTERS).toString()
    }

    /** The URLs that [node]'s own text already links, which the link menu offers. */
    private fun linkedUrls(node: AccessibilityNodeInfoCompat): Set<String> {
      val text = node.text as? Spanned ?: return emptySet()
      return text.getSpans(0, text.length, URLSpan::class.java).mapNotNull { it.url }.toSet()
    }

    /**
     * Whether the digits starting at [start] are part of a word, a user name, a link, a time, an
     * amount or a copyright year. A hyphen after a single letter, as in G-482916, leaves them on
     * their own.
     */
    private fun joinedBefore(text: String, start: Int): Boolean {
      val c = text.getOrNull(start - 1) ?: return false
      if (c.isLetterOrDigit() || c in "_@/\\#&=%+.,'–—" || isCurrency(c)) return true
      // Part of a time, such as 10:45, but not after a word, as in Code:482916.
      if (c == ':') return text.getOrNull(start - 2)?.isDigit() == true
      if (c == '-') {
        val p = text.getOrNull(start - 2) ?: return false
        if (!p.isLetterOrDigit()) return false
        return p.isDigit() || text.getOrNull(start - 3)?.isLetterOrDigit() == true
      }
      val p = if (c.isWhitespace()) text.getOrNull(start - 2) else c
      return isCurrency(p) || p == '©'
    }

    /** Whether the digits ending before [end] continue into something else. */
    private fun joinedAfter(text: String, end: Int): Boolean {
      val c = text.getOrNull(end) ?: return false
      if (c.isLetterOrDigit() || c in "_@/\\%" || isCurrency(c)) return true
      if (c in ".,:-–—'") return text.getOrNull(end + 1)?.isLetterOrDigit() == true
      return c.isWhitespace() && isCurrency(text.getOrNull(end + 1))
    }

    private fun isCurrency(c: Char?): Boolean =
      c != null && Character.getType(c) == Character.CURRENCY_SYMBOL.toInt()

    /**
     * The number to dial for [shown], a phone number the classifier found. +44 (0)20 is dialled
     * as +4420.
     */
    private fun dialledNumber(shown: String): String {
      val number = if (shown.trimStart().startsWith('+')) shown.replace("(0)", "") else shown
      return PhoneNumberUtils.normalizeNumber(number)
    }

    /**
     * Finds the entities in [text], leaving out links in [linked]. [classified] is what the text
     * classifier found; when it is null, only web and email addresses are listed, since nothing
     * then tells a code from a date.
     */
    @JvmStatic
    fun find(
      text: String,
      linked: Set<String> = emptySet(),
      classified: List<Classified>? = null,
    ): List<Entity> {
      if (text.isBlank()) return emptyList()
      val spannable = SpannableString(text)
      Linkify.addLinks(spannable, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
      val found = LinkedHashMap<String, Entity>()
      val taken = mutableListOf<IntRange>()
      val spans =
        spannable.getSpans(0, spannable.length, URLSpan::class.java).sortedBy {
          spannable.getSpanStart(it)
        }
      for (span in spans) {
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)
        val shown = text.substring(start, end)
        val url = span.url ?: continue
        taken += start until end
        val entity =
          when {
            url.startsWith("mailto:") -> Entity.Email(shown, url.removePrefix("mailto:"))
            url in linked || shown in linked -> null
            else -> Entity.Web(shown, url)
          }
        if (entity != null) found.putIfAbsent(entity.text, entity)
      }
      if (classified == null) return found.values.take(MAX_ENTITIES)
      for (item in classified.sortedBy { it.start }) {
        if (item.start < 0 || item.end > text.length || item.start >= item.end) continue
        taken += item.start until item.end
        if (item.type != TextClassifier.TYPE_PHONE) continue
        val shown = text.substring(item.start, item.end)
        val number = dialledNumber(shown)
        if (number.isNotEmpty()) found.putIfAbsent(shown, Entity.Phone(shown, number))
      }
      for (match in CODE.findAll(text)) {
        val range = match.range
        if (taken.any { range.first in it || range.last in it }) continue
        if (joinedBefore(text, range.first) || joinedAfter(text, range.last + 1)) continue
        found.putIfAbsent(match.value, Entity.Code(match.value))
      }
      return found.values.take(MAX_ENTITIES)
    }
  }
}
