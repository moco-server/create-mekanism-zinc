// 既存packageを維持する。
@file:Suppress("ktlint:standard:package-name", "PackageNaming")

package org.mocoserver.create_mekanism_zinc

import mekanism.api.MekanismAPI
import mekanism.api.chemical.Chemical
import mekanism.api.chemical.ChemicalBuilder
import net.minecraft.world.item.Item
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import org.mocoserver.create_mekanism_zinc.content.ZincContent
import java.util.Collections
import java.util.function.Supplier

/** coreの識別子をNeoForgeの登録へ変換する。登録順は定義一覧の順を維持する。 */
object ZincRegistries {
    @JvmField
    val ITEMS: DeferredRegister.Items = DeferredRegister.Items.createItems(ZincContent.MOD_ID)

    @JvmField
    val CHEMICALS: DeferredRegister<Chemical> =
        DeferredRegister.create(
            MekanismAPI.CHEMICAL_REGISTRY_NAME,
            ZincContent.MOD_ID,
        )

    @JvmField
    val REGISTERED_ITEMS: Map<String, DeferredHolder<Item, Item>> =
        Collections.unmodifiableMap(
            ZincContent.ITEMS.associateTo(linkedMapOf()) { it.id to ITEMS.registerSimpleItem(it.id) },
        )

    @JvmField
    val REGISTERED_CHEMICALS: Map<String, DeferredHolder<Chemical, Chemical>> =
        Collections.unmodifiableMap(
            ZincContent.CHEMICALS.associateTo(linkedMapOf()) { definition ->
                definition.id to
                    CHEMICALS.register(
                        definition.id,
                        Supplier {
                            val builder =
                                when (definition.kind) {
                                    ZincContent.SlurryKind.DIRTY -> ChemicalBuilder.dirtySlurry()
                                    ZincContent.SlurryKind.CLEAN -> ChemicalBuilder.cleanSlurry()
                                }
                            Chemical(builder.tint(definition.tint))
                        },
                    )
            },
        )
}
