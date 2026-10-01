"""Extract the approved zinc icons from one fixed, original RGBA artwork canvas."""

import argparse
import hashlib
import json
from pathlib import Path
import struct
import sys
import zlib


PROJECT_ROOT = Path(__file__).resolve().parents[1]
CROP_SPEC = PROJECT_ROOT / "artwork/zinc-v2-crops.json"
DEFAULT_OUTPUT = PROJECT_ROOT / "common/src/main/resources/assets/create_mekanism_zinc/textures/item"
ICON_IDS = ("dust_zinc", "dirty_dust_zinc", "clump_zinc", "shard_zinc", "crystal_zinc")
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


class IconError(ValueError):
    """Raised when the fixed crop manifest or source PNG is invalid."""


def paeth_predictor(left, above, upper_left):
    prediction = left + above - upper_left
    distance_left = abs(prediction - left)
    distance_above = abs(prediction - above)
    distance_upper_left = abs(prediction - upper_left)
    if distance_left <= distance_above and distance_left <= distance_upper_left:
        return left
    if distance_above <= distance_upper_left:
        return above
    return upper_left


def decode_rgba_png(data):
    """Decode an 8-bit, non-interlaced RGBA PNG, including filters 0 through 4."""
    if not data.startswith(PNG_SIGNATURE):
        raise IconError("source is not a PNG")

    offset = len(PNG_SIGNATURE)
    dimensions = None
    idat = bytearray()
    seen_idat = False
    ended = False
    while offset < len(data):
        if offset + 12 > len(data):
            raise IconError("truncated PNG chunk header")
        length = struct.unpack_from(">I", data, offset)[0]
        chunk_type = data[offset + 4:offset + 8]
        chunk_end = offset + 12 + length
        if chunk_end > len(data):
            raise IconError("truncated PNG chunk data")
        chunk_data = data[offset + 8:offset + 8 + length]
        expected_crc = struct.unpack_from(">I", data, offset + 8 + length)[0]
        actual_crc = zlib.crc32(chunk_type + chunk_data) & 0xFFFFFFFF
        if actual_crc != expected_crc:
            raise IconError(f"invalid CRC in {chunk_type.decode('ascii', errors='replace')} chunk")

        if dimensions is None and chunk_type != b"IHDR":
            raise IconError("PNG does not begin with IHDR")
        if chunk_type == b"IHDR":
            if dimensions is not None or length != 13:
                raise IconError("invalid or duplicate IHDR chunk")
            width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(">IIBBBBB", chunk_data)
            if width <= 0 or height <= 0:
                raise IconError("source PNG dimensions must be positive")
            if (bit_depth, color_type, compression, filtering, interlace) != (8, 6, 0, 0, 0):
                raise IconError("source PNG must use RGBA8, standard compression/filtering, and no interlace")
            dimensions = (width, height)
        elif chunk_type == b"IDAT":
            if ended:
                raise IconError("IDAT appears after IEND")
            seen_idat = True
            idat.extend(chunk_data)
        elif chunk_type == b"IEND":
            if length != 0 or not seen_idat:
                raise IconError("invalid or premature IEND chunk")
            ended = True
            offset = chunk_end
            break
        elif chunk_type not in (b"PLTE",) and chunk_type[0] & 0x20 == 0:
            raise IconError(f"unsupported critical PNG chunk {chunk_type!r}")
        offset = chunk_end

    if not ended or offset != len(data) or dimensions is None:
        raise IconError("PNG is missing IEND or has trailing data")

    width, height = dimensions
    stride = width * 4
    try:
        filtered = zlib.decompress(idat)
    except zlib.error as error:
        raise IconError(f"invalid PNG image data: {error}") from error
    if len(filtered) != height * (stride + 1):
        raise IconError("PNG decompressed data has an unexpected length")

    rows = []
    cursor = 0
    previous = bytes(stride)
    for _ in range(height):
        filter_type = filtered[cursor]
        cursor += 1
        scanline = bytearray(filtered[cursor:cursor + stride])
        cursor += stride
        if filter_type > 4:
            raise IconError(f"unsupported PNG filter type {filter_type}")
        for index in range(stride):
            left = scanline[index - 4] if index >= 4 else 0
            above = previous[index]
            upper_left = previous[index - 4] if index >= 4 else 0
            if filter_type == 1:
                predictor = left
            elif filter_type == 2:
                predictor = above
            elif filter_type == 3:
                predictor = (left + above) // 2
            elif filter_type == 4:
                predictor = paeth_predictor(left, above, upper_left)
            else:
                predictor = 0
            scanline[index] = (scanline[index] + predictor) & 0xFF
        rows.append(bytes(scanline))
        previous = scanline
    return width, height, rows


def load_crop_spec(path):
    try:
        spec = json.loads(path.read_text(encoding="utf-8"))
    except OSError as error:
        raise IconError(f"cannot read crop specification: {error}") from error
    except json.JSONDecodeError as error:
        raise IconError(f"invalid crop specification JSON at line {error.lineno}: {error.msg}") from error

    if not isinstance(spec, dict) or spec.get("version") != 1:
        raise IconError("crop specification must be an object with version 1")
    source_relative = spec.get("source")
    if not isinstance(source_relative, str):
        raise IconError("crop specification source must be a relative path string")
    source_path = (PROJECT_ROOT / source_relative).resolve()
    if PROJECT_ROOT not in source_path.parents:
        raise IconError("crop specification source must remain inside the addon project")
    width = spec.get("source_width")
    height = spec.get("source_height")
    canvas_size = spec.get("canvas_size")
    source_sha256 = spec.get("source_sha256")
    if any(type(value) is not int or value <= 0 for value in (width, height, canvas_size)):
        raise IconError("source dimensions and canvas_size must be positive integers")
    if canvas_size != 512:
        raise IconError("the approved icon canvas size must be 512")
    if not isinstance(source_sha256, str) or len(source_sha256) != 64:
        raise IconError("source_sha256 must be a 64-character hex digest")
    try:
        bytes.fromhex(source_sha256)
    except ValueError as error:
        raise IconError("source_sha256 is not hexadecimal") from error
    boxes = spec.get("icons")
    if not isinstance(boxes, dict) or set(boxes) != set(ICON_IDS):
        raise IconError("crop specification must contain exactly the five required icon IDs")
    normalized_boxes = {}
    for icon_id in ICON_IDS:
        box = boxes[icon_id]
        if not isinstance(box, list) or len(box) != 4 or any(type(v) is not int for v in box):
            raise IconError(f"crop box for {icon_id} must contain four integer coordinates")
        x1, y1, x2, y2 = box
        if not (0 <= x1 < x2 <= width and 0 <= y1 < y2 <= height):
            raise IconError(f"crop box for {icon_id} is outside the source dimensions")
        if x2 - x1 > canvas_size or y2 - y1 > canvas_size:
            raise IconError(f"crop box for {icon_id} is larger than the icon canvas")
        normalized_boxes[icon_id] = box
    return source_path, source_sha256.lower(), width, height, canvas_size, normalized_boxes


def build_icons(source_override, spec_path):
    """Validate the complete input, then return all encoded cropped icons."""
    source_default, expected_sha256, expected_width, expected_height, canvas_size, boxes = load_crop_spec(spec_path)
    source_path = source_override if source_override is not None else source_default
    source_data = source_path.read_bytes()
    actual_sha256 = hashlib.sha256(source_data).hexdigest()
    if actual_sha256 != expected_sha256:
        raise IconError(f"source SHA256 mismatch: expected {expected_sha256}, got {actual_sha256}")

    width, height, rows = decode_rgba_png(source_data)
    if (width, height) != (expected_width, expected_height):
        raise IconError(f"source dimensions mismatch: expected {expected_width}x{expected_height}, got {width}x{height}")

    encoded = {}
    for icon_id in ICON_IDS:
        x1, y1, x2, y2 = boxes[icon_id]
        crop_width, crop_height = x2 - x1, y2 - y1
        left = (canvas_size - crop_width) // 2
        top = (canvas_size - crop_height) // 2
        pixels = bytearray(canvas_size * canvas_size * 4)
        for crop_y, source_y in enumerate(range(y1, y2)):
            source_start = x1 * 4
            source_end = x2 * 4
            destination_start = ((top + crop_y) * canvas_size + left) * 4
            pixels[destination_start:destination_start + crop_width * 4] = rows[source_y][source_start:source_end]
        encoded[icon_id] = encode_png(canvas_size, canvas_size, bytes(pixels))
    return encoded, actual_sha256


def encode_png(width, height, rgba):
    def chunk(kind, payload):
        crc = zlib.crc32(kind + payload) & 0xFFFFFFFF
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", crc)

    stride = width * 4
    scanlines = b"".join(b"\0" + rgba[row * stride:(row + 1) * stride] for row in range(height))
    png = PNG_SIGNATURE
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(scanlines, 9))
    png += chunk(b"IEND", b"")
    return png


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, help="source RGBA8 artwork PNG (defaults to the crop manifest)")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="directory for generated icon PNGs")
    return parser.parse_args(argv)


def main(argv=None):
    args = parse_args(argv)
    try:
        icon_pngs, source_hash = build_icons(args.source, CROP_SPEC)
    except (OSError, IconError) as error:
        print(f"generate_icons: {error}", file=sys.stderr)
        return 2

    # No output path is touched until the manifest, source integrity, image data,
    # and all five resulting PNG byte strings have been validated and rendered.
    try:
        args.output.mkdir(parents=True, exist_ok=True)
        for icon_id, data in icon_pngs.items():
            (args.output / f"{icon_id}.png").write_bytes(data)
    except OSError as error:
        print(f"generate_icons: could not write PNG output: {error}", file=sys.stderr)
        return 2
    print(f"Rendered {len(icon_pngs)} icons from source SHA256 {source_hash}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
