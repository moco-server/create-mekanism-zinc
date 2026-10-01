package org.mocoserver.create_mekanism_zinc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ZincRecipeGenerationTest {
    private static final Map<String, ExpectedRecipe> EXPECTED = Map.ofEntries(
            Map.entry("dust/from_ore", itemRecipe("mekanism:enriching", "input", "tag", "c:ores/zinc", 1,
                    "item", "create_mekanism_zinc:dust_zinc", 2)),
            Map.entry("dust/from_raw_ore", itemRecipe("mekanism:enriching", "input", "tag", "c:raw_materials/zinc", 3,
                    "item", "create_mekanism_zinc:dust_zinc", 4)),
            Map.entry("dust/from_raw_block", itemRecipe("mekanism:enriching", "input", "tag", "c:storage_blocks/raw_zinc", 1,
                    "item", "create_mekanism_zinc:dust_zinc", 12)),
            Map.entry("dust/from_ingot", itemRecipe("mekanism:crushing", "input", "tag", "c:ingots/zinc", 1,
                    "item", "create_mekanism_zinc:dust_zinc", 1)),
            Map.entry("ingot/from_dust_smelting", cooking("minecraft:smelting", 200)),
            Map.entry("ingot/from_dust_blasting", cooking("minecraft:blasting", 100)),
            Map.entry("clump/from_ore", itemWithChemical("mekanism:purifying", "c:ores/zinc", 1, "create_mekanism_zinc:clump_zinc", 3, "mekanism:oxygen", 1)),
            Map.entry("clump/from_raw_ore", itemWithChemical("mekanism:purifying", "c:raw_materials/zinc", 1, "create_mekanism_zinc:clump_zinc", 2, "mekanism:oxygen", 1)),
            Map.entry("clump/from_raw_block", itemWithChemical("mekanism:purifying", "c:storage_blocks/raw_zinc", 1, "create_mekanism_zinc:clump_zinc", 18, "mekanism:oxygen", 2)),
            Map.entry("clump/from_shard", itemWithChemical("mekanism:purifying", "c:shards/zinc", 1, "create_mekanism_zinc:clump_zinc", 1, "mekanism:oxygen", 1)),
            Map.entry("dirty_dust/from_clump", itemRecipe("mekanism:crushing", "input", "tag", "c:clumps/zinc", 1,
                    "item", "create_mekanism_zinc:dirty_dust_zinc", 1)),
            Map.entry("dust/from_dirty_dust", itemRecipe("mekanism:enriching", "input", "tag", "c:dirty_dusts/zinc", 1,
                    "item", "create_mekanism_zinc:dust_zinc", 1)),
            Map.entry("shard/from_ore", itemWithChemical("mekanism:injecting", "c:ores/zinc", 1, "create_mekanism_zinc:shard_zinc", 4, "mekanism:hydrogen_chloride", 1)),
            Map.entry("shard/from_raw_ore", itemWithChemical("mekanism:injecting", "c:raw_materials/zinc", 3, "create_mekanism_zinc:shard_zinc", 8, "mekanism:hydrogen_chloride", 1)),
            Map.entry("shard/from_raw_block", itemWithChemical("mekanism:injecting", "c:storage_blocks/raw_zinc", 1, "create_mekanism_zinc:shard_zinc", 24, "mekanism:hydrogen_chloride", 2)),
            Map.entry("shard/from_crystal", itemWithChemical("mekanism:injecting", "c:crystals/zinc", 1, "create_mekanism_zinc:shard_zinc", 1, "mekanism:hydrogen_chloride", 1)),
            Map.entry("slurry/dirty/from_ore", itemWithChemical("mekanism:dissolution", "c:ores/zinc", 1, "create_mekanism_zinc:dirty_zinc", 1000, "mekanism:sulfuric_acid", 1)),
            Map.entry("slurry/dirty/from_raw_ore", itemWithChemical("mekanism:dissolution", "c:raw_materials/zinc", 3, "create_mekanism_zinc:dirty_zinc", 2000, "mekanism:sulfuric_acid", 1)),
            Map.entry("slurry/dirty/from_raw_block", itemWithChemical("mekanism:dissolution", "c:storage_blocks/raw_zinc", 1, "create_mekanism_zinc:dirty_zinc", 6000, "mekanism:sulfuric_acid", 2)),
            Map.entry("slurry/clean", washing()),
            Map.entry("crystal/from_slurry", crystallizing()));

    @Test
    void generatedRecipesMatchIndependentCoefficientsAndSupplementaryInputs() throws IOException, URISyntaxException {
        var recipeFiles = generatedRecipePaths();
        assertEquals(EXPECTED.keySet(), recipeFiles);
        assertEquals(21, recipeFiles.size());
        for (var path : recipeFiles) {
            var expected = EXPECTED.get(path);
            var recipe = json("data/create_mekanism_zinc/recipe/processing/zinc/" + path + ".json");
            assertEquals(expected.type(), recipe.get("type").getAsString(), path);
            var input = recipe.getAsJsonObject(expected.inputField());
            assertEquals(expected.inputId(), input.get(expected.inputSelector()).getAsString(), path);
            if (expected.inputAmountKey() != null) {
                assertEquals(expected.inputCount(), input.get(expected.inputAmountKey()).getAsLong(), path);
            }
            var output = recipe.getAsJsonObject(expected.outputField());
            assertEquals(expected.outputId(), output.get("id").getAsString(), path);
            assertEquals(expected.outputCount(), output.get(expected.outputAmountKey()).getAsLong(), path);
            if (expected.chemicalInput() != null) {
                var chemical = recipe.getAsJsonObject("chemical_input");
                assertEquals(expected.chemicalInput().id(), chemical.get("chemical").getAsString(), path);
                assertEquals(expected.chemicalInput().amount(), chemical.get("amount").getAsLong(), path);
            } else if (!expected.inputField().equals("chemical_input")) {
                assertFalse(recipe.has("chemical_input"), path);
            }
            if (expected.fluidInput() != null) {
                var fluid = recipe.getAsJsonObject("fluid_input");
                assertEquals(expected.fluidInput().tag(), fluid.get("tag").getAsString(), path);
                assertEquals(expected.fluidInput().amount(), fluid.get("amount").getAsInt(), path);
            } else {
                assertFalse(recipe.has("fluid_input"), path);
            }
            assertEquals(expected.perTickUsage(), recipe.has("per_tick_usage"), path);
            if (expected.perTickUsage()) assertTrue(recipe.get("per_tick_usage").getAsBoolean(), path);
            assertFalse(recipe.toString().contains("crushed_raw_zinc"), path);
        }
        assertCooking("ingot/from_dust_smelting", 200);
        assertCooking("ingot/from_dust_blasting", 100);
    }

    @Test
    void generatedTagsAndChemicalSolidTagMatchTheContentDefinitions() throws IOException {
        Map.of("dusts", "dust_zinc", "dirty_dusts", "dirty_dust_zinc", "clumps", "clump_zinc",
                        "shards", "shard_zinc", "crystals", "crystal_zinc")
                .forEach((category, item) -> {
                    try {
                        assertEquals(1, json("data/c/tags/item/" + category + "/zinc.json").getAsJsonArray("values").size());
                        assertEquals("create_mekanism_zinc:" + item,
                                json("data/c/tags/item/" + category + "/zinc.json").getAsJsonArray("values").get(0).getAsString());
                        assertEquals(1, json("data/c/tags/item/" + category + ".json").getAsJsonArray("values").size());
                        assertEquals("#c:" + category + "/zinc",
                                json("data/c/tags/item/" + category + ".json").getAsJsonArray("values").get(0).getAsString());
                    } catch (IOException e) {
                        throw new AssertionError(e);
                    }
                });
        var map = json("data/mekanism/data_maps/mekanism/chemical/chemical_solid_tag.json").getAsJsonObject("values");
        assertEquals(1, map.size());
        assertEquals("c:ores/zinc", map.getAsJsonObject("create_mekanism_zinc:clean_zinc").get("representation").getAsString());
    }

    private static void assertCooking(String path, int ticks) throws IOException {
        var recipe = json("data/create_mekanism_zinc/recipe/processing/zinc/" + path + ".json");
        assertEquals(ticks, recipe.get("cookingtime").getAsInt());
        assertEquals(0.35f, recipe.get("experience").getAsFloat());
    }

    private static Set<String> generatedRecipePaths() throws IOException, URISyntaxException {
        var resourceRoot = "data/create_mekanism_zinc/recipe/processing/zinc";
        var directory = ZincRecipeGenerationTest.class.getClassLoader().getResource(resourceRoot);
        if (directory == null) throw new AssertionError("Missing generated recipe directory: " + resourceRoot);
        var root = Path.of(directory.toURI());
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(root::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/').substring(0, path.length() - ".json".length()))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    private static JsonObject json(String path) throws IOException {
        var stream = ZincRecipeGenerationTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new AssertionError("Missing generated resource: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static ExpectedRecipe itemRecipe(String type, String inputField, String inputSelector, String inputId,
                                              int inputCount, String outputType, String outputId, int outputCount) {
        return new ExpectedRecipe(type, inputField, inputSelector, inputId, inputField.equals("ingredient") ? null : "count",
                inputCount, "output", outputType, outputId, outputType.equals("item") ? "count" : "amount", outputCount,
                null, null, false, 0, 0);
    }

    private static ExpectedRecipe itemWithChemical(String type, String inputTag, int inputCount, String outputId,
                                                    int outputCount, String chemicalId, long chemicalAmount) {
        return new ExpectedRecipe(type, "item_input", "tag", inputTag, "count", inputCount,
                "output", type.equals("mekanism:dissolution") ? "chemical" : "item", outputId,
                type.equals("mekanism:dissolution") ? "amount" : "count", outputCount,
                new ChemicalInput(chemicalId, chemicalAmount), null, true, 0, 0);
    }

    private static ExpectedRecipe washing() {
        return new ExpectedRecipe("mekanism:washing", "chemical_input", "chemical", "create_mekanism_zinc:dirty_zinc", "amount", 1,
                "output", "chemical", "create_mekanism_zinc:clean_zinc", "amount", 1, null,
                new FluidInput("minecraft:water", 5), false, 0, 0);
    }

    private static ExpectedRecipe crystallizing() {
        return new ExpectedRecipe("mekanism:crystallizing", "input", "chemical", "create_mekanism_zinc:clean_zinc", "amount", 200,
                "output", "item", "create_mekanism_zinc:crystal_zinc", "count", 1, null, null, false, 0, 0);
    }

    private static ExpectedRecipe cooking(String type, int ticks) {
        var id = "create_mekanism_zinc:dust_zinc";
        var output = "create:zinc_ingot";
        return new ExpectedRecipe(type, "ingredient", "item", id, null, 1,
                "result", "item", output, "count", 1, null, null, false, ticks, 0.35f);
    }

    private record ChemicalInput(String id, long amount) {}
    private record FluidInput(String tag, int amount) {}
    private record ExpectedRecipe(String type, String inputField, String inputSelector, String inputId,
                                  String inputAmountKey, long inputCount, String outputField, String outputType,
                                  String outputId, String outputAmountKey, long outputCount,
                                  ChemicalInput chemicalInput, FluidInput fluidInput, boolean perTickUsage,
                                  int cookingTicks, float experience) {}
}
