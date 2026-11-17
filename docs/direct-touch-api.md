# Choosing where your app gets direct touch

With direct touch, touches go straight to your app instead of to SandFly's explore by touch. An app can take direct touch on the whole screen or only on parts of it, such as a game board, while explore by touch keeps working on the rest of the screen. It can declare this in its manifest, by activity and by a state it sets, and change it while it runs.

## What the user controls

*   Direct touch is turned on for your app the first time SandFly sees it, if your app declares where it wants direct touch or holds the permission below. The user can turn it off for your app at any time in **Controls** > **Direct touch**, and nothing your app declares or asks for has any effect then.
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
    android:name="fyi.quin.backtalk.direct_touch"
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

    <!-- The whole screen in any activity while the state is "racing". -->
    <screen state="racing" />
</direct-touch>
```

| Element | What it gives |
| --- | --- |
| `<screen>` | The whole screen. |
| `<area>` with `view` | The view with that resource ID, wherever it is on screen, while it's shown. Write `@id/board`, `board` or `com.example.game:id/board`. |
| `<area>` with edges | Part of your app's window. `left`, `top`, `right` and `bottom` are fractions of the window, as `50%` or `0.5`. They default to the window's edges, so `<area top="50%" />` is the bottom half. |

Each rule can have these conditions. A rule without one applies in every activity or every state.

| Attribute | Matches |
| --- | --- |
| `activity` | The activity in front, as the manifest writes it: `.GameActivity`, `GameActivity` or `com.example.game.GameActivity`. `*` matches any activity. |
| `state` | The state your app last set with `setState`. A rule with a state doesn't match until your app sets that state. |

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

Copy `talkback/src/main/aidl/fyi/quin/backtalk/directtouch/IDirectTouch.aidl` into your app, keeping the package `fyi.quin.backtalk.directtouch`, and bind to the service:

```kotlin
val intent =
  Intent("fyi.quin.backtalk.action.DIRECT_TOUCH").setPackage("io.github.aaron_gh.sandfly")
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
