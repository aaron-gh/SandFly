# Synthesises SandFly's agent sounds, talkback/src/main/res/raw/agentic_interaction_*.ogg: bell-like
# tones (a few harmonics, soft attack, exponential decay), peak-normalised to -3 dBFS and written as
# 44.1 kHz mono WAV. To rebuild them:
#   python3 tools/agent_sounds.py /tmp/sounds
#   for f in /tmp/sounds/*.wav; do ffmpeg -i "$f" -af volume=-1dB -c:a vorbis -strict -2 -q:a 6 \
#     -ac 2 "talkback/src/main/res/raw/$(basename "${f%.wav}").ogg"; done
# ffmpeg's built-in Vorbis encoder writes only stereo; the -1 dB brings the peak to about -7 dB.
import math, struct, wave, sys, os

RATE = 44100

def tone(freq, start, dur, decay, gain=1.0, harmonics=((1, 1.0), (2, 0.35), (3, 0.15)), bend_cents=0.0, attack=0.006):
    """A note at `start` seconds; `decay` is the time constant of the envelope; `bend_cents`
    glides the pitch linearly over the note."""
    out = {}
    n = int(dur * RATE)
    s0 = int(start * RATE)
    phase = [0.0] * len(harmonics)
    for i in range(n):
        t = i / RATE
        f = freq * 2 ** (bend_cents * (t / dur) / 1200)
        env = (min(1.0, t / attack)) * math.exp(-t / decay)
        # Fade the last 10 ms so nothing clicks.
        env *= min(1.0, (n - i) / (0.01 * RATE))
        v = 0.0
        for k, (h, w) in enumerate(harmonics):
            phase[k] += 2 * math.pi * f * h / RATE
            # Higher harmonics die away faster, like a struck bar.
            v += w * math.sin(phase[k]) * math.exp(-t * (h - 1) * 6)
        out[s0 + i] = v * env * gain
    return out

def mix(notes, length):
    buf = [0.0] * int(length * RATE)
    for note in notes:
        for i, v in note.items():
            if i < len(buf):
                buf[i] += v
    peak = max(abs(x) for x in buf) or 1.0
    scale = 10 ** (-3 / 20) / peak
    return [x * scale for x in buf]

def write(name, samples):
    with wave.open(name, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, x)) * 32767)) for x in samples))

hollow = ((1, 1.0), (3, 0.3), (5, 0.12))   # odd harmonics: a muted, wooden knock
warm = ((1, 1.0), (2, 0.25), (3, 0.2), (4, 0.08))

sounds = {
    # Two quick rising notes: something is starting.
    "agentic_interaction_start": mix([
        tone(784.0, 0.00, 0.30, 0.10),
        tone(1174.7, 0.08, 0.40, 0.14),
    ], 0.50),
    # A rising major chord, last note ringing: done.
    "agentic_interaction_complete": mix([
        tone(1046.5, 0.00, 0.35, 0.10, 0.9),
        tone(1318.5, 0.08, 0.35, 0.10, 0.9),
        tone(1568.0, 0.16, 0.70, 0.22),
    ], 0.90),
    # A rising fourth whose second note bends up: a question.
    "agentic_interaction_ask": mix([
        tone(659.3, 0.00, 0.25, 0.09),
        tone(880.0, 0.12, 0.45, 0.16, bend_cents=60),
    ], 0.62),
    # Two firm, low, muted knocks on the same note: stop.
    "agentic_interaction_sensitive_stop": mix([
        tone(392.0, 0.00, 0.22, 0.06, harmonics=hollow),
        tone(392.0, 0.16, 0.30, 0.07, harmonics=hollow),
    ], 0.50),
    # Two falling notes, the second sliding further down: it didn't work.
    "agentic_interaction_fail": mix([
        tone(523.3, 0.00, 0.28, 0.10, harmonics=warm),
        tone(392.0, 0.18, 0.55, 0.18, harmonics=warm, bend_cents=-120),
    ], 0.78),
}
out = sys.argv[1]
for name, samples in sounds.items():
    write(os.path.join(out, name + ".wav"), samples)
    print(name, f"{len(samples) / RATE:.2f} s")
