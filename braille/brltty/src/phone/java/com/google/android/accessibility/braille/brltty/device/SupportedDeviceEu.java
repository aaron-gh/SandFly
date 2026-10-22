package com.google.android.accessibility.braille.brltty.device;

import android.util.Pair;
import com.google.android.accessibility.braille.brltty.KeyNameMapBuilder;
import com.google.android.accessibility.braille.brltty.R;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Supported device info for EuroBraille. */
public class SupportedDeviceEu extends SupportedDevice {
  private static final ImmutableList<Pattern> NAME_REGEXES =
      ImmutableList.of(Pattern.compile("Esys-"));
  private static final ImmutableSet<Pair<Integer, Integer>> VENDOR_PROD_IDS =
      ImmutableSet.of(
          // EuroBraille [Esys (version < 3.0, no SD card)]
          new Pair<>(0XC251, 0X1122),
          // EuroBraille [reserved]
          new Pair<>(0XC251, 0X1123),
          // EuroBraille [Esys (version < 3.0, with SD card)]
          new Pair<>(0XC251, 0X1124),
          // EuroBraille [reserved]
          new Pair<>(0XC251, 0X1125),
          // EuroBraille [Esys (version >= 3.0, no SD card)]
          new Pair<>(0XC251, 0X1126),
          // EuroBraille [reserved]
          new Pair<>(0XC251, 0X1127),
          // EuroBraille [Esys (version >= 3.0, with SD card)]
          new Pair<>(0XC251, 0X1128),
          // EuroBraille [reserved]
          new Pair<>(0XC251, 0X1129),
          // EuroBraille [reserved]
          new Pair<>(0XC251, 0X112A),
          // EuroBraille [reserved]
          new Pair<>(0XC251, 0X112B),
          // EuroBraille [reserved]
          new Pair<>(0XC251, 0X112C),
          // EuroBraille [reserved]
          new Pair<>(0XC251, 0X112D),
          // EuroBraille [reserved]
          new Pair<>(0XC251, 0X112E),
          // EuroBraille [reserved]
          new Pair<>(0XC251, 0X112F),
          // EuroBraille [Esytime (firmware 1.03, 2014-03-31)]
          // EuroBraille [Esytime]
          new Pair<>(0XC251, 0X1130),
          // EuroBraille [reserved]
          new Pair<>(0XC251, 0X1131),
          // EuroBraille [reserved]
          new Pair<>(0XC251, 0X1132),
          // EuroBraille [b.note]
          new Pair<>(0X28AC, 0X0012),
          // EuroBraille [b.note 2]
          new Pair<>(0X28AC, 0X0013),
          // EuroBraille [b.book (internal)]
          new Pair<>(0X28AC, 0X0020),
          // EuroBraille [b.book (external)]
          new Pair<>(0X28AC, 0X0021));

  @Override
  public String driverCode() {
    return "eu";
  }

  @Override
  public boolean connectSecurely() {
    return true;
  }

  @Override
  public Map<String, Integer> friendlyKeyNames() {
    return new KeyNameMapBuilder()
        .dots8()
        .add("Switch1Left", R.string.key_esys_SwitchLeft)
        .add("Switch1Right", R.string.key_esys_SwitchRight)
        .dualJoysticks()
        .add("Backspace", R.string.key_Backspace)
        .add("Space", R.string.key_Space)
        .add("RoutingKey1", R.string.key_Routing)
        // The b.note and b.book: one row of routing keys, keypads and navigation keys.
        .add("RoutingKey", R.string.key_Routing)
        .add("LeftKeypadLeft", R.string.key_LeftJoystickLeft)
        .add("LeftKeypadRight", R.string.key_LeftJoystickRight)
        .add("LeftKeypadUp", R.string.key_LeftJoystickUp)
        .add("LeftKeypadDown", R.string.key_LeftJoystickDown)
        .add("RightKeypadLeft", R.string.key_RightJoystickLeft)
        .add("RightKeypadRight", R.string.key_RightJoystickRight)
        .add("RightKeypadUp", R.string.key_RightJoystickUp)
        .add("RightKeypadDown", R.string.key_RightJoystickDown)
        .add("Backward", R.string.key_Backward)
        .add("Forward", R.string.key_Forward)
        .build();
  }

  @Override
  public Set<Pair<Integer, Integer>> vendorProdIds() {
    return VENDOR_PROD_IDS;
  }

  @Override
  public List<Pattern> nameRegexes() {
    return NAME_REGEXES;
  }
}
