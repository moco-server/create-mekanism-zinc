// 既存packageを維持する。
@file:Suppress("ktlint:standard:package-name", "PackageNaming")

package org.mocoserver.create_mekanism_zinc

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/** Java版で受け入れ済みのJSONを固定fixtureとして使い、datagenから期待値を作らない。 */
class ZincRecipeGenerationTest {
    private val approved = json("approved-zinc-data.json")

    @Test
    fun generatedRecipesMatchIndependentCoefficientsAndSupplementaryInputs() {
        val prefix = "data/create_mekanism_zinc/recipe/processing/zinc/"
        val expected = approved.keySet().filter { it.startsWith(prefix) }.toSet()
        assertEquals(21, expected.size)
        val directory = requireNotNull(javaClass.classLoader.getResource(prefix))
        val root = Path.of(directory.toURI())
        val actual =
            Files.walk(root).use { paths ->
                paths
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".json") }
                    .map { prefix + root.relativize(it).toString().replace('\\', '/') }
                    .toList()
                    .toSet()
            }
        assertEquals(expected, actual)
        for (path in expected) assertEquals(approved.getAsJsonObject(path), json(path), path)
    }

    @Test
    fun generatedTagsAndChemicalSolidTagMatchTheContentDefinitions() {
        val paths = approved.keySet().filter { !it.startsWith("data/create_mekanism_zinc/recipe/") }
        for (path in paths) assertEquals(approved.getAsJsonObject(path), json(path), path)
    }

    private fun json(path: String): JsonObject =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) {
            "Missing generated resource: $path"
        }.reader(Charsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject }
}
