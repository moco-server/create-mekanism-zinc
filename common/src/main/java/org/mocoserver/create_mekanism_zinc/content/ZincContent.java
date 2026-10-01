package org.mocoserver.create_mekanism_zinc.content;

import java.util.List;
import java.util.Objects;

/** Pure Java content identities shared by runtime registration and data generation. */
public final class ZincContent {
    public static final String MOD_ID = "create_mekanism_zinc";

    public static final List<ItemDefinition> ITEMS = List.of(
            new ItemDefinition("dust_zinc", "c:dusts/zinc"),
            new ItemDefinition("dirty_dust_zinc", "c:dirty_dusts/zinc"),
            new ItemDefinition("clump_zinc", "c:clumps/zinc"),
            new ItemDefinition("shard_zinc", "c:shards/zinc"),
            new ItemDefinition("crystal_zinc", "c:crystals/zinc"));

    /** Display colors are provisional. */
    public static final List<ChemicalDefinition> CHEMICALS = List.of(
            new ChemicalDefinition("dirty_zinc", SlurryKind.DIRTY, 0x59685F),
            new ChemicalDefinition("clean_zinc", SlurryKind.CLEAN, 0xB6C8C0));

    private ZincContent() {}

    public enum SlurryKind { DIRTY, CLEAN }

    public record ItemDefinition(String id, String tag) {
        public ItemDefinition {
            Objects.requireNonNull(id);
            Objects.requireNonNull(tag);
        }
    }

    public record ChemicalDefinition(String id, SlurryKind kind, int tint) {
        public ChemicalDefinition {
            Objects.requireNonNull(id);
            Objects.requireNonNull(kind);
        }
    }
}
