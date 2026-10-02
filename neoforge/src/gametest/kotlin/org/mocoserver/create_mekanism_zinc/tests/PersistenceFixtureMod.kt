// packageと旧版保存fixtureの識別子・数量を維持する。
@file:Suppress("ktlint:standard:package-name", "PackageNaming", "MagicNumber")

package org.mocoserver.create_mekanism_zinc.tests

import mekanism.api.Action
import mekanism.api.AutomationType
import mekanism.api.MekanismAPI
import mekanism.api.chemical.ChemicalStack
import mekanism.common.tile.TileEntityChemicalTank
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import org.mocoserver.create_mekanism_zinc.content.ZincContent

@Mod(PersistenceFixtureMod.MOD_ID)
class PersistenceFixtureMod {
    init {
        NeoForge.EVENT_BUS.addListener(::registerCommands)
    }

    private fun registerCommands(event: RegisterCommandsEvent) {
        event.dispatcher.register(
            Commands
                .literal("cmz_verify")
                .requires { it.hasPermission(4) }
                .then(Commands.literal("seed").executes { seed(it.source) })
                .then(Commands.literal("check").executes { check(it.source) }),
        )
    }

    private fun seed(source: CommandSourceStack): Int {
        val level = source.server.overworld()
        level.setChunkForced(0, 0, true)
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3)
        val chest = level.getBlockEntity(chestPos) as ChestBlockEntity
        chest.clearContent()
        itemIds.forEachIndexed { index, id ->
            chest.setItem(
                index,
                ItemStack(
                    BuiltInRegistries.ITEM.get(id("${ZincContent.MOD_ID}:$id")),
                    index + 1,
                ),
            )
        }
        chest.setChanged()
        if (!seedTank(source, level, dirtyPos, "dirty_zinc", 1111) ||
            !seedTank(source, level, cleanPos, "clean_zinc", 2222)
        ) {
            return 0
        }
        level.chunkSource.save(true)
        source.sendSuccess(
            { Component.literal("CMZ_PERSIST_SEED_PASS world=${level.server.worldData.levelName}") },
            true,
        )
        return 1
    }

    private fun seedTank(
        source: CommandSourceStack,
        level: ServerLevel,
        pos: BlockPos,
        chemical: String,
        amount: Long,
    ): Boolean {
        level.setBlock(pos, BuiltInRegistries.BLOCK.get(id("mekanism:advanced_chemical_tank")).defaultBlockState(), 3)
        val tile = level.getBlockEntity(pos) as TileEntityChemicalTank
        val tank = tile.getChemicalTanks(null).first()
        tank.setEmpty()
        val remainder =
            tank.insert(
                ChemicalStack(
                    MekanismAPI.CHEMICAL_REGISTRY.getHolder(id("${ZincContent.MOD_ID}:$chemical")).orElseThrow(),
                    amount,
                ),
                Action.EXECUTE,
                AutomationType.INTERNAL,
            )
        tile.setChanged()
        if (!remainder.isEmpty || tank.stack.amount != amount) {
            source.sendFailure(
                Component.literal(
                    (
                        "CMZ_PERSIST_SEED_FAIL tank=$chemical requested=$amount " +
                            "remainder=$remainder stored=${tank.stack}"
                    ),
                ),
            )
            return false
        }
        return true
    }

    private fun check(source: CommandSourceStack): Int {
        val level = source.server.overworld()
        level.setChunkForced(0, 0, true)
        var valid = true
        val chest = level.getBlockEntity(chestPos) as? ChestBlockEntity
        for ((index, id) in itemIds.withIndex()) {
            val stack = chest?.getItem(index) ?: ItemStack.EMPTY
            val expected = BuiltInRegistries.ITEM.get(id("${ZincContent.MOD_ID}:$id"))
            if (!stack.`is`(expected) || stack.count != index + 1) {
                valid = false
                source.sendFailure(
                    Component.literal("CMZ_PERSIST_FAIL slot=$index expected=${expected}x${index + 1} actual=$stack"),
                )
            }
        }
        val dirtyValid = checkTank(source, level, dirtyPos, "dirty_zinc", 1111)
        val cleanValid = checkTank(source, level, cleanPos, "clean_zinc", 2222)
        if (valid && dirtyValid && cleanValid) {
            source.sendSuccess({ Component.literal("CMZ_PERSIST_CHECK_PASS items=5 chemicals=2") }, true)
            return 1
        }
        source.sendFailure(Component.literal("CMZ_PERSIST_CHECK_FAIL"))
        return 0
    }

    private fun checkTank(
        source: CommandSourceStack,
        level: ServerLevel,
        pos: BlockPos,
        chemical: String,
        amount: Long,
    ): Boolean {
        val tile = level.getBlockEntity(pos) as? TileEntityChemicalTank
        if (tile == null) {
            source.sendFailure(Component.literal("CMZ_PERSIST_FAIL missing tank at $pos"))
            return false
        }
        val stack = tile.getChemicalTanks(null).first().stack
        val valid =
            stack.chemical == MekanismAPI.CHEMICAL_REGISTRY.get(id("${ZincContent.MOD_ID}:$chemical")) &&
                stack.amount == amount
        if (!valid) {
            source.sendFailure(
                Component.literal("CMZ_PERSIST_FAIL tank=$chemical expected=$amount actual=$stack"),
            )
        }
        return valid
    }

    private fun id(value: String): ResourceLocation = ResourceLocation.parse(value)

    companion object {
        const val MOD_ID: String = "create_mekanism_zinc_tests"
        private val chestPos = BlockPos(1, 80, 2)
        private val dirtyPos = BlockPos(2, 80, 2)
        private val cleanPos = BlockPos(3, 80, 2)
        private val itemIds = listOf("dust_zinc", "dirty_dust_zinc", "clump_zinc", "shard_zinc", "crystal_zinc")
    }
}
