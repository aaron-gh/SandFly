/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.android.accessibility.talkback.contextmenu.radial

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.tan

/**
 * Draws the circle menu and turns a finger sliding over it into item focus and selection. The
 * circle is always in the middle of the screen. Each slice reaches out to the edge of the screen, so
 * the direction from the middle picks the item. Touching or sliding into a slice focuses its item
 * and lifting selects it. Lifting in the middle of the circle selects nothing.
 *
 * Touches arrive as plain touch events when the screen passes touches straight through to the menu,
 * and as hover events when they go through touch exploration. Both are handled the same way.
 *
 * Ported from the radial menu in TalkBack 8.1.
 */
@SuppressLint("ViewConstructor")
internal class RadialMenuView(context: Context, private val listener: Listener) : View(context) {

  interface Listener {
    /** The finger touched the screen. */
    fun onTouchStarted()

    /** The finger moved to the item at [index], or to no item if [index] is [NO_ITEM]. */
    fun onItemFocused(index: Int)

    /** The finger lifted over the item at [index], or over no item if [index] is [NO_ITEM]. */
    fun onItemSelected(index: Int)
  }

  class Item(val title: String, val hasSubMenu: Boolean)

  private var items: List<Item> = emptyList()

  // Sized for a phone, and scaled down to fit smaller screens such as a watch's.
  private var innerRadius = dp(INNER_RADIUS_DP)
  private var outerRadius = dp(OUTER_RADIUS_DP)
  private var extremeRadius = dp(EXTREME_RADIUS_DP)
  private val spacing = dp(2f)
  private val shadowRadius = dp(4f)
  private val textSize =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, resources.displayMetrics)

  private val paint = Paint().apply { isAntiAlias = true }
  private val subMenuFilter = PorterDuffColorFilter(SUBMENU_OVERLAY_COLOR, PorterDuff.Mode.SCREEN)
  private val gradientBackground =
    GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(GRADIENT_INNER_COLOR, GRADIENT_OUTER_COLOR),
      )
      .apply {
        gradientType = GradientDrawable.RADIAL_GRADIENT
        gradientRadius = extremeRadius * 2
      }

  // Bounds of the circles, centered on (extremeRadius, extremeRadius).
  private var outerBound = circleBounds(outerRadius)
  private var extremeBound = circleBounds(extremeRadius)

  // A single slice pointing up, and the same outline in reverse for drawing text the right way up.
  private val slicePath = Path()
  private val slicePathReverse = Path()
  private var slicePathWidth = 0f
  private val tempMatrix = Matrix()

  /** The middle of the circle, which is the middle of the screen. */
  private val center = PointF()
  /** Where fingers that were already down when the menu opened started following it. */
  private val heldTouchStart = PointF()
  /** Whether a finger is on the screen. */
  private var tracking = false
  private var focusedIndex = NO_ITEM

  init {
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
  }

  /** Shows [items] as a new circle. */
  fun setItems(items: List<Item>) {
    this.items = items
    tracking = false
    focusedIndex = NO_ITEM
    updateSliceShapes()
    invalidate()
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    center.set(w / 2f, h / 2f)
    // On a screen smaller than the circle, such as a round watch, shrink the circle so that the
    // item names, which run along its outer edge, stay on the screen.
    val scale = (minOf(w, h) / 2f / dp(EXTREME_RADIUS_DP)).coerceAtMost(1f)
    innerRadius = dp(INNER_RADIUS_DP) * scale
    outerRadius = dp(OUTER_RADIUS_DP) * scale
    extremeRadius = dp(EXTREME_RADIUS_DP) * scale
    outerBound = circleBounds(outerRadius)
    extremeBound = circleBounds(extremeRadius)
    gradientBackground.gradientRadius = extremeRadius * 2
    updateSliceShapes()
  }

  override fun onHoverEvent(event: MotionEvent): Boolean = onTouchEvent(event)

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN,
      MotionEvent.ACTION_HOVER_ENTER -> {
        onEnter()
        onMove(event.x, event.y)
      }
      MotionEvent.ACTION_MOVE,
      MotionEvent.ACTION_HOVER_MOVE -> {
        // A hover move can come without an enter, for example when the menu opens under a finger
        // that is already exploring.
        if (!tracking) {
          onEnter()
        }
        onMove(event.x, event.y)
      }
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_HOVER_EXIT -> onUp(event.x, event.y)
      MotionEvent.ACTION_CANCEL -> {
        tracking = false
        focusedIndex = NO_ITEM
        invalidate()
      }
      else -> return false
    }
    return true
  }

  /**
   * Follows fingers that were already down when the menu opened, which do not reach the menu as
   * touches. Where they were counts as the middle, since they were not put down to choose an item,
   * so they must slide out from there. Lifting before reaching an item leaves the menu open, so that
   * it can be touched again.
   */
  fun followHeldTouch(x: Float, y: Float, lifted: Boolean) {
    if (!tracking) {
      if (lifted) {
        return
      }
      heldTouchStart.set(x, y)
      onEnter()
    }
    val menuX = center.x + x - heldTouchStart.x
    val menuY = center.y + y - heldTouchStart.y
    if (!lifted) {
      onMove(menuX, menuY)
    } else if (focusedIndex == NO_ITEM && itemAt(menuX, menuY) == NO_ITEM) {
      tracking = false
    } else {
      onUp(menuX, menuY)
    }
  }

  private fun onEnter() {
    tracking = true
    listener.onTouchStarted()
  }

  private fun onMove(x: Float, y: Float) {
    if (!tracking) {
      return
    }
    focusItem(itemAt(x, y))
  }

  private fun onUp(x: Float, y: Float) {
    if (!tracking) {
      return
    }
    tracking = false
    val touched = itemAt(x, y)
    focusedIndex = touched
    invalidate()
    listener.onItemSelected(touched)
  }

  private fun focusItem(index: Int) {
    if (index == focusedIndex) {
      return
    }
    focusedIndex = index
    invalidate()
    listener.onItemFocused(index)
  }

  /**
   * Returns the item in the direction of ([x], [y]), or [NO_ITEM] in the middle of the circle. Once
   * an item is focused, the middle shrinks, so that a finger drifting back a little as it lifts does
   * not cancel.
   */
  private fun itemAt(x: Float, y: Float): Int {
    val middleRadius =
      if (focusedIndex == NO_ITEM) innerRadius else innerRadius * KEEP_ITEM_MIDDLE_FRACTION
    if (items.isEmpty() || distSq(center, x, y) <= middleRadius * middleRadius) {
      return NO_ITEM
    }
    val wedgeArc = 360.0 / items.size
    // Degrees clockwise from straight up, shifted so that the first slice, which is centered on
    // straight up, starts at 0.
    val angle = Math.toDegrees(atan2((x - center.x).toDouble(), (center.y - y).toDouble()))
    val arc = (angle + 360.0 + wedgeArc / 2) % 360.0
    return (arc / wedgeArc).toInt().coerceIn(0, items.size - 1)
  }

  /** Rebuilds the shape of a single slice, which depends on how many items there are. */
  private fun updateSliceShapes() {
    slicePath.rewind()
    slicePathReverse.rewind()
    if (items.isEmpty()) {
      return
    }
    val wedgeArc = 360f / items.size
    val offsetArc = wedgeArc / 2 + 90f
    val spacingArc = Math.toDegrees(tan(spacing / outerRadius.toDouble())).toFloat()
    val left = wedgeArc - spacingArc - offsetArc
    val middle = wedgeArc / 2 - offsetArc
    val right = spacingArc - offsetArc

    slicePath.arcTo(outerBound, middle, left - middle)
    slicePath.arcTo(extremeBound, left, right - left)
    slicePath.arcTo(outerBound, right, middle - right)
    slicePath.close()

    slicePathWidth = arcLength(left - right, extremeRadius)

    slicePathReverse.arcTo(outerBound, middle, right - middle)
    slicePathReverse.arcTo(extremeBound, right, left - right)
    slicePathReverse.arcTo(outerBound, left, middle - left)
    slicePathReverse.close()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    gradientBackground.setGradientCenter(center.x / width, center.y / height)
    gradientBackground.setBounds(0, 0, width, height)
    gradientBackground.draw(canvas)

    drawCancel(canvas)
    val wedgeArc = 360f / items.size.coerceAtLeast(1)
    items.forEachIndexed { index, item -> drawWedge(canvas, index, item, wedgeArc * index) }
  }

  private fun drawCancel(canvas: Canvas) {
    val selected = focusedIndex == NO_ITEM
    val iconRadius = innerRadius / 4
    paint.style = Paint.Style.FILL
    paint.color = if (selected) SELECTION_COLOR else CENTER_FILL_COLOR
    paint.setShadowLayer(
      shadowRadius,
      0f,
      0f,
      if (selected) SELECTION_SHADOW_COLOR else TEXT_SHADOW_COLOR,
    )
    canvas.drawOval(centeredBounds(center.x, center.y, innerRadius), paint)
    paint.clearShadowLayer()

    paint.style = Paint.Style.STROKE
    paint.color = if (selected) SELECTION_TEXT_COLOR else TEXT_COLOR
    paint.strokeCap = Paint.Cap.SQUARE
    paint.strokeWidth = dp(4f)
    canvas.drawLine(
      center.x - iconRadius,
      center.y - iconRadius,
      center.x + iconRadius,
      center.y + iconRadius,
      paint,
    )
    canvas.drawLine(
      center.x + iconRadius,
      center.y - iconRadius,
      center.x - iconRadius,
      center.y + iconRadius,
      paint,
    )
  }

  private fun drawWedge(canvas: Canvas, index: Int, item: Item, rotation: Float) {
    val selected = index == focusedIndex
    paint.colorFilter = if (item.hasSubMenu) subMenuFilter else null

    tempMatrix.setRotate(rotation, extremeRadius, extremeRadius)
    tempMatrix.postTranslate(center.x - extremeRadius, center.y - extremeRadius)
    canvas.save()
    canvas.concat(tempMatrix)

    paint.style = Paint.Style.FILL
    paint.color = if (selected) SELECTION_COLOR else OUTER_FILL_COLOR
    paint.setShadowLayer(
      shadowRadius,
      0f,
      0f,
      if (selected) SELECTION_SHADOW_COLOR else TEXT_SHADOW_COLOR,
    )
    canvas.drawPath(slicePath, paint)
    paint.clearShadowLayer()

    paint.color = if (selected) SELECTION_TEXT_COLOR else TEXT_COLOR
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = textSize
    paint.setShadowLayer(shadowRadius, 0f, 0f, TEXT_SHADOW_COLOR)
    val text = ellipsize(item.title, slicePathWidth)
    // Keep the text the right way up on the bottom half of the circle.
    if (rotation < 90 || rotation > 270) {
      canvas.drawTextOnPath(text, slicePathReverse, 0f, 2 * textSize, paint)
    } else {
      canvas.drawTextOnPath(text, slicePath, 0f, -textSize, paint)
    }
    paint.clearShadowLayer()
    paint.colorFilter = null
    canvas.restore()
  }

  private fun ellipsize(title: String, maxWidth: Float): String {
    if (paint.measureText(title) <= maxWidth) {
      return title
    }
    val length = paint.breakText(title, true, maxWidth - paint.measureText(ELLIPSIS), null)
    // Try to end on a word break.
    val space = title.lastIndexOf(' ', length)
    return (if (space > 0) title.substring(0, space) else title.substring(0, length)) + ELLIPSIS
  }

  private fun circleBounds(radius: Float): RectF =
    centeredBounds(extremeRadius, extremeRadius, radius)

  private fun dp(value: Float): Float = value * resources.displayMetrics.density

  companion object {
    const val NO_ITEM = -1

    private const val INNER_RADIUS_DP = 48f
    private const val OUTER_RADIUS_DP = 56f
    private const val EXTREME_RADIUS_DP = 160f

    /** How much of the middle still cancels once an item is focused. */
    private const val KEEP_ITEM_MIDDLE_FRACTION = 0.6f

    private const val ELLIPSIS = "…"

    private val OUTER_FILL_COLOR = Color.parseColor("#DD333333")
    private val TEXT_COLOR = Color.parseColor("#FFEEEEEE")
    private val CENTER_FILL_COLOR = Color.parseColor("#DD333333")
    private val GRADIENT_INNER_COLOR = Color.parseColor("#00000000")
    private val GRADIENT_OUTER_COLOR = Color.parseColor("#CC000000")
    private val SELECTION_COLOR = Color.parseColor("#FF1A73E8")
    private val SELECTION_TEXT_COLOR = Color.parseColor("#FFFFFFFF")
    private val SELECTION_SHADOW_COLOR = Color.parseColor("#99FFFFFF")
    private val SUBMENU_OVERLAY_COLOR = Color.parseColor("#3300FF00")
    private val TEXT_SHADOW_COLOR = Color.parseColor("#AA000000")

    private fun centeredBounds(x: Float, y: Float, radius: Float) =
      RectF(x - radius, y - radius, x + radius, y + radius)

    private fun distSq(p: PointF, x: Float, y: Float): Float {
      val dx = x - p.x
      val dy = y - p.y
      return dx * dx + dy * dy
    }

    private fun arcLength(angle: Float, radius: Float): Float =
      (2f * Math.PI.toFloat() * radius) * (angle / 360f)
  }
}
