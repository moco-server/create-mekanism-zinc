package org.mocoserver.create_mekanism_zinc.processing;

import java.util.List;
import java.util.Objects;

/** Milestone-one recipe coefficients, independent of Minecraft and loader APIs. */
public final class ZincProcessing {
    private static final List<ProcessingRecipe> RECIPES = List.of(
            new ProcessingRecipe("dust/from_ore", Kind.ENRICHING, "c:ores/zinc", true, 1, "create_mekanism_zinc:dust_zinc", 2, 0, 0),
            new ProcessingRecipe("dust/from_raw_ore", Kind.ENRICHING, "c:raw_materials/zinc", true, 3, "create_mekanism_zinc:dust_zinc", 4, 0, 0),
            new ProcessingRecipe("dust/from_raw_block", Kind.ENRICHING, "c:storage_blocks/raw_zinc", true, 1, "create_mekanism_zinc:dust_zinc", 12, 0, 0),
            new ProcessingRecipe("dust/from_ingot", Kind.CRUSHING, "c:ingots/zinc", true, 1, "create_mekanism_zinc:dust_zinc", 1, 0, 0),
            new ProcessingRecipe("ingot/from_dust_smelting", Kind.SMELTING, "create_mekanism_zinc:dust_zinc", false, 1, "create:zinc_ingot", 1, 200, 0.35f),
            new ProcessingRecipe("ingot/from_dust_blasting", Kind.BLASTING, "create_mekanism_zinc:dust_zinc", false, 1, "create:zinc_ingot", 1, 100, 0.35f));

    private ZincProcessing() {}

    public static List<ProcessingRecipe> recipes() {
        return RECIPES;
    }

    public enum Kind { ENRICHING, CRUSHING, SMELTING, BLASTING }

    public record ProcessingRecipe(String path, Kind kind, String inputId, boolean inputTag, int inputCount,
                                   String outputId, int outputCount, int cookingTicks, float experience) {
        public ProcessingRecipe {
            Objects.requireNonNull(path);
            Objects.requireNonNull(kind);
            Objects.requireNonNull(inputId);
            Objects.requireNonNull(outputId);
        }
    }
}
