package org.mocoserver.create_mekanism_zinc.tests;

import java.util.Set;
import java.util.stream.Collectors;
import org.mocoserver.create_mekanism_zinc.CreateMekanismZinc;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateMekanismZinc.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RegistrySmokeGameTests {
    private static final String NAMESPACE = CreateMekanismZinc.MOD_ID;

    @GameTest(template = "empty")
    public static void zincRegistryContentsAreExact(GameTestHelper helper) {
        Set<String> itemIds = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals(NAMESPACE))
                .map(ResourceLocation::getPath)
                .collect(Collectors.toSet());
        Set<String> chemicalIds = MekanismAPI.CHEMICAL_REGISTRY.keySet().stream()
                .filter(id -> id.getNamespace().equals(NAMESPACE))
                .map(ResourceLocation::getPath)
                .collect(Collectors.toSet());

        helper.assertTrue(itemIds.equals(Set.of("dust_zinc", "dirty_dust_zinc", "clump_zinc", "shard_zinc", "crystal_zinc")),
                "Unexpected item registry contents: " + itemIds);
        helper.assertTrue(chemicalIds.equals(Set.of("dirty_zinc", "clean_zinc")),
                "Unexpected chemical registry contents: " + chemicalIds);
        helper.assertTrue(!itemIds.contains("zinc_ore") && !itemIds.contains("zinc_ingot"),
                "No new zinc ore or ingot is expected");

        Chemical dirty = MekanismAPI.CHEMICAL_REGISTRY.get(ResourceLocation.fromNamespaceAndPath(NAMESPACE, "dirty_zinc"));
        Chemical clean = MekanismAPI.CHEMICAL_REGISTRY.get(ResourceLocation.fromNamespaceAndPath(NAMESPACE, "clean_zinc"));
        helper.assertTrue(dirty.getTint() == 0x59685F, "Dirty slurry tint does not match provisional definition");
        helper.assertTrue(clean.getTint() == 0xB6C8C0, "Clean slurry tint does not match provisional definition");
        helper.succeed();
    }
}
