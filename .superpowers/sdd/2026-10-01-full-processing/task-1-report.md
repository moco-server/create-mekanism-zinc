# Follow-up Task 1 report — full zinc processing lines

Status: implementation complete; controller review pending.

Commit: `a2dfc75` (`Add zinc 3x to 5x processing recipes`).

## Implemented

- Expanded `ZincProcessing` to 21 immutable recipes while preserving the six existing paths and adding the fifteen approved purifying, injecting, crushing/enriching, dissolution, washing and crystallizing entries.
- Added pure Java `ChemicalInput(id, amount)`, `FluidInput(tag, amount)`, new recipe kinds, nullable supplements and `perTickUsage` to `ProcessingRecipe`; retained the original nine-argument convenience constructor.
- Extended native datagen using the pinned Mekanism 10.7.19.85 public builders. Addon slurry IDs remain `create_mekanism_zinc:*`; Mekanism helper chemicals use their `mekanism:*` holders. Existing item tags and the chemical solid-tag map remain unchanged.
- Added independent tests for all 21 full recipe definitions, input/output counts, supplements, per-tick use, exact generated recipe IDs/schema, and all twelve end-to-end final ingot yields. The expected recipe tables are separate from the production definitions.
- Changed the version source to `gradle.properties` (`0.2.0-dev`), aligned the root Gradle project version, fixture dependency metadata, CI artifact path, and production/fixture paths in `scripts/verify-server.py`.
- Generated recipe JSON through native datagen; no recipe JSON was hand-written.

## RED-GREEN and commands

Environment: Java 21 at `/home/osamu/develop/moco-server/mc/.local/issue7-gradle/jdks/eclipse_adoptium-21-amd64-linux.2`; `GRADLE_USER_HOME=/home/osamu/develop/moco-server/mc/.local/issue7-gradle`.

- RED: `./gradlew :common:test` failed to compile the new tests because `Kind.PURIFYING`/other new kinds and the `ChemicalInput`/`FluidInput` records did not exist yet.
- GREEN common: `./gradlew :common:test` — passed after implementation.
- RED datagen assertions: `./gradlew :neoforge:test --tests org.mocoserver.create_mekanism_zinc.ZincRecipeGenerationTest --rerun-tasks` failed because only the prior six expected recipe resources existed.
- Datagen: `./gradlew :neoforge:runData` — passed; generated 15 new recipe files and reported no data errors.
- Main validation: `./gradlew :common:test :neoforge:test :neoforge:build :neoforge:gameTestJar` — passed. This builds but does not launch GameTests, as Task 2 owns runtime machine tests.
- Determinism: captured `sha256sum` for every generated resource except ignored `.cache`, reran `:neoforge:runData`, and compared the before/after lists with `diff -u`; identical file set and hashes. Datagen reported `written: 0`.
- Clean artifact validation: `./gradlew clean :common:test :neoforge:test :neoforge:build :neoforge:gameTestJar` — passed using the already generated source resources.
- Ordinary jar inspection: `create-mekanism-zinc-0.2.0-dev.jar` contains all 21 processing recipes, has no fixture/test classes/resources or nested dependency jars. Fixture jar is separate as `create-mekanism-zinc-0.2.0-dev-gametests.jar`.
- Metadata inspection: production and fixture mod metadata both report `0.2.0-dev`; fixture dependency range requires production `[0.2.0-dev]`.

No `:neoforge:runGameTestServer` was run for this task.

## Diff and remaining scope

Changed common processing definitions/tests; NeoForge datagen/generated recipe JSON/generated recipe test; `gradle.properties`, root version source, CI artifact path, fixture metadata range and server-helper artifact references. Existing tags/data map and all six original recipe resources are unchanged. Controller-owned `docs/plans/2026-10-01-full-processing.md` changes are not part of this task commit.

The runtime inputs, energy/power behavior, machine throughput and full raw-block-to-ingot process remain unverified until Task 2 GameTests. Client acceptance and release are also still pending.

---

# Review correction — full dissolution yield calculation

Status: complete.

Commit: `e6a78c5` (`test: verify complete dissolution yield conversion`).

## Change

Updated only `common/src/test/java/org/mocoserver/create_mekanism_zinc/processing/ZincProcessingTest.java`. The dissolution branch now calculates whole batches through washing, crystallizing, crystal-to-shard injecting, shard-to-clump purifying, clump crushing, dirty-dust enriching, and dust smelting, using each actual recipe definition. `convertWholeBatches` asserts zero remainder at every step, exposing any partial batch instead of truncating it. Production definitions were unchanged.

## Verification

Environment: Java 21 at `/home/osamu/develop/moco-server/mc/.local/issue7-gradle/jdks/eclipse_adoptium-21-amd64-linux.2`; `GRADLE_USER_HOME=/home/osamu/develop/moco-server/mc/.local/issue7-gradle`.

Command: `./gradlew :common:test :neoforge:test` — **BUILD SUCCESSFUL** (5s); common tests executed, NeoForge tests were up-to-date. All twelve independent expected final-ingot yields passed, including the full dissolution paths and per-stage no-remainder assertions.

`git diff --check` was run before commit. Only the common test and this report are included in the follow-up commit; controller-owned `docs/plans/2026-10-01-full-processing.md` remains unstaged.
