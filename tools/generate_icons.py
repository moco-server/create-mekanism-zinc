"""Render zinc item icons from a validated 16x16 character-grid artwork file."""

import argparse
import json
from pathlib import Path
import struct
import sys
import zlib


PROJECT_ROOT = Path(__file__).resolve().parents[1]
ICON_IDS = (
    "dust_zinc",
    "dirty_dust_zinc",
    "clump_zinc",
    "shard_zinc",
    "crystal_zinc",
)
DEFAULT_SOURCE = PROJECT_ROOT / "artwork/zinc-v1.json"
DEFAULT_OUTPUT = PROJECT_ROOT / "common/src/main/resources/assets/create_mekanism_zinc/textures/item"
SIZE = 16


class ArtworkError(ValueError):
    """Raised when the artwork JSON does not match the supported schema."""


def encode_png(pixels):
    """Encode a validated RGBA pixel grid as a 16x16 PNG."""

    def chunk(kind, data):
        crc = zlib.crc32(kind + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", crc)

    rows = b"".join(b"\0" + b"".join(bytes(pixel) for pixel in row) for row in pixels)
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">2I5B", SIZE, SIZE, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(rows, 9))
    data += chunk(b"IEND", b"")
    return data


def validate_artwork(document):
    """Validate all input and return all five expanded pixel grids."""
    if not isinstance(document, dict):
        raise ArtworkError("root must be a JSON object")
    if type(document.get("version")) is not int or document["version"] != 1:
        raise ArtworkError("version must be integer 1")
    if type(document.get("size")) is not int or document["size"] != SIZE:
        raise ArtworkError("size must be integer 16")

    palette = document.get("palette")
    if not isinstance(palette, dict) or not palette:
        raise ArtworkError("palette must be a non-empty object")
    rgba_palette = {}
    for symbol, channels in palette.items():
        if not isinstance(symbol, str) or len(symbol) != 1:
            raise ArtworkError(f"palette key {symbol!r} must be one character")
        if (not isinstance(channels, list) or len(channels) != 4 or
                any(type(channel) is not int or not 0 <= channel <= 255 for channel in channels)):
            raise ArtworkError(f"palette entry {symbol!r} must contain four integer channels from 0 to 255")
        rgba_palette[symbol] = tuple(channels)

    icons = document.get("icons")
    if not isinstance(icons, dict):
        raise ArtworkError("icons must be an object")
    actual_ids = set(icons)
    expected_ids = set(ICON_IDS)
    if actual_ids != expected_ids:
        missing = sorted(expected_ids - actual_ids)
        extra = sorted(actual_ids - expected_ids)
        raise ArtworkError(f"icons must contain exactly the five required IDs (missing={missing}, extra={extra})")

    expanded = {}
    for icon_id in ICON_IDS:
        rows = icons[icon_id]
        if not isinstance(rows, list) or len(rows) != SIZE:
            raise ArtworkError(f"icons.{icon_id} must contain exactly {SIZE} rows")
        pixels = []
        for row_number, row in enumerate(rows, start=1):
            if not isinstance(row, str) or len(row) != SIZE:
                raise ArtworkError(f"icons.{icon_id} row {row_number} must be a {SIZE}-character string")
            undefined = sorted(set(row) - rgba_palette.keys())
            if undefined:
                raise ArtworkError(f"icons.{icon_id} row {row_number} uses undefined palette character(s): {undefined}")
            pixels.append([rgba_palette[symbol] for symbol in row])
        expanded[icon_id] = pixels
    return expanded


def load_artwork(source):
    try:
        with source.open(encoding="utf-8") as artwork_file:
            document = json.load(artwork_file)
    except json.JSONDecodeError as error:
        raise ArtworkError(f"invalid JSON at line {error.lineno}, column {error.colno}: {error.msg}") from error
    return validate_artwork(document)


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE, help="artwork JSON input")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="directory for generated PNG files")
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    try:
        icons = load_artwork(args.source)
    except (OSError, ArtworkError) as error:
        print(f"generate_icons: {error}", file=sys.stderr)
        return 2

    # Encode every output before touching the destination, after the full input
    # has passed schema and pixel-grid validation.
    pngs = {icon_id: encode_png(pixels) for icon_id, pixels in icons.items()}
    try:
        args.output.mkdir(parents=True, exist_ok=True)
        for icon_id, image in pngs.items():
            (args.output / f"{icon_id}.png").write_bytes(image)
    except OSError as error:
        print(f"generate_icons: could not write PNG output: {error}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
