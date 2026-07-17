#!/usr/bin/env python3
"""Crop full-window IDE captures to the NightVision tool-window panel.

Takes the 2x (retina) full-window PNGs produced by the capture step (see
README.md) and crops each to the panel with one fixed box, so every shot
shares an aspect ratio (JetBrains rejects mixed aspect ratios in a listing).

Usage:
    crop.py <src-dir> <out-dir> [--box left,top,right,bottom] [--glob PATTERN]

The default box is calibrated to a capture taken with the IDE window at
logical (56, 54), size 1400x908, on a 2x display. Recompute it if you
capture at a different geometry (see README.md, "Crop").
"""

import argparse
import sys
from pathlib import Path

try:
    from PIL import Image
except ModuleNotFoundError:
    sys.exit("Pillow is required: python3 -m venv venv && venv/bin/pip install Pillow")

# left, top, right, bottom in native (2x) pixels.
DEFAULT_BOX = (1799, 171, 2730, 1490)


def parse_box(text):
    parts = [int(p) for p in text.split(",")]
    if len(parts) != 4:
        raise argparse.ArgumentTypeError("box must be left,top,right,bottom")
    return tuple(parts)


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("src_dir", type=Path, help="directory of full-window PNGs")
    ap.add_argument("out_dir", type=Path, help="directory for cropped PNGs")
    ap.add_argument("--box", type=parse_box, default=DEFAULT_BOX,
                    help="crop box as left,top,right,bottom (native px)")
    ap.add_argument("--glob", default="*.png", help="which files to crop")
    args = ap.parse_args()

    left, top, right, bottom = args.box
    args.out_dir.mkdir(parents=True, exist_ok=True)

    sources = sorted(args.src_dir.glob(args.glob))
    if not sources:
        sys.exit(f"no files matching {args.glob} in {args.src_dir}")

    for src in sources:
        im = Image.open(src)
        if right > im.width or bottom > im.height:
            print(f"skip {src.name}: box {args.box} exceeds image {im.size}")
            continue
        out = args.out_dir / src.name
        im.crop(args.box).save(out)
        print(f"{src.name} -> {out}  ({right - left}x{bottom - top})")


if __name__ == "__main__":
    main()
