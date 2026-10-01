# Zinc item artwork

## Current artwork: zinc-v1

The five production icons are original 16×16 RGBA PNGs for dust, dirty dust, clump, shard and crystal. Clean zinc uses a silver/sage palette; dirt uses charcoal/brown grains. Each processing stage has a different silhouette. The artwork replaces the original playtesting placeholders in `0.2.1-dev`. User visual acceptance of this revision is pending.

The controller used built-in `image_gen` to create five independent concept images with no third-party image inputs. The complete prompts and original concept outputs are under [`artwork/concepts/zinc-v1`](../artwork/concepts/zinc-v1); exact prompts are in [`prompts.json`](../artwork/concepts/zinc-v1/prompts.json). The production pixel positions were then authored separately in [`artwork/zinc-v1.json`](../artwork/zinc-v1.json). The generated concept PNGs are not resized, sampled or included in the runtime JAR. `tools/generate_icons.py` renders only the editable native grid definitions using the Python standard library. These project artwork sources and production PNGs are distributed under the project MIT license; see [`LICENSE`](../LICENSE).

Regenerate deterministically from the repository root:

```sh
python3 tools/generate_icons.py
```

A non-destructive candidate output is supported with `--output .local/artwork/zinc-v1/native-candidates`. The generator checks the entire palette and all five grids before writing. The preview [`artwork/previews/zinc-v1.png`](../artwork/previews/zinc-v1.png) renders those same native definitions directly, with an enlarged row and a 16px row. Left to right: dust, dirty dust, clump, shard, crystal. Concept images, prompts, pixel sources and previews stay outside the runtime resource tree.

## Third-party image license review (2026-10-01)

The user asked whether editing upstream item images would be safe to distribute. The controller checked the source commits corresponding to the pinned dependency versions:

- [Create 6.0.10 LICENSE.md at ac0c444](https://github.com/Creators-of-Create/Create/blob/ac0c444d9828da3453ae8cc65338e8de063286fb/LICENSE.md): code is MIT, while files under `src/main/resources/assets/` are **All Rights Reserved**. The code license does not permit copying or recoloring the item textures for distribution. This project does not copy or modify Create image files.
- [Mekanism 10.7.19.85 LICENSE at a00109e](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/LICENSE): MIT, copyright 2017–2025 Aidan C. Brady. The complete repository tree at that commit has no separate asset license file. Under that published license, modified/redistributed covered images require preservation of its copyright and license notice. No Mekanism item texture files are copied or recolored in this revision, so the new five-icon set does not derive from those assets.

Slurry visuals continue to refer to Mekanism's existing chemical textures, supplied by the installed dependency. This project does not copy or redistribute those texture files. The fixed-version resource IDs are `mekanism:textures/slurry/dirty.png` and `mekanism:textures/slurry/clean.png`. Slurry tint settings were not changed by the item-icon revision.

## Historical placeholders

The original `0.1.0-dev` and accepted `0.2.0-dev` icons were hand-authored temporary shapes produced by the earlier `tools/generate_icons.py`, including the scattered yellow-tan dust criticized during playtesting. Their source and artifacts remain in Git history and the prior trial JAR. The artwork revision does not change registered IDs, recipes or processing rates.
