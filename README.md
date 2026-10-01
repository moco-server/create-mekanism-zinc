# Create Mekanism Zinc

Private MIT addon for Minecraft 1.21.1 and NeoForge 21.1.252, targeting Create 6.0.10 and Mekanism 10.7.19.85. The project registers five zinc processing intermediates and two Mekanism slurries, with six recipes: zinc ore and raw zinc enrichment, raw zinc block enrichment, ingot crushing, and dust smelting/blasting. It does not add extra crushed raw zinc input or 3x–5x recipes.

The textures and labels are provisional and are not release-ready. The production artifact is `neoforge/build/libs/create-mekanism-zinc-0.1.0-dev.jar`; it includes common classes and assets, with no test fixtures or dependency jars. Exact targets and loader dependency ranges are in [docs/compatibility.md](docs/compatibility.md).

## Build and verify

Use Java 21 and Gradle 9.2.1 via the wrapper. Run data generation as its own Gradle invocation before the separate tests/build invocation:

```sh
./gradlew :neoforge:runData
./gradlew :common:test :neoforge:test :neoforge:build
./gradlew :neoforge:runGameTestServer
./gradlew :neoforge:gameTestJar
python3 scripts/verify-server.py clean-boot
python3 scripts/verify-server.py persistence
```

The scripts use an ignored disposable server under `.local/verification/`. See [docs/verification.md](docs/verification.md) for commands, runtime versions, and observed results.

## Client acceptance

The authenticated GUI and visual check is part of milestone acceptance and is pending the user's confirmation. Use the Japanese [client acceptance checklist](docs/client-acceptance.md) for the artifact, setup, and expected results. Public distribution, parent-pack installation, and issue closure are outside this milestone.
