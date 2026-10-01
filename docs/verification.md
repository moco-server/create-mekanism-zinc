# Verification record

## Pinned runtime

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

- Task2's final ordinary-JVM run of `./gradlew :neoforge:runGameTestServer` passed all 33 required tests: `33 GAME TESTS COMPLETE IN 1.130 s`, `All 33 required tests passed :)`, `BUILD SUCCESSFUL`. During the sequence of reruns, one intermediate launch had a native C2 compiler SIGSEGV in `PhaseIFG::effective_degree` while compiling vanilla `StateDefinition$$Lambda.accept`, before recipe loading/tests. The same source, Java 21 build, and ordinary JVM settings passed on the following run; no C2 workaround was used or persisted. Detailed Task2 report: `.superpowers/sdd/2026-10-01-full-processing/task-2-report.md`.
- Task3 build command `./gradlew :common:test :neoforge:test :neoforge:build :neoforge:gameTestJar` completed `BUILD SUCCESSFUL` (18 tasks, 2 executed); common boundary check passed with `0 artifacts`. Python syntax compilation for `scripts/verify-server.py` passed.
- Production JAR SHA-512: `707b8672c82a5483cb6cb775e4899a9488faf1f6452d729efe9585547dfdd278f7f99b53190cd9fe405dd5484f7f0b251d59c691bff29d64fd8b6133e7c86dfd`. The ordinary JAR has production metadata and no `tests/` classes, GameTest fixture, or embedded dependency JAR. The separate fixture JAR SHA-512 is `231696a4684d9cb05f8f9440483ea02c8b327279fe72b5fdd055efd5ded06874228b7cf53e227883a1f3f70520ad96801d42f0d8e6077fb036d477efc56b9f1f`.
- `python3 scripts/verify-server.py prepare` passed with the pinned NeoForge installer hash and created only the marked `full-processing-server` runtime. `python3 scripts/verify-server.py clean-boot` passed with JVM PID 27 and `fixture=False`; the server log listed Create 6.0.10, addon 0.2.0-dev and Mekanism 10.7.19, loaded recipes and started on `127.0.0.1:25566`, logged `CLEAN_PRODUCTION_JAR_BOOT_PASS`, then exited 0.
- `python3 scripts/verify-server.py persistence` used two Java 21 JVMs sequentially against the same new disposable world. PID 27 loaded the fixture, wrote actual chest/tank contents and logged `CMZ_PERSIST_SEED_PASS world=cmz-full-processing`, saved and exited 0. PID 214 loaded the same world in a separate process, read its saved contents and logged `CMZ_PERSIST_CHECK_PASS items=5 chemicals=2` and `TWO_JVM_WORLD_PERSISTENCE_PASS`, then exited 0. The fixture verifies five actual item stacks and two actual Mekanism chemical-tank stacks; it does not synthesize expected output. After the run, the `mods` directory contained only the ordinary addon, Create, and Mekanism JARs; the fixture was removed.
- GameTest evidence for 3x routes: normal and deepslate zinc ore each yielded 3 Clumps and 3 final ingots; one raw zinc yielded 2 Clumps and 2 ingots; one raw zinc block yielded 18 Clumps and 18 ingots. For 4x, normal/deepslate ore yielded 4 Shards→4 Clumps→4 ingots; 3 raw zinc yielded 8→8→8; a raw zinc block yielded 24→24→24. For 5x, normal/deepslate ore produced 1000 mB dirty/clean slurry→5 crystals→5 Clumps/ingots; 3 raw zinc produced 2000 mB→10→10; a raw block produced 6000 mB→30→30. Actual machine outputs were carried between stages.
- Negative real-machine checks: missing power or oxygen/hydrogen chloride/sulfuric acid retained inputs and produced no output. For per-tick chemical stages, tests observed actual chemical consumption, then removed supply and confirmed stall with retained input/no output. Washer consumed water at exactly 5 mB per slurry mB (5,000/10,000/30,000 mB); 4 mB water retained dirty slurry, and the 30,000 mB route replenished a real 10,000 mB tank. Crystallizer retained 199 mB with no crystal and consumed slurry in exact 200 mB units. All addon purification/injection/dissolution recipes rejected 64 `create:crushed_raw_zinc` and 64 `create:zinc_ingot`; 1–2 raw zinc items were retained at 4x injection and 5x dissolution entrances.
- After persistence and automatic fixture removal, `python3 scripts/verify-server.py clean-boot` was run again against the same saved `cmz-full-processing` world. It started PID 27 with `fixture=False`, loaded only the ordinary addon, Create and Mekanism JARs, logged `CLEAN_PRODUCTION_JAR_BOOT_PASS`, and exited 0. Transcript: `.local/verification/full-processing-server/clean-boot-after-persistence.log`. NeoForge logged that saved metadata referenced the now-removed test-fixture mod (`create_mekanism_zinc_tests -> MISSING`); production startup and world load succeeded. This is expected for a test-seeded world; the fixture JAR remained absent.
- The verified production JAR was copied to the parent project's ignored `dist/cmz-trial/create-mekanism-zinc-0.2.0-dev.jar`; the prior 0.1.0-dev JAR remains there unchanged. This is a local trial handoff, not a public release or parent-pack installation.
- The dedicated server uses `online-mode=false` for this private loopback test. It does not authenticate clients. GUI, authenticated user login, and user-side 3x–5x processing remain unverified; see [client-acceptance.md](client-acceptance.md). The user previously reported 2x processing worked.
