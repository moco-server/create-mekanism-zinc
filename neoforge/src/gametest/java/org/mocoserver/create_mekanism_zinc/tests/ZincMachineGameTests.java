package org.mocoserver.create_mekanism_zinc.tests;

import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.prefab.TileEntityElectricMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.mocoserver.create_mekanism_zinc.content.ZincContent;

@GameTestHolder(ZincContent.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ZincMachineGameTests {
    private static final BlockPos MACHINE = new BlockPos(1, 2, 1);
    private static final BlockPos FURNACE = new BlockPos(1, 2, 1);
    private static final TagKey<Item> ZINC_ORES = TagKey.create(Registries.ITEM, id("c", "ores/zinc"));
    private static final TagKey<Item> RAW_ZINC = TagKey.create(Registries.ITEM, id("c", "raw_materials/zinc"));
    private static final TagKey<Item> RAW_ZINC_BLOCK = TagKey.create(Registries.ITEM, id("c", "storage_blocks/raw_zinc"));
    private static final TagKey<Item> ZINC_INGOTS = TagKey.create(Registries.ITEM, id("c", "ingots/zinc"));

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void normalOreEnrichesToTwoDust(GameTestHelper helper) {
        Item ore = item("create:zinc_ore");
        helper.assertTrue(isMember(ZINC_ORES, ore), "create:zinc_ore is absent from c:ores/zinc");
        process(helper, "mekanism:enrichment_chamber", ore, 1, 2, 240, "normal ore");
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void deepslateOreEnrichesToTwoDust(GameTestHelper helper) {
        Item ore = item("create:deepslate_zinc_ore");
        helper.assertTrue(isMember(ZINC_ORES, ore), "create:deepslate_zinc_ore is absent from c:ores/zinc");
        process(helper, "mekanism:enrichment_chamber", ore, 1, 2, 240, "deepslate ore");
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void threeRawZincEnrichesToFourDust(GameTestHelper helper) {
        Item raw = item("create:raw_zinc");
        helper.assertTrue(isMember(RAW_ZINC, raw), "create:raw_zinc is absent from c:raw_materials/zinc");
        process(helper, "mekanism:enrichment_chamber", raw, 3, 4, 240, "3 raw zinc");
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void rawZincBlockEnrichesToTwelveDust(GameTestHelper helper) {
        Item block = item("create:raw_zinc_block");
        helper.assertTrue(isMember(RAW_ZINC_BLOCK, block), "create:raw_zinc_block is absent from c:storage_blocks/raw_zinc");
        process(helper, "mekanism:enrichment_chamber", block, 1, 12, 240, "raw zinc block");
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void oneRawZincIsRetained(GameTestHelper helper) {
        remainsBlocked(helper, item("create:raw_zinc"), 1, true, "one raw zinc");
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void twoRawZincAreRetained(GameTestHelper helper) {
        remainsBlocked(helper, item("create:raw_zinc"), 2, true, "two raw zinc");
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void oreDoesNotProcessWithoutPower(GameTestHelper helper) {
        remainsBlocked(helper, item("create:zinc_ore"), 1, false, "no power");
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void ingotCrushesToOneDust(GameTestHelper helper) {
        Item ingot = item("create:zinc_ingot");
        helper.assertTrue(isMember(ZINC_INGOTS, ingot), "create:zinc_ingot is absent from c:ingots/zinc");
        process(helper, "mekanism:crusher", ingot, 1, 1, 240, "zinc ingot crushing");
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void dustSmeltsToCreateIngot(GameTestHelper helper) {
        furnace(helper, Blocks.FURNACE, 200, "smelting");
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void dustBlastsToCreateIngot(GameTestHelper helper) {
        furnace(helper, Blocks.BLAST_FURNACE, 100, "blasting");
    }

    @GameTest(template = "empty")
    public static void crushedRawZincIsRejected(GameTestHelper helper) {
        Item crushed = item("create:crushed_raw_zinc");
        var recipe = helper.getLevel().getRecipeManager().getAllRecipesFor(
                mekanism.common.recipe.MekanismRecipeType.ENRICHING.get());
        boolean accepted = recipe.stream().filter(holder -> holder.id().getNamespace().equals(ZincContent.MOD_ID))
                .anyMatch(holder -> ((mekanism.api.recipes.ItemStackToItemStackRecipe) holder.value()).test(new ItemStack(crushed)));
        helper.assertTrue(!accepted, "Addon enrichment recipes must not accept create:crushed_raw_zinc");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cleanSlurrySolidTagLoadsWithBaseMapping(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var dataType = mekanism.api.datamaps.IMekanismDataMapTypes.INSTANCE.chemicalSolidTag();
        var clean = mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.getHolder(id(ZincContent.MOD_ID, "clean_zinc")).orElseThrow();
        var iron = mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.getHolder(id("mekanism", "clean_iron")).orElseThrow();
        var cleanData = mekanism.api.datamaps.IMekanismDataMapTypes.INSTANCE.getData(access, mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.key(), clean, dataType);
        var ironData = mekanism.api.datamaps.IMekanismDataMapTypes.INSTANCE.getData(access, mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.key(), iron, dataType);
        helper.assertTrue(cleanData != null && cleanData.solidRepresentation().location().equals(id("c", "ores/zinc")), "clean_zinc lacks c:ores/zinc data map entry: " + cleanData);
        helper.assertTrue(ironData != null && ironData.solidRepresentation().location().equals(id("c", "ores/iron")), "Base clean_iron data map entry was lost: " + ironData);
        helper.succeed();
    }

    private static void process(GameTestHelper helper, String machineId, Item input, int inputCount, int outputCount, int maxTicks, String label) {
        TileEntityElectricMachine machine = placeMachine(helper, machineId);
        InputInventorySlot in = machine.getInventorySlots(null).stream().filter(InputInventorySlot.class::isInstance).map(InputInventorySlot.class::cast).findFirst().orElseThrow();
        OutputInventorySlot out = machine.getInventorySlots(null).stream().filter(OutputInventorySlot.class::isInstance).map(OutputInventorySlot.class::cast).findFirst().orElseThrow();
        in.setStack(new ItemStack(input, inputCount));
        machine.getEnergyContainer().setEnergy(machine.getEnergyContainer().getMaxEnergy());
        helper.runAfterDelay(maxTicks, () -> {
            ItemStack result = out.getStack();
            helper.assertTrue(result.is(ZincContentItem.DUST) && result.getCount() == outputCount,
                    label + " expected exactly " + outputCount + " dust, got " + result);
            helper.assertTrue(in.getStack().isEmpty(), label + " input was not consumed: " + in.getStack());
            helper.succeed();
        });
    }

    private static void remainsBlocked(GameTestHelper helper, Item input, int count, boolean powered, String label) {
        TileEntityElectricMachine machine = placeMachine(helper, "mekanism:enrichment_chamber");
        InputInventorySlot in = machine.getInventorySlots(null).stream().filter(InputInventorySlot.class::isInstance).map(InputInventorySlot.class::cast).findFirst().orElseThrow();
        OutputInventorySlot out = machine.getInventorySlots(null).stream().filter(OutputInventorySlot.class::isInstance).map(OutputInventorySlot.class::cast).findFirst().orElseThrow();
        in.setStack(new ItemStack(input, count));
        if (powered) machine.getEnergyContainer().setEnergy(machine.getEnergyContainer().getMaxEnergy());
        helper.runAfterDelay(260, () -> {
            helper.assertTrue(in.getStack().is(input) && in.getStack().getCount() == count, label + " unexpectedly changed input: " + in.getStack());
            helper.assertTrue(out.getStack().isEmpty(), label + " unexpectedly produced output: " + out.getStack());
            helper.succeed();
        });
    }

    private static TileEntityElectricMachine placeMachine(GameTestHelper helper, String id) {
        Block block = BuiltInRegistries.BLOCK.get(id(id));
        helper.setBlock(MACHINE, block);
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE));
        helper.assertTrue(entity instanceof TileEntityElectricMachine, id + " did not create an electric machine tile: " + entity);
        return (TileEntityElectricMachine) entity;
    }

    private static void furnace(GameTestHelper helper, Block block, int cookingTicks, String mode) {
        helper.setBlock(FURNACE, block);
        BlockEntity raw = helper.getLevel().getBlockEntity(helper.absolutePos(FURNACE));
        helper.assertTrue(raw instanceof AbstractFurnaceBlockEntity, mode + " block did not create a furnace block entity");
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) raw;
        Item dust = ZincContentItem.DUST;
        Item ingot = item("create:zinc_ingot");
        furnace.setItem(0, new ItemStack(dust));
        furnace.setItem(1, new ItemStack(Items.COAL, 1));
        helper.runAfterDelay(cookingTicks - 1, () -> helper.assertTrue(furnace.getItem(2).isEmpty(), mode + " completed before " + cookingTicks + " ticks"));
        helper.runAfterDelay(cookingTicks + 80, () -> {
            helper.assertTrue(furnace.getItem(0).isEmpty(), mode + " did not consume dust");
            helper.assertTrue(furnace.getItem(2).is(ingot) && furnace.getItem(2).getCount() == 1,
                    mode + " expected one create:zinc_ingot, got " + furnace.getItem(2));
            helper.succeed();
        });
    }

    private static boolean isMember(TagKey<Item> tag, Item item) {
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) if (holder.value() == item) return true;
        return false;
    }

    private static Item item(String location) { return BuiltInRegistries.ITEM.get(id(location)); }
    private static ResourceLocation id(String location) { return ResourceLocation.parse(location); }
    private static ResourceLocation id(String namespace, String path) { return ResourceLocation.fromNamespaceAndPath(namespace, path); }

    private static final class ZincContentItem {
        private static final Item DUST = item(ZincContent.MOD_ID + ":dust_zinc");
    }

    private static final class Items {
        private static final Item COAL = item("minecraft:coal");
    }
}
