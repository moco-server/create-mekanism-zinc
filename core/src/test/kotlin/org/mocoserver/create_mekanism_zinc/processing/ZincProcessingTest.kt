// 既存のpackage名を維持し、移行以外の識別子変更を避ける。
@file:Suppress("ktlint:standard:package-name", "PackageNaming")

package org.mocoserver.create_mekanism_zinc.processing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ZincProcessingTest {
    private val expected =
        listOf(
            ZincProcessing.ProcessingRecipe(
                "dust/from_ore",
                ZincProcessing.Kind.ENRICHING,
                "c:ores/zinc",
                true,
                1,
                "create_mekanism_zinc:dust_zinc",
                2,
                0,
                0f,
            ),
            ZincProcessing.ProcessingRecipe(
                "dust/from_raw_ore",
                ZincProcessing.Kind.ENRICHING,
                "c:raw_materials/zinc",
                true,
                3,
                "create_mekanism_zinc:dust_zinc",
                4,
                0,
                0f,
            ),
            ZincProcessing.ProcessingRecipe(
                "dust/from_raw_block",
                ZincProcessing.Kind.ENRICHING,
                "c:storage_blocks/raw_zinc",
                true,
                1,
                "create_mekanism_zinc:dust_zinc",
                12,
                0,
                0f,
            ),
            ZincProcessing.ProcessingRecipe(
                "dust/from_ingot",
                ZincProcessing.Kind.CRUSHING,
                "c:ingots/zinc",
                true,
                1,
                "create_mekanism_zinc:dust_zinc",
                1,
                0,
                0f,
            ),
            ZincProcessing.ProcessingRecipe(
                "ingot/from_dust_smelting",
                ZincProcessing.Kind.SMELTING,
                "create_mekanism_zinc:dust_zinc",
                false,
                1,
                "create:zinc_ingot",
                1,
                200,
                0.35f,
            ),
            ZincProcessing.ProcessingRecipe(
                "ingot/from_dust_blasting",
                ZincProcessing.Kind.BLASTING,
                "create_mekanism_zinc:dust_zinc",
                false,
                1,
                "create:zinc_ingot",
                1,
                100,
                0.35f,
            ),
            ZincProcessing.ProcessingRecipe(
                "clump/from_ore",
                ZincProcessing.Kind.PURIFYING,
                "c:ores/zinc",
                true,
                1,
                "create_mekanism_zinc:clump_zinc",
                3,
                0,
                0f,
                ZincProcessing.ChemicalInput("mekanism:oxygen", 1),
                null,
                true,
            ),
            ZincProcessing.ProcessingRecipe(
                "clump/from_raw_ore",
                ZincProcessing.Kind.PURIFYING,
                "c:raw_materials/zinc",
                true,
                1,
                "create_mekanism_zinc:clump_zinc",
                2,
                0,
                0f,
                ZincProcessing.ChemicalInput("mekanism:oxygen", 1),
                null,
                true,
            ),
            ZincProcessing.ProcessingRecipe(
                "clump/from_raw_block",
                ZincProcessing.Kind.PURIFYING,
                "c:storage_blocks/raw_zinc",
                true,
                1,
                "create_mekanism_zinc:clump_zinc",
                18,
                0,
                0f,
                ZincProcessing.ChemicalInput("mekanism:oxygen", 2),
                null,
                true,
            ),
            ZincProcessing.ProcessingRecipe(
                "clump/from_shard",
                ZincProcessing.Kind.PURIFYING,
                "c:shards/zinc",
                true,
                1,
                "create_mekanism_zinc:clump_zinc",
                1,
                0,
                0f,
                ZincProcessing.ChemicalInput("mekanism:oxygen", 1),
                null,
                true,
            ),
            ZincProcessing.ProcessingRecipe(
                "dirty_dust/from_clump",
                ZincProcessing.Kind.CRUSHING,
                "c:clumps/zinc",
                true,
                1,
                "create_mekanism_zinc:dirty_dust_zinc",
                1,
                0,
                0f,
            ),
            ZincProcessing.ProcessingRecipe(
                "dust/from_dirty_dust",
                ZincProcessing.Kind.ENRICHING,
                "c:dirty_dusts/zinc",
                true,
                1,
                "create_mekanism_zinc:dust_zinc",
                1,
                0,
                0f,
            ),
            ZincProcessing.ProcessingRecipe(
                "shard/from_ore",
                ZincProcessing.Kind.INJECTING,
                "c:ores/zinc",
                true,
                1,
                "create_mekanism_zinc:shard_zinc",
                4,
                0,
                0f,
                ZincProcessing.ChemicalInput("mekanism:hydrogen_chloride", 1),
                null,
                true,
            ),
            ZincProcessing.ProcessingRecipe(
                "shard/from_raw_ore",
                ZincProcessing.Kind.INJECTING,
                "c:raw_materials/zinc",
                true,
                3,
                "create_mekanism_zinc:shard_zinc",
                8,
                0,
                0f,
                ZincProcessing.ChemicalInput("mekanism:hydrogen_chloride", 1),
                null,
                true,
            ),
            ZincProcessing.ProcessingRecipe(
                "shard/from_raw_block",
                ZincProcessing.Kind.INJECTING,
                "c:storage_blocks/raw_zinc",
                true,
                1,
                "create_mekanism_zinc:shard_zinc",
                24,
                0,
                0f,
                ZincProcessing.ChemicalInput("mekanism:hydrogen_chloride", 2),
                null,
                true,
            ),
            ZincProcessing.ProcessingRecipe(
                "shard/from_crystal",
                ZincProcessing.Kind.INJECTING,
                "c:crystals/zinc",
                true,
                1,
                "create_mekanism_zinc:shard_zinc",
                1,
                0,
                0f,
                ZincProcessing.ChemicalInput("mekanism:hydrogen_chloride", 1),
                null,
                true,
            ),
            ZincProcessing.ProcessingRecipe(
                "slurry/dirty/from_ore",
                ZincProcessing.Kind.DISSOLUTION,
                "c:ores/zinc",
                true,
                1,
                "create_mekanism_zinc:dirty_zinc",
                1000,
                0,
                0f,
                ZincProcessing.ChemicalInput("mekanism:sulfuric_acid", 1),
                null,
                true,
            ),
            ZincProcessing.ProcessingRecipe(
                "slurry/dirty/from_raw_ore",
                ZincProcessing.Kind.DISSOLUTION,
                "c:raw_materials/zinc",
                true,
                3,
                "create_mekanism_zinc:dirty_zinc",
                2000,
                0,
                0f,
                ZincProcessing.ChemicalInput("mekanism:sulfuric_acid", 1),
                null,
                true,
            ),
            ZincProcessing.ProcessingRecipe(
                "slurry/dirty/from_raw_block",
                ZincProcessing.Kind.DISSOLUTION,
                "c:storage_blocks/raw_zinc",
                true,
                1,
                "create_mekanism_zinc:dirty_zinc",
                6000,
                0,
                0f,
                ZincProcessing.ChemicalInput("mekanism:sulfuric_acid", 2),
                null,
                true,
            ),
            ZincProcessing.ProcessingRecipe(
                "slurry/clean",
                ZincProcessing.Kind.WASHING,
                "create_mekanism_zinc:dirty_zinc",
                false,
                1,
                "create_mekanism_zinc:clean_zinc",
                1,
                0,
                0f,
                null,
                ZincProcessing.FluidInput("minecraft:water", 5),
                false,
            ),
            ZincProcessing.ProcessingRecipe(
                "crystal/from_slurry",
                ZincProcessing.Kind.CRYSTALLIZING,
                "create_mekanism_zinc:clean_zinc",
                false,
                200,
                "create_mekanism_zinc:crystal_zinc",
                1,
                0,
                0f,
            ),
        )

    @Test
    fun definesExactlyTwentyOneRecipesWithApprovedCoefficientsAndSupplements() {
        assertEquals(expected, ZincProcessing.recipes())
        assertEquals(21, ZincProcessing.recipes().size)
        assertEquals(
            21,
            ZincProcessing
                .recipes()
                .map { it.path }
                .distinct()
                .size,
        )
        assertThrows(UnsupportedOperationException::class.java) {
            (ZincProcessing.recipes() as MutableList<ZincProcessing.ProcessingRecipe>).clear()
        }
    }

    @Test
    fun allTwelveProcessingBranchesResolveToTheirApprovedFinalIngotYields() {
        val byPath = ZincProcessing.recipes().associateBy { it.path }
        val yields =
            mapOf(
                "dust/from_ore" to (1 to 2),
                "clump/from_ore" to (1 to 3),
                "shard/from_ore" to (1 to 4),
                "slurry/dirty/from_ore" to (1 to 5),
                "dust/from_raw_ore" to (3 to 4),
                "clump/from_raw_ore" to (1 to 2),
                "shard/from_raw_ore" to (3 to 8),
                "slurry/dirty/from_raw_ore" to (3 to 10),
                "dust/from_raw_block" to (1 to 12),
                "clump/from_raw_block" to (1 to 18),
                "shard/from_raw_block" to (1 to 24),
                "slurry/dirty/from_raw_block" to (1 to 30),
            )
        for ((path, yield) in yields) {
            val entry = byPath.getValue(path)
            assertEquals(yield.first, entry.inputCount, path)
            val stages =
                when (entry.kind) {
                    ZincProcessing.Kind.ENRICHING -> listOf("ingot/from_dust_smelting")
                    ZincProcessing.Kind.PURIFYING ->
                        listOf(
                            "dirty_dust/from_clump",
                            "dust/from_dirty_dust",
                            "ingot/from_dust_smelting",
                        )
                    ZincProcessing.Kind.INJECTING ->
                        listOf(
                            "clump/from_shard",
                            "dirty_dust/from_clump",
                            "dust/from_dirty_dust",
                            "ingot/from_dust_smelting",
                        )
                    ZincProcessing.Kind.DISSOLUTION ->
                        listOf(
                            "slurry/clean",
                            "crystal/from_slurry",
                            "shard/from_crystal",
                            "clump/from_shard",
                            "dirty_dust/from_clump",
                            "dust/from_dirty_dust",
                            "ingot/from_dust_smelting",
                        )
                    else -> error("Not a primary branch: ${entry.kind}")
                }
            val finalIngots =
                stages.fold(entry.outputCount) { amount, stage ->
                    val recipe = byPath.getValue(stage)
                    assertEquals(0, amount % recipe.inputCount, "$stage must not leave a partial batch")
                    amount / recipe.inputCount * recipe.outputCount
                }
            assertEquals(yield.second, finalIngots, path)
        }
    }
}
