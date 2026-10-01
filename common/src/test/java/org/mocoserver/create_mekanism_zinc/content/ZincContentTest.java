package org.mocoserver.create_mekanism_zinc.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ZincContentTest {
    private static final Pattern REGISTRY_PATH = Pattern.compile("[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*");

    @Test
    void definesExactlyTheFiveZincItemsAndTwoSlurries() {
        assertEquals(Set.of("dust_zinc", "dirty_dust_zinc", "clump_zinc", "shard_zinc", "crystal_zinc"),
                ZincContent.ITEMS.stream().map(ZincContent.ItemDefinition::id).collect(Collectors.toSet()));
        assertEquals(Set.of("dirty_zinc", "clean_zinc"),
                ZincContent.CHEMICALS.stream().map(ZincContent.ChemicalDefinition::id).collect(Collectors.toSet()));
        assertEquals("create_mekanism_zinc", ZincContent.MOD_ID);
    }

    @Test
    void registryPathsAreValidAndUniqueAcrossContent() {
        var paths = new java.util.ArrayList<String>();
        ZincContent.ITEMS.forEach(item -> paths.add(item.id()));
        ZincContent.CHEMICALS.forEach(chemical -> paths.add(chemical.id()));

        assertEquals(paths.size(), new HashSet<>(paths).size());
        assertTrue(paths.stream().allMatch(path -> REGISTRY_PATH.matcher(path).matches()));
    }

    @Test
    void eachItemUsesItsSpecifiedCommonTag() {
        Map<String, String> tags = ZincContent.ITEMS.stream()
                .collect(Collectors.toMap(ZincContent.ItemDefinition::id, ZincContent.ItemDefinition::tag));

        assertEquals(Map.of(
                "dust_zinc", "c:dusts/zinc",
                "dirty_dust_zinc", "c:dirty_dusts/zinc",
                "clump_zinc", "c:clumps/zinc",
                "shard_zinc", "c:shards/zinc",
                "crystal_zinc", "c:crystals/zinc"), tags);
    }

    @Test
    void slurryKindsAndProvisionalTintsMatchDefinitions() {
        Map<String, ZincContent.ChemicalDefinition> chemicals = ZincContent.CHEMICALS.stream()
                .collect(Collectors.toMap(ZincContent.ChemicalDefinition::id, chemical -> chemical));

        assertEquals(ZincContent.SlurryKind.DIRTY, chemicals.get("dirty_zinc").kind());
        assertEquals(0x59685F, chemicals.get("dirty_zinc").tint());
        assertEquals(ZincContent.SlurryKind.CLEAN, chemicals.get("clean_zinc").kind());
        assertEquals(0xB6C8C0, chemicals.get("clean_zinc").tint());
    }
}
