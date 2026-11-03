"""SandFly's icon: a stylised sandfly with S and F in braille in its eyes.

python3 tools/icon/make_icon.py writes the adaptive icon's foreground for SandFly and the SandFly
add-on, and the Google Play icon and feature graphic as SVG in docs/play-store. Render those with
render.swift (macOS): swift tools/icon/render.swift in.svg out.png width height."""
import pathlib
BG = "#14324B"; ADDON_BG = "#1E4D3A"; BODY = "#E3B36A"; LIGHT = "#F2D29B"; EYE = "#FFF3D6"

def ellipse(cx, cy, rx, ry):
    return f"M{cx-rx:g},{cy} a{rx:g},{ry:g} 0 1,0 {2*rx:g},0 a{rx:g},{ry:g} 0 1,0 {-2*rx:g},0 Z"

# Each shape: (path, fill, fill_alpha, stroke, stroke_width, stroke_alpha)
shapes = []
def add(d, fill=None, fa=1, stroke=None, sw=0, sa=1):
    shapes.append((d, fill, fa, stroke, sw, sa))

for d in ["M48,50 L40,43 L33,36", "M47,55 L36,53 L26,58", "M48,59 L42,69 L37,81",
          "M60,50 L68,43 L75,36", "M61,55 L72,53 L82,58", "M60,59 L66,69 L71,81",
          "M51,30 Q47.5,26 45,23", "M57,30 Q60.5,26 63,23"]:
    add(d, stroke=BODY, sw=1.7)
add("M50,51 C39,54 31,66 30,77 C38,71 46,64 52,59 Z", LIGHT, 0.38, LIGHT, 0.8)
add("M58,51 C69,54 77,66 78,77 C70,71 62,64 56,59 Z", LIGHT, 0.38, LIGHT, 0.8)
add("M50,54 C43,59 36,67 32,74", stroke=LIGHT, sw=0.5, sa=0.9)
add("M58,54 C65,59 72,67 76,74", stroke=LIGHT, sw=0.5, sa=0.9)
add(ellipse(54, 72, 5.5, 11), BODY)
for d in ["M49.2,68 H58.8", "M48.6,73 H59.4", "M49.3,78 H58.7"]:
    add(d, stroke=BG, sw=0.9, sa=0.5)
add(ellipse(54, 54, 8, 8.5), BODY)
add(ellipse(45, 37, 8.6, 10.4), EYE, 1, BODY, 1.1)
add(ellipse(63, 37, 8.6, 10.4), EYE, 1, BODY, 1.1)
# Braille dot positions 1 to 3 down the left column, 4 to 6 down the right.
POS = {1: (-2.4, -4.8), 2: (-2.4, 0), 3: (-2.4, 4.8), 4: (2.4, -4.8), 5: (2.4, 0), 6: (2.4, 4.8)}
for cx, raised in [(45, {2, 3, 4}), (63, {1, 2, 4})]:  # S, then F
    for dot, (dx, dy) in POS.items():
        add(ellipse(cx + dx, 37 + dy, 1.9, 1.9), BG, 1 if dot in raised else 0.16)

def vector(bg_color=None):
    out = ['<?xml version="1.0" encoding="utf-8"?>',
           '<!-- SandFly: a sandfly with S and F in braille in its eyes. Made by tools/make_icon.py. -->',
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    android:width="108dp" android:height="108dp"',
           '    android:viewportWidth="108" android:viewportHeight="108">']
    for d, fill, fa, stroke, sw, sa in shapes:
        attrs = [f'android:pathData="{d}"']
        if fill: attrs.append(f'android:fillColor="{fill}"')
        if fill and fa != 1: attrs.append(f'android:fillAlpha="{fa}"')
        if stroke:
            attrs += [f'android:strokeColor="{stroke}"', f'android:strokeWidth="{sw}"',
                      'android:strokeLineCap="round"', 'android:strokeLineJoin="round"']
            if sa != 1: attrs.append(f'android:strokeAlpha="{sa}"')
        out.append('  <path ' + '\n      '.join(attrs) + ' />')
    out.append('</vector>')
    return '\n'.join(out) + '\n'

def svg_shapes():
    parts = []
    for d, fill, fa, stroke, sw, sa in shapes:
        a = [f'd="{d}"', f'fill="{fill or "none"}"']
        if fill and fa != 1: a.append(f'fill-opacity="{fa}"')
        if stroke: a += [f'stroke="{stroke}"', f'stroke-width="{sw}"', 'stroke-linecap="round"', 'stroke-linejoin="round"']
        if stroke and sa != 1: a.append(f'stroke-opacity="{sa}"')
        parts.append('<path ' + ' '.join(a) + '/>')
    return ''.join(parts)

def play_icon(bg):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="512" height="512">'
            f'<rect width="108" height="108" fill="{bg}"/>{svg_shapes()}</svg>')

def feature_graphic():
    # 1024 x 500: the fly on the left, the name on the right.
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 500" width="1024" height="500">'
            f'<rect width="1024" height="500" fill="{BG}"/>'
            f'<g transform="translate(60,10) scale(4.5)">{svg_shapes()}</g>'
            f'<text x="560" y="270" font-family="Helvetica, Arial, sans-serif" font-size="120" font-weight="bold" fill="{BODY}">SandFly</text>'
            f'</svg>')

if __name__ == "__main__":
    root = pathlib.Path(__file__).resolve().parents[2]
    for res in ["talkback/src/main/res", "addon/src/main/res"]:
        (root / res / "drawable" / "sandfly_icon_foreground.xml").write_text(vector())
    store = root / "docs" / "play-store"
    store.mkdir(parents=True, exist_ok=True)
    (store / "play_icon.svg").write_text(play_icon(BG))
    (store / "addon_icon.svg").write_text(play_icon(ADDON_BG))
    (store / "feature_graphic.svg").write_text(feature_graphic())
