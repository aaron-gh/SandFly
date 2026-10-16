# Emoji names

When Verbosity > Emoji is set to "Read by Backtalk", Backtalk replaces each emoji with its name
before the speech engine gets the text. The names are the text-to-speech names from the
[Unicode CLDR](https://cldr.unicode.org/) emoji annotations, in every CLDR language that names
at least half the emoji (138 languages and regional variants). They cover every emoji sequence:
skin tones, families and other joined sequences, flags and keycaps.

`make_emoji_names.py` writes them into `utils/src/main/assets/emoji_names`, as one gzipped list
of the emoji and a gzipped file of names for each language. The Android Gradle plugin unzips them
into the APK, which compresses them again. The script's comments say how to download the data, at
pinned versions so that the files come out the same each time, and describe the files. The names
are from Emoji 18.0 and CLDR 49.0.0-BETA1, the first CLDR with names for Emoji 18.0; the final
CLDR 49 isn't out yet. To update to a new CLDR release, download that release and the emoji
version it names, change the versions in the script, and run it again. Emoji newer than the data
are left to the speech engine.

The data is copyright Unicode, Inc. and is used under the Unicode License v3, which is in
`utils/src/main/assets/emoji_names/LICENSE`.
