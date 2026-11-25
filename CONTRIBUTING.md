# Contributing to SandFly

## Scope

SandFly accepts features that are within the scope of a screen reader. Features outside that scope are not accepted.

## Features need an agreed issue

1.  Open an issue that describes the feature: what it does, who needs it, and how it is used.
2.  Wait until the feature is agreed in the issue.
3.  Implement it, and link the issue in the pull request.

A pull request for a feature with no agreed issue is closed without comment.

Bug fixes do not need an issue first. The pull request describes the problem and how it was reproduced.

## Testing

Every change must be tested on a device running a build with the change. Passing unit tests is not enough.

The pull request states:

*   The device and Android version.
*   What was tested and the result.

## Touch, gestures and scrolling

A change that affects touch exploration, gestures or scrolling must include timing measurements:

*   One set measured on a build of a clean `master`.
*   One set measured on a build of `master` with the change.

Both sets use the same device, the same settings and the same actions. Repeat each action at least 20 times, and report the median and the maximum for each build.

A pull request that changes touch exploration, gestures or scrolling without these measurements is rejected.

Debug builds log touch state changes and each detected gesture under the `BacktalkGesture` logcat tag. To record them with millisecond timestamps:

```
adb logcat -v epoch -s BacktalkGesture
```

Event processing also has trace sections, which show in [Perfetto](https://perfetto.dev) system traces.

## Pull requests

*   One change per pull request.
*   Base the branch on the current `master`.
*   Fill in the pull request template.
