package org.mocoserver.create_mekanism_zinc.tests;

import com.mojang.brigadier.CommandDispatcher;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.MekanismAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import mekanism.common.tile.TileEntityChemicalTank;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.common.Mod;
import org.mocoserver.create_mekanism_zinc.content.ZincContent;

@Mod(PersistenceFixtureMod.MOD_ID)
public final class PersistenceFixtureMod {
    public static final String MOD_ID = "create_mekanism_zinc_tests";
    private static final BlockPos CHEST_POS = new BlockPos(1, 80, 2);
    private static final BlockPos DIRTY_POS = new BlockPos(2, 80, 2);
    private static final BlockPos CLEAN_POS = new BlockPos(3, 80, 2);
    private static final String[] ITEM_IDS = {"dust_zinc", "dirty_dust_zinc", "clump_zinc", "shard_zinc", "crystal_zinc"};

    public PersistenceFixtureMod(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("cmz_verify")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("seed").executes(context -> seed(context.getSource())))
                .then(Commands.literal("check").executes(context -> check(context.getSource()))));
    }

    private static int seed(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        level.setChunkForced(0, 0, true);
        level.setBlock(CHEST_POS, Blocks.CHEST.defaultBlockState(), 3);
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(CHEST_POS);
        chest.clearContent();
        for (int i = 0; i < ITEM_IDS.length; i++) {
            chest.setItem(i, new ItemStack(BuiltInRegistries.ITEM.get(id(ZincContent.MOD_ID + ":" + ITEM_IDS[i])), i + 1));
        }
        chest.setChanged();
        if (!seedTank(source, level, DIRTY_POS, "dirty_zinc", 1111)) return 0;
        if (!seedTank(source, level, CLEAN_POS, "clean_zinc", 2222)) return 0;
        level.getChunkSource().save(true);
        source.sendSuccess(() -> Component.literal("CMZ_PERSIST_SEED_PASS world=" + level.getServer().getWorldData().getLevelName()), true);
        return 1;
    }

    private static boolean seedTank(CommandSourceStack source, ServerLevel level, BlockPos pos, String chemical, long amount) {
        level.setBlock(pos, BuiltInRegistries.BLOCK.get(id("mekanism:advanced_chemical_tank")).defaultBlockState(), 3);
        TileEntityChemicalTank tile = (TileEntityChemicalTank) level.getBlockEntity(pos);
        IChemicalTank tank = tile.getChemicalTanks(null).getFirst();
        tank.setEmpty();
        ChemicalStack remainder = tank.insert(new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.getHolder(id(ZincContent.MOD_ID + ":" + chemical)).orElseThrow(), amount), Action.EXECUTE, AutomationType.INTERNAL);
        tile.setChanged();
        if (!remainder.isEmpty() || tank.getStack().getAmount() != amount) {
            source.sendFailure(Component.literal("CMZ_PERSIST_SEED_FAIL tank=" + chemical + " requested=" + amount + " remainder=" + remainder + " stored=" + tank.getStack()));
            return false;
        }
        return true;
    }

    private static int check(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        level.setChunkForced(0, 0, true);
        boolean valid = true;
        ChestBlockEntity chest = level.getBlockEntity(CHEST_POS) instanceof ChestBlockEntity value ? value : null;
        for (int i = 0; i < ITEM_IDS.length; i++) {
            ItemStack stack = chest == null ? ItemStack.EMPTY : chest.getItem(i);
            Item expected = BuiltInRegistries.ITEM.get(id(ZincContent.MOD_ID + ":" + ITEM_IDS[i]));
            if (!stack.is(expected) || stack.getCount() != i + 1) {
                valid = false;
                source.sendFailure(Component.literal("CMZ_PERSIST_FAIL slot=" + i + " expected=" + expected + "x" + (i + 1) + " actual=" + stack));
            }
        }
        valid &= checkTank(source, level, DIRTY_POS, "dirty_zinc", 1111);
        valid &= checkTank(source, level, CLEAN_POS, "clean_zinc", 2222);
        if (valid) {
            source.sendSuccess(() -> Component.literal("CMZ_PERSIST_CHECK_PASS items=5 chemicals=2"), true);
            return 1;
        }
        source.sendFailure(Component.literal("CMZ_PERSIST_CHECK_FAIL"));
        return 0;
    }

    private static boolean checkTank(CommandSourceStack source, ServerLevel level, BlockPos pos, String id, long amount) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityChemicalTank tile)) {
            source.sendFailure(Component.literal("CMZ_PERSIST_FAIL missing tank at " + pos));
            return false;
        }
        ChemicalStack stack = tile.getChemicalTanks(null).getFirst().getStack();
        boolean valid = stack.getChemical() == MekanismAPI.CHEMICAL_REGISTRY.get(id(ZincContent.MOD_ID + ":" + id)) && stack.getAmount() == amount;
        if (!valid) source.sendFailure(Component.literal("CMZ_PERSIST_FAIL tank=" + id + " expected=" + amount + " actual=" + stack));
        return valid;
    }

    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
}
