# Compatibility targets

The initial compatibility target is intentionally narrow:

| Component | Target |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.252 |
| Java | 21 |
| Create | 6.0.10 |
| Mekanism installed file | 10.7.19.85 (Modrinth file ID `5KzzycBT`, build 85) |

This addon registers the following IDs in namespace `create_mekanism_zinc`:

- Items: `dust_zinc`, `dirty_dust_zinc`, `clump_zinc`, `shard_zinc`, `crystal_zinc`.
- Mekanism chemicals: `dirty_zinc`, `clean_zinc`.

The installed build-85 jar reports the Mekanism mod metadata version `10.7.19`. The addon metadata requires Mekanism `[10.7.19]`, Create `[6.0.10]`, NeoForge `[21.1.252]`, and Minecraft `[1.21.1]`; these are exact inclusive ranges from the production metadata template. The selected Modrinth files are pinned as Create `UjX6dr61` and Mekanism `5KzzycBT` in the Gradle dependencies. The target Mekanism source revision used for API checks is [a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80](https://github.com/mekanism/Mekanism/tree/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80).

These targets do not claim compatibility with other versions. Recheck game, loader and both dependency files together before changing them.
