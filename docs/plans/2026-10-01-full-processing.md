# 亜鉛3〜5倍加工 Implementation Plan

> Workers: use subagent-driven-development. User-authorized continuation of Issue #7. The controller owns specification, review and acceptance; existing Luna agents implement sequentially and must not spawn agents.

**Goal:** 既存2倍加工に3〜5倍加工を追加し、実装・専用サーバーでの加工検証・Windows向け試遊JARの準備まで行う。

**Architecture:** 既存commonの純粋Java定義を拡張し、NeoForge側の対象版公開レシピビルダーで生成する。既存の5アイテム・2スラリーとIDを維持する。通常JARとテストfixtureを分離する。

**Tech Stack:** Minecraft 1.21.1, NeoForge 21.1.252, Java 21, Gradle 9.2.1, ModDevGradle 2.0.148, Create 6.0.10 (UjX6dr61), Mekanism 10.7.19.85 (5KzzycBT).

**Spec:** Issue #7の加工表、`docs/milestone-1-design.md`、本計画の確定係数。2026-10-01のユーザー指示「2倍は問題なく動作してたので、残りのラインも進めてほしい」に基づく。

## Global Constraints

- MOD ID / Java package / license: `create_mekanism_zinc` / `org.mocoserver.create_mekanism_zinc` / MIT。新開発版は `0.2.0-dev`。
- commonはMinecraft・ローダー・Create・Mekanism具体APIに依存しない。
- 既存6レシピIDと7素材IDを維持。全21レシピは `create_mekanism_zinc:processing/zinc/...`。
- Create粉砕済み亜鉛を入口へ追加しない。鉱石再生成、新規装置・鉱石・インゴットを追加しない。
- 画像は仮画像を継続。公開リリース、main packへの導入、Issue全体の完了扱いを行わない。
- 稼働中の試遊サーバーやユーザーの保存済みワールドを編集しない。新しい所有済み検証ディレクトリを用いる。
- Lunaへ設計判断・レビューを委任しない。各タスクの差分をcontrollerがレビューしてから次へ進む。

## 確定係数とAPI

固定JAR SHA512はpack metadataと照合済み。一次情報は https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/datagen/main/java/mekanism/common/recipe/impl/OreProcessingRecipeProvider.java 。機械の時間・電力は既存Mekanism設定に従う。

| 追加パス | 種別 | 入力（タグは#） | 出力 | 補助入力 | per_tick_usage |
| --- | --- | --- | --- | --- | --- |
| clump/from_ore | purifying | #c:ores/zinc ×1 | clump_zinc ×3 | oxygen 1 | true |
| clump/from_raw_ore | purifying | #c:raw_materials/zinc ×1 | clump_zinc ×2 | oxygen 1 | true |
| clump/from_raw_block | purifying | #c:storage_blocks/raw_zinc ×1 | clump_zinc ×18 | oxygen 2 | true |
| clump/from_shard | purifying | #c:shards/zinc ×1 | clump_zinc ×1 | oxygen 1 | true |
| dirty_dust/from_clump | crushing | #c:clumps/zinc ×1 | dirty_dust_zinc ×1 | — | — |
| dust/from_dirty_dust | enriching | #c:dirty_dusts/zinc ×1 | dust_zinc ×1 | — | — |
| shard/from_ore | injecting | #c:ores/zinc ×1 | shard_zinc ×4 | hydrogen_chloride 1 | true |
| shard/from_raw_ore | injecting | #c:raw_materials/zinc ×3 | shard_zinc ×8 | hydrogen_chloride 1 | true |
| shard/from_raw_block | injecting | #c:storage_blocks/raw_zinc ×1 | shard_zinc ×24 | hydrogen_chloride 2 | true |
| shard/from_crystal | injecting | #c:crystals/zinc ×1 | shard_zinc ×1 | hydrogen_chloride 1 | true |
| slurry/dirty/from_ore | dissolution | #c:ores/zinc ×1 | dirty_zinc 1000 | sulfuric_acid 1 | true |
| slurry/dirty/from_raw_ore | dissolution | #c:raw_materials/zinc ×3 | dirty_zinc 2000 | sulfuric_acid 1 | true |
| slurry/dirty/from_raw_block | dissolution | #c:storage_blocks/raw_zinc ×1 | dirty_zinc 6000 | sulfuric_acid 2 | true |
| slurry/clean | washing | dirty_zinc 1 | clean_zinc 1 | #minecraft:water 5 | — |
| crystal/from_slurry | crystallizing | clean_zinc 200 | crystal_zinc ×1 | — | — |

Item/chemical outputs use the addon namespace; oxygen, hydrogen_chloride and sulfuric_acid use `mekanism:`. Dirty/clean slurry inputs are direct chemicals. Standard builders: `ItemStackChemicalToItemStackRecipeBuilder.purifying/injecting`, `ChemicalDissolutionRecipeBuilder.dissolution`, `FluidChemicalToChemicalRecipeBuilder.washing`, `ChemicalCrystallizerRecipeBuilder.crystallizing`. Reference JSON/API files are in the parent `.local/issue7-control/milestone-2/`.

## Review Focus

- 原石1〜2個は4/5倍入口で保持される。3倍は原石1個で進む。
- 5倍入口量を200単位で結晶化し、原石ブロック6000→30インゴットまで過不足なく進む。
- 補助chemical欠如、電力欠如、水不足、結晶化200未満で入力を失わない。
- Create粉砕済み亜鉛と精錬済みインゴットが3〜5倍入口に入らない。
- 原石ブロックでは補助chemical量2を毎tick使用する。一般入力の1と混同しない。水5対スラリー1を維持する。

### Task 1: 加工定義・データ生成・単体検証

**Files:** common `ZincProcessing.java`, `ZincProcessingTest.java`; neoforge `ZincDataGenerators.java`, `ZincRecipeGenerationTest.java`; `neoforge/src/generated/resources`; `gradle.properties`; root `build.gradle` version source only; fixture metadata dependency version in `neoforge/src/gametest/templates/META-INF/neoforge.mods.toml`; `.github/workflows/build.yml` artifact path; production JAR references in `scripts/verify-server.py`.

**Interfaces:** Preserve `ZincProcessing.recipes()` as an immutable list; extend recipe Kind with PURIFYING, INJECTING, DISSOLUTION, WASHING, CRYSTALLIZING. Extend `ProcessingRecipe`'s existing fields with nullable `ChemicalInput(String id, long amount)`, nullable `FluidInput(String tag, int amount)` and boolean `perTickUsage`. `inputId`/`inputCount` mean primary input: item for item recipes, chemical for WASHING/CRYSTALLIZING; supplementary ChemicalInput is only for PURIFYING/INJECTING/DISSOLUTION. WASHING supplementary FluidInput is water. Chemical outputs are identified by Kind DISSOLUTION/WASHING. Use a convenience constructor for the existing 9-field recipes if needed. Do not create a general recipe framework.

- [ ] Add failing tests for the 15 additional independent coefficients, 21 unique paths and exact supplements/perTickUsage. Verify all 12 final yields by traversing recipe coefficients to Create ingots: ore 2/3/4/5, raw batches 3→4 / 1→2 / 3→8 / 3→10, raw block 12/18/24/30. Reject crushed raw and ingot multiplication entries. Preserve cooking conditions 200/100 ticks, XP0.35.
- [ ] Run `./gradlew :common:test :neoforge:test` and record expected RED before implementing.
- [ ] Implement the common definition and native datagen switch using the fixed builders. Tags/data map and existing six recipes remain semantically unchanged.
- [ ] Generate data in separate invocation `./gradlew :neoforge:runData`; then run `./gradlew :common:test :neoforge:test :neoforge:build`. Add generated-JSON tests asserting each exact type/input/output amount and per_tick_usage field using independent expected data, not a copy of actual definitions.
- [ ] Use `0.2.0-dev` artifact naming in properties/CI/server helper only. No dependency upgrades. Re-run datagen and confirm deterministic hashes including file set. Inspect ordinary JAR for 21 recipes and absence of fixture/nested dependencies.
- [ ] Commit allowed files; write full commands/results and diff summary to the designated task report. Do not touch controller-owned existing client-acceptance edit or plan. Controller review precedes Task 2.

### Task 2: 実機による3〜5倍全工程テスト

**Files:** `neoforge/src/gametest/java/.../tests/ZincAdvancedMachineGameTests.java` (new); `neoforge/src/gametest/resources/data/create_mekanism_zinc/structure/processing_line.nbt` (new, bounds7×4×7); existing `ZincMachineGameTests.java` only for shared regression behavior if necessary. No production code changes without controller finding/approval.

**Interfaces:** Use Task 1's generated recipes and installed fixed Mekanism machinery. `TileEntityAdvancedElectricMachine` exposes `chemicalTank`; dissolution/washer/crystallizer expose `getChemicalTanks(null)` and energy container; washer `getFluidTanks(null)`; actual machine inventory slots can be found by type. No reflection or fabricated recipe results. All test setup belongs to gametest source set.

- [ ] Add actual-machine pipelines for each of 3/4/5x, each accepting normal ore, deepslate ore, raw batch and raw block (12 cases). Run every applicable stage through real block entities to `create:zinc_ingot`, asserting intermediate output kinds/counts, final exact yield, all consumed input, and chemical amounts at dissolution/washing/crystallization. Transfer machine outputs to next machine through fixture code if needed; do not reconstruct expected intermediates as inputs. Start with empty outputs.
- [ ] Preserve all previous 13 GameTests. Test 4/5x raw1 and raw2 blocked; missing oxygen/HCl/acid, power (all new powered machine types), water and clean slurry199 blocked with retained inputs. Include wrong-chemical rejection and crushed-raw/ingot exclusion at all additive entrance recipe types.
- [ ] Use legitimate Mekanism SPEED upgrades if needed to keep test duration practical, with default recipe settings and sufficient replenished energy/chemical/water; document actual upgrade counts. Do not set progress counters or directly invoke recipe process outputs. For per_tick chemical usage assert depletion during working and stalled behavior when empty; do not assert a constant 200 consumption because machine settings include usage scaling.
- [ ] 最終精錬は実出力の粉を各1個に分け、最大30台の実溶鉱炉で並行処理してよい。燃料を供給し、実インゴットを集計する。既存size1×1×1のempty templateは変更せず、新しい境界内で配置し、他テストとの干渉を防ぐ。
- [ ] Run `./gradlew :neoforge:runGameTestServer`, inspect complete report and return count/names/failures. Any production defect is escalated for controller judgment before a scoped correction.
- [ ] Commit fixture-only changes, report commands/results, consumed resources and limits. Controller review precedes Task 3.

### Task 3: 専用サーバー・保存・手順書・試遊JAR

**Files:** `scripts/verify-server.py` (minimal isolated-runtime support if required), `README.md`, `docs/verification.md`, `docs/client-acceptance.md`, `docs/compatibility.md`. Ignored `.local/verification/full-processing-server/` and parent `dist/cmz-trial/` are runtime/artifact destinations.

**Interfaces:** Ordinary `neoforge/build/libs/create-mekanism-zinc-0.2.0-dev.jar`; separate `-gametests.jar` only for persistence fixture. Use a new isolated runtime/world, a free loopback port, and the existing accepted EULA file. Do not write into the running old clean-server runtime. Source old NeoForge library install may be reused read-only.

- [ ] Build ordinary and gametest JARs. Start a clean three-MOD dedicated server with ordinary JAR only and inspect data load/mod initialization. Never include fixture in the clean boot or user client.
- [ ] Persist the same five items/two slurry types in the new disposable world, gracefully save/stop and verify after a separate JVM restart. Preserve ordinary JAR checks and reference all logs.
- [ ] Update README to list 2〜5x and 21 recipes, versioned artifact and coefficients. Update verification and Japanese client instructions with all stage sequences, supplement amounts, Windows Prism instance, direct IPv4 address, known pending visual art and user-reported 2x success. Do not invent user-confirmed 3〜5x results or mark Issue complete.
- [ ] Copy verified new ordinary JAR to parent `dist/cmz-trial/`; prepare exact Windows replacement steps while keeping old artifact. Do not change a running Windows instance or start user GUI; controller handles runtime handoff after review.
- [ ] Commit source/docs only; report commands, results, hashes, isolation and any unverified GUI/transport behavior. Controller performs final review, own validation, private push and parent design/history update.
