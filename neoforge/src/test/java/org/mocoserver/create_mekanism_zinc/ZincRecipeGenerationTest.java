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
    private static final Map<String, ExpectedRecipe> EXPECTED = Map.of(
            "dust/from_ore", new ExpectedRecipe("mekanism:enriching", "c:ores/zinc", true, 1, "create_mekanism_zinc:dust_zinc", 2, 0, 0),
            "dust/from_raw_ore", new ExpectedRecipe("mekanism:enriching", "c:raw_materials/zinc", true, 3, "create_mekanism_zinc:dust_zinc", 4, 0, 0),
            "dust/from_raw_block", new ExpectedRecipe("mekanism:enriching", "c:storage_blocks/raw_zinc", true, 1, "create_mekanism_zinc:dust_zinc", 12, 0, 0),
            "dust/from_ingot", new ExpectedRecipe("mekanism:crushing", "c:ingots/zinc", true, 1, "create_mekanism_zinc:dust_zinc", 1, 0, 0),
            "ingot/from_dust_smelting", new ExpectedRecipe("minecraft:smelting", "create_mekanism_zinc:dust_zinc", false, 1, "create:zinc_ingot", 1, 200, 0.35f),
            "ingot/from_dust_blasting", new ExpectedRecipe("minecraft:blasting", "create_mekanism_zinc:dust_zinc", false, 1, "create:zinc_ingot", 1, 100, 0.35f));

    @Test
    void generatedRecipesMatchIndependentApprovedCoefficients() throws IOException, URISyntaxException {
        var recipeFiles = generatedRecipePaths();
        assertEquals(EXPECTED.keySet(), recipeFiles);
        assertEquals(6, recipeFiles.size());
        var recipePaths = recipeFiles.stream()
                .map(path -> "data/create_mekanism_zinc/recipe/processing/zinc/" + path + ".json").toList();
        for (var path : recipePaths) {
            var recipe = json(path);
            var expected = EXPECTED.get(path.substring("data/create_mekanism_zinc/recipe/processing/zinc/".length(), path.length() - 5));
            assertEquals(expected.type(), recipe.get("type").getAsString(), path);
            var input = recipe.has("input") ? recipe.getAsJsonObject("input") : recipe.getAsJsonObject("ingredient");
            assertEquals(expected.input(), input.get(expected.tag() ? "tag" : "item").getAsString(), path);
            assertEquals(expected.inputCount(), expected.tag() ? input.get("count").getAsInt() : 1, path);
            var output = recipe.has("output") ? recipe.getAsJsonObject("output") : null;
            var outputId = output == null ? recipe.getAsJsonObject("result").get("id").getAsString() : output.get("id").getAsString();
            assertEquals(expected.output(), outputId, path);
            var outputCount = output == null ? recipe.getAsJsonObject("result").get("count").getAsInt() : output.get("count").getAsInt();
            assertEquals(expected.outputCount(), outputCount, path);
            if (expected.ticks() > 0) {
                assertEquals(expected.ticks(), recipe.get("cookingtime").getAsInt(), path);
                assertEquals(expected.experience(), recipe.get("experience").getAsFloat(), path);
            }
            assertFalse(recipe.toString().contains("crushed_raw_zinc"), path);
        }
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

    private static JsonObject json(String path) throws IOException {
        var stream = ZincRecipeGenerationTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new AssertionError("Missing generated resource: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private record ExpectedRecipe(String type, String input, boolean tag, int inputCount,
                                  String output, int outputCount, int ticks, float experience) {}
}
