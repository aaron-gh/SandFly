/*
 * Copyright (C) 2021s The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.utils.gestures;

import static android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SWIPE_DOWN;
import static android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SWIPE_LEFT;
import static android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SWIPE_RIGHT;
import static android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SWIPE_UP;
import static com.google.android.accessibility.utils.gestures.TwoFingerSecondFingerMultiTap.ROTATE_DIRECTION_BACKWARD;
import static com.google.android.accessibility.utils.gestures.TwoFingerSecondFingerMultiTap.ROTATE_DIRECTION_FORWARD;

import android.accessibilityservice.AccessibilityGestureEvent;
import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import androidx.annotation.RequiresApi;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.gestures.GestureMatcher.AnalyticsEventLogger;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.List;

/**
 * This class coordinates a series of individual gesture matchers to serve as a unified gesture
 * detector. Gesture matchers are tied to a single gesture. It calls listener callback functions
 * when a gesture starts or completes.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class GestureManifold implements GestureMatcher.StateChangeListener {
  public static final int GESTURE_FAKED_SPLIT_TYPING = -3;
  public static final int GESTURE_TAP_HOLD_AND_2ND_FINGER_FORWARD_DOUBLE_TAP = -4;
  public static final int GESTURE_TAP_HOLD_AND_2ND_FINGER_BACKWARD_DOUBLE_TAP = -5;
  public static final int GESTURE_TOUCH_EXPLORE = -6;
  public static final int GESTURE_TAP_UP_TOUCH_EXPLORE = -7;
  public static final int GESTURE_FAKED_SPLIT_TYPING_AND_HOLD = -8;
  // Two fingers turning around each other. Reported once per step of rotation, while the fingers
  // are still down, by TalkBack's TouchInteractionMonitor rather than by a gesture matcher.
  public static final int GESTURE_2_FINGER_ROTATE_CLOCKWISE = -9;
  public static final int GESTURE_2_FINGER_ROTATE_COUNTERCLOCKWISE = -10;
  // Android has no gestures beyond triple taps, so Backtalk's own detection adds this one.
  public static final int GESTURE_3_FINGER_QUADRUPLE_TAP = -11;
  // One finger sliding in from an edge of the screen, as on iOS, and the two-finger scrub. Reported
  // by TalkBack's TouchInteractionMonitor rather than by a gesture matcher.
  public static final int GESTURE_EDGE_SWIPE_UP_FROM_BOTTOM = -12;
  public static final int GESTURE_EDGE_LONG_SWIPE_UP_FROM_BOTTOM = -13;
  public static final int GESTURE_EDGE_SWIPE_DOWN_FROM_TOP_LEFT = -14;
  public static final int GESTURE_EDGE_SWIPE_DOWN_FROM_TOP_RIGHT = -15;
  public static final int GESTURE_2_FINGER_SCRUB = -16;
  // Fingers moving toward their middle or away from it. Reported by TalkBack's
  // TouchInteractionMonitor rather than by a gesture matcher.
  public static final int GESTURE_2_FINGER_PINCH = -17;
  public static final int GESTURE_2_FINGER_SPREAD = -18;
  public static final int GESTURE_3_FINGER_PINCH = -19;
  public static final int GESTURE_3_FINGER_SPREAD = -20;
  public static final int GESTURE_4_FINGER_PINCH = -21;
  public static final int GESTURE_4_FINGER_SPREAD = -22;
  // More edge gestures: the second stage from the top edge, and both stages from each half of the
  // left and right edges.
  public static final int GESTURE_EDGE_LONG_SWIPE_DOWN_FROM_TOP_LEFT = -23;
  public static final int GESTURE_EDGE_LONG_SWIPE_DOWN_FROM_TOP_RIGHT = -24;
  public static final int GESTURE_EDGE_SWIPE_RIGHT_FROM_LEFT_TOP = -25;
  public static final int GESTURE_EDGE_LONG_SWIPE_RIGHT_FROM_LEFT_TOP = -26;
  public static final int GESTURE_EDGE_SWIPE_RIGHT_FROM_LEFT_BOTTOM = -27;
  public static final int GESTURE_EDGE_LONG_SWIPE_RIGHT_FROM_LEFT_BOTTOM = -28;
  public static final int GESTURE_EDGE_SWIPE_LEFT_FROM_RIGHT_TOP = -29;
  public static final int GESTURE_EDGE_LONG_SWIPE_LEFT_FROM_RIGHT_TOP = -30;
  public static final int GESTURE_EDGE_SWIPE_LEFT_FROM_RIGHT_BOTTOM = -31;
  public static final int GESTURE_EDGE_LONG_SWIPE_LEFT_FROM_RIGHT_BOTTOM = -32;
  // One-finger swipes whose finger rests at the end before lifting, which SandFly's own detection
  // reports in place of the swipe when they have an action.
  public static final int GESTURE_SWIPE_UP_AND_HOLD = -40;
  public static final int GESTURE_SWIPE_DOWN_AND_HOLD = -41;
  public static final int GESTURE_SWIPE_LEFT_AND_HOLD = -42;
  public static final int GESTURE_SWIPE_RIGHT_AND_HOLD = -43;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_AND_HOLD = -44;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_AND_HOLD = -45;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_AND_HOLD = -46;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_AND_HOLD = -47;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_AND_HOLD = -48;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_AND_HOLD = -49;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_AND_HOLD = -50;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_AND_HOLD = -51;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_AND_HOLD = -52;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_AND_HOLD = -53;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_AND_HOLD = -54;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_AND_HOLD = -55;
  // One finger sliding along an edge of the screen. Reported by TalkBack's TouchInteractionMonitor
  // rather than by a gesture matcher.
  public static final int GESTURE_EDGE_SWIPE_UP_ALONG_LEFT = -56;
  public static final int GESTURE_EDGE_SWIPE_DOWN_ALONG_LEFT = -57;
  public static final int GESTURE_EDGE_SWIPE_UP_ALONG_RIGHT = -58;
  public static final int GESTURE_EDGE_SWIPE_DOWN_ALONG_RIGHT = -59;
  public static final int GESTURE_EDGE_SWIPE_LEFT_ALONG_TOP = -60;
  public static final int GESTURE_EDGE_SWIPE_RIGHT_ALONG_TOP = -61;
  public static final int GESTURE_EDGE_SWIPE_LEFT_ALONG_BOTTOM = -62;
  public static final int GESTURE_EDGE_SWIPE_RIGHT_ALONG_BOTTOM = -63;
  // A four-finger tap on the top or bottom half of the screen. Reported by TalkBack's
  // TouchInteractionMonitor in place of the four-finger tap when one of them has an action.
  public static final int GESTURE_4_FINGER_SINGLE_TAP_TOP = -64;
  public static final int GESTURE_4_FINGER_SINGLE_TAP_BOTTOM = -65;
  // One-finger taps in a corner of the screen. Reported by TalkBack's TouchInteractionMonitor in
  // place of the taps when the corner gesture has an action.
  public static final int GESTURE_CORNER_TOP_LEFT_TAP = -66;
  public static final int GESTURE_CORNER_TOP_LEFT_DOUBLE_TAP = -67;
  public static final int GESTURE_CORNER_TOP_LEFT_TRIPLE_TAP = -68;
  public static final int GESTURE_CORNER_TOP_LEFT_DOUBLE_TAP_AND_HOLD = -69;
  public static final int GESTURE_CORNER_TOP_LEFT_TRIPLE_TAP_AND_HOLD = -70;
  public static final int GESTURE_CORNER_TOP_RIGHT_TAP = -71;
  public static final int GESTURE_CORNER_TOP_RIGHT_DOUBLE_TAP = -72;
  public static final int GESTURE_CORNER_TOP_RIGHT_TRIPLE_TAP = -73;
  public static final int GESTURE_CORNER_TOP_RIGHT_DOUBLE_TAP_AND_HOLD = -74;
  public static final int GESTURE_CORNER_TOP_RIGHT_TRIPLE_TAP_AND_HOLD = -75;
  public static final int GESTURE_CORNER_BOTTOM_LEFT_TAP = -76;
  public static final int GESTURE_CORNER_BOTTOM_LEFT_DOUBLE_TAP = -77;
  public static final int GESTURE_CORNER_BOTTOM_LEFT_TRIPLE_TAP = -78;
  public static final int GESTURE_CORNER_BOTTOM_LEFT_DOUBLE_TAP_AND_HOLD = -79;
  public static final int GESTURE_CORNER_BOTTOM_LEFT_TRIPLE_TAP_AND_HOLD = -80;
  public static final int GESTURE_CORNER_BOTTOM_RIGHT_TAP = -81;
  public static final int GESTURE_CORNER_BOTTOM_RIGHT_DOUBLE_TAP = -82;
  public static final int GESTURE_CORNER_BOTTOM_RIGHT_TRIPLE_TAP = -83;
  public static final int GESTURE_CORNER_BOTTOM_RIGHT_DOUBLE_TAP_AND_HOLD = -84;
  public static final int GESTURE_CORNER_BOTTOM_RIGHT_TRIPLE_TAP_AND_HOLD = -85;

  // Taps with two, three or four fingers followed by a swipe with as many fingers. Recognized by
  // SandFly's own detection only while they have an action.
  public static final int GESTURE_2_FINGER_SINGLE_TAP_SWIPE_UP = -86;
  public static final int GESTURE_2_FINGER_SINGLE_TAP_SWIPE_DOWN = -87;
  public static final int GESTURE_2_FINGER_SINGLE_TAP_SWIPE_LEFT = -88;
  public static final int GESTURE_2_FINGER_SINGLE_TAP_SWIPE_RIGHT = -89;
  public static final int GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_UP = -90;
  public static final int GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_DOWN = -91;
  public static final int GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_LEFT = -92;
  public static final int GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_RIGHT = -93;
  public static final int GESTURE_2_FINGER_TRIPLE_TAP_SWIPE_UP = -94;
  public static final int GESTURE_2_FINGER_TRIPLE_TAP_SWIPE_DOWN = -95;
  public static final int GESTURE_2_FINGER_TRIPLE_TAP_SWIPE_LEFT = -96;
  public static final int GESTURE_2_FINGER_TRIPLE_TAP_SWIPE_RIGHT = -97;
  public static final int GESTURE_3_FINGER_SINGLE_TAP_SWIPE_UP = -98;
  public static final int GESTURE_3_FINGER_SINGLE_TAP_SWIPE_DOWN = -99;
  public static final int GESTURE_3_FINGER_SINGLE_TAP_SWIPE_LEFT = -100;
  public static final int GESTURE_3_FINGER_SINGLE_TAP_SWIPE_RIGHT = -101;
  public static final int GESTURE_3_FINGER_DOUBLE_TAP_SWIPE_UP = -102;
  public static final int GESTURE_3_FINGER_DOUBLE_TAP_SWIPE_DOWN = -103;
  public static final int GESTURE_3_FINGER_DOUBLE_TAP_SWIPE_LEFT = -104;
  public static final int GESTURE_3_FINGER_DOUBLE_TAP_SWIPE_RIGHT = -105;
  public static final int GESTURE_3_FINGER_TRIPLE_TAP_SWIPE_UP = -106;
  public static final int GESTURE_3_FINGER_TRIPLE_TAP_SWIPE_DOWN = -107;
  public static final int GESTURE_3_FINGER_TRIPLE_TAP_SWIPE_LEFT = -108;
  public static final int GESTURE_3_FINGER_TRIPLE_TAP_SWIPE_RIGHT = -109;
  public static final int GESTURE_4_FINGER_SINGLE_TAP_SWIPE_UP = -110;
  public static final int GESTURE_4_FINGER_SINGLE_TAP_SWIPE_DOWN = -111;
  public static final int GESTURE_4_FINGER_SINGLE_TAP_SWIPE_LEFT = -112;
  public static final int GESTURE_4_FINGER_SINGLE_TAP_SWIPE_RIGHT = -113;
  public static final int GESTURE_4_FINGER_DOUBLE_TAP_SWIPE_UP = -114;
  public static final int GESTURE_4_FINGER_DOUBLE_TAP_SWIPE_DOWN = -115;
  public static final int GESTURE_4_FINGER_DOUBLE_TAP_SWIPE_LEFT = -116;
  public static final int GESTURE_4_FINGER_DOUBLE_TAP_SWIPE_RIGHT = -117;
  public static final int GESTURE_4_FINGER_TRIPLE_TAP_SWIPE_UP = -118;
  public static final int GESTURE_4_FINGER_TRIPLE_TAP_SWIPE_DOWN = -119;
  public static final int GESTURE_4_FINGER_TRIPLE_TAP_SWIPE_LEFT = -120;
  public static final int GESTURE_4_FINGER_TRIPLE_TAP_SWIPE_RIGHT = -121;

  // Two fingers swiping one way and then turning at a right angle, as the one-finger angle
  // gestures do. Recognized by SandFly's own detection while it handles two-finger swipes.
  public static final int GESTURE_2_FINGER_SWIPE_UP_AND_LEFT = -122;
  public static final int GESTURE_2_FINGER_SWIPE_UP_AND_RIGHT = -123;
  public static final int GESTURE_2_FINGER_SWIPE_DOWN_AND_LEFT = -124;
  public static final int GESTURE_2_FINGER_SWIPE_DOWN_AND_RIGHT = -125;
  public static final int GESTURE_2_FINGER_SWIPE_LEFT_AND_UP = -126;
  public static final int GESTURE_2_FINGER_SWIPE_LEFT_AND_DOWN = -127;
  public static final int GESTURE_2_FINGER_SWIPE_RIGHT_AND_UP = -128;
  public static final int GESTURE_2_FINGER_SWIPE_RIGHT_AND_DOWN = -129;

  // Taps with two fingers followed by a swipe with one finger. Recognized by SandFly's own
  // detection only while they have an action.
  public static final int GESTURE_2_FINGER_SINGLE_TAP_1_FINGER_SWIPE_UP = -130;
  public static final int GESTURE_2_FINGER_SINGLE_TAP_1_FINGER_SWIPE_DOWN = -131;
  public static final int GESTURE_2_FINGER_SINGLE_TAP_1_FINGER_SWIPE_LEFT = -132;
  public static final int GESTURE_2_FINGER_SINGLE_TAP_1_FINGER_SWIPE_RIGHT = -133;
  public static final int GESTURE_2_FINGER_DOUBLE_TAP_1_FINGER_SWIPE_UP = -134;
  public static final int GESTURE_2_FINGER_DOUBLE_TAP_1_FINGER_SWIPE_DOWN = -135;
  public static final int GESTURE_2_FINGER_DOUBLE_TAP_1_FINGER_SWIPE_LEFT = -136;
  public static final int GESTURE_2_FINGER_DOUBLE_TAP_1_FINGER_SWIPE_RIGHT = -137;
  public static final int GESTURE_2_FINGER_TRIPLE_TAP_1_FINGER_SWIPE_UP = -138;
  public static final int GESTURE_2_FINGER_TRIPLE_TAP_1_FINGER_SWIPE_DOWN = -139;
  public static final int GESTURE_2_FINGER_TRIPLE_TAP_1_FINGER_SWIPE_LEFT = -140;
  public static final int GESTURE_2_FINGER_TRIPLE_TAP_1_FINGER_SWIPE_RIGHT = -141;

  // One-finger swipes in three directions in one stroke, each turning from the one before, such
  // as right, up, then right. Recognized by SandFly's own detection only while they have an action.
  public static final int GESTURE_SWIPE_UP_AND_DOWN_AND_UP = -142;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_AND_LEFT = -143;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_AND_RIGHT = -144;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_AND_UP = -145;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_AND_DOWN = -146;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_AND_RIGHT = -147;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_AND_UP = -148;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_AND_DOWN = -149;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_AND_LEFT = -150;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_AND_DOWN = -151;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_AND_LEFT = -152;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_AND_RIGHT = -153;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_AND_UP = -154;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_AND_DOWN = -155;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_AND_RIGHT = -156;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_AND_UP = -157;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_AND_DOWN = -158;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_AND_LEFT = -159;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_AND_DOWN = -160;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_AND_LEFT = -161;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_AND_RIGHT = -162;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_AND_UP = -163;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_AND_LEFT = -164;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_AND_RIGHT = -165;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_AND_UP = -166;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_AND_DOWN = -167;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_AND_LEFT = -168;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_AND_DOWN = -169;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_AND_LEFT = -170;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_AND_RIGHT = -171;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_AND_UP = -172;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_AND_LEFT = -173;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_AND_RIGHT = -174;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_AND_UP = -175;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_AND_DOWN = -176;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_AND_RIGHT = -177;

  // Two one-finger swipes with the finger lifted between them, within the time set for them.
  // Reported by TalkBack's TouchInteractionMonitor in place of the two swipes while they have an
  // action.
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_UP = -178;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_DOWN = -179;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_LEFT = -180;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_RIGHT = -181;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_UP = -182;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_DOWN = -183;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_LEFT = -184;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_RIGHT = -185;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_UP = -186;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_DOWN = -187;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_LEFT = -188;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_RIGHT = -189;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP = -190;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_DOWN = -191;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_LEFT = -192;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT = -193;

  // A two-finger double tap with one finger in the top third of the screen and the other in the
  // bottom third. Reported by TalkBack's TouchInteractionMonitor in place of the two-finger double
  // tap while it has an action.
  public static final int GESTURE_2_FINGER_DOUBLE_TAP_TOP_AND_BOTTOM = -194;

  // Three and four taps with one finger. Android has no one-finger gestures beyond the double tap,
  // so SandFly's own detection adds these. Recognized only while they have an action.
  public static final int GESTURE_TRIPLE_TAP = -195;
  public static final int GESTURE_QUADRUPLE_TAP = -196;

  // Two-stroke swipes with a back-and-forth or angle swipe as either stroke, numbered down from
  // -1000, sixteen for each first stroke; see TwoStrokeSwipeIds.
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_UP_AND_DOWN = -1004;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_DOWN_AND_UP = -1005;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_LEFT_AND_RIGHT = -1006;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_RIGHT_AND_LEFT = -1007;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_UP_AND_LEFT = -1008;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_UP_AND_RIGHT = -1009;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_DOWN_AND_LEFT = -1010;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_DOWN_AND_RIGHT = -1011;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_LEFT_AND_UP = -1012;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_LEFT_AND_DOWN = -1013;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_RIGHT_AND_UP = -1014;
  public static final int GESTURE_SWIPE_UP_LIFT_SWIPE_RIGHT_AND_DOWN = -1015;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_UP_AND_DOWN = -1020;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_DOWN_AND_UP = -1021;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_LEFT_AND_RIGHT = -1022;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_RIGHT_AND_LEFT = -1023;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_UP_AND_LEFT = -1024;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_UP_AND_RIGHT = -1025;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_DOWN_AND_LEFT = -1026;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_DOWN_AND_RIGHT = -1027;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_LEFT_AND_UP = -1028;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_LEFT_AND_DOWN = -1029;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_RIGHT_AND_UP = -1030;
  public static final int GESTURE_SWIPE_DOWN_LIFT_SWIPE_RIGHT_AND_DOWN = -1031;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_UP_AND_DOWN = -1036;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_DOWN_AND_UP = -1037;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_LEFT_AND_RIGHT = -1038;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_RIGHT_AND_LEFT = -1039;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_UP_AND_LEFT = -1040;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_UP_AND_RIGHT = -1041;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_DOWN_AND_LEFT = -1042;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_DOWN_AND_RIGHT = -1043;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_LEFT_AND_UP = -1044;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_LEFT_AND_DOWN = -1045;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_RIGHT_AND_UP = -1046;
  public static final int GESTURE_SWIPE_LEFT_LIFT_SWIPE_RIGHT_AND_DOWN = -1047;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP_AND_DOWN = -1052;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_DOWN_AND_UP = -1053;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_LEFT_AND_RIGHT = -1054;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT_AND_LEFT = -1055;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP_AND_LEFT = -1056;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_UP_AND_RIGHT = -1057;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_DOWN_AND_LEFT = -1058;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_DOWN_AND_RIGHT = -1059;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_LEFT_AND_UP = -1060;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_LEFT_AND_DOWN = -1061;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT_AND_UP = -1062;
  public static final int GESTURE_SWIPE_RIGHT_LIFT_SWIPE_RIGHT_AND_DOWN = -1063;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_UP = -1064;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_DOWN = -1065;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_LEFT = -1066;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_RIGHT = -1067;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_UP_AND_DOWN = -1068;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_DOWN_AND_UP = -1069;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_LEFT_AND_RIGHT = -1070;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_RIGHT_AND_LEFT = -1071;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_UP_AND_LEFT = -1072;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_UP_AND_RIGHT = -1073;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_DOWN_AND_LEFT = -1074;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_DOWN_AND_RIGHT = -1075;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_LEFT_AND_UP = -1076;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_LEFT_AND_DOWN = -1077;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_RIGHT_AND_UP = -1078;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_LIFT_SWIPE_RIGHT_AND_DOWN = -1079;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_UP = -1080;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_DOWN = -1081;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_LEFT = -1082;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_RIGHT = -1083;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_UP_AND_DOWN = -1084;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_DOWN_AND_UP = -1085;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_LEFT_AND_RIGHT = -1086;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_RIGHT_AND_LEFT = -1087;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_UP_AND_LEFT = -1088;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_UP_AND_RIGHT = -1089;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_DOWN_AND_LEFT = -1090;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_DOWN_AND_RIGHT = -1091;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_LEFT_AND_UP = -1092;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_LEFT_AND_DOWN = -1093;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_RIGHT_AND_UP = -1094;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_LIFT_SWIPE_RIGHT_AND_DOWN = -1095;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_UP = -1096;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_DOWN = -1097;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_LEFT = -1098;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_RIGHT = -1099;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_UP_AND_DOWN = -1100;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_DOWN_AND_UP = -1101;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_LEFT_AND_RIGHT = -1102;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_RIGHT_AND_LEFT = -1103;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_UP_AND_LEFT = -1104;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_UP_AND_RIGHT = -1105;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_DOWN_AND_LEFT = -1106;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_DOWN_AND_RIGHT = -1107;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_LEFT_AND_UP = -1108;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_LEFT_AND_DOWN = -1109;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_RIGHT_AND_UP = -1110;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_LIFT_SWIPE_RIGHT_AND_DOWN = -1111;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_UP = -1112;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_DOWN = -1113;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_LEFT = -1114;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_RIGHT = -1115;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_UP_AND_DOWN = -1116;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_DOWN_AND_UP = -1117;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_LEFT_AND_RIGHT = -1118;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_RIGHT_AND_LEFT = -1119;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_UP_AND_LEFT = -1120;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_UP_AND_RIGHT = -1121;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_DOWN_AND_LEFT = -1122;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_DOWN_AND_RIGHT = -1123;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_LEFT_AND_UP = -1124;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_LEFT_AND_DOWN = -1125;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_RIGHT_AND_UP = -1126;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_LIFT_SWIPE_RIGHT_AND_DOWN = -1127;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_UP = -1128;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_DOWN = -1129;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_LEFT = -1130;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_RIGHT = -1131;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_UP_AND_DOWN = -1132;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_DOWN_AND_UP = -1133;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_LEFT_AND_RIGHT = -1134;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_RIGHT_AND_LEFT = -1135;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_UP_AND_LEFT = -1136;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_UP_AND_RIGHT = -1137;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_DOWN_AND_LEFT = -1138;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_DOWN_AND_RIGHT = -1139;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_LEFT_AND_UP = -1140;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_LEFT_AND_DOWN = -1141;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_RIGHT_AND_UP = -1142;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_LIFT_SWIPE_RIGHT_AND_DOWN = -1143;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_UP = -1144;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_DOWN = -1145;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_LEFT = -1146;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_RIGHT = -1147;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_UP_AND_DOWN = -1148;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_DOWN_AND_UP = -1149;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_LEFT_AND_RIGHT = -1150;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_RIGHT_AND_LEFT = -1151;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_UP_AND_LEFT = -1152;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_UP_AND_RIGHT = -1153;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_DOWN_AND_LEFT = -1154;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_DOWN_AND_RIGHT = -1155;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_LEFT_AND_UP = -1156;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_LEFT_AND_DOWN = -1157;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_RIGHT_AND_UP = -1158;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_LIFT_SWIPE_RIGHT_AND_DOWN = -1159;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_UP = -1160;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_DOWN = -1161;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_LEFT = -1162;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_RIGHT = -1163;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_UP_AND_DOWN = -1164;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_DOWN_AND_UP = -1165;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_LEFT_AND_RIGHT = -1166;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_RIGHT_AND_LEFT = -1167;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_UP_AND_LEFT = -1168;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_UP_AND_RIGHT = -1169;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_DOWN_AND_LEFT = -1170;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_DOWN_AND_RIGHT = -1171;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_LEFT_AND_UP = -1172;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_LEFT_AND_DOWN = -1173;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_RIGHT_AND_UP = -1174;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_LIFT_SWIPE_RIGHT_AND_DOWN = -1175;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_UP = -1176;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_DOWN = -1177;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_LEFT = -1178;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_RIGHT = -1179;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_UP_AND_DOWN = -1180;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_DOWN_AND_UP = -1181;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_LEFT_AND_RIGHT = -1182;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_RIGHT_AND_LEFT = -1183;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_UP_AND_LEFT = -1184;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_UP_AND_RIGHT = -1185;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_DOWN_AND_LEFT = -1186;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_DOWN_AND_RIGHT = -1187;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_LEFT_AND_UP = -1188;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_LEFT_AND_DOWN = -1189;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_RIGHT_AND_UP = -1190;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_LIFT_SWIPE_RIGHT_AND_DOWN = -1191;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_UP = -1192;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_DOWN = -1193;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_LEFT = -1194;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_RIGHT = -1195;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_UP_AND_DOWN = -1196;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_DOWN_AND_UP = -1197;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_LEFT_AND_RIGHT = -1198;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_RIGHT_AND_LEFT = -1199;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_UP_AND_LEFT = -1200;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_UP_AND_RIGHT = -1201;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_DOWN_AND_LEFT = -1202;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_DOWN_AND_RIGHT = -1203;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_LEFT_AND_UP = -1204;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_LEFT_AND_DOWN = -1205;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_RIGHT_AND_UP = -1206;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_LIFT_SWIPE_RIGHT_AND_DOWN = -1207;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_UP = -1208;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_DOWN = -1209;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_LEFT = -1210;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_RIGHT = -1211;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_UP_AND_DOWN = -1212;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_DOWN_AND_UP = -1213;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_LEFT_AND_RIGHT = -1214;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_RIGHT_AND_LEFT = -1215;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_UP_AND_LEFT = -1216;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_UP_AND_RIGHT = -1217;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_DOWN_AND_LEFT = -1218;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_DOWN_AND_RIGHT = -1219;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_LEFT_AND_UP = -1220;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_LEFT_AND_DOWN = -1221;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_RIGHT_AND_UP = -1222;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_LIFT_SWIPE_RIGHT_AND_DOWN = -1223;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_UP = -1224;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_DOWN = -1225;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_LEFT = -1226;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_RIGHT = -1227;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_UP_AND_DOWN = -1228;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_DOWN_AND_UP = -1229;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_LEFT_AND_RIGHT = -1230;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_RIGHT_AND_LEFT = -1231;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_UP_AND_LEFT = -1232;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_UP_AND_RIGHT = -1233;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_DOWN_AND_LEFT = -1234;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_DOWN_AND_RIGHT = -1235;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_LEFT_AND_UP = -1236;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_LEFT_AND_DOWN = -1237;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_RIGHT_AND_UP = -1238;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_LIFT_SWIPE_RIGHT_AND_DOWN = -1239;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_UP = -1240;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_DOWN = -1241;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_LEFT = -1242;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_RIGHT = -1243;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_UP_AND_DOWN = -1244;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_DOWN_AND_UP = -1245;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_LEFT_AND_RIGHT = -1246;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_RIGHT_AND_LEFT = -1247;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_UP_AND_LEFT = -1248;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_UP_AND_RIGHT = -1249;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_DOWN_AND_LEFT = -1250;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_DOWN_AND_RIGHT = -1251;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_LEFT_AND_UP = -1252;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_LEFT_AND_DOWN = -1253;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_RIGHT_AND_UP = -1254;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_LIFT_SWIPE_RIGHT_AND_DOWN = -1255;

  // Match the value of GESTURE_ID_2FINGER_1TAP_HOLD in TalkBack.
  public static final int GESTURE_2_FINGER_SINGLE_TAP_AND_HOLD = 63;

  private static final String LOG_TAG = "GestureManifold";

  private final List<GestureMatcher> gestures = new ArrayList<>();
  private final int displayId;
  // Listener to be notified of gesture start and end.
  private final Listener listener;
  // Whether multi-finger gestures are enabled.
  boolean multiFingerGesturesEnabled;
  // Whether the two-finger passthrough is enabled when multi-finger gestures are enabled.
  private boolean twoFingerPassthroughEnabled;
  // A list of all the multi-finger gestures, for easy adding and removal.
  private final List<GestureMatcher> multiFingerGestures = new ArrayList<>();
  // A list of two-finger swipes, including the angle gestures, for easy adding and removal when
  // turning on or off two-finger passthrough.
  private final List<GestureMatcher> twoFingerSwipes = new ArrayList<>();
  private boolean logMotionEvent = false;
  // Whether a gesture completed on the move event being processed.
  private boolean completedInMove = false;

  /** Define the interface to get project base feature settings. */
  /** Returns the swipe and hold version of the one-finger swipe {@code swipeGestureId}, or 0. */
  public static int swipeAndHoldOf(int swipeGestureId) {
    return switch (swipeGestureId) {
      case AccessibilityService.GESTURE_SWIPE_UP -> GESTURE_SWIPE_UP_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_DOWN -> GESTURE_SWIPE_DOWN_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_LEFT -> GESTURE_SWIPE_LEFT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_RIGHT -> GESTURE_SWIPE_RIGHT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN -> GESTURE_SWIPE_UP_AND_DOWN_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_DOWN_AND_UP -> GESTURE_SWIPE_DOWN_AND_UP_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT -> GESTURE_SWIPE_LEFT_AND_RIGHT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT -> GESTURE_SWIPE_RIGHT_AND_LEFT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT -> GESTURE_SWIPE_UP_AND_LEFT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT -> GESTURE_SWIPE_UP_AND_RIGHT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_DOWN_AND_LEFT -> GESTURE_SWIPE_DOWN_AND_LEFT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_DOWN_AND_RIGHT -> GESTURE_SWIPE_DOWN_AND_RIGHT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_UP -> GESTURE_SWIPE_LEFT_AND_UP_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_DOWN -> GESTURE_SWIPE_LEFT_AND_DOWN_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP -> GESTURE_SWIPE_RIGHT_AND_UP_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_DOWN -> GESTURE_SWIPE_RIGHT_AND_DOWN_AND_HOLD;
      default -> 0;
    };
  }

  public interface GestureConfigProvider {
    default float getDoubleTapSlopMultiplier() {
      return (float) 1.0;
    }

    default boolean getSpeedUpTouchExploreState() {
      return false;
    }

    default boolean invalidSwipeGestureEarlyDetection() {
      return false;
    }

    default boolean useMultipleGestureSet() {
      return false;
    }

    default boolean enableSplitTapAndHold() {
      return false;
    }

    /** Returns whether {@code gestureId} has an action, so that it is worth matching. */
    default boolean isGestureAssigned(int gestureId) {
      return false;
    }

    /**
     * Returns whether the touch may still become another kind of gesture, such as an edge swipe,
     * so that a swipe must not run before the finger lifts.
     */
    default boolean holdsSwipeUntilLift() {
      return false;
    }

    /**
     * Returns whether a one-finger swipe runs before the finger lifts while no gesture that starts
     * with it, such as a back-and-forth or angle gesture, has an action.
     */
    default boolean swipesBeforeLiftWhenNothingFollows() {
      return false;
    }

    /**
     * Returns whether a multi-finger tap gesture runs at its last tap, without waiting for another
     * tap, while no gesture that goes on after its taps has an action.
     */
    default boolean tapsRunWhenNothingFollows() {
      return false;
    }
  }

  public GestureManifold(
      Context context,
      Listener listener,
      GestureConfigProvider configResolver,
      AnalyticsEventLogger logger,
      int displayId,
      ImmutableList<String> supportGestureList) {
    this.listener = listener;
    this.displayId = displayId;
    multiFingerGesturesEnabled = false;
    twoFingerPassthroughEnabled = false;

    // Set up gestures.
    List<GestureMatcher> gestureMatcherList =
        GestureMatcherFactory.getGestureMatcherList(
            context, supportGestureList, this, configResolver, logger);

    for (GestureMatcher gestureMatcher : gestureMatcherList) {
      if (gestureMatcher != null) {
        if ((gestureMatcher instanceof Swipe)
            || (gestureMatcher instanceof MultiTap)
            || (gestureMatcher instanceof MultiTapAndHold)
            || (gestureMatcher instanceof SecondFingerTap)) {
          gestures.add(gestureMatcher);
        } else {
          multiFingerGestures.add(gestureMatcher);
          int gestureId = gestureMatcher.getGestureId();
          if ((gestureId == GESTURE_2_FINGER_SWIPE_DOWN)
              || (gestureId == GESTURE_2_FINGER_SWIPE_LEFT)
              || (gestureId == GESTURE_2_FINGER_SWIPE_RIGHT)
              || (gestureId == GESTURE_2_FINGER_SWIPE_UP)
              || GestureConfiguration.isTwoFingerAngle(gestureId)) {
            twoFingerSwipes.add(gestureMatcher);
          }
        }
      }
    }
    setFollowingTapGestures(configResolver);
    if (configResolver.useMultipleGestureSet()) {
      gestures.add(
          new TwoFingerSecondFingerMultiTap(
              context,
              2,
              ROTATE_DIRECTION_FORWARD,
              GESTURE_TAP_HOLD_AND_2ND_FINGER_FORWARD_DOUBLE_TAP,
              this,
              logger));
      gestures.add(
          new TwoFingerSecondFingerMultiTap(
              context,
              2,
              ROTATE_DIRECTION_BACKWARD,
              GESTURE_TAP_HOLD_AND_2ND_FINGER_BACKWARD_DOUBLE_TAP,
              this,
              logger));
    }
  }

  /**
   * Gives each plain one-finger and multi-finger multi-tap the gestures that start with its taps and
   * go on after them, such as more taps or a tap and hold with as many fingers.
   */
  private void setFollowingTapGestures(GestureConfigProvider configResolver) {
    // One finger: a double or triple tap waits for a further tap only while a gesture of more taps
    // has an action.
    for (GestureMatcher matcher : gestures) {
      if (matcher.getClass() != MultiTap.class) {
        continue;
      }
      MultiTap taps = (MultiTap) matcher;
      List<Integer> following = new ArrayList<>();
      for (GestureMatcher other : gestures) {
        if (other instanceof MultiTap longer && longer.continuesAfterTaps(taps.targetTaps)) {
          following.add(other.getGestureId());
        }
      }
      int[] gestureIds = new int[following.size()];
      for (int i = 0; i < gestureIds.length; i++) {
        gestureIds[i] = following.get(i);
      }
      taps.setFollowingGestures(gestureIds);
    }
    for (GestureMatcher matcher : multiFingerGestures) {
      if (matcher.getClass() != MultiFingerMultiTap.class) {
        continue;
      }
      MultiFingerMultiTap taps = (MultiFingerMultiTap) matcher;
      List<Integer> following = new ArrayList<>();
      for (GestureMatcher other : multiFingerGestures) {
        if (other instanceof MultiFingerMultiTap longer
            && longer.continuesAfterTaps(taps.targetFingerCount, taps.mTargetTapCount)) {
          following.add(other.getGestureId());
        }
      }
      int[] gestureIds = new int[following.size()];
      for (int i = 0; i < gestureIds.length; i++) {
        gestureIds[i] = following.get(i);
      }
      taps.setFollowingGestures(configResolver, gestureIds);
    }
  }

  public void onConfigurationChanged(Context context) {
    for (GestureMatcher gestureDetector : gestures) {
      gestureDetector.onConfigurationChanged(context);
    }
  }

  public void enableLogMotionEvent() {
    logMotionEvent = true;
    for (GestureMatcher gestureDetector : gestures) {
      gestureDetector.enableLogMotionEvent();
    }
  }

  /**
   * Processes a motion event.
   *
   * @param event The event as received from the previous entry in the event stream.
   * @return True if the event has been appropriately handled by the gesture manifold and related
   *     callback functions, false if it should be handled further by the calling function.
   */
  @CanIgnoreReturnValue
  public boolean onMotionEvent(EventId eventId, MotionEvent event) {
    completedInMove = false;
    for (GestureMatcher matcher : gestures) {
      if (matcher.getState() != GestureMatcher.STATE_GESTURE_CANCELED) {
        if (logMotionEvent) {
          LogUtils.v(LOG_TAG, matcher.toString());
        }
        matcher.onMotionEvent(eventId, event);
        if (logMotionEvent) {
          LogUtils.v(LOG_TAG, matcher.toString());
        }

        // A gesture completed before the lift, whose listener may have cleared every matcher: the
        // rest of the touch is not a gesture.
        if (matcher.getState() == GestureMatcher.STATE_GESTURE_COMPLETED || completedInMove) {
          // Here we just return. The actual gesture dispatch is done in
          // onStateChanged().
          // No need to process this event any further.
          return true;
        }
      }
    }
    return false;
  }

  /** Resets single-finger swipes for a new touch after adding a second finger cancelled them. */
  public void clearOneFingerSwipes() {
    for (GestureMatcher matcher : gestures) {
      if (matcher instanceof Swipe) {
        matcher.clear();
      }
    }
  }

  public void clear() {
    for (GestureMatcher matcher : gestures) {
      matcher.clear();
    }
  }

  /**
   * Clears every matcher but the one-finger tap gestures that wait for a further tap, for a touch
   * that starts after Android ended the touch interaction. Android ends it the system double-tap
   * timeout after the finger lifts, which a longer time between one-finger taps outlasts. A kept
   * matcher whose next tap comes too late or too far away starts over with that touch as its first
   * tap.
   */
  public void clearExceptWaitingTaps() {
    for (GestureMatcher matcher : gestures) {
      if (matcher instanceof MultiTap taps && taps.waitsForNextTap()) {
        taps.keepForNextInteraction();
      } else {
        matcher.clear();
      }
    }
  }

  /**
   * Listener that receives notifications of the state of the gesture detector. Listener functions
   * are called as a result of onMotionEvent(). The current MotionEvent in the context of these
   * functions is the event passed into onMotionEvent.
   */
  public interface Listener {

    /**
     * Called when the system has decided the event stream is a potential gesture.
     *
     * @param gestureId the gesture which is start matching.
     */
    void onGestureStarted(int gestureId);

    /**
     * Called when an event stream is recognized as a gesture.
     *
     * @param gestureEvent Information about the gesture.
     */
    void onGestureCompleted(AccessibilityGestureEvent gestureEvent);

    /**
     * Called when the system has decided an event stream doesn't match any known gesture.
     *
     * @param gestureId the gesture which is fail to match.
     */
    void onGestureCancelled(int gestureId);

    /**
     * Called when the gesture is processing and should be avoided to be interrupted. It's mainly be
     * used to extend the multi-tap timeout even the user sets the touch focus delay with a shorter
     * time.
     *
     * @param gestureId the gesture which is fail to match.
     */
    default void onGestureProcessing(int gestureId) {}
  }

  @Override
  public void onStateChanged(int gestureId, int state, MotionEvent event) {
    if (state == GestureMatcher.STATE_GESTURE_STARTED) {
      listener.onGestureStarted(gestureId);
    } else if (state == GestureMatcher.STATE_GESTURE_COMPLETED) {
      onGestureCompleted(gestureId, event);
    } else if (state == GestureMatcher.STATE_GESTURE_CANCELED) {
      listener.onGestureCancelled(gestureId);
    } else if (state == GestureMatcher.STATE_GESTURE_PROCESSING) {
      listener.onGestureProcessing(gestureId);
    }
  }

  /**
   * Called when the gesture detector has successfully identified the gesture by a series of
   * MotionEvent.
   *
   * @param gestureId the gesture which is fail to match.
   * @param event the last MotionEvent to match the identified gesture.
   */
  private void onGestureCompleted(int gestureId, MotionEvent event) {
    if (event != null && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
      completedInMove = true;
    }
    // Note that gestures that complete immediately call clear() from onMotionEvent.
    // Gestures that complete on a delay call clear() here.
    ArrayList<MotionEvent> eventList = new ArrayList<>();
    eventList.add(event);
    AccessibilityGestureEvent gestureEvent =
        new AccessibilityGestureEvent(gestureId, displayId, eventList);
    for (GestureMatcher matcher : gestures) {
      if (gestureId == GESTURE_TAP_UP_TOUCH_EXPLORE
          && matcher.bypassCancelByTapUpToTouchExplore()) {
        // Skip to cancel the gesture which claims itself to bypass cancel for this event.
        continue;
      } else if (gestureId == GESTURE_FAKED_SPLIT_TYPING
          && matcher.getGestureId() == GESTURE_FAKED_SPLIT_TYPING_AND_HOLD) {
        matcher.restart(true);
        continue;
      } else if (gestureId == GESTURE_FAKED_SPLIT_TYPING_AND_HOLD
          && matcher.getGestureId() == GESTURE_FAKED_SPLIT_TYPING) {
        matcher.restart(true);
        continue;
      }
      if (matcher.getGestureId() != gestureId) {
        matcher.cancelGesture(event, false);
      }
    }
    listener.onGestureCompleted(gestureEvent);
  }

  /** Returns whether a one-finger swipe in three directions has started and not yet ended. */
  public boolean isThreeDirectionSwipeStarted() {
    for (GestureMatcher matcher : gestures) {
      if (matcher.getState() == GestureMatcher.STATE_GESTURE_STARTED
          && GestureConfiguration.isThreeDirectionSwipe(matcher.getGestureId())) {
        return true;
      }
    }
    return false;
  }

  public boolean isMultiFingerGesturesEnabled() {
    return multiFingerGesturesEnabled;
  }

  public void setMultiFingerGesturesEnabled(boolean mode) {
    if (multiFingerGesturesEnabled != mode) {
      multiFingerGesturesEnabled = mode;
      if (mode) {
        gestures.addAll(multiFingerGestures);
      } else {
        gestures.removeAll(multiFingerGestures);
      }
    }
  }

  public boolean isTwoFingerPassthroughEnabled() {
    return twoFingerPassthroughEnabled;
  }

  public void setTwoFingerPassthroughEnabled(boolean mode) {
    if (twoFingerPassthroughEnabled != mode) {
      twoFingerPassthroughEnabled = mode;
      if (!mode) {
        multiFingerGestures.addAll(twoFingerSwipes);
        if (multiFingerGesturesEnabled) {
          gestures.addAll(twoFingerSwipes);
        }
      } else {
        multiFingerGestures.removeAll(twoFingerSwipes);
        gestures.removeAll(twoFingerSwipes);
      }
    }
  }

  /**
   * This class helps to collect data (saved time to enter Touch Explore), in addition to the
   * fundamental Gesture analytic event.
   */
  public static class TapToTouchExploreAnalyticsEvent extends GestureAnalyticsEvent {
    public int savedTimeMs;

    public TapToTouchExploreAnalyticsEvent(int event, int gestureId, int savedTimeMs) {
      super(event, gestureId);
      this.savedTimeMs = savedTimeMs;
    }
  }
}
