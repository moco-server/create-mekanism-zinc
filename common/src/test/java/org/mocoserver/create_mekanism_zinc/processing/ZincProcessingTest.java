package org.mocoserver.create_mekanism_zinc.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ZincProcessingTest {
    @Test
    void definesExactlyTheSixApprovedZincRecipes() {
        var expected = List.of(
                new ZincProcessing.ProcessingRecipe("dust/from_ore", ZincProcessing.Kind.ENRICHING,
                        "c:ores/zinc", true, 1, "create_mekanism_zinc:dust_zinc", 2, 0, 0),
                new ZincProcessing.ProcessingRecipe("dust/from_raw_ore", ZincProcessing.Kind.ENRICHING,
                        "c:raw_materials/zinc", true, 3, "create_mekanism_zinc:dust_zinc", 4, 0, 0),
                new ZincProcessing.ProcessingRecipe("dust/from_raw_block", ZincProcessing.Kind.ENRICHING,
                        "c:storage_blocks/raw_zinc", true, 1, "create_mekanism_zinc:dust_zinc", 12, 0, 0),
                new ZincProcessing.ProcessingRecipe("dust/from_ingot", ZincProcessing.Kind.CRUSHING,
                        "c:ingots/zinc", true, 1, "create_mekanism_zinc:dust_zinc", 1, 0, 0),
                new ZincProcessing.ProcessingRecipe("ingot/from_dust_smelting", ZincProcessing.Kind.SMELTING,
                        "create_mekanism_zinc:dust_zinc", false, 1, "create:zinc_ingot", 1, 200, 0.35f),
                new ZincProcessing.ProcessingRecipe("ingot/from_dust_blasting", ZincProcessing.Kind.BLASTING,
                        "create_mekanism_zinc:dust_zinc", false, 1, "create:zinc_ingot", 1, 100, 0.35f));

        assertEquals(expected, ZincProcessing.recipes());
        assertThrows(UnsupportedOperationException.class, () -> ZincProcessing.recipes().clear());
    }
}
