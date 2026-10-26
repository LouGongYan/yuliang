# -*- coding: utf-8 -*-
"""Build launcher / splash assets from the source icon.

Source: ../../icon.png (a full-bleed square icon with a white glyph on mint).
Output: app/src/main/res/mipmap-*/... and drawable-nodpi/splash_icon.png
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "icon.png"
RES = ROOT / "app" / "src" / "main" / "res"

DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}


def rounded_mask(size: int, radius_ratio: float) -> Image.Image:
    mask = Image.new("L", (size * 4, size * 4), 0)
    d = ImageDraw.Draw(mask)
    r = int(size * 4 * radius_ratio)
    d.rounded_rectangle([0, 0, size * 4 - 1, size * 4 - 1], radius=r, fill=255)
    return mask.resize((size, size), Image.LANCZOS)


def circle_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size * 4, size * 4), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse([0, 0, size * 4 - 1, size * 4 - 1], fill=255)
    return mask.resize((size, size), Image.LANCZOS)


def extract_glyph(src: Image.Image) -> Image.Image:
    """Color-key the mint background away and keep the white mark."""
    src = src.convert("RGBA")
    px = src.load()
    w, h = src.size
    bg = px[4, 4][:3]
    out = Image.new("RGBA", (w, h), (255, 255, 255, 0))
    op = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            # Per-channel fraction of white mixed over the known background.
            fracs = []
            for c, bc in ((r, bg[0]), (g, bg[1]), (b, bg[2])):
                denom = max(1, 255 - bc)
                fracs.append((c - bc) / denom)
            f = max(0.0, min(1.0, sum(fracs) / len(fracs)))
            alpha = int(round(255 * f))
            if alpha < 8:
                alpha = 0
            elif alpha > 247:
                alpha = 255
            op[x, y] = (255, 255, 255, alpha)
    # Trim to the visible mark, leaving a small breathing border.
    bbox = out.getbbox()
    if bbox:
        out = out.crop(bbox)
    return out


def fit_on_canvas(glyph: Image.Image, canvas: int, target: int) -> Image.Image:
    """Fit the glyph inside target px without distortion, centered on canvas."""
    w, h = glyph.size
    scale = target / max(w, h)
    nw, nh = max(1, int(round(w * scale))), max(1, int(round(h * scale)))
    glyph = glyph.resize((nw, nh), Image.LANCZOS)
    layer = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    layer.alpha_composite(glyph, ((canvas - nw) // 2, (canvas - nh) // 2))
    return layer


def main() -> None:
    if not SRC.exists():
        raise SystemExit(f"source icon not found: {SRC}")
    src = Image.open(SRC).convert("RGBA")
    glyph = extract_glyph(src)
    print("source", src.size, "glyph", glyph.size)

    # Legacy launcher icons (rounded square), and round icons.
    for density, scale in DENSITIES.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)

        legacy_px = int(round(48 * scale))
        legacy = src.resize((legacy_px, legacy_px), Image.LANCZOS)
        legacy.putalpha(rounded_mask(legacy_px, 0.23))
        legacy.save(folder / "ic_launcher.png", optimize=True)

        rnd = src.resize((legacy_px, legacy_px), Image.LANCZOS)
        rnd.putalpha(circle_mask(legacy_px))
        rnd.save(folder / "ic_launcher_round.png", optimize=True)

        # Adaptive foreground: 108dp canvas, mark inside the 66dp safe zone.
        fg_px = int(round(108 * scale))
        fg = fit_on_canvas(glyph, fg_px, int(round(66 * scale)))
        fg.save(folder / "ic_launcher_foreground.png", optimize=True)

    # Monochrome layer (Android 13 themed icons), and splash artwork.
    mono_dir = RES / "drawable-nodpi"
    mono_dir.mkdir(parents=True, exist_ok=True)
    fit_on_canvas(glyph, 432, 280).save(mono_dir / "ic_launcher_monochrome.png", optimize=True)

    splash_size = 512
    splash = Image.new("RGBA", (splash_size, splash_size), (0, 0, 0, 0))
    d = ImageDraw.Draw(splash)
    # Soft mint disc, slightly inset so the system splash mask never clips it.
    d.ellipse([16, 16, splash_size - 16, splash_size - 16], fill=(141, 231, 220, 255))
    mark = fit_on_canvas(glyph, splash_size, 270)
    splash.alpha_composite(mark)
    splash.save(mono_dir / "splash_icon.png", optimize=True)

    # A small crisp preview for the README.
    preview = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    p = src.resize((512, 512), Image.LANCZOS)
    p.putalpha(rounded_mask(512, 0.23))
    preview.alpha_composite(p)
    preview.save(ROOT / "app" / "icon-preview.png", optimize=True)
    print("icons written")


if __name__ == "__main__":
    main()
