package org.mocoserver.create_mekanism_zinc;

import net.neoforged.fml.common.Mod;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.mocoserver.create_mekanism_zinc.content.ZincContent;

@Mod(CreateMekanismZinc.MOD_ID)
public final class CreateMekanismZinc {
    public static final String MOD_ID = ZincContent.MOD_ID;

    public CreateMekanismZinc(net.neoforged.bus.api.IEventBus modEventBus) {
        ZincRegistries.ITEMS.register(modEventBus);
        ZincRegistries.CHEMICALS.register(modEventBus);
        modEventBus.addListener(ZincDataGenerators::gatherData);
        modEventBus.addListener(this::addCreativeTabItems);
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            ZincRegistries.REGISTERED_ITEMS.values().forEach(item -> event.accept(item.get()));
        }
    }
}
