// 既存のpackage名を維持し、移行以外の識別子変更を避ける。
@file:Suppress("ktlint:standard:package-name", "PackageNaming")

package org.mocoserver.create_mekanism_zinc.content

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZincContentTest {
    @Test
    fun definesExactlyTheFiveZincItemsAndTwoSlurries() {
        assertEquals(
            setOf(
                "dust_zinc",
                "dirty_dust_zinc",
                "clump_zinc",
                "shard_zinc",
                "crystal_zinc",
            ),
            ZincContent.ITEMS
                .map {
                    it.id
                }.toSet(),
        )
        assertEquals(setOf("dirty_zinc", "clean_zinc"), ZincContent.CHEMICALS.map { it.id }.toSet())
        assertEquals("create_mekanism_zinc", ZincContent.MOD_ID)
    }

    @Test
    fun registryPathsAreValidAndUniqueAcrossContent() {
        val paths = ZincContent.ITEMS.map { it.id } + ZincContent.CHEMICALS.map { it.id }
        assertEquals(paths.size, paths.toSet().size)
        assertTrue(paths.all { it.matches(Regex("[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*")) })
    }

    @Test
    fun eachItemUsesItsSpecifiedCommonTag() {
        assertEquals(
            mapOf(
                "dust_zinc" to "c:dusts/zinc",
                "dirty_dust_zinc" to "c:dirty_dusts/zinc",
                "clump_zinc" to "c:clumps/zinc",
                "shard_zinc" to "c:shards/zinc",
                "crystal_zinc" to "c:crystals/zinc",
            ),
            ZincContent.ITEMS.associate { it.id to it.tag },
        )
    }

    @Test
    fun slurryKindsAndProvisionalTintsMatchDefinitions() {
        val chemicals = ZincContent.CHEMICALS.associateBy { it.id }
        assertEquals(ZincContent.SlurryKind.DIRTY, chemicals.getValue("dirty_zinc").kind)
        assertEquals(0x59685F, chemicals.getValue("dirty_zinc").tint)
        assertEquals(ZincContent.SlurryKind.CLEAN, chemicals.getValue("clean_zinc").kind)
        assertEquals(0xB6C8C0, chemicals.getValue("clean_zinc").tint)
    }

    @Test
    fun publishedDefinitionsCannotBeMutatedByConsumers() {
        assertThrows(UnsupportedOperationException::class.java) {
            (ZincContent.ITEMS as MutableList<ZincContent.ItemDefinition>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (ZincContent.CHEMICALS as MutableList<ZincContent.ChemicalDefinition>).clear()
        }
    }
}
