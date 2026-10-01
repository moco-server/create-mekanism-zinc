package org.mocoserver.create_mekanism_zinc.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ZincProcessingTest {
    private static final List<ZincProcessing.ProcessingRecipe> EXPECTED = List.of(
            recipe("dust/from_ore", ZincProcessing.Kind.ENRICHING, "c:ores/zinc", true, 1,
                    "create_mekanism_zinc:dust_zinc", 2, 0, 0),
            recipe("dust/from_raw_ore", ZincProcessing.Kind.ENRICHING, "c:raw_materials/zinc", true, 3,
                    "create_mekanism_zinc:dust_zinc", 4, 0, 0),
            recipe("dust/from_raw_block", ZincProcessing.Kind.ENRICHING, "c:storage_blocks/raw_zinc", true, 1,
                    "create_mekanism_zinc:dust_zinc", 12, 0, 0),
            recipe("dust/from_ingot", ZincProcessing.Kind.CRUSHING, "c:ingots/zinc", true, 1,
                    "create_mekanism_zinc:dust_zinc", 1, 0, 0),
            recipe("ingot/from_dust_smelting", ZincProcessing.Kind.SMELTING, "create_mekanism_zinc:dust_zinc", false, 1,
                    "create:zinc_ingot", 1, 200, 0.35f),
            recipe("ingot/from_dust_blasting", ZincProcessing.Kind.BLASTING, "create_mekanism_zinc:dust_zinc", false, 1,
                    "create:zinc_ingot", 1, 100, 0.35f),
            recipe("clump/from_ore", ZincProcessing.Kind.PURIFYING, "c:ores/zinc", true, 1,
                    "create_mekanism_zinc:clump_zinc", 3, 0, 0, chemical("mekanism:oxygen", 1), null, true),
            recipe("clump/from_raw_ore", ZincProcessing.Kind.PURIFYING, "c:raw_materials/zinc", true, 1,
                    "create_mekanism_zinc:clump_zinc", 2, 0, 0, chemical("mekanism:oxygen", 1), null, true),
            recipe("clump/from_raw_block", ZincProcessing.Kind.PURIFYING, "c:storage_blocks/raw_zinc", true, 1,
                    "create_mekanism_zinc:clump_zinc", 18, 0, 0, chemical("mekanism:oxygen", 2), null, true),
            recipe("clump/from_shard", ZincProcessing.Kind.PURIFYING, "c:shards/zinc", true, 1,
                    "create_mekanism_zinc:clump_zinc", 1, 0, 0, chemical("mekanism:oxygen", 1), null, true),
            recipe("dirty_dust/from_clump", ZincProcessing.Kind.CRUSHING, "c:clumps/zinc", true, 1,
                    "create_mekanism_zinc:dirty_dust_zinc", 1, 0, 0),
            recipe("dust/from_dirty_dust", ZincProcessing.Kind.ENRICHING, "c:dirty_dusts/zinc", true, 1,
                    "create_mekanism_zinc:dust_zinc", 1, 0, 0),
            recipe("shard/from_ore", ZincProcessing.Kind.INJECTING, "c:ores/zinc", true, 1,
                    "create_mekanism_zinc:shard_zinc", 4, 0, 0, chemical("mekanism:hydrogen_chloride", 1), null, true),
            recipe("shard/from_raw_ore", ZincProcessing.Kind.INJECTING, "c:raw_materials/zinc", true, 3,
                    "create_mekanism_zinc:shard_zinc", 8, 0, 0, chemical("mekanism:hydrogen_chloride", 1), null, true),
            recipe("shard/from_raw_block", ZincProcessing.Kind.INJECTING, "c:storage_blocks/raw_zinc", true, 1,
                    "create_mekanism_zinc:shard_zinc", 24, 0, 0, chemical("mekanism:hydrogen_chloride", 2), null, true),
            recipe("shard/from_crystal", ZincProcessing.Kind.INJECTING, "c:crystals/zinc", true, 1,
                    "create_mekanism_zinc:shard_zinc", 1, 0, 0, chemical("mekanism:hydrogen_chloride", 1), null, true),
            recipe("slurry/dirty/from_ore", ZincProcessing.Kind.DISSOLUTION, "c:ores/zinc", true, 1,
                    "create_mekanism_zinc:dirty_zinc", 1000, 0, 0, chemical("mekanism:sulfuric_acid", 1), null, true),
            recipe("slurry/dirty/from_raw_ore", ZincProcessing.Kind.DISSOLUTION, "c:raw_materials/zinc", true, 3,
                    "create_mekanism_zinc:dirty_zinc", 2000, 0, 0, chemical("mekanism:sulfuric_acid", 1), null, true),
            recipe("slurry/dirty/from_raw_block", ZincProcessing.Kind.DISSOLUTION, "c:storage_blocks/raw_zinc", true, 1,
                    "create_mekanism_zinc:dirty_zinc", 6000, 0, 0, chemical("mekanism:sulfuric_acid", 2), null, true),
            recipe("slurry/clean", ZincProcessing.Kind.WASHING, "create_mekanism_zinc:dirty_zinc", false, 1,
                    "create_mekanism_zinc:clean_zinc", 1, 0, 0, null, new ZincProcessing.FluidInput("minecraft:water", 5), false),
            recipe("crystal/from_slurry", ZincProcessing.Kind.CRYSTALLIZING, "create_mekanism_zinc:clean_zinc", false, 200,
                    "create_mekanism_zinc:crystal_zinc", 1, 0, 0));

    @Test
    void definesExactlyTwentyOneRecipesWithApprovedCoefficientsAndSupplements() {
        assertEquals(EXPECTED, ZincProcessing.recipes());
        assertEquals(21, ZincProcessing.recipes().size());
        assertEquals(21, ZincProcessing.recipes().stream().map(ZincProcessing.ProcessingRecipe::path).distinct().count());
        assertThrows(UnsupportedOperationException.class, () -> ZincProcessing.recipes().clear());
    }

    @Test
    void allTwelveProcessingBranchesResolveToTheirApprovedFinalIngotYields() {
        var byPath = ZincProcessing.recipes().stream()
                .collect(Collectors.toUnmodifiableMap(ZincProcessing.ProcessingRecipe::path, Function.identity()));
        Map<String, FinalYield> expected = Map.ofEntries(
                Map.entry("dust/from_ore", new FinalYield(1, 2)),
                Map.entry("clump/from_ore", new FinalYield(1, 3)),
                Map.entry("shard/from_ore", new FinalYield(1, 4)),
                Map.entry("slurry/dirty/from_ore", new FinalYield(1, 5)),
                Map.entry("dust/from_raw_ore", new FinalYield(3, 4)),
                Map.entry("clump/from_raw_ore", new FinalYield(1, 2)),
                Map.entry("shard/from_raw_ore", new FinalYield(3, 8)),
                Map.entry("slurry/dirty/from_raw_ore", new FinalYield(3, 10)),
                Map.entry("dust/from_raw_block", new FinalYield(1, 12)),
                Map.entry("clump/from_raw_block", new FinalYield(1, 18)),
                Map.entry("shard/from_raw_block", new FinalYield(1, 24)),
                Map.entry("slurry/dirty/from_raw_block", new FinalYield(1, 30)));

        assertEquals(expected.size(), 12);
        expected.forEach((path, yield) -> {
            assertEquals(yield.inputCount(), byPath.get(path).inputCount(), path);
            assertEquals(yield.finalIngots(), resolveFinalIngots(byPath.get(path), byPath), path);
        });
    }

    private static int resolveFinalIngots(ZincProcessing.ProcessingRecipe entry,
                                          Map<String, ZincProcessing.ProcessingRecipe> recipes) {
        return switch (entry.kind()) {
            case ENRICHING -> entry.outputCount() * recipes.get("ingot/from_dust_smelting").outputCount();
            case PURIFYING -> entry.outputCount()
                    * recipes.get("dirty_dust/from_clump").outputCount()
                    * recipes.get("dust/from_dirty_dust").outputCount()
                    * recipes.get("ingot/from_dust_smelting").outputCount();
            case INJECTING -> entry.outputCount()
                    * recipes.get("clump/from_shard").outputCount()
                    * recipes.get("dirty_dust/from_clump").outputCount()
                    * recipes.get("dust/from_dirty_dust").outputCount()
                    * recipes.get("ingot/from_dust_smelting").outputCount();
            case DISSOLUTION -> entry.outputCount()
                    / recipes.get("slurry/clean").inputCount()
                    / recipes.get("crystal/from_slurry").inputCount()
                    * recipes.get("crystal/from_slurry").outputCount();
            default -> throw new AssertionError("Not a primary raw zinc branch: " + entry.kind());
        };
    }

    private static ZincProcessing.ProcessingRecipe recipe(String path, ZincProcessing.Kind kind, String inputId,
                                                           boolean inputTag, int inputCount, String outputId,
                                                           int outputCount, int cookingTicks, float experience) {
        return recipe(path, kind, inputId, inputTag, inputCount, outputId, outputCount, cookingTicks, experience,
                null, null, false);
    }

    private static ZincProcessing.ProcessingRecipe recipe(String path, ZincProcessing.Kind kind, String inputId,
                                                           boolean inputTag, int inputCount, String outputId,
                                                           int outputCount, int cookingTicks, float experience,
                                                           ZincProcessing.ChemicalInput chemicalInput,
                                                           ZincProcessing.FluidInput fluidInput, boolean perTickUsage) {
        return new ZincProcessing.ProcessingRecipe(path, kind, inputId, inputTag, inputCount, outputId, outputCount,
                cookingTicks, experience, chemicalInput, fluidInput, perTickUsage);
    }

    private static ZincProcessing.ChemicalInput chemical(String id, long amount) {
        return new ZincProcessing.ChemicalInput(id, amount);
    }

    private record FinalYield(int inputCount, int finalIngots) {}
}
