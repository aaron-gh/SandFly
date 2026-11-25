# Choosing where your app gets direct touch

With direct touch, touches go straight to your app instead of to SandFly's explore by touch. An app can take direct touch on the whole screen or only on parts of it, such as a game board, while explore by touch keeps working on the rest of the screen. It can declare this in its manifest, by activity and by a state it sets, and change it while it runs.

## What the user controls

*   Direct touch is turned on for your app the first time SandFly sees it, if your app declares where it wants direct touch or holds the permission below. The user can turn it off for your app at any time in **Touch** > **Direct touch**, and nothing your app declares or asks for has any effect then.
*   SandFly takes touch back when a dialog appears, another app opens on top of yours, the notification shade or quick settings open, a text field takes focus, or the screen turns off. Touch goes back to your app's areas when they're gone.
*   System windows, such as the status bar, stay with SandFly. So does the keyboard, unless the user turns on direct typing for your app. The navigation bar takes touches directly only when the user turns on **Tap to activate navigation bar buttons**.
*   SandFly says "Direct touch on" when your app's areas start taking touches, and "Direct touch off" when they stop, unless the user turned off **Speak when direct touch changes**.

## Which areas apply

While your app is in front, SandFly uses the first of these that applies:

1.  The last request your app made while it runs, until it calls `reset`.
2.  The rules in your manifest declaration that match the current activity and state.
3.  No direct touch, for an app that has a declaration or holds the permission.
4.  The whole screen, for an app with neither, such as a game the user turned on in the **Apps** list or one that declares `dev.nvgt.capability.DIRECT_TOUCH`.

## Declaring areas in the manifest

Add a `<meta-data>` element inside your `<application>` element that names an XML resource:

```xml
<meta-data
    android:name="io.github.aaron_gh.sandfly.direct_touch"
    android:resource="@xml/direct_touch" />
```

In `res/xml/direct_touch.xml`, each element inside `<direct-touch>` is a rule:

```xml
<direct-touch>
    <!-- The whole screen in GameActivity. -->
    <screen activity=".GameActivity" />

    <!-- The board view in MainActivity while the state is "playing". -->
    <area activity=".MainActivity" state="playing" view="@id/board" />

    <!-- The bottom half of the window in MainActivity while the state is "playing". -->
    <area activity=".MainActivity" state="playing" top="50%" />

    <!-- A strip 200 dp tall along the bottom of the window while the state is "racing". -->
    <area state="racing" bottom="100%" height="200dp" />

    <!-- The whole screen in any activity while the state is "racing". -->
    <screen state="racing" />
</direct-touch>
```

| Element | What it gives |
| --- | --- |
| `<screen>` | The whole screen. |
| `<area>` with `view` | The view with that resource ID, wherever it is on screen, while it's shown. Write `@id/board`, `board` or `com.example.game:id/board`. |
| `<area>` with edges | Part of your app's window, described in [Areas of the window](#areas-of-the-window). |

Each rule can have these conditions. A rule without one applies in every activity or every state.

| Attribute | Matches |
| --- | --- |
| `activity` | The activity in front, as the manifest writes it: `.GameActivity`, `GameActivity` or `com.example.game.GameActivity`. `*` matches any activity. |
| `state` | The state your app last set with `setState`. A rule with a state doesn't match until your app sets that state. |

### Areas of the window

An `<area>` without `view` takes these attributes, all optional:

| Attribute | Meaning |
| --- | --- |
| `left`, `right` | Distance from the window's left edge. |
| `top`, `bottom` | Distance from the window's top edge. |
| `width`, `height` | Size, for an area that has only one edge on that axis, or none. |

Each value is a fraction of the window's width or height, as `30%` or `0.3`, a size in dp, as `48dp`, or a size in pixels, as `120px`. Fractions above 100% count as 100%, and values can't be negative.

Every edge is measured from the window's top left corner, not from the opposite side, and an edge that is left out is the window's own edge:

| Declaration | Area |
| --- | --- |
| `top="30%"` | From 30% of the way down to the bottom. |
| `bottom="30%"` | The top 30%. |
| `left="30%" right="70%"` | The middle 40% of the width, the full height. |
| `bottom="100%" height="200dp"` | A strip 200 dp tall along the bottom. |
| `left="100%" width="80dp"` | Nothing: the area starts at the right edge. Use `right="100%" width="80dp"` for a strip along the right side. |
| `width="150px" height="150px"` | A square of 150 pixels in the top left corner. |

With an edge on each side of an axis, `width` or `height` is ignored. Measurements are from your app's window, which can start below the status bar, not from the screen. Areas are cut off at the window's edges. dp scale with the display's density, and pixels don't, so pixels suit only a known screen size.

When rules for the whole screen and for areas both match, the whole screen applies. Otherwise the matching areas add up. SandFly reads up to 64 rules and skips elements it doesn't know.

An empty `<direct-touch />` gives no direct touch until your app asks for it.

SandFly learns which activity is in front when the activity opens. If SandFly starts while one of your activities is already open, rules that name an activity apply from the next activity that opens. SandFly reads the declaration again when your app is updated.

## Asking while your app runs

Declare the permission in your manifest:

```xml
<uses-permission android:name="io.github.aaron_gh.sandfly.permission.DIRECT_TOUCH" />
```

On Android 11 and later, also make SandFly visible to your app:

```xml
<queries>
    <package android:name="io.github.aaron_gh.sandfly" />
</queries>
```

Copy `talkback/src/main/aidl/io/github/aaron_gh/sandfly/directtouch/IDirectTouch.aidl` into your app, keeping the package `io.github.aaron_gh.sandfly.directtouch`, and bind to the service:

```kotlin
val intent =
  Intent("io.github.aaron_gh.sandfly.action.DIRECT_TOUCH").setPackage("io.github.aaron_gh.sandfly")
context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
```

Every call passes a client binder: any `Binder` your app creates once and keeps for as long as its process runs. When the process ends, SandFly drops your app's request and state.

```kotlin
val client = Binder()
val directTouch = IDirectTouch.Stub.asInterface(service)

// The whole screen.
directTouch.requestScreen(client)

// The top half of the screen, and the joystick view wherever it is.
val board = Rect(0, 0, screenWidth, screenHeight / 2)
directTouch.requestAreas(client, arrayOf(board), arrayOf("joystick"))

// No direct touch, whatever the manifest declares, until the next request.
directTouch.stop(client)

// Back to what the manifest declares.
directTouch.reset(client)

// The state that the manifest's rules match.
directTouch.setState(client, "playing")
directTouch.setState(client, null)
```

| Method | What it does |
| --- | --- |
| `requestScreen(client)` | Direct touch on the whole screen. |
| `requestAreas(client, areas, viewIds)` | Direct touch in `areas`, rectangles in screen pixels, and on the views with the resource IDs in `viewIds`. A bare name, such as `joystick`, is a view in your package. Either array can be empty. Up to 32 of each. |
| `stop(client)` | No direct touch for your app, even when the user turned it on or the manifest declares it, until your app makes another request or calls `reset`. |
| `reset(client)` | Drops the request, so that the manifest declaration applies again. |
| `setState(client, state)` | Sets the state that the declaration's rules match. `null` clears it. |

Each request replaces the one before it. Calls are one-way and return at once. They apply only while your app is in front; a request made in the background applies when your app comes to the front. Areas are limited to your app's window. Rectangles in screen pixels don't follow your layout, so send them again when it changes, or use view IDs, which SandFly looks up again as your content changes.

If SandFly restarts, the connection reports `onServiceDisconnected`, then `onServiceConnected` when SandFly is back. Send your request and state again in `onServiceConnected`.

## Keyboards in an accessibility overlay

A keyboard that has an accessibility service of its own and draws its keys in an accessibility overlay sets the touch exploration pass-through region for that overlay itself. Android keeps one region for all accessibility services, so any region SandFly set would replace the keyboard's. While such a keyboard is the default keyboard, its accessibility service is on and its overlay is on screen, SandFly adds the overlay's bounds to every pass-through region it sets, such as for direct touch or the navigation bar. A SandFly feature that takes the whole pass-through region for a moment, such as the circle menu, still gets it alone, and locking touch still clears it.

To check whether the installed SandFly does this, read the `io.github.aaron_gh.sandfly.keeps_keyboard_region` meta-data of its direct touch service. It needs no permission, only the `<queries>` element above:

```kotlin
val keepsKeyboardRegion =
  context.packageManager
    .queryIntentServices(
      Intent("io.github.aaron_gh.sandfly.action.DIRECT_TOUCH").setPackage("io.github.aaron_gh.sandfly"),
      PackageManager.GET_META_DATA,
    )
    .firstOrNull()
    ?.serviceInfo
    ?.metaData
    ?.getBoolean("io.github.aaron_gh.sandfly.keeps_keyboard_region") == true
```

SandFly 0.7.0 and earlier have no such meta-data, and can replace the keyboard's region.

## Deprecated names

These names are deprecated:

| Deprecated | Use |
| --- | --- |
| `fyi.quin.backtalk.direct_touch` | `io.github.aaron_gh.sandfly.direct_touch` |
| `fyi.quin.backtalk.action.DIRECT_TOUCH` | `io.github.aaron_gh.sandfly.action.DIRECT_TOUCH` |
| `fyi.quin.backtalk.directtouch.IDirectTouch` | `io.github.aaron_gh.sandfly.directtouch.IDirectTouch` |

The deprecated action binds the deprecated interface, so change both together. A manifest with both meta-data keys is read with the new one.

SandFly 27.0 and later still take these names from apps built against them, and log a warning with the tag `SandFlyApi` the first time each app uses one while SandFly runs. To see the warnings:

```sh
adb logcat -s SandFlyApi
```

If your app is a debuggable build, SandFly also posts a notification that lists the deprecated names it used.

The deprecated names will be removed in a later release. SandFly 0.7.0 and earlier take only the deprecated names. To work with those too, bind with the new action first and with the deprecated one when `bindService` returns false.
