// 既存のpackage名を維持し、移行以外の識別子変更を避ける。
@file:Suppress("ktlint:standard:package-name", "PackageNaming")

package org.mocoserver.create_mekanism_zinc.content

import java.util.Collections

/** 登録とdatagenで共有する識別子。公開一覧は消費側から変更できない。 */
public object ZincContent {
    public const val MOD_ID: String = "create_mekanism_zinc"

    @JvmField
    public val ITEMS: List<ItemDefinition> =
        Collections.unmodifiableList(
            listOf(
                ItemDefinition("dust_zinc", "c:dusts/zinc"),
                ItemDefinition("dirty_dust_zinc", "c:dirty_dusts/zinc"),
                ItemDefinition("clump_zinc", "c:clumps/zinc"),
                ItemDefinition("shard_zinc", "c:shards/zinc"),
                ItemDefinition("crystal_zinc", "c:crystals/zinc"),
            ),
        )

    // 表示色は既存版の固定RGB値をそのまま維持する。
    @Suppress("MagicNumber")
    @JvmField
    public val CHEMICALS: List<ChemicalDefinition> =
        Collections.unmodifiableList(
            listOf(
                ChemicalDefinition("dirty_zinc", SlurryKind.DIRTY, 0x59685F),
                ChemicalDefinition("clean_zinc", SlurryKind.CLEAN, 0xB6C8C0),
            ),
        )

    public enum class SlurryKind { DIRTY, CLEAN }

    public data class ItemDefinition(
        public val id: String,
        public val tag: String,
    )

    public data class ChemicalDefinition(
        public val id: String,
        public val kind: SlurryKind,
        public val tint: Int,
    )
}
