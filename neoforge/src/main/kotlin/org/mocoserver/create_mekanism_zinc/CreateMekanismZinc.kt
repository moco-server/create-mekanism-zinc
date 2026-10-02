// 既存packageを維持する。
@file:Suppress("ktlint:standard:package-name", "PackageNaming")

package org.mocoserver.create_mekanism_zinc

import net.minecraft.world.item.CreativeModeTabs
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import org.mocoserver.create_mekanism_zinc.content.ZincContent

@Mod(CreateMekanismZinc.MOD_ID)
class CreateMekanismZinc(
    modEventBus: IEventBus,
) {
    init {
        ZincRegistries.ITEMS.register(modEventBus)
        ZincRegistries.CHEMICALS.register(modEventBus)
        modEventBus.addListener(ZincDataGenerators::gatherData)
        modEventBus.addListener(::addCreativeTabItems)
    }

    private fun addCreativeTabItems(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == CreativeModeTabs.INGREDIENTS) {
            ZincRegistries.REGISTERED_ITEMS.values.forEach { event.accept(it.get()) }
        }
    }

    companion object {
        const val MOD_ID: String = ZincContent.MOD_ID
    }
}
