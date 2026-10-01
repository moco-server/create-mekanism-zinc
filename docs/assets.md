# Zinc item artwork

## Current artwork: zinc-v2 crops (0.2.3-dev)

The five production icons are transparent 512×512 RGBA PNGs cut from the original 2172×724 RGBA8 artwork canvas at [`artwork/concepts/zinc-v2/style-study.png`](../artwork/concepts/zinc-v2/style-study.png), following the user's request to use crops from that single canvas. The controller reviewed the design and fixed these exclusive-right/bottom boxes in [`artwork/zinc-v2-crops.json`](../artwork/zinc-v2-crops.json): dust `[42,292,436,585]`, dirty dust `[504,292,888,586]`, clump `[946,214,1303,590]`, shard `[1383,238,1707,585]`, and crystal `[1829,195,2130,596]`. Each unchanged crop is centered on its canvas with integer offsets. Pixels within each box, including their original alpha values, are copied as-is; there is no resize, recolor, or alpha threshold. The source SHA256 and dimensions are recorded in the crop manifest and verified before generation. On 2026-10-02 the user checked the 0.2.3-dev icons in-game and accepted them as sufficiently practical (「確認した。十分実用的なので良さそう」). This five-icon crop set is the accepted artwork baseline.

Regenerate deterministically from the repository root:

```sh
python3 tools/generate_icons.py
```

The standard-library PNG reader checks the source hash, dimensions, chunk CRCs and RGBA8 format, then handles PNG filters 0–4. All five encoded outputs are prepared before files are written. Use `--output .local/artwork/zinc-v2/cropped-candidates` for a non-destructive candidate build. The preview [`artwork/previews/zinc-v2-cropped.png`](../artwork/previews/zinc-v2-cropped.png) places the five actual production PNGs side-by-side at their native 512×512 size. The previous native 16×16 zinc-v2 grid renderer is preserved as [`tools/render_native_icons.py`](../tools/render_native_icons.py) for historical reconstruction; its default output is isolated under `.local/`.

The controller created one original style-study canvas using the user's reference screenshots as shape/style guidance; the screenshots themselves are not included. The canvas is used only through the five fixed crop rectangles listed above. Project artwork sources and production PNGs are distributed under the project MIT license; see [`LICENSE`](../LICENSE).

## Third-party image license review (2026-10-01)

The user asked whether editing upstream item images would be safe to distribute. The controller checked the source commits corresponding to the pinned dependency versions:

- [Create 6.0.10 LICENSE.md at ac0c444](https://github.com/Creators-of-Create/Create/blob/ac0c444d9828da3453ae8cc65338e8de063286fb/LICENSE.md): code is MIT, while files under `src/main/resources/assets/` are **All Rights Reserved**. The code license does not permit copying or recoloring the item textures for distribution. This project does not copy or modify Create image files.
- [Mekanism 10.7.19.85 LICENSE at a00109e](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/LICENSE): MIT, copyright 2017–2025 Aidan C. Brady. The complete repository tree at that commit has no separate asset license file. Under that published license, modified/redistributed covered images require preservation of its copyright and license notice. No Mekanism item texture files are copied or recolored in this revision, so the new five-icon set does not derive from those assets.

Slurry visuals continue to refer to Mekanism's existing chemical textures, supplied by the installed dependency. This project does not copy or redistribute those texture files. The fixed-version resource IDs are `mekanism:textures/slurry/dirty.png` and `mekanism:textures/slurry/clean.png`. Slurry tint settings were not changed by the item-icon revision.

## Historical placeholders

The original `0.1.0-dev` and accepted `0.2.0-dev` icons were hand-authored temporary shapes produced by the earlier `tools/generate_icons.py`, including the scattered yellow-tan dust criticized during playtesting. Their source and artifacts remain in Git history and the prior trial JAR. The artwork revision does not change registered IDs, recipes or processing rates.

## Historical zinc-v1 artwork

The `0.2.1-dev` version used five independently generated concepts without image inputs, then separately authored native pixel grids. `0.2.2-dev` also used a native 16×16 grid renderer with a separately authored grid based on the zinc-v2 study's intended silhouettes. These native-grid steps remain available in Git history and `tools/render_native_icons.py`; neither renderer is the current production pipeline. The earlier v1 concepts, prompts, grids and previews remain under `artwork/` for history.
