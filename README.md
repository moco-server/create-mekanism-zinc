# Create Mekanism Zinc

Private MIT addon for Minecraft 1.21.1 and NeoForge 21.1.252, targeting Create 6.0.10 and Mekanism 10.7.19.85. Version `0.2.0-dev` registers five zinc processing intermediates, two Mekanism slurries, and 21 recipes covering 2x through 5x processing. The textures and labels are provisional; this is not a public release.

The ordinary artifact is `neoforge/build/libs/create-mekanism-zinc-0.2.0-dev.jar`. It contains production classes and assets only. GameTest fixtures are built separately as `create-mekanism-zinc-0.2.0-dev-gametests.jar` and must not be installed in the client or ordinary server. Exact supported versions are in [docs/compatibility.md](docs/compatibility.md).

Nominal zinc-ore outputs are 2x Zinc Dust, 3x Clumps, 4x Shards, and 5x final Clumps/ingots. For raw zinc, the corresponding batches are three pieces for 2x Dust ×4, one piece for 3x Clumps ×2, three pieces for 4x Shards ×8, and three pieces for 5x final output ×10. A raw zinc block yields 12 Dust, 18 Clumps, 24 Shards, and 30 final output. The 5x slurry stage makes 1,000 / 2,000 / 6,000 mB dirty slurry for ore / raw batch / block, uses water at 5:1 slurry ratio, and crystallizes one Zinc Crystal per 200 mB clean slurry.

The user reported successful processing through 4x on 2026-10-01. The 5x client acceptance remains pending; the Windows trial now includes pinned client-side JEI for recipe browsing. See [client acceptance](docs/client-acceptance.md) for the full 5x chain and viewer controls.

## Build and verify

Use Java 21 and Gradle 9.2.1 via the wrapper. Keep data generation in its own invocation. Then run unit/build checks, actual GameTests, and the isolated runtime checks:

```sh
./gradlew :neoforge:runData
./gradlew :common:test :neoforge:test :neoforge:build :neoforge:gameTestJar
./gradlew :neoforge:runGameTestServer
python3 scripts/verify-server.py prepare
python3 scripts/verify-server.py clean-boot
python3 scripts/verify-server.py persistence
```

The disposable server is under `.local/verification/full-processing-server/`, binds to `127.0.0.1:25566`, and uses a previously accepted EULA. See [docs/verification.md](docs/verification.md) for exact environment values and recorded results.

## Client acceptance

User-facing GUI and processing confirmation for the 3x–5x lines remains pending. The user confirmed the previous 2x behavior. Follow the Japanese [client acceptance checklist](docs/client-acceptance.md); it recommends a new Prism profile and preserves the older trial profile. Public distribution, parent-pack installation, and issue closure are outside this milestone.
