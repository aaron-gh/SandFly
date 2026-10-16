#!/usr/bin/env python3
"""Packs the MIT KEMAR compact HRTFs into utils/src/main/res/raw/hrtf_kemar.bin.

Download and unpack the measurements first:

    curl -LO https://sound.media.mit.edu/resources/KEMAR/compact.tar.Z
    tar xzf compact.tar.Z
    python3 make_hrtf.py compact

Only the elevations control sounds use, -40 to +10 degrees, are kept. Each compact file holds a
left and right 128 point response at 44.1 kHz for a source on the right; sources on the left use
the same responses with the ears swapped.

Output, all little-endian:
    char[4]  "HRTF"
    int16    taps per response (128)
    int16    number of elevation rings
    float32  scale that turns the stored samples into filter coefficients
    per ring:
        int16  elevation in degrees
        int16  number of azimuths
        per azimuth, in increasing order from 0 (front) to 180 (behind):
            int16       azimuth in degrees
            int16[taps] left ear
            int16[taps] right ear
"""

import math
import os
import re
import struct
import sys

ELEVATIONS = [-40, -30, -20, -10, 0, 10]
TAPS = 128
OUT = os.path.join(
    os.path.dirname(__file__), "..", "..", "utils", "src", "main", "res", "raw", "hrtf_kemar.bin"
)


def read_ring(compact_dir, elevation):
    ring_dir = os.path.join(compact_dir, f"elev{elevation}")
    entries = []
    for name in os.listdir(ring_dir):
        match = re.fullmatch(r"H-?\d+e(\d+)a\.dat", name)
        if not match:
            continue
        with open(os.path.join(ring_dir, name), "rb") as f:
            samples = struct.unpack(f">{TAPS * 2}h", f.read())
        entries.append((int(match.group(1)), samples[0::2], samples[1::2]))
    return sorted(entries)


def main():
    compact_dir = sys.argv[1] if len(sys.argv) > 1 else "compact"
    rings = [(elevation, read_ring(compact_dir, elevation)) for elevation in ELEVATIONS]

    # Scale so the loudest ear response has unit energy: a sound that passes through it keeps
    # about its own level, and the other responses are quieter in proportion.
    loudest = max(
        math.sqrt(sum(s * s for s in ear))
        for _, entries in rings
        for _, left, right in entries
        for ear in (left, right)
    )

    with open(OUT, "wb") as out:
        out.write(b"HRTF")
        out.write(struct.pack("<hhf", TAPS, len(rings), 1.0 / loudest))
        for elevation, entries in rings:
            out.write(struct.pack("<hh", elevation, len(entries)))
            for azimuth, left, right in entries:
                out.write(struct.pack("<h", azimuth))
                out.write(struct.pack(f"<{TAPS}h", *left))
                out.write(struct.pack(f"<{TAPS}h", *right))
    print(f"Wrote {os.path.normpath(OUT)}: {sum(len(e) for _, e in rings)} positions")


if __name__ == "__main__":
    main()
