// packageと検証済みの色・数量を維持する。
@file:Suppress("ktlint:standard:package-name", "PackageNaming", "MagicNumber")

package org.mocoserver.create_mekanism_zinc.tests

import mekanism.api.MekanismAPI
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import org.mocoserver.create_mekanism_zinc.CreateMekanismZinc

@GameTestHolder(CreateMekanismZinc.MOD_ID)
@PrefixGameTestTemplate(false)
object RegistrySmokeGameTests {
    private const val NAMESPACE = CreateMekanismZinc.MOD_ID

    @JvmStatic
    @GameTest(template = "empty")
    fun zincRegistryContentsAreExact(helper: GameTestHelper) {
        val itemIds =
            BuiltInRegistries.ITEM
                .keySet()
                .filter { it.namespace == NAMESPACE }
                .map { it.path }
                .toSet()
        val chemicalIds =
            MekanismAPI.CHEMICAL_REGISTRY
                .keySet()
                .filter { it.namespace == NAMESPACE }
                .map { it.path }
                .toSet()
        helper.assertTrue(
            itemIds == setOf("dust_zinc", "dirty_dust_zinc", "clump_zinc", "shard_zinc", "crystal_zinc"),
            "Unexpected item registry contents: $itemIds",
        )
        helper.assertTrue(
            chemicalIds == setOf("dirty_zinc", "clean_zinc"),
            "Unexpected chemical registry contents: $chemicalIds",
        )
        helper.assertTrue("zinc_ore" !in itemIds && "zinc_ingot" !in itemIds, "No new zinc ore or ingot is expected")
        val dirty = MekanismAPI.CHEMICAL_REGISTRY.get(ResourceLocation.fromNamespaceAndPath(NAMESPACE, "dirty_zinc"))
        val clean = MekanismAPI.CHEMICAL_REGISTRY.get(ResourceLocation.fromNamespaceAndPath(NAMESPACE, "clean_zinc"))
        helper.assertTrue(dirty.tint == 0x59685F, "Dirty slurry tint does not match provisional definition")
        helper.assertTrue(clean.tint == 0xB6C8C0, "Clean slurry tint does not match provisional definition")
        helper.succeed()
    }
}
