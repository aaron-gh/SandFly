/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.brailleime.input;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.common.BrailleUtils;
import com.google.android.accessibility.braille.common.Constants.BrailleType;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.brailleime.BrailleIme.OrientationSensitive;
import com.google.android.accessibility.brailleime.BrailleInputOptions;
import com.google.android.accessibility.brailleime.HeldOrientationTracker;
import com.google.android.accessibility.brailleime.OrientationMonitor;
import com.google.android.accessibility.brailleime.R;
import com.google.android.accessibility.brailleime.Utils;
import com.google.android.accessibility.brailleime.input.BrailleInputPlane.CustomOnGestureListener;
import com.google.android.accessibility.brailleime.input.BrailleInputPlane.DotTarget;
import com.google.android.accessibility.brailleime.input.MultitouchHandler.HoldRecognizer;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;
import java.util.Optional;

/**
 * View that displays braille dots and handles braille input and gestures. Itself doesn't
 * automatically save dot regions. Please call {@link #savePoints} to save them.
 *
 * <p>Important signals, such as the commission of input, are fed through the {@link Callback},
 * which is a required parameter for the constructor.
 *
 * <p>We do not provide a constructor with {@link android.util.AttributeSet} parameter because we
 * have no need for it and we have a {@link Callback} that we need passed in during construction.
 */
@SuppressWarnings("ViewConstructor")
public class BrailleInputView extends View
    implements OrientationSensitive, OnAttachStateChangeListener {

  /** A callback for receiving signals from BrailleInputView. */
  public interface Callback {
    /** Signals that invalid gesture has been produced. Returns true if the action is consumed. */
    default boolean onInvalidGesture() {
      return false;
    }

    /**
     * Signals that {@link Swipe} input has been produced. Returns true if the action is consumed.
     */
    @CanIgnoreReturnValue
    boolean onSwipeProduced(Swipe swipe);

    /**
     * Signals that hold and dot swipe input has been produced. Returns true if the action is
     * consumed.
     */
    @CanIgnoreReturnValue
    boolean onDotHoldAndDotSwipe(DotHoldSwipe dotHoldSwipe);

    /**
     * Allows the client to state which potential hold events it cares about. If the client returns
     * true, the BrailleInputView will invoke onHoldProduced, and then it (along with the input
     * chain with which it interfaces) will halt subsequent callbacks until the user releases (lifts
     * up) the press.
     */
    @CanIgnoreReturnValue
    default boolean isCalibrationHoldRecognized(
        boolean inTwoStepCalibration, int pointersHeldCount) {
      return false;
    }

    /**
     * Signals that the side of the charging port in tabletop mode was decided afresh, so turns of
     * the device on the table count from here.
     */
    default void onTabletopPortSideDecided() {}

    /**
     * Whether the device has been held upright since the keyboard last switched to screen-away
     * mode, so that typing now is screen-away typing rather than typing on a device tilted in the
     * hands.
     */
    default boolean wasHeldUprightInScreenAway() {
      return true;
    }

    /** Signals that hold has been produced. Returns true if the action is consumed. */
    @CanIgnoreReturnValue
    boolean onHoldProduced(int pointersHeldCount);

    /**
     * Signals that {@link BrailleCharacter} input has been produced.
     *
     * @return the client's notion of the latest update to the print text so that this View can
     *     render it (for low-vision users).
     */
    default String onBrailleProduced(BrailleCharacter brailleCharacter) {
      return "";
    }

    /** Signals that calibration has been produced. Returns true if the action is consumed. */
    @CanIgnoreReturnValue
    default boolean onCalibration(CalibrationTriggeredType type, FingersPattern hand) {
      return false;
    }

    /** Signals that calibration has failed. */
    default void onCalibrationFailed(CalibrationTriggeredType calibration) {}

    /** Signals that two step calibration needs retry. */
    default void onTwoStepCalibrationRetry(boolean isFirstStep) {}
  }

  /** Indicates which finger pattern. */
  public enum FingersPattern {
    NO_FINGERS,
    SIX_FINGERS,
    SEVEN_FINGERS,
    EIGHT_FINGERS,
    FIVE_FINGERS,
    FIRST_THREE_FINGERS,
    REMAINING_THREE_FINGERS,
    FIRST_FOUR_FINGERS,
    REMAINING_FOUR_FINGERS,
  }

  /** Calibration triggered types. */
  public enum CalibrationTriggeredType {
    FIVE_FINGERS,
    SIX_FINGERS,
    SEVEN_FINGERS,
    EIGHT_FINGERS,
    MANUAL
  }

  private static final String TAG = "BrailleInputView";
  private static final boolean DRAW_DEBUG_BACKGROUND = false; // Leave as false in version control.

  private final Callback callback;
  private final InputViewCaption inputViewCaption;
  private final BrailleInputPlane inputPlane;
  private CaptionText captionText;

  /** The size the dots are laid out in, which is turned from the screen's on a tablet. */
  private Size screenSizeInPixels;

  /** The orientation the dots are laid out in, which is turned from the screen's on a tablet. */
  private int orientation;

  /** The screen's size as displayed, before any turn. */
  private Size screenSizeAsDisplayed;

  /** The screen's orientation as displayed, before any turn. */
  private int orientationAsDisplayed;

  private boolean tabletopMode;

  /**
   * On a tablet, how many quarter turns clockwise the dots are drawn from the screen as displayed,
   * so that they face the user as if auto-rotate had turned the screen. The screen does not turn
   * when auto-rotate is off, nor upside down when it is on.
   */
  private int quarterTurns;

  /**
   * On a tablet in tabletop mode, the screen rotation that faces the user, decided when the tablet
   * was laid flat, as a {@link android.view.Surface} rotation.
   */
  private int tabletopRotation;

  /**
   * Whether the charging port should be on the other side from where the layout expects it. Then
   * touches and drawing are turned 180 degrees so the dots still fit the hands.
   */
  private boolean turnedAround;

  /** On a phone, where the charging port is in tabletop mode. */
  private final PhoneTabletopSide phoneTabletop = new PhoneTabletopSide();

  /** When a tablet's tabletop rotation was last decided, in uptime milliseconds, or -1. */
  private long tabletopSideDecidedAtMs = -1;
  private AutoPerformer autoPerformer;
  private BrailleInputOptions options;
  private boolean touchInteracting;
  private CalibrationTriggeredType calibrationType;

  /**
   * Construct a BrailleInputView.
   *
   * <p>We do not provide a constructor with {@link android.util.AttributeSet} because we have no
   * need for it and we have a Callback that we need passed in during construction.
   */
  public BrailleInputView(
      Context context,
      Callback callback,
      Size screenSizeInPixels,
      BrailleInputOptions options,
      boolean tabletopMode) {
    super(context);
    this.callback = callback;
    this.screenSizeInPixels = screenSizeInPixels;
    this.orientation = getResources().getConfiguration().orientation;
    this.screenSizeAsDisplayed = screenSizeInPixels;
    this.orientationAsDisplayed = orientation;
    this.options = options;
    this.inputPlane = getInputPlane(context);
    this.inputPlane.setTableTopMode(tabletopMode);
    this.tabletopMode = tabletopMode;
    if (tabletopMode) {
      decideTabletopPortSideIfNeeded(/* fromScreenAway= */ false);
      updateTurnedAround();
    }
    setBackgroundColor(getResources().getColor(R.color.input_plane_background));
    this.inputViewCaption = new InputViewCaption(context.getString(R.string.input_view_caption));
    addOnAttachStateChangeListener(this);
  }

  private BrailleInputPlane getInputPlane(Context context) {
    HoldRecognizer holdRecognizer =
        new HoldRecognizer() {
          @Override
          public boolean isCalibrationHoldRecognized(int pointersHeldCount) {
            return callback.isCalibrationHoldRecognized(
                inputPlane.inTwoStepCalibration(), pointersHeldCount);
          }

          @Override
          public boolean isHoldRecognized(int pointersHeldCount) {
            return !inputPlane.inTwoStepCalibration() && pointersHeldCount <= 3;
          }
        };
    return BrailleUtils.isPhoneSizedDevice(context.getResources())
        ? new BrailleInputPlanePhone(
            context,
            screenSizeInPixels,
            holdRecognizer,
            orientation,
            options,
            customOnGestureListener)
        : new BrailleInputPlaneTablet(
            context,
            screenSizeInPixels,
            holdRecognizer,
            orientation,
            options,
            customOnGestureListener);
  }

  @Override
  public void onOrientationChanged(int orientation, Size screenSize) {
    this.orientation = orientation;
    this.screenSizeInPixels = screenSize;
    this.orientationAsDisplayed = orientation;
    this.screenSizeAsDisplayed = screenSize;
    this.inputPlane.setOrientation(orientation, screenSizeInPixels);
    if (!isPhone()) {
      // The screen turned, so the dots turn by a different amount to keep facing the user.
      quarterTurns = 0;
      updateQuarterTurns();
    }
    invalidate();
    requestLayout();
  }

  @Override
  public void onViewAttachedToWindow(View view) {}

  @Override
  public void onViewDetachedFromWindow(View view) {
    if (inTwoStepCalibration()) {
      callback.onCalibrationFailed(calibrationType);
    }
    removeOnAttachStateChangeListener(this);
  }

  public void setOptions(BrailleInputOptions options) {
    this.options = options;
    inputPlane.setOptions(options);
    invalidate();
    requestLayout();
  }

  /** Sets keyboard to table layout. */
  public void setTabletopMode(boolean enabled) {
    if (tabletopMode != enabled) {
      inputPlane.setTableTopMode(enabled);
      tabletopMode = enabled;
      if (enabled) {
        decideTabletopPortSideIfNeeded(/* fromScreenAway= */ true);
        updateTurnedAround();
      }
      invalidate();
      requestLayout();
    }
  }

  /** Stores points positions in the SharedPreference. */
  public void savePoints() {
    inputPlane.storeLayoutPoints();
  }

  /**
   * Sets the accumulation mode.
   *
   * <p>An accumulation mode set to true means that all press-and-releases contribute to a potential
   * commission even if the release occurred far before the release of the final touch point.
   */
  public void setAccumulationMode(boolean accumulationMode) {
    inputPlane.setAccumulationMode(accumulationMode);
  }

  /** Gets the accumulation mode. */
  public boolean isAccumulationMode() {
    return inputPlane.isAccumulationMode();
  }

  /** Returns whether in the process of two step calibration. */
  public boolean inTwoStepCalibration() {
    return inputPlane.inTwoStepCalibration();
  }

  public List<DotTarget> getDotTargets() {
    return inputPlane.getDotTargets();
  }

  /** Gets braille input view dot count. */
  public int getBrailleDotCount() {
    return options.brailleType().getDotCount();
  }

  /** Boolean value for whether a finger is currently touching the braille input view. */
  public boolean isTouchInteracting() {
    return touchInteracting;
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (changed) {
      reduceSystemGestureArea();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (DRAW_DEBUG_BACKGROUND) {
      drawDebugBackground(canvas);
    }
    canvas.save();
    canvas.concat(layoutToScreen());
    inputPlane.onDraw(canvas);
    if (captionText != null) {
      captionText.onDraw(canvas);
    }
    inputViewCaption.onDraw(canvas);
    canvas.restore();
  }

  private final CustomOnGestureListener customOnGestureListener =
      new CustomOnGestureListener() {
        @Override
        public boolean detect(Optional<BrailleInputPlaneResult> resultOptional) {
          if (resultOptional.isPresent()) {
            if (shouldPerformCalibrationAnimation(
                resultOptional.get().type, resultOptional.get().pointersHeldCount)) {
              inputPlane.createAnimator(BrailleInputView.this);
            }
            boolean result = processResult(resultOptional.get());
            invalidate();
            return result;
          }
          return false;
        }

        @Override
        public void onTwoStepCalibrationFailed() {
          callback.onCalibrationFailed(calibrationType);
          invalidate();
        }

        @Override
        public void onTwoStepCalibrationRetry(boolean isFirstStep) {
          callback.onTwoStepCalibrationRetry(isFirstStep);
        }
      };

  @Override
  @SuppressWarnings("ClickableViewAccessibility")
  public boolean onTouchEvent(MotionEvent event) {
    updateTouchAction(event);
    // Decide at the start of each gesture, so a gesture is never turned partway through.
    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
      updateTurnedAround();
      if (isPhone() && tabletopMode) {
        phoneTabletop.onTabletopTouch();
      } else if (isPhone()) {
        phoneTabletop.onScreenAwayTouch(
            layoutExpectsPortOnRight() != turnedAround, callback.wasHeldUprightInScreenAway());
      }
    }
    boolean result;
    Matrix layoutToScreen = layoutToScreen();
    if (layoutToScreen.isIdentity()) {
      result = inputPlane.onTouchEvent(event);
    } else {
      Matrix screenToLayout = new Matrix();
      layoutToScreen.invert(screenToLayout);
      MotionEvent turned = MotionEvent.obtain(event);
      turned.transform(screenToLayout);
      result = inputPlane.onTouchEvent(turned);
      turned.recycle();
    }
    invalidate();
    return result;
  }

  /**
   * Turns the layout around when the charging port should be on the other side of the user from
   * where the layout expects it. In screen-away mode, the side comes from the orientation lock, or
   * from how the phone is held. In tabletop mode, it is the side decided when the phone was laid
   * flat: see {@link PhoneTabletopSide}. A tablet is turned by quarter turns instead: see {@link
   * #updateQuarterTurns}.
   */
  private void updateTurnedAround() {
    if (!isPhone()) {
      turnedAround = false;
      updateQuarterTurns();
      return;
    }
    int lock = readOrientationLock();
    if (tabletopMode) {
      boolean layoutExpectsPortOnRight =
          DotsOrientation.tabletopLayoutExpectsPortOnRight(isPortrait(), displayRotation());
      boolean portOnRight =
          lock != DotsOrientation.UNLOCKED
              ? DotsOrientation.phoneLockPortOnRight(lock)
              : phoneTabletop.getPortOnRight();
      turnedAround = portOnRight != layoutExpectsPortOnRight;
      return;
    }
    boolean layoutExpectsPortOnRight = layoutExpectsPortOnRight();
    Boolean portOnRight =
        lock != DotsOrientation.UNLOCKED
            ? (Boolean) DotsOrientation.phoneLockPortOnRight(lock)
            : heldPortOnRight();
    if (portOnRight != null) {
      turnedAround = portOnRight != layoutExpectsPortOnRight;
    }
  }

  /**
   * Decides the tabletop side again only when something new shows it: see {@link
   * DotsOrientation#shouldDecideTabletopAgain}.
   *
   * @param fromScreenAway whether the keyboard was in screen-away mode before it was laid flat
   */
  private void decideTabletopPortSideIfNeeded(boolean fromScreenAway) {
    if (isPhone()) {
      if (phoneTabletop.decideIfNeeded(
          HeldOrientationTracker.getLastHeld(),
          HeldOrientationTracker.getLastHeldSeenMs(),
          isPortrait(),
          displayRotation(),
          SystemClock.uptimeMillis())) {
        callback.onTabletopPortSideDecided();
      }
      return;
    }
    if (!DotsOrientation.shouldDecideTabletopAgain(
        tabletopSideDecidedAtMs,
        /* typedInScreenAway= */ false,
        HeldOrientationTracker.getLastHeldSeenMs())) {
      return;
    }
    // Laying a tablet flat keeps the rotation it was last held up with, as auto-rotate does,
    // unless it was held from behind: see DotsOrientation#tabletTabletopRotation.
    int held = HeldOrientationTracker.getLastHeldRotation();
    if (held < 0) {
      tabletopRotation = displayRotation();
    } else if (Utils.isDeviceDefaultPortrait(getContext())) {
      tabletopRotation =
          DotsOrientation.tabletTabletopRotation(
              held,
              HeldOrientationTracker.getLastHeld(),
              fromScreenAway,
              BrailleUserPreferences.readTabletHeldUpFacesAway(getContext()));
    } else {
      tabletopRotation = held;
    }
    tabletopSideDecidedAtMs = SystemClock.uptimeMillis();
    callback.onTabletopPortSideDecided();
  }

  /**
   * On a tablet standing up facing the user, turns the tabletop dots to the rotation it is held up
   * with now, as auto-rotate would. Returns whether they turned.
   */
  public boolean faceHeldRotation() {
    int held = HeldOrientationTracker.getLastHeldRotation();
    if (isPhone() || held < 0) {
      return false;
    }
    boolean turned = held != tabletopRotation;
    tabletopRotation = held;
    tabletopSideDecidedAtMs = SystemClock.uptimeMillis();
    callback.onTabletopPortSideDecided();
    if (turned && tabletopMode) {
      updateTurnedAround();
      invalidate();
    }
    return turned;
  }

  /** The screen rotation, which works before the view is attached to a window. */
  private int displayRotation() {
    return getDisplay() != null
        ? getDisplay().getRotation()
        : Utils.getDisplayRotationDegrees(getContext());
  }

  private boolean isPortrait() {
    return orientation == Configuration.ORIENTATION_PORTRAIT;
  }

  /**
   * The device was turned while lying flat, by this many quarter turns clockwise seen from above. A
   * phone's dots only turn once its charging port reaches the other side. Returns where the
   * charging port is now in tabletop mode.
   */
  @Nullable
  public PortPosition turnTabletop(int quarters) {
    if (isPhone()) {
      phoneTabletop.turn(quarters, tabletopMode);
    } else {
      tabletopRotation = DotsOrientation.turnRotation(tabletopRotation, quarters);
    }
    if (tabletopMode) {
      updateTurnedAround();
      invalidate();
    }
    return getTabletopPortPosition();
  }

  /**
   * Where the charging port is in tabletop mode, as decided when the device was laid flat. Null on
   * a tablet whose natural orientation is landscape, where the port could be on any edge.
   */
  @Nullable
  public PortPosition getTabletopPortPosition() {
    int lock =
        BrailleUserPreferences.readOrientationLock(getContext(), !isPhone(), /* tabletop= */ true);
    if (isPhone()) {
      return lock != DotsOrientation.UNLOCKED
          ? DotsOrientation.phonePortPosition(DotsOrientation.phoneLockPortOnRight(lock))
          : phoneTabletop.getPort();
    }
    return tabletPortPosition(
        lock != DotsOrientation.UNLOCKED ? lock : tabletopRotation, /* tabletop= */ true);
  }

  /**
   * Where the charging port is on a tablet facing the user at this screen rotation, or null on a
   * tablet whose natural orientation is landscape, where the port could be on any edge.
   */
  @Nullable
  private PortPosition tabletPortPosition(int rotation, boolean tabletop) {
    return Utils.isDeviceDefaultPortrait(getContext())
        ? DotsOrientation.tabletPortPosition(rotation, tabletop)
        : null;
  }

  /**
   * Locks the dots facing the way they are now in the current mode, or unlocks them to follow how
   * the device is held again. Each screen size has its own locks, so a foldable can be locked one
   * way folded and another way unfolded. Returns whether it is now locked.
   */
  public boolean toggleOrientationLock() {
    boolean locked = isOrientationLocked();
    int value;
    if (locked) {
      value = DotsOrientation.UNLOCKED;
    } else if (!isPhone()) {
      value = Math.floorMod(displayRotation() + quarterTurns, 4);
    } else if (tabletopMode) {
      value = DotsOrientation.phoneLock(phoneTabletop.getPortOnRight());
    } else {
      value = DotsOrientation.phoneLock(layoutExpectsPortOnRight() != turnedAround);
    }
    BrailleUserPreferences.writeOrientationLock(getContext(), !isPhone(), tabletopMode, value);
    updateTurnedAround();
    invalidate();
    return !locked;
  }

  /** Whether the dots are locked facing one way in the current mode. */
  public boolean isOrientationLocked() {
    return readOrientationLock() != DotsOrientation.UNLOCKED;
  }

  /**
   * Where the charging port is locked in the current mode, or null when it is not locked or that
   * cannot be said.
   */
  @Nullable
  public PortPosition getLockedPortPosition() {
    int lock = readOrientationLock();
    if (lock == DotsOrientation.UNLOCKED) {
      return null;
    }
    if (isPhone()) {
      return DotsOrientation.phonePortPosition(DotsOrientation.phoneLockPortOnRight(lock));
    }
    return tabletPortPosition(lock, tabletopMode);
  }

  /**
   * The orientation lock for this screen size and the current mode, or {@link
   * DotsOrientation#UNLOCKED}. On a phone it is the side of the charging port, and on a tablet the
   * screen rotation that faces the user.
   */
  private int readOrientationLock() {
    return BrailleUserPreferences.readOrientationLock(getContext(), !isPhone(), tabletopMode);
  }

  private boolean isPhone() {
    return BrailleUtils.isPhoneSizedDevice(getResources());
  }

  /**
   * On a tablet, turns the dots to face the user as if auto-rotate had turned the screen: to the
   * orientation lock, to the rotation decided when it was laid flat in tabletop mode, or else to how
   * it is held up.
   */
  private void updateQuarterTurns() {
    int wanted;
    int lock = readOrientationLock();
    if (lock != DotsOrientation.UNLOCKED) {
      wanted = lock;
    } else if (tabletopMode) {
      wanted = tabletopRotation;
    } else {
      int held = HeldOrientationTracker.getLastHeldRotation();
      wanted = held >= 0 ? held : displayRotation();
    }
    int turns = DotsOrientation.quarterTurns(wanted, displayRotation());
    if (turns == quarterTurns) {
      return;
    }
    quarterTurns = turns;
    boolean sideways = turns % 2 == 1;
    screenSizeInPixels =
        sideways
            ? new Size(screenSizeAsDisplayed.getHeight(), screenSizeAsDisplayed.getWidth())
            : screenSizeAsDisplayed;
    orientation =
        !sideways
            ? orientationAsDisplayed
            : orientationAsDisplayed == Configuration.ORIENTATION_PORTRAIT
                ? Configuration.ORIENTATION_LANDSCAPE
                : Configuration.ORIENTATION_PORTRAIT;
    inputPlane.setOrientation(orientation, screenSizeInPixels);
    invalidate();
  }

  /** Maps the layout of the dots onto the screen, turning it to face the user. */
  private Matrix layoutToScreen() {
    Matrix matrix = new Matrix();
    matrix.setValues(
        DotsOrientation.layoutToScreen(turnedAround ? 2 : quarterTurns, getWidth(), getHeight()));
    return matrix;
  }

  /** Which side a phone's layout expects the charging port on in screen-away mode. */
  private boolean layoutExpectsPortOnRight() {
    return DotsOrientation.screenAwayLayoutExpectsPortOnRight(isPortrait(), displayRotation());
  }

  /**
   * Which side the charging port is on as the phone is held, or null when it is not clearly in
   * landscape. With the screen in landscape, it has already turned to match.
   */
  @Nullable
  private Boolean heldPortOnRight() {
    if (!isPortrait()) {
      return layoutExpectsPortOnRight();
    }
    return DotsOrientation.heldPortOnRight(OrientationMonitor.getInstance().getCurrentOrientation());
  }

  /** Calibrates dots positions. */
  public void calibrateByTwoSteps() {
    inputPlane.calibrateByTwoSteps();
    calibrationType = CalibrationTriggeredType.MANUAL;
    callback.onCalibration(CalibrationTriggeredType.MANUAL, FingersPattern.NO_FINGERS);
    invalidate();
  }

  private void updateTouchAction(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN -> touchInteracting = true;
      case MotionEvent.ACTION_UP -> touchInteracting = false;
      default -> {}
    }
  }

  private boolean shouldPerformCalibrationAnimation(int type, int heldCount) {
    if (type != MultitouchResult.TYPE_CALIBRATION_HOLD) {
      return false;
    }
    if (options.brailleType() == BrailleType.EIGHT_DOT) {
      return heldCount == BrailleType.EIGHT_DOT.getDotCount()
          || heldCount == BrailleType.EIGHT_DOT.getDotCount() / 2;
    } else {
      return heldCount == BrailleType.SIX_DOT.getDotCount()
          || heldCount == BrailleType.SIX_DOT.getDotCount() / 2;
    }
  }

  @CanIgnoreReturnValue
  private boolean processResult(BrailleInputPlaneResult result) {
    if (result.type == MultitouchResult.TYPE_INVALID) {
      return callback.onInvalidGesture();
    } else if (result.type == MultitouchResult.TYPE_TAP) {
      String newAddition = callback.onBrailleProduced(result.releasedBrailleCharacter);
      if (newAddition != null) {
        if (captionText != null) {
          captionText.animator.cancel();
        }
        captionText = new CaptionText(newAddition);
        captionText.animator.start();
      }
    } else if (result.type == MultitouchResult.TYPE_SWIPE) {
      boolean processed = callback.onSwipeProduced(result.swipe);
      if (Utils.isDebugBuild()) {
        if (result.swipe.getTouchCount() == 5) {
          if (autoPerformer == null) {
            this.autoPerformer = new AutoPerformer(getContext(), new AutoPerformerCallback());
            this.autoPerformer.start();
          }
        }
      }
      return processed;
    } else if (result.type == MultitouchResult.TYPE_CALIBRATION_HOLD) {
      if (result.pointersHeldCount == 5) {
        calibrationType = CalibrationTriggeredType.FIVE_FINGERS;
        return callback.onCalibration(calibrationType, FingersPattern.FIVE_FINGERS);
      } else if (result.pointersHeldCount == 6) {
        calibrationType = CalibrationTriggeredType.SIX_FINGERS;
        return callback.onCalibration(calibrationType, FingersPattern.SIX_FINGERS);
      } else if (result.pointersHeldCount == 7) {
        calibrationType = CalibrationTriggeredType.SEVEN_FINGERS;
        return callback.onCalibration(calibrationType, FingersPattern.SEVEN_FINGERS);
      } else if (result.pointersHeldCount == 8) {
        calibrationType = CalibrationTriggeredType.EIGHT_FINGERS;
        return callback.onCalibration(calibrationType, FingersPattern.EIGHT_FINGERS);
      } else if (result.pointersHeldCount == 3) {
        return callback.onCalibration(
            calibrationType,
            result.isLeft
                ? FingersPattern.FIRST_THREE_FINGERS
                : FingersPattern.REMAINING_THREE_FINGERS);
      } else if (result.pointersHeldCount == 4) {
        return callback.onCalibration(
            calibrationType,
            result.isLeft
                ? FingersPattern.FIRST_FOUR_FINGERS
                : FingersPattern.REMAINING_FOUR_FINGERS);
      } else {
        callback.onCalibrationFailed(calibrationType);
      }
    } else if (result.type == MultitouchResult.TYPE_HOLD) {
      return callback.onHoldProduced(result.pointersHeldCount);
    } else if (result.type == MultitouchResult.TYPE_HOLD_AND_SWIPE) {
      return callback.onDotHoldAndDotSwipe(
          new DotHoldSwipe(result.swipe, result.heldBrailleCharacter));
    }
    return false;
  }

  private void drawDebugBackground(Canvas canvas) {
    // Drawing the debug background helps expose problems with coordinates and screensize.
    Paint paint = new Paint();
    paint.setColor(getResources().getColor(R.color.input_plane_debug_background));
    int backgroundInset =
        getResources().getDimensionPixelSize(R.dimen.input_plane_debug_background_inset);
    RectF rect =
        new RectF(
            backgroundInset,
            backgroundInset,
            screenSizeInPixels.getWidth() - backgroundInset,
            screenSizeInPixels.getHeight() - backgroundInset);
    canvas.drawRect(rect, paint);
  }

  private void reduceSystemGestureArea() {
    // Reduce the possibility of triggering system-level gestures; note that the exclusion zone
    // is restricted to 200dp, and is counted from the bottom edge upward.
    Rect boundingBox = new Rect();
    boundingBox.set(getLeft(), getTop(), getRight(), getBottom());
    ViewCompat.setSystemGestureExclusionRects(this, ImmutableList.of(boundingBox));
  }

  /** Draws the print translation of the most recently inputted text (for low-vision users). */
  private class CaptionText {
    private final String text;
    private final ValueAnimator animator;
    private final Paint textPaint;
    private static final int ANIMATION_DURATION_MS = 400;

    private CaptionText(String text) {
      this.text = text;
      Resources resources = getResources();
      animator = ValueAnimator.ofFloat(1f, 0f);
      animator.setDuration(ANIMATION_DURATION_MS);
      animator.addListener(
          new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
              BrailleInputView.this.captionText = null;
            }
          });
      animator.addUpdateListener(valueAnimator -> BrailleInputView.this.invalidate());

      textPaint = new Paint();
      textPaint.setColor(resources.getColor(R.color.input_plane_most_recent_animation));
      textPaint.setTextAlign(Paint.Align.CENTER);
      textPaint.setTextSize(
          resources.getDimensionPixelSize(R.dimen.input_view_most_recent_text_size));
      textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    private void onDraw(Canvas canvas) {
      int alpha = (int) (((Float) animator.getAnimatedValue()) * 255);
      textPaint.setAlpha(alpha);

      Rect textBounds = new Rect();
      textPaint.getTextBounds(text, 0, text.length(), textBounds);
      PointF textPoint = inputPlane.getCaptionCenterPoint(screenSizeInPixels);
      canvas.save();
      canvas.rotate(inputPlane.getRotateDegree(), textPoint.x, textPoint.y);
      canvas.drawText(text, textPoint.x, textPoint.y, textPaint);
      canvas.restore();
    }
  }

  /** Draws the caption text on the bottom-center of the screen. */
  private class InputViewCaption {
    private final String text;
    private final Paint textPaint;
    private final int captionBottomMarginInPixels;

    private InputViewCaption(String text) {
      this.text = text;
      Resources resources = getResources();
      textPaint = new Paint();
      textPaint.setColor(resources.getColor(R.color.input_plane_caption));
      textPaint.setTextAlign(Paint.Align.CENTER);
      textPaint.setTextSize(resources.getDimensionPixelSize(R.dimen.input_view_caption_text_size));
      textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
      textPaint.setTypeface(
          Typeface.create(getContext().getString(R.string.accessibility_font), Typeface.NORMAL));
      captionBottomMarginInPixels =
          resources.getDimensionPixelOffset(R.dimen.input_view_caption_bottom_margin);
    }

    private void onDraw(Canvas canvas) {
      Rect textBounds = new Rect();
      canvas.save();
      textPaint.getTextBounds(text, 0, text.length(), textBounds);
      Size screenSize = inputPlane.getInputViewCaptionScreenSize(screenSizeInPixels);
      canvas.rotate(inputPlane.getRotateDegree());
      int[] dxy = inputPlane.getInputViewCaptionTranslate(screenSize);
      canvas.translate(/* dx= */ dxy[0], /* dy= */ dxy[1]);
      drawText(canvas, screenSize);
      canvas.restore();
    }

    private void drawText(Canvas canvas, Size screenSize) {
      float textX = screenSize.getWidth() / 2.0f;
      float textY = screenSize.getHeight() - captionBottomMarginInPixels;
      canvas.drawText(text, textX, textY, textPaint);
    }
  }

  private class AutoPerformerCallback implements AutoPerformer.Callback {

    @Override
    public void onPerform(BrailleInputPlaneResult bipr) {
      processResult(bipr);
    }

    @Override
    public BrailleInputPlaneResult createSwipe(Swipe swipe) {
      return inputPlane.createSwipe(swipe);
    }

    @Override
    public void onFinish() {
      autoPerformer = null;
    }
  }
}
