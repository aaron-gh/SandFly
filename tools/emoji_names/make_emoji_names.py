#!/usr/bin/env python3
"""Writes the emoji names Backtalk speaks into utils/src/main/assets/emoji_names.

Download the Unicode emoji list, the CLDR emoji names and CLDR's locale data first, at the
versions pinned here, so that the files come out the same each time:

    curl -LO https://www.unicode.org/Public/18.0.0/emoji/emoji-test.txt
    npm pack cldr-annotations-full@49.0.0-BETA1 cldr-annotations-derived-full@49.0.0-BETA1 \
        cldr-core@49.0.0-BETA1
    mkdir annotations derived core
    tar xzf cldr-annotations-full-49.*.tgz -C annotations
    tar xzf cldr-annotations-derived-full-49.*.tgz -C derived
    tar xzf cldr-core-49.*.tgz -C core
    python3 make_emoji_names.py emoji-test.txt annotations/package derived/package core/package

The files are made from Emoji 18.0 and CLDR 49.0.0-BETA1, the first CLDR with names for Emoji
18.0. Use the emoji version that matches the CLDR release, so every emoji has a name, and move
to the final CLDR 49 once it is out.

The files are gzipped UTF-8 text. emoji.gz lists the emoji, one per line: the fully qualified
sequences from emoji-test.txt, components such as skin tones included. Every CLDR language with
its own names for at least half the emoji gets a file named by its CLDR locale, such as
en-GB.gz, with a line per emoji:

    <line of the emoji in emoji.gz, from 0>\t<name>

The name is CLDR's text-to-speech name. Derived names cover every sequence: skin tones,
families, flags and keycaps. A regional or other variant, such as en-GB or sr-Latn-BA, starts
with the line

    @parent\t<locale>

and holds only the names that differ from that locale's. Backtalk loads the parent first.
Parents are CLDR's parentLocales, or the locale with its last part dropped. Names CLDR lacks for
a language fall back to its parent, then to English.
"""

import gzip
import json
import os
import sys

OUT_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "..", "utils", "src", "main", "assets", "emoji_names")

# Languages with names for fewer emoji than this would mostly speak English names.
MIN_COVERAGE = 0.5

VARIATION_SELECTOR = "\ufe0f"


def read_emoji(path):
    """Returns the fully qualified emoji and components in emoji-test.txt, in its order."""
    emoji = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("#") or ";" not in line:
                continue
            code_points, rest = line.split(";", 1)
            status = rest.split("#", 1)[0].strip()
            if status in ("fully-qualified", "component"):
                emoji.append("".join(chr(int(c, 16)) for c in code_points.split()))
    return emoji


def read_names(directory, kind, locale):
    """Returns CLDR's text-to-speech names for one locale, by emoji without variation selectors."""
    path = os.path.join(directory, kind, locale, "annotations.json")
    if not os.path.exists(path):
        return {}
    with open(path, encoding="utf-8") as f:
        annotations = json.load(f)[kind].get("annotations", {})
    return {
        key.replace(VARIATION_SELECTOR, ""): value["tts"][0]
        for key, value in annotations.items()
        if value.get("tts")
    }


def read_parents(core_dir):
    """Returns CLDR's parent locales that differ from dropping the last part."""
    path = os.path.join(core_dir, "supplemental", "parentLocales.json")
    with open(path, encoding="utf-8") as f:
        return json.load(f)["supplemental"]["parentLocales"]["parentLocale"]


def write_gzip(name, lines):
    """Writes lines to a gzipped file in OUT_DIR, without a time stamp, so runs match."""
    data = ("\n".join(lines) + "\n").encode("utf-8")
    with open(os.path.join(OUT_DIR, name + ".gz"), "wb") as f:
        with gzip.GzipFile(filename="", mode="wb", fileobj=f, compresslevel=9, mtime=0) as z:
            z.write(data)


def main():
    if len(sys.argv) != 5:
        sys.exit(__doc__)
    emoji_test, annotations_dir, derived_dir, core_dir = sys.argv[1:]
    emoji = read_emoji(emoji_test)
    explicit_parents = read_parents(core_dir)

    locales = sorted(
        (set(os.listdir(os.path.join(annotations_dir, "annotations")))
         | set(os.listdir(os.path.join(derived_dir, "annotationsDerived"))))
        - {"root", "und"})

    def parent_of(locale):
        parent = explicit_parents.get(locale)
        if parent is None and "-" in locale:
            parent = locale.rsplit("-", 1)[0]
        return None if parent in (None, "root", "und") else parent

    own = {}

    def own_names(locale):
        if locale not in own:
            names = read_names(annotations_dir, "annotations", locale)
            names.update(read_names(derived_dir, "annotationsDerived", locale))
            own[locale] = names
        return own[locale]

    resolved = {}

    def resolve(locale):
        """Returns the names a locale has, from itself or its parents, then English."""
        if locale not in resolved:
            parent = parent_of(locale)
            fallback = resolve(parent) if parent else resolve("en") if locale != "en" else {}
            names = {}
            for e in emoji:
                name = own_names(locale).get(e.replace(VARIATION_SELECTOR, "")) or fallback.get(e)
                if name:
                    names[e] = name
            resolved[locale] = names
        return resolved[locale]

    missing = [e for e in emoji if e not in resolve("en")]
    if missing:
        sys.exit("No English name for %d emoji, such as %s. Use the emoji version of this CLDR "
                 "release." % (len(missing), " ".join(missing[:5])))

    def coverage(locale):
        """Returns how many emoji a locale or its parents name, without English."""
        chain = []
        while locale:
            chain.append(own_names(locale))
            locale = parent_of(locale)
        return sum(1 for e in emoji if any(e.replace(VARIATION_SELECTOR, "") in n for n in chain))

    # Languages of their own, and the variants that change their names.
    included = [l for l in locales if parent_of(l) is None and coverage(l) >= MIN_COVERAGE * len(emoji)]

    def included_ancestor(locale):
        parent = parent_of(locale)
        while parent and parent not in included:
            parent = parent_of(parent)
        return parent

    # Parents come before their children, as each child is diffed against an included parent.
    for locale in sorted(locales, key=lambda l: l.count("-")):
        if locale in included or parent_of(locale) is None:
            continue
        ancestor = included_ancestor(locale)
        if ancestor and resolve(locale) != resolve(ancestor):
            included.append(locale)

    os.makedirs(OUT_DIR, exist_ok=True)
    for old in os.listdir(OUT_DIR):
        if old.endswith(".txt") or old.endswith(".gz"):
            os.remove(os.path.join(OUT_DIR, old))
    write_gzip("emoji", emoji)
    index = {e: i for i, e in enumerate(emoji)}
    for locale in included:
        names = resolve(locale)
        ancestor = included_ancestor(locale)
        lines = []
        if ancestor:
            lines.append("@parent\t" + ancestor)
            ancestor_names = resolve(ancestor)
            lines += ["%d\t%s" % (index[e], n) for e, n in names.items()
                      if ancestor_names.get(e) != n]
        else:
            lines += ["%d\t%s" % (index[e], n) for e, n in names.items()]
        write_gzip(locale, lines)
    print("Wrote %d languages, %d emoji" % (len(included), len(emoji)))
    print(" ".join(sorted(included)))


if __name__ == "__main__":
    main()
