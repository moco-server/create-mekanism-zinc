# Create Mekanism Zinc: milestone 1 design

Approved 2026-10-01 via user implementation request. Goal: independently buildable private MIT addon; register five zinc items and two slurries; implement only the six 2x recipes; preserve common/NeoForge boundary; server actual-machine tests and process-restart persistence; user performs authenticated client acceptance.

## Global constraints
- MC 1.21.1; NeoForge 21.1.252; Java 21; Create 6.0.10 Modrinth UjX6dr61; Mekanism 10.7.19.85 Modrinth 5KzzycBT.
- Gradle 9.2.1, ModDevGradle 2.0.148; official MDK base commit 7819b902a351b03fe71db00754d103b5a31c4ebf.
- Mod ID create_mekanism_zinc; package org.mocoserver.create_mekanism_zinc; version 0.1.0-dev; name Create Mekanism Zinc; license MIT; author moco-server.
- common contains pure Java ID/content/processing definitions and assets. No Minecraft, loader, Create, Mekanism API dependency in production compile/runtime classpaths.
- neoforge owns registrations/datagen/runtime fixtures. Final production jar incorporates common code/assets but no dependency jars or test-only classes/resources.
- Item IDs: dust_zinc, dirty_dust_zinc, clump_zinc, shard_zinc, crystal_zinc. Chemical IDs: dirty_zinc, clean_zinc.
- Recipes under create_mekanism_zinc:processing/zinc/: enriching ore tag c:ores/zinc 1 -> dust 2; raw tag c:raw_materials/zinc 3 -> dust 4; raw block c:storage_blocks/raw_zinc 1 -> dust 12; crushing c:ingots/zinc 1 -> dust 1; dust smelting/blasting -> create:zinc_ingot 1, 200/100 ticks and 0.35 XP.
- Item tags c:dusts/zinc, c:dirty_dusts/zinc, c:clumps/zinc, c:shards/zinc, c:crystals/zinc. Clean slurry solid tag data map references c:ores/zinc.
- No crushed_raw_zinc amplification input, new ores/devices/ingots/worldgen, recombination, extra 3x-5x recipes, KubeJS/Architectury runtime requirement.
- Original provisional assets with ja_jp/en_us labels; mark not release-ready. Public distribution, pack install, issue closure are out of milestone scope.
- Private remote moco-server/create-mekanism-zinc; reviewed source may be pushed. Parent pack docs reflect exact verified/pending state.
- Main controller does design/API investigation/review/acceptance. Only Luna implementation workers, sequentially, with no nested delegation or external messages.

## Verification
Unit tests: identity uniqueness, exact independent recipe coefficients and tags, deterministic generation, boundary enforcement. GameTests actual machines: normal/deepslate ore, 3 raw, block; 1/2 raw retained; no power stop; ingot crushing and smelting/blasting. Dedicated disposable world: all 5 items and both slurries persisted across actual JVM restart. Clean ordinary jar server boot. GUI/login/client connection confirmation remains pending user acceptance.

## References
https://github.com/moco-server/mc/issues/7
https://docs.neoforged.net/docs/1.21.1/gettingstarted/
https://docs.neoforged.net/docs/1.21.1/misc/gametest/
https://github.com/mekanism/Mekanism/tree/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80
https://support.modrinth.com/en/articles/8801191-modrinth-maven
