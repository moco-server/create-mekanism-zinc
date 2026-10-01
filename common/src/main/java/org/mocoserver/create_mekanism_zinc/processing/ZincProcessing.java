package org.mocoserver.create_mekanism_zinc.processing;

import java.util.List;
import java.util.Objects;

/** Zinc processing coefficients, independent of Minecraft and loader APIs. */
public final class ZincProcessing {
    private static final List<ProcessingRecipe> RECIPES = List.of(
            new ProcessingRecipe("dust/from_ore", Kind.ENRICHING, "c:ores/zinc", true, 1, "create_mekanism_zinc:dust_zinc", 2, 0, 0),
            new ProcessingRecipe("dust/from_raw_ore", Kind.ENRICHING, "c:raw_materials/zinc", true, 3, "create_mekanism_zinc:dust_zinc", 4, 0, 0),
            new ProcessingRecipe("dust/from_raw_block", Kind.ENRICHING, "c:storage_blocks/raw_zinc", true, 1, "create_mekanism_zinc:dust_zinc", 12, 0, 0),
            new ProcessingRecipe("dust/from_ingot", Kind.CRUSHING, "c:ingots/zinc", true, 1, "create_mekanism_zinc:dust_zinc", 1, 0, 0),
            new ProcessingRecipe("ingot/from_dust_smelting", Kind.SMELTING, "create_mekanism_zinc:dust_zinc", false, 1, "create:zinc_ingot", 1, 200, 0.35f),
            new ProcessingRecipe("ingot/from_dust_blasting", Kind.BLASTING, "create_mekanism_zinc:dust_zinc", false, 1, "create:zinc_ingot", 1, 100, 0.35f),
            new ProcessingRecipe("clump/from_ore", Kind.PURIFYING, "c:ores/zinc", true, 1, "create_mekanism_zinc:clump_zinc", 3, 0, 0,
                    new ChemicalInput("mekanism:oxygen", 1), null, true),
            new ProcessingRecipe("clump/from_raw_ore", Kind.PURIFYING, "c:raw_materials/zinc", true, 1, "create_mekanism_zinc:clump_zinc", 2, 0, 0,
                    new ChemicalInput("mekanism:oxygen", 1), null, true),
            new ProcessingRecipe("clump/from_raw_block", Kind.PURIFYING, "c:storage_blocks/raw_zinc", true, 1,
                    "create_mekanism_zinc:clump_zinc", 18, 0, 0, new ChemicalInput("mekanism:oxygen", 2), null, true),
            new ProcessingRecipe("clump/from_shard", Kind.PURIFYING, "c:shards/zinc", true, 1, "create_mekanism_zinc:clump_zinc", 1, 0, 0,
                    new ChemicalInput("mekanism:oxygen", 1), null, true),
            new ProcessingRecipe("dirty_dust/from_clump", Kind.CRUSHING, "c:clumps/zinc", true, 1,
                    "create_mekanism_zinc:dirty_dust_zinc", 1, 0, 0),
            new ProcessingRecipe("dust/from_dirty_dust", Kind.ENRICHING, "c:dirty_dusts/zinc", true, 1,
                    "create_mekanism_zinc:dust_zinc", 1, 0, 0),
            new ProcessingRecipe("shard/from_ore", Kind.INJECTING, "c:ores/zinc", true, 1, "create_mekanism_zinc:shard_zinc", 4, 0, 0,
                    new ChemicalInput("mekanism:hydrogen_chloride", 1), null, true),
            new ProcessingRecipe("shard/from_raw_ore", Kind.INJECTING, "c:raw_materials/zinc", true, 3,
                    "create_mekanism_zinc:shard_zinc", 8, 0, 0, new ChemicalInput("mekanism:hydrogen_chloride", 1), null, true),
            new ProcessingRecipe("shard/from_raw_block", Kind.INJECTING, "c:storage_blocks/raw_zinc", true, 1,
                    "create_mekanism_zinc:shard_zinc", 24, 0, 0, new ChemicalInput("mekanism:hydrogen_chloride", 2), null, true),
            new ProcessingRecipe("shard/from_crystal", Kind.INJECTING, "c:crystals/zinc", true, 1,
                    "create_mekanism_zinc:shard_zinc", 1, 0, 0, new ChemicalInput("mekanism:hydrogen_chloride", 1), null, true),
            new ProcessingRecipe("slurry/dirty/from_ore", Kind.DISSOLUTION, "c:ores/zinc", true, 1,
                    "create_mekanism_zinc:dirty_zinc", 1000, 0, 0, new ChemicalInput("mekanism:sulfuric_acid", 1), null, true),
            new ProcessingRecipe("slurry/dirty/from_raw_ore", Kind.DISSOLUTION, "c:raw_materials/zinc", true, 3,
                    "create_mekanism_zinc:dirty_zinc", 2000, 0, 0, new ChemicalInput("mekanism:sulfuric_acid", 1), null, true),
            new ProcessingRecipe("slurry/dirty/from_raw_block", Kind.DISSOLUTION, "c:storage_blocks/raw_zinc", true, 1,
                    "create_mekanism_zinc:dirty_zinc", 6000, 0, 0, new ChemicalInput("mekanism:sulfuric_acid", 2), null, true),
            new ProcessingRecipe("slurry/clean", Kind.WASHING, "create_mekanism_zinc:dirty_zinc", false, 1,
                    "create_mekanism_zinc:clean_zinc", 1, 0, 0, null, new FluidInput("minecraft:water", 5), false),
            new ProcessingRecipe("crystal/from_slurry", Kind.CRYSTALLIZING, "create_mekanism_zinc:clean_zinc", false, 200,
                    "create_mekanism_zinc:crystal_zinc", 1, 0, 0));

    private ZincProcessing() {}

    public static List<ProcessingRecipe> recipes() {
        return RECIPES;
    }

    public enum Kind { ENRICHING, CRUSHING, SMELTING, BLASTING, PURIFYING, INJECTING, DISSOLUTION, WASHING, CRYSTALLIZING }

    public record ChemicalInput(String id, long amount) {
        public ChemicalInput {
            Objects.requireNonNull(id);
        }
    }

    public record FluidInput(String tag, int amount) {
        public FluidInput {
            Objects.requireNonNull(tag);
        }
    }

    public record ProcessingRecipe(String path, Kind kind, String inputId, boolean inputTag, int inputCount,
                                   String outputId, int outputCount, int cookingTicks, float experience,
                                   ChemicalInput chemicalInput, FluidInput fluidInput, boolean perTickUsage) {
        public ProcessingRecipe(String path, Kind kind, String inputId, boolean inputTag, int inputCount,
                                String outputId, int outputCount, int cookingTicks, float experience) {
            this(path, kind, inputId, inputTag, inputCount, outputId, outputCount, cookingTicks, experience,
                    null, null, false);
        }

        public ProcessingRecipe {
            Objects.requireNonNull(path);
            Objects.requireNonNull(kind);
            Objects.requireNonNull(inputId);
            Objects.requireNonNull(outputId);
        }
    }
}
