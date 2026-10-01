# Verification record

## Pinned runtime for accepted processing build (`0.2.0-dev`)

- Minecraft 1.21.1, NeoForge 21.1.252, Java 21 (`21.0.12.1+1-LTS`), Gradle 9.2.1, ModDevGradle 2.0.148.
- Create 6.0.10, pinned Modrinth file `UjX6dr61`; Mekanism 10.7.19.85, pinned Modrinth file `5KzzycBT` (installed metadata version 10.7.19).
- Version `0.2.0-dev` registers 21 recipes. The production JAR is `neoforge/build/libs/create-mekanism-zinc-0.2.0-dev.jar`; the separate `-gametests.jar` contains fixtures and is not part of the client/ordinary server artifact.
- `scripts/verify-server.py` owns only `.local/verification/full-processing-server/instance`, guarded by `.cmz-owned-runtime`; it refuses a different `CMZ_GAME_DIR`. It binds the private offline server to `127.0.0.1:25566` and names its world `cmz-full-processing`. The existing `.local/verification/clean-server/instance/eula.txt` was read as the already accepted EULA source and copied to the isolated runtime. The script verifies the NeoForge 21.1.252 installer URL and pinned SHA-512 `11addafebd1dbb583252230c7ff97bf80c8b5f6d980a6ae8ffd409ba302e8beaee87e51c35fd8daf8a589b1c9fce9845dd0406a7b7113185ad349748a3461de5`, copies hash-verified Create/Mekanism cache files, and invokes the selected Java 21 binary directly.

## Reproduction commands

From the standalone addon repository, configure the same Java/Gradle cache/EULA paths used for this verification:

```sh
export JAVA_HOME=/home/osamu/develop/moco-server/mc/.local/issue7-gradle/jdks/eclipse_adoptium-21-amd64-linux.2
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME=/home/osamu/develop/moco-server/mc/.local/issue7-gradle
export CMZ_EULA_FILE=/home/osamu/develop/moco-server/mc/.local/mod-development/create-mekanism-zinc/.local/verification/clean-server/instance/eula.txt
./gradlew :neoforge:runData
./gradlew :common:test :neoforge:test :neoforge:build :neoforge:gameTestJar
./gradlew :neoforge:runGameTestServer
python3 scripts/verify-server.py prepare
python3 scripts/verify-server.py clean-boot
python3 scripts/verify-server.py persistence
```

Data generation remains a separate Gradle invocation. Persistence adds the fixture JAR for the seed and check JVMs only; a `finally` cleanup removes it on both normal and error exits. The production JAR does not contain fixture classes or dependency JARs. Runtime logs are in `.local/verification/full-processing-server/logs/`, with command transcripts `clean-boot-run.log` and `persistence-run.log` in the same ignored runtime directory.

## Observed results (2026-10-01)

- Current `0.2.0-dev` unit tests: common 6 tests (content 4, processing 2) and NeoForge 2 recipe-generation tests; all 8 passed with zero failures/errors. Two separate `:neoforge:runData` invocations completed; the second reported `written: 0`, and SHA-256 manifests of every generated resource file before/after were identical. Controller compared all 21 generated recipes with the pinned-version iron recipe JSON after substituting the ore/material IDs and zinc tags, and confirmed exact JSON equality. The original iron recipes remain unchanged.
- The final ordinary-JVM run of `./gradlew :neoforge:runGameTestServer` passed all 33 required tests: `33 GAME TESTS COMPLETE IN 1.130 s`, `All 33 required tests passed :)`, `BUILD SUCCESSFUL`. During the sequence of reruns, one intermediate launch had a native C2 compiler SIGSEGV in `PhaseIFG::effective_degree` while compiling vanilla `StateDefinition$$Lambda.accept`, before recipe loading/tests. The same source, Java 21 build, and ordinary JVM settings passed on the following run; no C2 workaround was used or persisted.
- Task3 build command `./gradlew :common:test :neoforge:test :neoforge:build :neoforge:gameTestJar` completed `BUILD SUCCESSFUL` (18 tasks, 2 executed); common boundary check passed with `0 artifacts`. Python syntax compilation for `scripts/verify-server.py` passed.
- Production JAR SHA-512: `707b8672c82a5483cb6cb775e4899a9488faf1f6452d729efe9585547dfdd278f7f99b53190cd9fe405dd5484f7f0b251d59c691bff29d64fd8b6133e7c86dfd`. The ordinary JAR has production metadata and no `tests/` classes, GameTest fixture, or embedded dependency JAR. The separate fixture JAR SHA-512 is `231696a4684d9cb05f8f9440483ea02c8b327279fe72b5fdd055efd5ded06874228b7cf53e227883a1f3f70520ad96801d42f0d8e6077fb036d477efc56b9f1f`.
- `python3 scripts/verify-server.py prepare` passed with the pinned NeoForge installer hash and created only the marked `full-processing-server` runtime. `python3 scripts/verify-server.py clean-boot` passed with JVM PID 27 and `fixture=False`; the server log listed Create 6.0.10, addon 0.2.0-dev and Mekanism 10.7.19, loaded recipes and started on `127.0.0.1:25566`, logged `CLEAN_PRODUCTION_JAR_BOOT_PASS`, then exited 0.
- `python3 scripts/verify-server.py persistence` used two Java 21 JVMs sequentially against the same new disposable world. PID 27 loaded the fixture, wrote actual chest/tank contents and logged `CMZ_PERSIST_SEED_PASS world=cmz-full-processing`, saved and exited 0. PID 214 loaded the same world in a separate process, read its saved contents and logged `CMZ_PERSIST_CHECK_PASS items=5 chemicals=2` and `TWO_JVM_WORLD_PERSISTENCE_PASS`, then exited 0. The fixture verifies five actual item stacks and two actual Mekanism chemical-tank stacks; it does not synthesize expected output. After the run, the `mods` directory contained only the ordinary addon, Create, and Mekanism JARs; the fixture was removed.
- GameTest evidence for 3x routes: normal and deepslate zinc ore each yielded 3 Clumps and 3 final ingots; one raw zinc yielded 2 Clumps and 2 ingots; one raw zinc block yielded 18 Clumps and 18 ingots. For 4x, normal/deepslate ore yielded 4 Shards→4 Clumps→4 ingots; 3 raw zinc yielded 8→8→8; a raw zinc block yielded 24→24→24. For 5x, normal/deepslate ore produced 1000 mB dirty/clean slurry→5 crystals→5 Clumps/ingots; 3 raw zinc produced 2000 mB→10→10; a raw block produced 6000 mB→30→30. Actual machine outputs were carried between stages.
- Negative real-machine checks: missing power or oxygen/hydrogen chloride/sulfuric acid retained inputs and produced no output. For per-tick chemical stages, tests observed actual chemical consumption, then removed supply and confirmed stall with retained input/no output. Washer consumed water at exactly 5 mB per slurry mB (5,000/10,000/30,000 mB); 4 mB water retained dirty slurry, and the 30,000 mB route replenished a real 10,000 mB tank. Crystallizer retained 199 mB with no crystal and consumed slurry in exact 200 mB units. All addon purification/injection/dissolution recipes rejected 64 `create:crushed_raw_zinc` and 64 `create:zinc_ingot`; 1–2 raw zinc items were retained at 4x injection and 5x dissolution entrances.
- After persistence and automatic fixture removal, `python3 scripts/verify-server.py clean-boot` was run again against the same saved `cmz-full-processing` world. It started PID 27 with `fixture=False`, loaded only the ordinary addon, Create and Mekanism JARs, logged `CLEAN_PRODUCTION_JAR_BOOT_PASS`, and exited 0. Transcript: `.local/verification/full-processing-server/clean-boot-after-persistence.log`. NeoForge logged that saved metadata referenced the now-removed test-fixture mod (`create_mekanism_zinc_tests -> MISSING`); production startup and world load succeeded. This is expected for a test-seeded world; the fixture JAR remained absent.
- The verified production JAR was copied to the parent project's ignored `dist/cmz-trial/create-mekanism-zinc-0.2.0-dev.jar`; the prior 0.1.0-dev JAR remains there unchanged. This is a local trial handoff, not a public release or parent-pack installation.
- The dedicated server uses `online-mode=false` for this private loopback test. It does not authenticate clients. GUI, authenticated user login, and user-side 3x–5x processing remain unverified; see [client-acceptance.md](client-acceptance.md). The user previously reported 2x processing worked.

## Final controller review (2026-10-01)

The controller accepted each Luna implementation task and its review corrections before proceeding. Final `:neoforge:runData` at 19:33 JST succeeded with `written: 0` and no tracked or untracked generated-resource diff. A separate `:common:test :neoforge:test :neoforge:build :neoforge:gameTestJar` invocation succeeded; cached JUnit reports matched the unchanged source (6 common + 2 NeoForge, zero failures/errors), and the boundary check again reported `0 artifacts`. A controller production-only boot at 19:35 JST used JVM 26, reopened `cmz-full-processing`, logged `CLEAN_PRODUCTION_JAR_BOOT_PASS`, and exited 0. Its transcript is in the parent project's `.local/issue7-control/milestone-2/controller-final-clean-boot.log`.

The final ordinary JAR in source build output, parent `dist/cmz-trial/`, isolated dedicated server, and fresh Windows Prism instance has the identical SHA-512 recorded above. All three trial/runtime JARs (the addon and two pinned dependencies) were verified against their recorded hashes; no fixture remains in either ordinary server or client. The controller then started the ordinary trial server at 19:38 JST; its bind is `127.0.0.1:25566`, and Windows PowerShell TCP reachability was confirmed. This proves transport reachability only. At that point Windows GUI startup and 3x–5x user acceptance were pending; see the later native-client and user-acceptance observations below.

## Native client startup (2026-10-01 20:37–20:39 JST)

At the user's request, Windows Prism launched `moco-cmz-trial-0.2.0-dev-client` with `--server 127.0.0.1:25566`. The ordinary client log confirmed addon `0.2.0-dev` and NVIDIA GeForce RTX 4060 Ti / OpenGL 4.6.0 NVIDIA 572.60 initialization. The client logged connection at 20:39:05, and the dedicated server logged a loopback player joining its world at 20:39:07. The player later disconnected normally. This establishes native startup and dedicated-server joining; at that point seven-material appearance and 3x–5x user processing acceptance were pending. The offline server does not authenticate accounts. Evidence is the new Prism instance's `minecraft/logs/latest.log` and the isolated server's `logs/latest.log`; no account data or raw player identifiers are copied into this record.

## User acceptance through 4x and recipe viewer (2026-10-01)

The user reported “4倍まで確認した”. Record successful client processing through 4x, without treating this as a per-input measured-results checklist or acceptance of all seven material displays. At that point the 5x user check was pending because they could not find its recipe chain without a recipe viewer.

After confirming the native trial client was closed, the controller added only JEI 19.51.0.418 (Modrinth `ufHUqt9b`) to `moco-cmz-trial-0.2.0-dev-client`. Existing three JAR hashes were checked before the addition; the fourth JAR's size and SHA-512 matched the pinned Modrinth metadata. No save or existing configuration was edited. The dedicated server retains three JARs. JEI is an optional trial-client tool, not an addon dependency. Its complete pin, controls, and 5x guide are in [client acceptance](client-acceptance.md).

Native Windows startup at 20:56–20:57 JST discovered JEI alongside the fixed addon/Create/Mekanism versions, initialized JEI ConfigManager and created the JEI GUI atlas without an ERROR line. At startup, world-dependent recipe registration, the displayed zinc chain, and actual 5x client processing were still pending.

## User acceptance of all processing lines (2026-10-01)

Following their earlier successful 2x and through-4x reports, the user reported “5倍ラインも正常動作を確認した”. Client processing-line acceptance for 2x–5x is complete based on these user observations. This report supplies the previously missing 5x client result; it does not provide measured results for each input variant or a separate seven-material appearance/translation checklist. The JEI screen itself was not separately reported as checked.

The accepted ordinary addon remains `0.2.0-dev` with the previously recorded artifact hash. This update changes acceptance records only; no source, recipes, resources, dependencies, worlds or live runtime settings were changed. No new Java test run was needed for this documentation update. Formal artwork and parent-pack installation/coexistence verification remain follow-up work. No public release or Issue #7 closure was performed.

## Original item artwork revision (`0.2.1-dev`, 2026-10-01)

At the user's request the controller created five original concept images with built-in `image_gen`, without third-party image inputs, then separately authored editable 16×16 pixel grids. Luna implemented the native-grid PNG writer and generated the production images; the controller reviewed the generator, all five silhouettes at enlarged/actual size, and the final JAR. Provenance, prompts and fixed-source license review are in [assets](assets.md).

`python3 tools/generate_icons.py` regenerated all five production PNGs from `artwork/zinc-v1.json`. Independent controller regeneration into a temporary directory produced byte-identical files. The controller JAR inspection confirmed five new 16×16 RGBA textures matching source, all 21 recipe JSONs unchanged from the accepted `0.2.0-dev` JAR, all production class files byte-identical, metadata `0.2.1-dev`, and no fixture/test classes or nested JARs.

Luna's `:common:test :neoforge:test :neoforge:build` completed successfully. The same controller command also completed `BUILD SUCCESSFUL`; 13 tasks were up-to-date and the common dependency boundary executed with `0 artifacts`. The unchanged-code JUnit reports have 6 common + 2 NeoForge tests, zero failures/errors. GameTests and server persistence were not repeated for this image/metadata-only revision; their recorded results describe the accepted `0.2.0-dev` build.

The ordinary JAR is 49,681 bytes, SHA-512 `40d1bbd634d23173a77295db484f6a5b7fedba744723e30862b99d7f006e69b6de54ed2a2b15d2b5114e2e4870e6447d9c891f562d62a8386ddf993d2022c8e8`. A byte-identical copy is in the parent project's ignored `dist/cmz-trial/create-mekanism-zinc-0.2.1-dev.jar`. The prior `0.2.0-dev` JAR, live trial server and native Windows instance were not replaced. In-game texture rendering and user visual acceptance of zinc-v1 remain pending. No public release or main-pack installation was performed.

## Historical observations — addon 0.1.0-dev at commit `f5f3024`

The following is a historical snapshot of the earlier six-recipe implementation, recorded 2026-10-01 at commit `f5f3024c1ee0f0a3430b8ee9c9f9b863d4f46b8a`. Its runtime directory, server port, JAR names, and GameTest count differ from the current `0.2.0-dev` procedure above. These results are retained for history and do not describe the current runtime.

- Target then: Minecraft 1.21.1, NeoForge 21.1.252, Java 21 (`21.0.12.1+1-LTS`), Gradle 9.2.1, ModDevGradle 2.0.148, Create 6.0.10 (`UjX6dr61`), Mekanism 10.7.19.85 (`5KzzycBT`). Artifact `create-mekanism-zinc-0.1.0-dev.jar`; six recipes.
- Runtime then: `.local/verification/clean-server/instance/cmz-verification`, loopback `127.0.0.1:25565`, `online-mode=false`. The previously accepted EULA file was copied; no saved user world was used. Runtime output was under `.local/verification/clean-server/`.
- At that revision `./gradlew :common:test :neoforge:test :neoforge:build :neoforge:gameTestJar` passed; 5 common tests and 2 NeoForge tests had zero failures/errors, and common-boundary inspection found `0 artifacts`. `./gradlew :neoforge:runGameTestServer` reported all 13 required GameTests passed.
- Historical generated-resource determinism also passed: `:neoforge:runData` was run in two separate Gradle invocations; both ended `BUILD SUCCESSFUL`, the second reported `written: 0`, and SHA-256 manifests of all generated resource files before/after were identical.
- Historical ordinary JAR SHA-512 was `ad22d5220015dfa09ec94ed7439f3f517ea89d1c2178b050b0dcfaf4fd7d11047a4378ef07085fef53bc515d273e18328128b1a88637afc71781dbc6f4b4b4d3`. Inspection at that stage found one production `META-INF/neoforge.mods.toml`, no fixture classes, no embedded dependency JARs, five textures, five models, and two language files. A production-only dedicated-server boot logged `CLEAN_PRODUCTION_JAR_BOOT_PASS` and exited 0.
- The old persistence harness used separate seed/check Java server processes. Controller reruns against the same historical world at 16:05 and 16:06 JST passed: `CMZ_PERSIST_SEED_PASS world=cmz-verification`, then `CMZ_PERSIST_CHECK_PASS items=5 chemicals=2` and `TWO_JVM_WORLD_PERSISTENCE_PASS`. The fixture checked actual chest slots and Advanced Chemical Tank stacks. The seed was later fixed to clear tank contents before insertion; repeated checks stored 1111 mB dirty and 2222 mB clean slurry correctly.
- The comparison of all 21 recipes against pinned-version iron JSON equivalents is a current `0.2.0-dev` check; it was not part of this six-recipe historical snapshot.
