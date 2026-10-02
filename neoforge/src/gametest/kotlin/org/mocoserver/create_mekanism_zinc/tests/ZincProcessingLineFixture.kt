// 既存packageと、独立した加工量・待機tickの期待値を維持する。
@file:Suppress("ktlint:standard:package-name", "PackageNaming", "MagicNumber")

package org.mocoserver.create_mekanism_zinc.tests

import mekanism.api.Action
import mekanism.api.AutomationType
import mekanism.api.Upgrade
import mekanism.api.chemical.ChemicalStack
import mekanism.common.tile.base.TileEntityMekanism
import mekanism.common.tile.machine.TileEntityChemicalCrystallizer
import mekanism.common.tile.machine.TileEntityChemicalDissolutionChamber
import mekanism.common.tile.machine.TileEntityChemicalWasher
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine
import mekanism.common.tile.prefab.TileEntityElectricMachine
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity

/** 実機出力を次工程へ渡し、非同期の加工ラインを検証する。 */
internal object ZincProcessingLineFixture {
    private const val STAGE_TIMEOUT = 800

    internal fun pipeline(
        helper: GameTestHelper,
        multiplier: Int,
        inputId: String,
        inputCount: Int,
        expectedIngots: Int,
    ) {
        val input = ItemStack(item(inputId), inputCount)
        when (multiplier) {
            3 ->
                runItemStage(helper, "mekanism:purification_chamber", input, "mekanism:oxygen") { stack ->
                    assertItem(helper, stack, "create_mekanism_zinc:clump_zinc", expectedIngots, "3x purifying")
                    runDustLine(helper, stack, expectedIngots)
                }
            4 ->
                runItemStage(
                    helper,
                    "mekanism:chemical_injection_chamber",
                    input,
                    "mekanism:hydrogen_chloride",
                ) { shards ->
                    assertItem(helper, shards, "create_mekanism_zinc:shard_zinc", expectedIngots, "4x injecting")
                    runItemStage(helper, "mekanism:purification_chamber", shards, "mekanism:oxygen") { clumps ->
                        assertItem(helper, clumps, "create_mekanism_zinc:clump_zinc", expectedIngots, "shard purifying")
                        runDustLine(helper, clumps, expectedIngots)
                    }
                }
            5 -> runDissolutionLine(helper, input, expectedIngots)
            else -> error("Unknown multiplier $multiplier")
        }
    }

    private fun runDissolutionLine(
        helper: GameTestHelper,
        input: ItemStack,
        expectedIngots: Int,
    ) {
        val dirtyAmount =
            when (expectedIngots) {
                5 -> 1000
                10 -> 2000
                else -> 6000
            }
        val chamber = dissolution(helper, null, 0)
        val inputSlot = inputSlot(chamber)
        inputSlot.stack = input.copy()
        configurePositiveMachine(chamber)
        giveChemical(chamber.injectTank, "mekanism:sulfuric_acid")
        val acidInitial = chamber.injectTank.stored
        pollMachine(helper, chamber, "mekanism:sulfuric_acid", 300) {
            val dirty = chamber.outputTank.stack.copy()
            helper.assertTrue(
                dirty.`is`(chemical("create_mekanism_zinc:dirty_zinc", 1).chemical) &&
                    dirty.amount == dirtyAmount.toLong(),
                "dissolution expected dirty_zinc $dirtyAmount, got $dirty",
            )
            helper.assertTrue(inputSlot.stack.isEmpty, "dissolution retained input ${inputSlot.stack}")
            helper.assertTrue(
                chamber.injectTank.stored < acidInitial,
                "sulfuric acid was not consumed during dissolution",
            )
            val washer = washer(helper)
            val initialWater = fillWater(washer, washer.fluidTank.capacity)
            helper.assertTrue(
                initialWater == washer.fluidTank.capacity,
                "could not fill washer's actual water capacity",
            )
            helper.assertTrue(
                washer.inputTank.insert(dirty.copy(), Action.EXECUTE, AutomationType.INTERNAL).isEmpty,
                "washer rejected actual dirty slurry output",
            )
            runWasher(helper, washer, dirtyAmount) { totalWater ->
                helper.assertTrue(
                    totalWater == dirtyAmount * 5L,
                    "washer consumed $totalWater water; expected ${dirtyAmount * 5L}",
                )
                val clean = washer.outputTank.stack.copy()
                helper.assertTrue(
                    clean.`is`(chemical("create_mekanism_zinc:clean_zinc", 1).chemical) &&
                        clean.amount == dirtyAmount.toLong(),
                    "washing expected clean slurry $dirtyAmount, got $clean",
                )
                runCrystallizerLine(helper, clean, dirtyAmount)
            }
        }
    }

    private fun runCrystallizerLine(
        helper: GameTestHelper,
        clean: ChemicalStack,
        dirtyAmount: Int,
    ) {
        val crystallizer = crystallizer(helper)
        helper.assertTrue(
            crystallizer.inputTank.insert(clean.copy(), Action.EXECUTE, AutomationType.INTERNAL).isEmpty,
            "crystallizer rejected washed slurry",
        )
        configurePositiveMachine(crystallizer)
        pollMachine(helper, crystallizer, null, 6500) {
            val remaining = crystallizer.inputTank.stack
            val crystals = dirtyAmount / 200
            helper.assertTrue(
                remaining.amount == dirtyAmount % 200L,
                "crystallizer remainder should be ${dirtyAmount % 200L}, got $remaining",
            )
            assertItem(
                helper,
                outputSlot(crystallizer).stack.copy(),
                "create_mekanism_zinc:crystal_zinc",
                crystals,
                "crystallizing",
            )
            val crystalInput = takeOutput(outputSlot(crystallizer))
            runItemStage(
                helper,
                "mekanism:chemical_injection_chamber",
                crystalInput,
                "mekanism:hydrogen_chloride",
            ) { shards ->
                assertItem(helper, shards, "create_mekanism_zinc:shard_zinc", crystals, "crystal injecting")
                runItemStage(helper, "mekanism:purification_chamber", shards, "mekanism:oxygen") { clumps ->
                    assertItem(helper, clumps, "create_mekanism_zinc:clump_zinc", crystals, "shard purifying")
                    runDustLine(helper, clumps, crystals)
                }
            }
        }
    }

    private fun runWasher(
        helper: GameTestHelper,
        washer: TileEntityChemicalWasher,
        expectedOutputAmount: Int,
        onComplete: (Long) -> Unit,
    ) {
        configurePositiveMachine(washer)
        var elapsed = 0
        var supplied = washer.fluidTank.fluidAmount.toLong()

        fun poll() {
            giveEnergy(washer)
            val needed = washer.fluidTank.capacity - washer.fluidTank.fluidAmount
            if (needed > 0) supplied += fillWater(washer, needed)
            helper.runAfterDelay(1) {
                elapsed++
                if (washer.inputTank.isEmpty && washer.outputTank.stack.amount == expectedOutputAmount.toLong()) {
                    val used = supplied - washer.fluidTank.fluidAmount
                    helper.assertTrue(
                        used == expectedOutputAmount * 5L,
                        "water used was $used mB for $expectedOutputAmount slurry",
                    )
                    helper.assertTrue(supplied > 0, "washer was not supplied with water")
                    onComplete(used)
                } else if (elapsed > STAGE_TIMEOUT) {
                    helper.fail(
                        (
                            "washer timed out: input=${washer.inputTank.stack} " +
                                "water=${washer.fluidTank.fluid} output=${washer.outputTank.stack}"
                        ),
                    )
                } else {
                    poll()
                }
            }
        }
        poll()
    }

    private fun runDustLine(
        helper: GameTestHelper,
        clumps: ItemStack,
        expectedIngots: Int,
    ) {
        runItemStage(helper, "mekanism:crusher", clumps, null) { dirty ->
            assertItem(helper, dirty, "create_mekanism_zinc:dirty_dust_zinc", expectedIngots, "clump crushing")
            runItemStage(helper, "mekanism:enrichment_chamber", dirty, null) { dust ->
                assertItem(helper, dust, "create_mekanism_zinc:dust_zinc", expectedIngots, "dirty dust enriching")
                smelt(helper, dust, expectedIngots)
            }
        }
    }

    private fun smelt(
        helper: GameTestHelper,
        dust: ItemStack,
        expected: Int,
    ) {
        helper.assertTrue(
            dust.`is`(item("create_mekanism_zinc:dust_zinc")) && dust.count == expected,
            "smelting input was not actual zinc dust: $dust",
        )
        val furnaces = mutableListOf<AbstractFurnaceBlockEntity>()
        for (x in 1..5) {
            for (z in 1..6) {
                if (furnaces.size >= expected) continue
                val pos = BlockPos(x, 1, z)
                helper.setBlock(pos, Blocks.BLAST_FURNACE)
                val entity = helper.level.getBlockEntity(helper.absolutePos(pos))
                helper.assertTrue(
                    entity is AbstractFurnaceBlockEntity,
                    "blast furnace did not create its vanilla block entity",
                )
                val furnace = entity as AbstractFurnaceBlockEntity
                furnace.setItem(0, ItemStack(dust.item, 1))
                furnace.setItem(1, ItemStack(item("minecraft:coal"), 1))
                furnaces.add(furnace)
                dust.shrink(1)
            }
        }
        helper.assertTrue(dust.isEmpty, "not all actual dust output was split across real blast furnaces: $dust")
        helper.runAfterDelay(99) {
            for (furnace in furnaces) {
                helper.assertTrue(
                    furnace.getItem(2).isEmpty,
                    "blasting finished before 100 ticks",
                )
            }
        }
        helper.runAfterDelay(180) {
            var total = 0
            for (furnace in furnaces) {
                helper.assertTrue(furnace.getItem(0).isEmpty, "blast furnace did not consume one dust")
                helper.assertTrue(
                    furnace.getItem(2).`is`(item("create:zinc_ingot")),
                    "blast furnace output was ${furnace.getItem(2)}",
                )
                total += furnace.getItem(2).count
            }
            helper.assertTrue(total == expected, "expected exactly $expected create:zinc_ingot, got $total")
            helper.succeed()
        }
    }

    private fun runItemStage(
        helper: GameTestHelper,
        machineId: String,
        actualInput: ItemStack,
        chemicalId: String?,
        onOutput: (ItemStack) -> Unit,
    ) {
        val raw = placeMachine(helper, machineId)
        helper.assertTrue(
            raw is TileEntityElectricMachine || raw is TileEntityAdvancedElectricMachine,
            "$machineId is not an actual item-processing machine: $raw",
        )
        val input = inputSlot(raw)
        val output = outputSlot(raw)
        helper.assertTrue(output.stack.isEmpty, "$machineId output was not empty at stage start: ${output.stack}")
        input.stack = actualInput.copy()
        configurePositiveMachine(raw)
        val stageTank = if (raw is TileEntityAdvancedElectricMachine && chemicalId != null) raw.chemicalTank else null
        if (stageTank != null) giveChemical(stageTank, requireNotNull(chemicalId))
        val chemicalBefore = stageTank?.stored ?: 0L
        pollMachine(helper, raw, chemicalId, 800) {
            val actualOutput = output.stack.copy()
            helper.assertTrue(input.stack.isEmpty, "$machineId retained input after producing output: ${input.stack}")
            if (stageTank !=
                null
            ) {
                helper.assertTrue(
                    stageTank.stored < chemicalBefore,
                    "$machineId did not consume its per-tick chemical",
                )
            }
            output.stack = ItemStack.EMPTY
            onOutput(actualOutput)
        }
    }

    private fun pollMachine(
        helper: GameTestHelper,
        machine: TileEntityMekanism,
        chemicalId: String?,
        maxTicks: Int,
        complete: () -> Unit,
    ) {
        var elapsed = 0

        fun poll() {
            replenish(machine, chemicalId)
            helper.runAfterDelay(1) {
                elapsed++
                val finished =
                    when (machine) {
                        is TileEntityElectricMachine ->
                            !outputSlot(machine).stack.isEmpty &&
                                inputSlot(machine).stack.isEmpty
                        is TileEntityAdvancedElectricMachine ->
                            !outputSlot(machine).stack.isEmpty &&
                                inputSlot(machine).stack.isEmpty
                        is TileEntityChemicalDissolutionChamber ->
                            !machine.outputTank.isEmpty &&
                                inputSlot(machine).stack.isEmpty
                        is TileEntityChemicalCrystallizer ->
                            machine.inputTank.isEmpty &&
                                !outputSlot(machine).stack.isEmpty
                        else -> false
                    }
                if (finished) {
                    complete()
                } else if (elapsed > maxTicks) {
                    helper.fail(
                        (
                            "machine timed out: ${machine.javaClass.simpleName} after $elapsed " +
                                "ticks; " +
                                "${machineState(
                                    machine,
                                )}"
                        ),
                    )
                } else {
                    poll()
                }
            }
        }
        poll()
    }

    private fun machineState(machine: TileEntityMekanism): String =
        buildString {
            append("active=").append(machine.active).append(" energy=")
            append(machine.getEnergyContainers(null).sumOf { it.energy })
            when (machine) {
                is TileEntityAdvancedElectricMachine -> {
                    append(
                        " input=",
                    ).append(inputSlot(machine).stack).append(" output=").append(outputSlot(machine).stack)
                    append(" chemical=").append(machine.chemicalTank.stack)
                    append(" ticks=").append(machine.operatingTicks).append('/').append(machine.ticksRequired)
                }
                is TileEntityChemicalDissolutionChamber -> {
                    append(
                        " input=",
                    ).append(inputSlot(machine).stack).append(" output=").append(machine.outputTank.stack)
                    append(" chemical=").append(machine.injectTank.stack)
                    append(" ticks=").append(machine.operatingTicks).append('/').append(machine.ticksRequired)
                }
                is TileEntityChemicalCrystallizer -> {
                    append(
                        " inputChemical=",
                    ).append(machine.inputTank.stack).append(" output=").append(outputSlot(machine).stack)
                    append(" ticks=").append(machine.operatingTicks).append('/').append(machine.ticksRequired)
                    append(" speedUpgrades=").append(machine.component.getUpgrades(Upgrade.SPEED))
                    append(" energyUpgrades=").append(machine.component.getUpgrades(Upgrade.ENERGY))
                }
                is TileEntityChemicalWasher -> {
                    append(
                        " slurryIn=",
                    ).append(machine.inputTank.stack).append(" fluid=").append(machine.fluidTank.fluid)
                    append(" slurryOut=").append(machine.outputTank.stack)
                }
                is TileEntityElectricMachine -> {
                    append(
                        " input=",
                    ).append(inputSlot(machine).stack).append(" output=").append(outputSlot(machine).stack)
                    append(" ticks=").append(machine.operatingTicks).append('/').append(machine.ticksRequired)
                }
            }
        }
}
