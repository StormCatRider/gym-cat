#!/usr/bin/env python3
"""
PLAIN-ENGLISH GUIDE — tools/images/prepare_cats.py
Gets your ChatGPT cat PNGs ready for the app:
  1. trims the empty (transparent) border around the drawing
  2. centers it on a SQUARE transparent canvas with a 6% margin
     (so every cat looks the same size in the app's square spots)
  3. shrinks it to 512 × 512 px (big enough for any phone screen spot in the app)
  4. saves an optimized PNG with the exact name the app looks for.
     If a 256-color version looks the same (average difference ≤ 1 out of 255 on the
     app's light background), that smaller version is saved instead — about 8× smaller.

The input files must already have their final names (cat_17_squat.png …).
Run:  python3 tools/images/prepare_cats.py  <folder with named PNGs>
Output goes to app/src/main/res/drawable-nodpi/ (existing files with the same name are replaced).
Needs Pillow (pip install pillow).
"""
import io, os, re, sys
from PIL import Image, ImageChops, ImageStat

SIZE = 512          # final width = height in pixels
MARGIN = 0.06       # 6% empty space on each side
MAX_DIFF = 1.0      # palette version allowed only if this close to the original (0–255 scale)
APP_BG = (241, 247, 253, 255)   # light background color of the app (blue palette)

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")


def prepare(src_path, dst_path):
    im = Image.open(src_path).convert("RGBA")
    # Fully transparent pixels can hold leftover colors (ChatGPT leaves dark ones).
    # Clear them so shrinking never creates a dark edge around the cat.
    r, g, b, a = im.split()
    mask = a.point(lambda v: 255 if v > 0 else 0)
    black = Image.new("RGBA", im.size, (0, 0, 0, 0))
    im = Image.composite(im, black, mask)

    box = a.point(lambda v: 255 if v > 8 else 0).getbbox()      # where the drawing is
    if box is None:
        raise ValueError(f"{os.path.basename(src_path)} is completely empty")
    im = im.crop(box)

    # Square canvas: longest side + margins.
    side = round(max(im.size) / (1 - 2 * MARGIN))
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(im, ((side - im.width) // 2, (side - im.height) // 2))

    # "RGBa" = premultiplied alpha → smooth, clean edges when shrinking.
    small = canvas.convert("RGBa").resize((SIZE, SIZE), Image.LANCZOS).convert("RGBA")

    # Try the 256-color version and keep it only if it looks the same.
    pal = small.quantize(colors=256, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.NONE)
    buf = io.BytesIO(); pal.save(buf, "PNG", optimize=True)
    back = Image.open(io.BytesIO(buf.getvalue())).convert("RGBA")
    bg = Image.new("RGBA", small.size, APP_BG)
    diff = ImageChops.difference(Image.alpha_composite(bg, small).convert("RGB"),
                                 Image.alpha_composite(bg, back).convert("RGB"))
    if max(ImageStat.Stat(diff).mean) <= MAX_DIFF:
        open(dst_path, "wb").write(buf.getvalue())
        return "palette"
    small.save(dst_path, optimize=True)
    return "full"


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    src = sys.argv[1]
    os.makedirs(OUT, exist_ok=True)
    done = 0; full = []
    for f in sorted(os.listdir(src)):
        name, ext = os.path.splitext(f)
        if ext.lower() != ".png":
            continue
        if not re.fullmatch(r"cat_\d\d_[a-z0-9_]+", name):
            print(f"skipped (name not like cat_17_squat): {f}")
            continue
        if prepare(os.path.join(src, f), os.path.join(OUT, name + ".png")) == "full":
            full.append(name)
        done += 1
    total = sum(os.path.getsize(os.path.join(OUT, x)) for x in os.listdir(OUT)) / 1e6
    print(f"prepared {done} images → {os.path.relpath(OUT, ROOT)}  (folder total {total:.2f} MB)")
    if full:
        print("kept at full quality (palette would change them):", ", ".join(full))


if __name__ == "__main__":
    main()
