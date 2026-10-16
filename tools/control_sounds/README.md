# Control sounds

Backtalk has no control sounds of its own. Each kind of control plays the sound the sound theme in
use gives it, and a control without one plays the usual focus sound. See [themes.md](../../themes.md)
for sound themes.

`make_hrtf.py` packs the head-related transfer functions that play the sounds in 3D into
`utils/src/main/res/raw/hrtf_kemar.bin`. Its comments say how to download the measurements.

The HRTFs are the compact set of the MIT Media Lab KEMAR measurements, which Unspoken also used:

> Bill Gardner and Keith Martin, "HRTF Measurements of a KEMAR Dummy-Head Microphone", MIT Media
> Lab Perceptual Computing Technical Report #280, 1994.
> https://sound.media.mit.edu/resources/KEMAR.html

The report says: "This HRTF data is Copyright 1994 by the MIT Media Lab. It is provided without
any usage restrictions. We request that you cite the authors when using this data for research or
commercial applications."
