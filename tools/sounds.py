# Synthesises SandFly's own sounds in place of the ones that came from TalkBack: the focus, click,
# scroll, window, list, browse mode, typo, link, formatting, loading, volume, screen off and circle
# menu sounds in talkback and utils, and the braille sounds in braille/common. The agent sounds
# (tools/agent_sounds.py) and the edge gesture sounds have their own scripts.
#
# Each sound is written as a 48 kHz mono WAV, peak-normalised to its own level, and, when ffmpeg is
# given, encoded to Ogg Vorbis at the path the app reads it from:
#   python3 tools/sounds.py /tmp/sounds /opt/homebrew/bin/ffmpeg
# ffmpeg's built-in Vorbis encoder writes only stereo, which takes 3 dB off each channel, so the WAV
# is raised by 3 dB on the way in. Peaks end up near the levels each sound is given below.
import math, os, random, struct, subprocess, sys, wave

RATE = 48000

TALKBACK = "talkback/src/main/res/raw"
UTILS = "utils/src/main/res/raw"
BRAILLE = "braille/common/src/phone/res/raw"


def note(freq, start, dur, decay, gain=1.0, glide=None, partials=((1, 1.0), (2, 0.3), (3, 0.1)),
         attack=0.003, brightness_decay=6.0):
    """A struck tone at `start` seconds. `decay` is the envelope's time constant; `glide` is the
    frequency at the end of the note, reached along an exponential curve; higher partials die away
    faster, by `brightness_decay` per second per partial above the first."""
    end_freq = glide or freq
    n = int(dur * RATE)
    s0 = int(start * RATE)
    phases = [0.0] * len(partials)
    out = {}
    for i in range(n):
        t = i / RATE
        f = freq * (end_freq / freq) ** (t / dur)
        env = min(1.0, t / attack) * math.exp(-t / decay)
        env *= min(1.0, (n - i) / (0.004 * RATE))  # No click at the end.
        v = 0.0
        for k, (h, w) in enumerate(partials):
            phases[k] += 2 * math.pi * f * h / RATE
            v += w * math.sin(phases[k]) * math.exp(-t * (h - 1) * brightness_decay)
        out[s0 + i] = v * env * gain
    return out


def click(start, dur, decay, gain=1.0, tone_hz=None, seed=1):
    """A short noise burst, smoothed by a one-pole low-pass, for the body of a tick."""
    rng = random.Random(seed)
    n = int(dur * RATE)
    s0 = int(start * RATE)
    out = {}
    smooth = 0.0
    coeff = 0.35
    for i in range(n):
        t = i / RATE
        smooth += coeff * (rng.uniform(-1, 1) - smooth)
        v = smooth
        if tone_hz:
            v = 0.5 * v + math.sin(2 * math.pi * tone_hz * t)
        env = min(1.0, t / 0.0005) * math.exp(-t / decay)
        out[s0 + i] = v * env * gain
    return out


def render(parts, length, peak_db):
    buf = [0.0] * int(length * RATE)
    for part in parts:
        for i, v in part.items():
            if i < len(buf):
                buf[i] += v
    peak = max(abs(x) for x in buf) or 1.0
    scale = 10 ** (peak_db / 20) / peak
    return [x * scale for x in buf]


def write_wav(path, samples):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, x)) * 32767)) for x in samples))


# Pitches, in Hz.
C5, D5, E5, F5, G5, A5, B5 = 523.25, 587.33, 659.26, 698.46, 783.99, 880.0, 987.77
C6, D6, E6, G6, A6, C7 = 1046.5, 1174.66, 1318.51, 1567.98, 1760.0, 2093.0
PENTATONIC = [C5, D5, E5, G5, A5, C6, D6, E6]


def sounds():
    """Each sound by file name, with the directories it goes in, its parts, length and peak."""
    s = {}

    # Focus moving to an item: a soft, short wooden tap. Items that can be activated are brighter.
    s["focus"] = ([TALKBACK], [click(0, 0.03, 0.004, 0.3, seed=2),
                               note(1400, 0, 0.05, 0.012, partials=((1, 1.0), (2.4, 0.2)))], 0.06, -6)
    s["focus_actionable"] = ([TALKBACK], [click(0, 0.03, 0.004, 0.3, seed=3),
                                          note(1900, 0, 0.06, 0.014, partials=((1, 1.0), (2, 0.35)))],
                             0.07, -6)
    # A finger moving onto an item or empty space while exploring.
    s["view_entered"] = ([TALKBACK], [note(700, 0, 0.04, 0.01, partials=((1, 1.0),))], 0.05, -10)
    # Something was clicked.
    s["tick"] = ([TALKBACK], [click(0, 0.02, 0.003, 1.0, tone_hz=2600, seed=4)], 0.03, -5)
    # Something was long-pressed: two taps, the second lower.
    s["long_clicked"] = ([TALKBACK], [click(0, 0.02, 0.003, 1.0, tone_hz=2400, seed=5),
                                      click(0.08, 0.02, 0.003, 1.0, tone_hz=1700, seed=6)], 0.11, -5)
    # Moving into and out of a scrolling list: a quick upward and downward slide.
    for name, dirs in (("chime_up", [TALKBACK, BRAILLE]), ("chime_down", [TALKBACK, BRAILLE])):
        up = name == "chime_up"
        s[name] = (dirs, [note(760 if up else 1150, 0, 0.05, 0.03, glide=1150 if up else 760,
                               partials=((1, 1.0), (2, 0.15)))], 0.05, -6)
    # A list scrolled; played faster or slower for the place in the list.
    s["scroll_tone"] = ([TALKBACK], [note(980, 0, 0.07, 0.02, partials=((1, 1.0), (2, 0.25)))],
                        0.08, -8)
    # A gesture that did nothing, such as a swipe past the last item, and the end of reading: two
    # quick falling notes.
    s["complete"] = ([TALKBACK, BRAILLE], [note(E6, 0, 0.06, 0.025), note(B5, 0.05, 0.1, 0.035)],
                     0.15, -6)
    # A new window or screen: a soft rising slide with a breath of noise.
    s["window_state"] = ([UTILS], [note(420, 0, 0.18, 0.08, glide=840, partials=((1, 1.0), (2, 0.2))),
                                   click(0, 0.12, 0.04, 0.15, seed=7)], 0.2, -7)
    # A link: two bright quick notes.
    s["hyperlink"] = ([UTILS], [note(E6, 0, 0.06, 0.03), note(A6, 0.045, 0.08, 0.04)], 0.13, -7)
    # Formatted text, such as bold: a low soft double note.
    s["formatting"] = ([UTILS], [note(440, 0, 0.14, 0.06, partials=((1, 1.0), (3, 0.2))),
                                 note(660, 0.04, 0.14, 0.06, partials=((1, 0.8),))], 0.18, -7)
    # A spelling mistake: two close low tones that beat against each other.
    s["typo"] = ([TALKBACK], [note(330, 0, 0.22, 0.09, partials=((1, 1.0), (2, 0.4), (3, 0.2))),
                              note(349, 0, 0.22, 0.09, partials=((1, 1.0), (2, 0.4)))], 0.24, -5)
    # Browse mode on: three fast rising notes. Off: the same falling.
    s["browse_mode_on_v4_2"] = ([TALKBACK], [note(G5, 0, 0.09, 0.04), note(C6, 0.06, 0.09, 0.04),
                                             note(E6, 0.12, 0.14, 0.06)], 0.26, -5)
    s["browse_mode_off_v4_2"] = ([TALKBACK], [note(E6, 0, 0.09, 0.04), note(C6, 0.06, 0.09, 0.04),
                                              note(G5, 0.12, 0.14, 0.06)], 0.26, -5)
    # Waiting, such as for an image description; repeats every 3 seconds.
    s["loading"] = ([TALKBACK], [note(A5, 0, 0.35, 0.12, partials=((1, 1.0), (2, 0.15))),
                                 note(E5, 0.25, 0.45, 0.16, partials=((1, 1.0), (2, 0.15)))], 0.7, -9)
    # A volume change, and the braille display's beep.
    s["volume_beep"] = ([TALKBACK, BRAILLE], [note(1000, 0, 0.15, 0.08, partials=((1, 1.0),),
                                                   attack=0.005)], 0.16, -6)
    # The screen turned off: a falling slide.
    s["screen_off"] = ([TALKBACK], [note(900, 0, 0.3, 0.12, glide=300, partials=((1, 1.0), (2, 0.2)))],
                       0.32, -6)
    # Circle menu items, one note each from a pentatonic scale.
    for i, pitch in enumerate(PENTATONIC, start=1):
        s[f"radial_menu_{i}"] = ([TALKBACK], [note(pitch, 0, 0.09, 0.035, brightness_decay=30,
                                                   partials=((1, 1.0), (4, 0.15)))], 0.1, -7)

    # Braille display sounds.
    s["calibration_done"] = ([BRAILLE], [note(C6, 0, 0.05, 0.02, glide=E6)], 0.05, -6)
    s["display_connected"] = ([BRAILLE], [note(G5, 0, 0.18, 0.08), note(D6, 0.12, 0.3, 0.12)], 0.42, -6)
    s["display_disconnected"] = ([BRAILLE], [note(D6, 0, 0.18, 0.08), note(G5, 0.12, 0.3, 0.12)], 0.42, -6)
    s["double_beep"] = ([BRAILLE], [note(800, 0, 0.1, 0.05, partials=((1, 1.0),)),
                                    note(800, 0.16, 0.1, 0.05, partials=((1, 1.0),))], 0.27, -6)
    s["turn_on"] = ([BRAILLE], [note(E5, 0, 0.1, 0.05), note(A5, 0.08, 0.14, 0.06)], 0.22, -6)
    s["turn_off"] = ([BRAILLE], [note(A5, 0, 0.1, 0.05), note(E5, 0.08, 0.16, 0.07)], 0.25, -6)
    return s


def main():
    out_dir = sys.argv[1]
    ffmpeg = sys.argv[2] if len(sys.argv) > 2 else None
    os.makedirs(out_dir, exist_ok=True)
    for name, (dirs, parts, length, peak_db) in sounds().items():
        wav = os.path.join(out_dir, name + ".wav")
        write_wav(wav, render(parts, length, peak_db))
        if not ffmpeg:
            continue
        for d in dirs:
            subprocess.run([ffmpeg, "-loglevel", "error", "-y", "-i", wav, "-af", "volume=3dB",
                            "-c:a", "vorbis", "-strict", "-2", "-q:a", "6", "-ac", "2",
                            os.path.join(d, name + ".ogg")], check=True)


if __name__ == "__main__":
    main()
