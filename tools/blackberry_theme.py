# Synthesises the BlackBerry sound theme, which BlackBerry mode installs, and writes it as a theme
# file to talkback/src/main/assets/blackberry_theme.zip.
#
# The BlackBerry 10 Screen Reader played no sound when the focus moved. It played audio cues only
# for a task error, a task starting or finishing, a screen changing or refreshing, a switch,
# checkbox or option changing, a menu opening or closing, the keyboard showing or hiding, and a
# scroll finishing (BlackBerry's 10.3.1 user guides). The theme has a sound for each of those and
# none for focus. The sounds are SandFly's own, made here; no BlackBerry audio is used.
#
# Run from the repository root:
#   python3 tools/blackberry_theme.py
import json, os, sys, zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from sounds import PURE, note, click, render, write_wav  # noqa: E402

OUTPUT = "talkback/src/main/assets/blackberry_theme.zip"

# Partials of a small glass bell: an inharmonic upper partial gives the bright, clean edge.
GLASS = ((1, 1.0), (2.76, 0.18), (5.4, 0.05))


def sounds():
    """Each sound by theme name: its parts, length and peak."""
    s = {}
    # A switch, checkbox or option changed, or anything activated: one short glass tap.
    s["tick"] = ([note(1480, 0, 0.06, 0.018, partials=GLASS, detune_cents=0)], 0.07, -9)
    # Press and hold: the tap, then a lower one.
    s["long_clicked"] = ([note(1480, 0, 0.06, 0.018, partials=GLASS, detune_cents=0),
                          note(1110, 0.07, 0.07, 0.02, partials=GLASS, detune_cents=0)], 0.15, -9)
    # A screen changed or refreshed: two soft rising notes, a fifth apart.
    s["window_state"] = ([note(880, 0, 0.09, 0.04, partials=GLASS),
                          note(1320, 0.055, 0.14, 0.05, partials=GLASS)], 0.2, -10)
    # A scroll finished: a quick upward flick.
    s["scroll_tone"] = ([note(900, 0, 0.06, 0.025, glide=1350, partials=PURE, detune_cents=0),
                         click(0, 0.03, 0.006, 0.12, cutoff_hz=3000, seed=21)], 0.07, -11)
    # A task finished, or the end of a list or of reading: two falling notes.
    s["complete"] = ([note(1320, 0, 0.07, 0.03, partials=GLASS),
                      note(990, 0.06, 0.12, 0.045, partials=GLASS)], 0.19, -10)
    # A task error: a low, short double knock.
    s["typo"] = ([note(310, 0, 0.07, 0.025, partials=((1, 1.0), (3, 0.25)), detune_cents=0),
                  note(290, 0.09, 0.09, 0.03, partials=((1, 1.0), (3, 0.25)), detune_cents=0)],
                 0.19, -8)
    # The keyboard showing and hiding: a short rising and falling slide.
    s["keyboard_opened"] = ([note(700, 0, 0.09, 0.05, glide=1050, partials=PURE)], 0.1, -12)
    s["keyboard_closed"] = ([note(1050, 0, 0.09, 0.05, glide=700, partials=PURE)], 0.1, -12)
    return s


# Moving the focus is silent, as on BlackBerry 10; so are the cues SandFly adds around it.
SILENT = [
    "focus", "focus_actionable", "actions_available", "view_entered", "chime_up", "chime_down",
    "next_reading_control", "previous_reading_control", "insertion_point_moved", "column_wrap",
    "column_wrap_back", "hyperlink", "formatting",
]

MANIFEST = {
    "format": 1,
    "name": "BlackBerry",
    "author": "SandFly",
    "description": "Sounds only for screen changes, clicks, scrolls, ends, errors and the keyboard, "
                   "with none for moving the focus, similar to the BlackBerry 10 Screen Reader.",
    "license": "Apache-2.0",
    "settings": {"control_sounds": False, "speak_roles": True},
    "sounds": {name: "none" for name in SILENT},
}


def wav_bytes(samples):
    # write_wav takes a path, so write to a temporary file and read it back.
    tmp = OUTPUT + ".tmp.wav"
    write_wav(tmp, samples)
    with open(tmp, "rb") as f:
        data = f.read()
    os.remove(tmp)
    return data


def main():
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    with zipfile.ZipFile(OUTPUT, "w", zipfile.ZIP_DEFLATED) as z:
        info = zipfile.ZipInfo("theme.json", date_time=(2026, 1, 1, 0, 0, 0))
        z.writestr(info, json.dumps(MANIFEST, indent=2) + "\n")
        for name, (parts, length, peak_db) in sorted(sounds().items()):
            info = zipfile.ZipInfo(name + ".wav", date_time=(2026, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, wav_bytes(render(parts, length, peak_db)))


if __name__ == "__main__":
    sys.exit(main())
