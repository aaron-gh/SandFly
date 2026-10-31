# Synthesises all of SandFly's built-in sounds and writes them where the app reads them: focus,
# clicks, scrolling, windows, list chimes, links, formatting, typos, browse mode, loading, volume,
# screen off, the circle menu, edge gestures and the agent in talkback and utils, and the braille
# display sounds in braille/common.
#
# Each sound is a 48 kHz, 16-bit mono WAV, peak-normalised to its own level and dithered. WAV is
# lossless, and Android plays it everywhere; ffmpeg's built-in Vorbis encoder, used before, added
# audible noise. Run from the repository root:
#   python3 tools/sounds.py
import math, os, random, struct, sys, wave

RATE = 48000

TALKBACK = "talkback/src/main/res/raw"
UTILS = "utils/src/main/res/raw"
BRAILLE = "braille/common/src/phone/res/raw"

# Partials, as (multiple of the pitch, level).
BELL = ((1, 1.0), (2, 0.3), (3, 0.1))
PURE = ((1, 1.0),)
HOLLOW = ((1, 1.0), (3, 0.3), (5, 0.12))  # Odd harmonics: a muted, wooden knock.
WARM = ((1, 1.0), (2, 0.25), (3, 0.2), (4, 0.08))


def note(freq, start, dur, decay, gain=1.0, glide=None, partials=BELL, attack=0.004,
         brightness_decay=6.0, detune_cents=4.0):
    """A struck tone at `start` seconds. `decay` is the envelope's time constant; `glide` is the
    frequency at the end of the note, reached along an exponential curve; higher partials die away
    faster, by `brightness_decay` per second per partial above the first. A second voice, quieter
    and `detune_cents` higher, gives the note some warmth."""
    end_freq = glide or freq
    n = int(dur * RATE)
    s0 = int(start * RATE)
    fade = int(0.006 * RATE)
    voices = [(1.0, 1.0)] + ([(2 ** (detune_cents / 1200), 0.35)] if detune_cents else [])
    phases = [[0.0] * len(partials) for _ in voices]
    norm = sum(level for _, level in voices)
    out = {}
    for i in range(n):
        t = i / RATE
        f = freq * (end_freq / freq) ** (t / dur)
        # A raised-cosine attack, an exponential decay and a raised-cosine fade at the end.
        env = 0.5 - 0.5 * math.cos(math.pi * t / attack) if t < attack else 1.0
        env *= math.exp(-t / decay)
        if n - i < fade:
            env *= 0.5 - 0.5 * math.cos(math.pi * (n - i) / fade)
        v = 0.0
        for vi, (ratio, level) in enumerate(voices):
            for k, (h, w) in enumerate(partials):
                phases[vi][k] += 2 * math.pi * f * ratio * h / RATE
                v += level * w * math.sin(phases[vi][k]) * math.exp(-t * (h - 1) * brightness_decay)
        out[s0 + i] = v / norm * env * gain
    return out


def click(start, dur, decay, gain=1.0, tone_hz=None, cutoff_hz=4000, seed=1):
    """A short burst of noise through a two-pole low-pass at `cutoff_hz`, with an optional tone,
    for the body of a tick."""
    rng = random.Random(seed)
    n = int(dur * RATE)
    s0 = int(start * RATE)
    # Butterworth low-pass biquad.
    w0 = 2 * math.pi * cutoff_hz / RATE
    alpha = math.sin(w0) / math.sqrt(2)
    cos_w0 = math.cos(w0)
    a0 = 1 + alpha
    b0 = b2 = (1 - cos_w0) / 2 / a0
    b1 = (1 - cos_w0) / a0
    a1 = -2 * cos_w0 / a0
    a2 = (1 - alpha) / a0
    x1 = x2 = y1 = y2 = 0.0
    out = {}
    for i in range(n):
        t = i / RATE
        x = rng.uniform(-1, 1)
        y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1, y2, y1 = x1, x, y1, y
        v = 2 * y
        if tone_hz:
            v = 0.5 * v + math.sin(2 * math.pi * tone_hz * t)
        env = (0.5 - 0.5 * math.cos(math.pi * t / 0.0008)) if t < 0.0008 else 1.0
        env *= math.exp(-t / decay)
        if n - i < 48:
            env *= (n - i) / 48
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
    """Writes 16-bit samples with triangular dither, so that quiet tails fade out as noise rather
    than as distortion."""
    rng = random.Random(0)
    frames = bytearray()
    for x in samples:
        dithered = x * 32767 + rng.random() - rng.random()
        frames += struct.pack("<h", int(max(-32768, min(32767, round(dithered)))))
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(bytes(frames))


# Pitches, in Hz.
G4, A4, C5, D5, E5, G5, A5, B5 = 392.0, 440.0, 523.25, 587.33, 659.26, 783.99, 880.0, 987.77
C6, D6, E6, G6, A6 = 1046.5, 1174.66, 1318.51, 1567.98, 1760.0
PENTATONIC = [C5, D5, E5, G5, A5, C6, D6, E6]


def sounds():
    """Each sound by file name, with the directories it goes in, its parts, length and peak."""
    s = {}

    # Focus moving to an item: a soft, short wooden tap. Items that can be activated are brighter.
    s["focus"] = ([TALKBACK], [click(0, 0.03, 0.004, 0.3, cutoff_hz=2500, seed=2),
                               note(700, 0, 0.05, 0.014, partials=((1, 1.0), (2.4, 0.2)))], 0.06, -6)
    s["focus_actionable"] = ([TALKBACK], [click(0, 0.03, 0.004, 0.3, cutoff_hz=3000, seed=3),
                                          note(950, 0, 0.06, 0.016, partials=((1, 1.0), (2, 0.35)))],
                             0.07, -6)
    # A finger moving onto an item or empty space while exploring.
    s["view_entered"] = ([TALKBACK], [note(700, 0, 0.04, 0.01, partials=PURE)], 0.05, -10)
    # Something was clicked.
    s["tick"] = ([TALKBACK], [click(0, 0.02, 0.003, 1.0, tone_hz=2600, cutoff_hz=6000, seed=4)],
                 0.03, -5)
    # Something was long-pressed: two taps, the second lower.
    s["long_clicked"] = ([TALKBACK], [
        click(0, 0.02, 0.003, 1.0, tone_hz=2400, cutoff_hz=6000, seed=5),
        click(0.08, 0.02, 0.003, 1.0, tone_hz=1700, cutoff_hz=5000, seed=6)], 0.11, -5)
    # Moving into and out of a scrolling list: a quick upward and downward slide.
    for name in ("chime_up", "chime_down"):
        up = name == "chime_up"
        s[name] = ([TALKBACK, BRAILLE], [note(420 if up else 640, 0, 0.05, 0.03,
                                              glide=640 if up else 420,
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
                                   click(0, 0.12, 0.04, 0.15, cutoff_hz=2000, seed=7)], 0.2, -7)
    # A link: two bright quick notes.
    s["hyperlink"] = ([UTILS], [note(E6, 0, 0.06, 0.03), note(A6, 0.045, 0.08, 0.04)], 0.13, -7)
    # Formatted text, such as bold: a low soft double note.
    s["formatting"] = ([UTILS], [note(A4, 0, 0.14, 0.06, partials=((1, 1.0), (3, 0.2))),
                                 note(660, 0.04, 0.14, 0.06, partials=((1, 0.8),))], 0.18, -7)
    # A spelling mistake: two close low tones that beat against each other.
    s["typo"] = ([TALKBACK], [
        note(330, 0, 0.22, 0.09, partials=((1, 1.0), (2, 0.4), (3, 0.2)), detune_cents=0),
        note(349, 0, 0.22, 0.09, partials=((1, 1.0), (2, 0.4)), detune_cents=0)], 0.24, -5)
    # Browse mode on: three fast rising notes. Off: the same falling.
    s["browse_mode_on_v4_2"] = ([TALKBACK], [note(G5, 0, 0.09, 0.04), note(C6, 0.06, 0.09, 0.04),
                                             note(E6, 0.12, 0.14, 0.06)], 0.26, -5)
    s["browse_mode_off_v4_2"] = ([TALKBACK], [note(E6, 0, 0.09, 0.04), note(C6, 0.06, 0.09, 0.04),
                                              note(G5, 0.12, 0.14, 0.06)], 0.26, -5)
    # Waiting, such as for an image description; repeats every 3 seconds.
    s["loading"] = ([TALKBACK], [note(A5, 0, 0.35, 0.12, partials=((1, 1.0), (2, 0.15))),
                                 note(E5, 0.25, 0.45, 0.16, partials=((1, 1.0), (2, 0.15)))], 0.7, -9)
    # A volume change, and the braille display's beep.
    s["volume_beep"] = ([TALKBACK, BRAILLE], [note(1000, 0, 0.15, 0.08, partials=PURE, attack=0.005,
                                                   detune_cents=0)], 0.16, -6)
    # The screen turned off: a falling slide.
    s["screen_off"] = ([TALKBACK], [note(900, 0, 0.3, 0.12, glide=300, partials=((1, 1.0), (2, 0.2)))],
                       0.32, -6)
    # Circle menu items, one note each from a pentatonic scale.
    for i, pitch in enumerate(PENTATONIC, start=1):
        s[f"radial_menu_{i}"] = ([TALKBACK], [note(pitch, 0, 0.09, 0.035, brightness_decay=30,
                                                   partials=((1, 1.0), (4, 0.15)))], 0.1, -7)
    # Edge gestures: a low tick at the edge, a higher tone at the first click, two rising tones at
    # the second.
    edge = ((1, 1.0), (2, 0.25))
    s["edge_gesture_bottom"] = ([TALKBACK], [note(C5, 0, 0.045, 0.01, partials=edge)], 0.055, -8)
    s["edge_gesture_middle"] = ([TALKBACK], [note(G5, 0, 0.07, 0.0156, partials=edge)], 0.08, -5)
    s["edge_gesture_top"] = ([TALKBACK], [note(C6, 0, 0.06, 0.0133, partials=edge),
                                          note(E6, 0.05, 0.08, 0.0178, partials=edge)], 0.14, -4)

    # The SandFly Pro agent: start, done, a question, a safety stop and a failure.
    s["agentic_interaction_start"] = ([TALKBACK], [note(G5, 0, 0.3, 0.1, attack=0.006),
                                                   note(D6, 0.08, 0.4, 0.14, attack=0.006)], 0.5, -7)
    s["agentic_interaction_complete"] = ([TALKBACK], [
        note(C6, 0, 0.35, 0.1, 0.9, attack=0.006),
        note(E6, 0.08, 0.35, 0.1, 0.9, attack=0.006),
        note(G6, 0.16, 0.7, 0.22, attack=0.006)], 0.9, -7)
    s["agentic_interaction_ask"] = ([TALKBACK], [
        note(E5, 0, 0.25, 0.09, attack=0.006),
        note(A5, 0.12, 0.45, 0.16, glide=A5 * 2 ** (60 / 1200), attack=0.006)], 0.62, -7)
    s["agentic_interaction_sensitive_stop"] = ([TALKBACK], [
        note(G4, 0, 0.22, 0.06, partials=HOLLOW, attack=0.006),
        note(G4, 0.16, 0.3, 0.07, partials=HOLLOW, attack=0.006)], 0.5, -7)
    s["agentic_interaction_fail"] = ([TALKBACK], [
        note(C5, 0, 0.28, 0.1, partials=WARM, attack=0.006),
        note(G4, 0.18, 0.55, 0.18, glide=G4 * 2 ** (-120 / 1200), partials=WARM, attack=0.006)],
        0.78, -7)

    # Braille display sounds.
    s["calibration_done"] = ([BRAILLE], [note(C6, 0, 0.05, 0.02, glide=E6)], 0.05, -6)
    s["display_connected"] = ([BRAILLE], [note(G5, 0, 0.18, 0.08), note(D6, 0.12, 0.3, 0.12)],
                              0.42, -6)
    s["display_disconnected"] = ([BRAILLE], [note(D6, 0, 0.18, 0.08), note(G5, 0.12, 0.3, 0.12)],
                                 0.42, -6)
    s["double_beep"] = ([BRAILLE], [note(800, 0, 0.1, 0.05, partials=PURE, detune_cents=0),
                                    note(800, 0.16, 0.1, 0.05, partials=PURE, detune_cents=0)],
                        0.27, -6)
    s["turn_on"] = ([BRAILLE], [note(E5, 0, 0.1, 0.05), note(A5, 0.08, 0.14, 0.06)], 0.22, -6)
    s["turn_off"] = ([BRAILLE], [note(A5, 0, 0.1, 0.05), note(E5, 0.08, 0.16, 0.07)], 0.25, -6)
    return s


def main():
    for name, (dirs, parts, length, peak_db) in sounds().items():
        samples = render(parts, length, peak_db)
        for d in dirs:
            for ext in (".ogg", ".mid"):
                old = os.path.join(d, name + ext)
                if os.path.exists(old):
                    os.remove(old)
            write_wav(os.path.join(d, name + ".wav"), samples)


if __name__ == "__main__":
    sys.exit(main())
