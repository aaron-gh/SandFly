# Providing a braille display from another app

An app can give Backtalk a braille display that Backtalk can't reach on its own, such as the built-in keys and cells of a braille notetaker, or a display behind its maker's own software. Backtalk then works with it as it does with a display connected through Bluetooth or USB: the keys navigate and type, and the cells show what Backtalk shows.

## What the user controls

A display can read everything Backtalk shows, which can be anything on the screen, and its keys can do anything that a braille display's keys can, so it can control the device. Backtalk only uses displays from apps that the user allows in Backtalk, after a warning that says so:

- When Backtalk finds an app that can be a display and the user hasn't answered for it yet, it asks with a notification, which opens the warning.
- An app can also open the warning itself (see below).
- **Displays from other apps** in Backtalk's braille display settings lists every app that can be a display, and the user can allow or stop allowing each one there. Allowing one there shows the warning too.

While a display from an app is in use, Backtalk shows a notification that says which app it is. Backtalk doesn't limit what an allowed display sends or how quickly.

## Your app's manifest

Ask for the permission, which only lets your app ask to be a display, and provide a service that only Backtalk can bind:

```xml
<uses-permission android:name="fyi.quin.backtalk.permission.BRAILLE_DISPLAY" />

<application>
    <service
        android:name=".DisplayService"
        android:exported="true"
        android:permission="fyi.quin.backtalk.permission.BIND_BRAILLE_DISPLAY">
        <intent-filter>
            <action android:name="fyi.quin.backtalk.action.BRAILLE_DISPLAY" />
        </intent-filter>
    </service>
</application>
```

On Android 11 and later, also make Backtalk visible to your app, so that you can reach it:

```xml
<queries>
    <package android:name="fyi.quin.backtalk" />
</queries>
```

Backtalk binds your service once the user allows your app, and keeps it bound while it's running, so your app keeps running while Backtalk does; it binds it again if it stops. Backtalk looks for displays when it starts, when an app is installed or updated, and when the user allows or stops allowing one.

To ask the user straight away, rather than waiting for Backtalk's notification, open the warning:

```kotlin
context.startActivity(
  Intent("fyi.quin.backtalk.action.ASK_BRAILLE_DISPLAY")
    .setPackage("fyi.quin.backtalk")
    .putExtra("fyi.quin.backtalk.extra.PACKAGE", context.packageName)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
```

It does nothing if your app is allowed already. To have Backtalk look for displays again, such as after your app's hardware appears, send:

```kotlin
context.sendBroadcast(
  Intent("fyi.quin.backtalk.action.BRAILLE_DISPLAYS_CHANGED").setPackage("fyi.quin.backtalk"))
```

## The interface

Copy `braille/brailledisplay/src/phone/aidl/fyi/quin/backtalk/braille/IBrailleDisplay.aidl` and `IBrailleDisplayHost.aidl` into your app, keeping the package `fyi.quin.backtalk.braille`. Your service returns an `IBrailleDisplay`, and Backtalk gives it an `IBrailleDisplayHost` to call:

```kotlin
class DisplayService : Service() {
  private var host: IBrailleDisplayHost? = null

  private val display =
    object : IBrailleDisplay.Stub() {
      override fun onHostConnected(host: IBrailleDisplayHost) {
        this@DisplayService.host = host
        // When your hardware is ready: its name and number of text cells.
        host.start("Example display", 40)
      }

      override fun showCells(cells: ByteArray) {
        // Show cells on the hardware.
      }
    }

  override fun onBind(intent: Intent): IBinder = display
}
```

| `IBrailleDisplayHost` method | What it does |
| --- | --- |
| `start(name, cellCount)` | The display is ready, with this name and number of text cells. Use 0 for a display that only has keys. Call it again if either changes. |
| `stop()` | The display can't be used any more, for example because its hardware went away. Call `start` when it's back. |
| `brailleKeys(dots, space)` | Braille keys pressed together, with bit 0 for dot 1 up to bit 7 for dot 8, and whether the space bar was one of them. They run the same command as on any other braille display, and otherwise type. Send `brailleKeys(0, true)` for the space bar alone. |
| `command(name, argument)` | Runs a braille display command for one of your other keys. `name` is the name of a command in Backtalk's `BrailleInputEvent` without its `CMD_` prefix, such as `NAV_ITEM_NEXT`, `NAV_PAN_DOWN`, `ACTIVATE_CURRENT` or `GLOBAL_BACK`. For `ROUTE`, `argument` is the cell, counting from 0; otherwise it's 0. |

`showCells(cells)` gives your display what to show, one byte per cell, with the same bits as `brailleKeys`.

All of these calls are one-way, so they return at once.

## One display at a time

Backtalk uses one display at a time. A display connected through Bluetooth or USB comes first: while one is connected, displays from apps wait, and one that is in use steps aside when it connects and comes back when it disconnects. If several allowed apps start a display, Backtalk uses the first, and the others wait until it stops.
