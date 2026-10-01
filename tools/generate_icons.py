"""Generate original provisional zinc processing item icons using only Python stdlib."""

from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1] / "common/src/main/resources/assets/create_mekanism_zinc"
COLORS = {
    "dust_zinc": (191, 157, 91),
    "dirty_dust_zinc": (100, 111, 105),
    "clump_zinc": (125, 142, 127),
    "shard_zinc": (143, 176, 163),
    "crystal_zinc": (182, 200, 192),
}


def png(path, pixels):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    rows = b"".join(b"\0" + b"".join(bytes(pixel) for pixel in row) for row in pixels)
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">2I5B", 16, 16, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(rows, 9))
    data += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def icon(name, color):
    pixels = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    dark = tuple(max(0, c - 45) for c in color)
    light = tuple(min(255, c + 38) for c in color)

    def put(x, y, value):
        if 0 <= x < 16 and 0 <= y < 16:
            pixels[y][x] = (*value, 255)

    # Each processing stage has a distinct silhouette as well as a different tint.
    if name.endswith("dust_zinc") and not name.startswith("dirty"):
        points = [(4, 4), (8, 3), (11, 5), (5, 8), (9, 9), (3, 11), (12, 12), (7, 13)]
        for x, y in points:
            for dx, dy in ((0, 0), (1, 0), (0, 1)):
                put(x + dx, y + dy, light if (x + y) % 2 else color)
    elif name.startswith("dirty"):
        for y in range(4, 12):
            for x in range(3, 13):
                if (x * 3 + y * 5) % 4 != 0:
                    put(x, y, dark if (x + y) % 3 == 0 else color)
    elif name.startswith("clump"):
        for x, y, radius in ((5, 8, 3), (9, 7, 3), (8, 11, 2)):
            for dy in range(-radius, radius + 1):
                for dx in range(-radius, radius + 1):
                    if abs(dx) + abs(dy) <= radius + 1:
                        put(x + dx, y + dy, light if dy < -1 else color)
    elif name.startswith("shard"):
        for y in range(3, 13):
            span = 1 if y < 7 else 2
            for x in range(8 - span, 9 + span):
                put(x + (1 if y > 8 else 0), y, light if x <= 8 else color)
    else:  # crystal
        for y in range(3, 13):
            half = min(y - 2, 12 - y, 4)
            for x in range(8 - half, 9 + half):
                put(x, y, light if x <= 8 else color)

    # Shared dark lower edge makes the hand-drawn silhouettes read at 16x16.
    for x in range(4, 12):
        if pixels[12][x][3]:
            pixels[13][x] = (*dark, 255)
    return pixels


for item, tint in COLORS.items():
    png(ROOT / "textures/item" / f"{item}.png", icon(item, tint))
