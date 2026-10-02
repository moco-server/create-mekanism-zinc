# 現行の対応構成（0.3.0-dev）

Minecraft 1.21.1、NeoForge 21.1.252、Java 21、Create 6.0.10（Modrinth `UjX6dr61`）、Mekanism 10.7.19.85（`5KzzycBT`）を維持する。MOD ID、登録ID、加工JSON、画像は旧Java版と同じ。

| ツール | 固定版 |
| --- | --- |
| Gradle Wrapper | 8.12.1（SHA256を固定） |
| ModDevGradle | 2.0.148 |
| Kotlin compiler／開発stdlib | 2.0.21 |
| KotlinForForge NeoForge | 5.5.0 |
| detekt | 1.23.8 |
| ktlint plugin／本体 | 12.1.2／1.5.0 |
| JUnit | 5.11.4 |

配布環境には[作者配布KFF 5.5.0](https://modrinth.com/mod/kotlin-for-forge/version/tdvjylfz)を追加する。取得URLは`https://cdn.modrinth.com/data/ordsPcFz/versions/tdvjylfz/kotlinforforge-5.5.0-all.jar`、SHA512は`4544a80300cac93ce92b44b2b73a386e6e568d7dfc0744dfb98b1f6c6cc3479cece8630155a2887e49b4120ebdb90441ec15a2e053f2d2d31fd58d28d1abb7b4`、sideはclient／server両方。Mavenの`kotlinforforge-neoforge-5.5.0-all.jar`は開発aggregateであり、配布JARの代用にはならない。KFF配布物のstdlibは2.0.10、開発側は2.0.21。この組合せの通常JAR起動と保存／旧版移行は実測した。

[テンプレートの対象版資料](https://github.com/moco-server/minecraft-mod-template)、[KFF module metadata](https://thedarkcolour.github.io/KotlinForForge/thedarkcolour/kfflang-neoforge/5.5.0/kfflang-neoforge-5.5.0.module)、[detekt 1.23.8互換表](https://detekt.dev/docs/1.23.8/introduction/compatibility/)、[NeoForge 1.21.1](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)を基準とする。

Mekanism 10.7.19.85の`IExtendedFluidTank.fill`は非推奨で、bytecode上は`stack.amount - insert(stack, Action.fromFluidAction(action), AutomationType.EXTERNAL).amount`へ委譲する。GameTestでは同じ挿入処理を明示して使う。[対象版作者ソース](https://github.com/mekanism/Mekanism/blob/v10.7.19/src/api/java/mekanism/api/fluid/IExtendedFluidTank.java)と固定Modrinth JARを照合した。対象版の署名が変わる更新時には再確認する。

既存の`gradle/verification-metadata.xml`のSHA512検証は保持し、追加したKotlin／KFF／buildSrc依存を記録した。coreとbuildSrcの本番・test compile/runtimeはlockを使用する。追加依存のhash生成後は差分を確認し、通常のstrict検証でcheck／起動を実行する。

対話的な開発clientだけにJEI 19.51.0.418（Modrinth `ufHUqt9b`）を追加する。取得元・SHA512は[クライアント検収記録](client-acceptance.md)の固定値を使う。通常addonやheadless serverの必須依存にしない。

coreはpure Kotlin/JVM。Fabric移植の実証はしていない。過去版の確認済み構成は以下の履歴と[検証記録](verification.md)を参照。

---

# Compatibility targets

This table documents the narrow target for addon `0.2.0-dev` and its 21 zinc-processing recipes. It does not claim support for adjacent versions.

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
