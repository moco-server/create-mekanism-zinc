package org.mocoserver.create_mekanism_zinc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.mocoserver.create_mekanism_zinc.content.ZincContent;

/** NeoForge-owned runtime registrations for the pure Java content definitions. */
public final class ZincRegistries {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.Items.createItems(ZincContent.MOD_ID);
    public static final DeferredRegister<Chemical> CHEMICALS =
            DeferredRegister.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, ZincContent.MOD_ID);

    public static final Map<String, DeferredHolder<Item, Item>> REGISTERED_ITEMS;
    public static final Map<String, DeferredHolder<Chemical, Chemical>> REGISTERED_CHEMICALS;

    static {
        var items = new LinkedHashMap<String, DeferredHolder<Item, Item>>();
        ZincContent.ITEMS.forEach(definition -> items.put(definition.id(), ITEMS.registerSimpleItem(definition.id())));
        REGISTERED_ITEMS = Collections.unmodifiableMap(items);

        var chemicals = new LinkedHashMap<String, DeferredHolder<Chemical, Chemical>>();
        ZincContent.CHEMICALS.forEach(definition -> chemicals.put(definition.id(), CHEMICALS.register(
                definition.id(), () -> new Chemical(switch (definition.kind()) {
                    case DIRTY -> ChemicalBuilder.dirtySlurry().tint(definition.tint());
                    case CLEAN -> ChemicalBuilder.cleanSlurry().tint(definition.tint());
                }))));
        REGISTERED_CHEMICALS = Collections.unmodifiableMap(chemicals);
    }

    private ZincRegistries() {}
}
