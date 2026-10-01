# Verification record

## Pinned runtime

- Minecraft 1.21.1; NeoForge 21.1.252; Java 21; Gradle 9.2.1; ModDevGradle 2.0.148.
- Create 6.0.10 Modrinth `UjX6dr61`; Mekanism 10.7.19.85, Modrinth file build 85 `5KzzycBT`.
- Production artifact: `neoforge/build/libs/create-mekanism-zinc-0.1.0-dev.jar`.
- Runtime script creates ignored files under `.local/verification/clean-server/`. It pins and checks NeoForge installer SHA-512 `11addafebd1dbb583252230c7ff97bf80c8b5f6d980a6ae8ffd409ba302e8beaee87e51c35fd8daf8a589b1c9fce9845dd0406a7b7113185ad349748a3461de5` for [this installer URL](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.252/neoforge-21.1.252-installer.jar), copies only hash-verified Create/Mekanism Gradle-cache artifacts and the production jar, binds the disposable offline server to localhost, and copies the existing accepted `eula=true` file. Set `CMZ_EULA_FILE` only to a previously accepted EULA file if the documented default is unavailable. The script refuses any `CMZ_GAME_DIR` other than its own disposable `instance` path and runs the selected Java 21 executable directly.
- Restart persistence uses the separately built `create-mekanism-zinc-0.1.0-dev-gametests.jar` fixture mod. The fixture checks real saved chest slots and Mekanism chemical tank block entities after a second Java server process loads the same world.

## Commands

Run data generation in its own invocation, then the unit/build checks in another invocation:

```sh
./gradlew :neoforge:runData
./gradlew :common:test :neoforge:test :neoforge:build
./gradlew :neoforge:runGameTestServer
./gradlew :neoforge:gameTestJar
python3 scripts/verify-server.py clean-boot
python3 scripts/verify-server.py persistence
```

The GameTest run exercises actual Mekanism machines with energy supplied through their real energy containers and vanilla furnaces with coal. Runtime harness classes are confined to the fixture source set and fixture jar. The production jar must have only production metadata and no GameTest fixture classes. Persistence requires building `gameTestJar` first; the jar stays out of the CI upload and is removed from the prepared server when the process exits.

For the verified local environment, use the selected Java 21 and Gradle cache paths below, and point `CMZ_EULA_FILE` at the existing accepted EULA file. The script defaults to Java on `PATH` (must report Java 21), Gradle's standard `~/.gradle` cache, and `.local/eula.txt` under this repository when these variables are unset.

```sh
export JAVA_HOME=/home/osamu/develop/moco-server/mc/.local/issue7-gradle/jdks/eclipse_adoptium-21-amd64-linux.2
export PATH="$JAVA_HOME/bin:$PATH"
export GRADLE_USER_HOME=/home/osamu/develop/moco-server/mc/.local/issue7-gradle
export CMZ_EULA_FILE=/home/osamu/develop/moco-server/mc/.local/runtime-check/server-instance/eula.txt
```

## Observed results

Verification ran 2026-10-01 with Eclipse Temurin/Adoptium Java `21.0.12.1+1-LTS`, Gradle wrapper 9.2.1, and the versions listed above.

- Datagen command `./gradlew :neoforge:runData` ran in two separate Gradle invocations. Both ended `BUILD SUCCESSFUL`; the second reported `written: 0`. SHA-256 manifests of all generated resource files before and after the second run were identical (`diff` had no output).
- `./gradlew :common:test :neoforge:test :neoforge:build :neoforge:gameTestJar` ended `BUILD SUCCESSFUL`. JUnit XML reports 5 common tests and 2 NeoForge tests, all with zero failures/errors. The common boundary check reported `0 artifacts`.
- `./gradlew :neoforge:runGameTestServer` ended `BUILD SUCCESSFUL`; its log reported `13 GAME TESTS COMPLETE` and `All 13 required tests passed :)`. Tests cover exact normal/deepslate ore identities, raw zinc and raw zinc block, retained one/two-item raw inputs, zero-power stop, crusher, furnace and blast furnace output, crushed raw zinc rejection, chemical solid-tag data map and the original clean iron mapping.
- `python3 scripts/verify-server.py persistence` launched actual dedicated Java 21 processes sequentially against the addon repository's `.local/verification/clean-server/instance/cmz-verification`. In addition to the worker's initial pass, the controller reran persistence twice against the same world without resetting it. First rerun used JVMs 26 and 208 and passed at 16:05 JST; second used JVMs 26 and 202 and passed at 16:06 JST. Each first process logged `CMZ_PERSIST_SEED_PASS world=cmz-verification`; each second process loaded that same world and logged `CMZ_PERSIST_CHECK_PASS items=5 chemicals=2` and `TWO_JVM_WORLD_PERSISTENCE_PASS`, then exited 0. Check reads the actual five chest slot ItemStacks and two Advanced Chemical Tank stacks after loading, including exact IDs/counts/amounts. Controller logs are in the parent project's `.local/issue7-control/controller-final-persistence-fixed.log` and `controller-final-persistence-repeat.log`; worker logs are retained under the addon repository's `.local/verification/clean-server/logs/`.
- `python3 scripts/verify-server.py clean-boot` ran with only Create, Mekanism and the production addon in the instance `mods` folder. The controller's final run used JVM 26 (`fixture=False`), logged `CLEAN_PRODUCTION_JAR_BOOT_PASS`, and exited 0. The log is in the parent project's `.local/issue7-control/controller-final-clean-boot.log`. The production jar SHA-512 was `ad22d5220015dfa09ec94ed7439f3f517ea89d1c2178b050b0dcfaf4fd7d11047a4378ef07085fef53bc515d273e18328128b1a88637afc71781dbc6f4b4b4d3`. Jar inspection found one production `META-INF/neoforge.mods.toml`, zero fixture classes, zero embedded dependency jars, five textures, five models and two language files. Create's Jar-in-Jar Flywheel/Ponder dependencies loaded in the server boot.
- The first fixture seed requested 1111/2222 mB through one insertion, but the tested tank accepted only 1000 mB from that insertion and truncated the stack. The fixture now clears the tank before insertion, uses Advanced Chemical Tanks, and fails unless the exact requested quantities are inserted. Two repeated two-JVM runs on the same world passed with 1111 mB dirty and 2222 mB clean slurry.
- The instance is stopped and contains only the production addon, Create and Mekanism jars. Its `eula.txt` was copied from the pre-existing `eula=true` file. It binds only to `127.0.0.1:25565` and uses `online-mode=false` for this private disposable runtime.
- Final controller rerun of `./gradlew :neoforge:runGameTestServer` at 16:08 JST ended `BUILD SUCCESSFUL` in 13 seconds; log `.local/issue7-control/controller-final-gametest.log` reported `All 13 required tests passed :)`.
- Authenticated GUI/client acceptance and visual review remain pending the user, as documented in [client-acceptance.md](client-acceptance.md). The server-side harness does not establish authenticated server login. Full modpack startup, parent-pack installation, public release and gameplay progression are outside this milestone.
