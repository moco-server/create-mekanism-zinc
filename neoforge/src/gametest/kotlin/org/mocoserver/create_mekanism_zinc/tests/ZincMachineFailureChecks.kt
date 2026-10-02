// 既存packageと、独立した加工量・待機tickの期待値を維持する。
@file:Suppress("ktlint:standard:package-name", "PackageNaming", "MagicNumber")

package org.mocoserver.create_mekanism_zinc.tests

import mekanism.api.Action
import mekanism.api.AutomationType
import mekanism.api.chemical.IChemicalTank
import mekanism.common.inventory.slot.OutputInventorySlot
import mekanism.common.tile.base.TileEntityMekanism
import mekanism.common.tile.machine.TileEntityChemicalCrystallizer
import mekanism.common.tile.machine.TileEntityChemicalDissolutionChamber
import mekanism.common.tile.machine.TileEntityChemicalWasher
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.ItemStack

/** 素材・電力・化学物質の不足時に実機が出力しないことを検証する。 */
internal object ZincMachineFailureChecks {
    internal fun checkRawBatchGate(
        helper: GameTestHelper,
        multiplier: Int,
        count: Int,
        next: () -> Unit,
    ) {
        val machineId =
            if (multiplier ==
                4
            ) {
                "mekanism:chemical_injection_chamber"
            } else {
                "mekanism:chemical_dissolution_chamber"
            }
        val machine = placeMachine(helper, machineId)
        val input = inputSlot(machine)
        input.stack = ItemStack(item("create:raw_zinc"), count)
        configurePositiveMachine(machine)
        if (machine is TileEntityAdvancedElectricMachine) {
            giveChemical(machine.chemicalTank, "mekanism:hydrogen_chloride")
        } else if (machine is TileEntityChemicalDissolutionChamber) {
            giveChemical(machine.injectTank, "mekanism:sulfuric_acid")
        }
        helper.runAfterDelay(240) {
            helper.assertTrue(
                input.stack.`is`(item("create:raw_zinc")) && input.stack.count == count,
                "${multiplier}x raw input $count unexpectedly changed",
            )
            if (machine is TileEntityAdvancedElectricMachine) {
                helper.assertTrue(
                    machine
                        .getInventorySlots(
                            null,
                        ).filterIsInstance<OutputInventorySlot>()
                        .all {
                            it.stack.isEmpty
                        },
                    "4x raw produced a shard from fewer than three raw zinc",
                )
            }
            if (machine is TileEntityChemicalDissolutionChamber) {
                helper.assertTrue(
                    machine.outputTank.isEmpty,
                    "5x raw produced slurry from fewer than three raw zinc",
                )
            }
            next()
        }
    }

    // 入力と補助素材、数量、次の非同期検査を明示して保持する。
    @Suppress("LongParameterList")
    internal fun checkPowerGate(
        helper: GameTestHelper,
        machineId: String,
        inputId: String?,
        chemicalId: String?,
        inputCount: Int,
        next: () -> Unit,
    ) {
        val machine = placeMachine(helper, machineId)
        if (inputId != null) inputSlot(machine).stack = ItemStack(item(inputId), inputCount)
        if (chemicalId != null) {
            val tank =
                when (machine) {
                    is TileEntityAdvancedElectricMachine -> machine.chemicalTank
                    else -> (machine as TileEntityChemicalDissolutionChamber).injectTank
                }
            giveChemical(tank, chemicalId)
        } else if (machine is TileEntityChemicalWasher) {
            fillWater(machine, 5)
            helper.assertTrue(
                machine.inputTank
                    .insert(
                        chemical("create_mekanism_zinc:dirty_zinc", 1),
                        Action.EXECUTE,
                        AutomationType.INTERNAL,
                    ).isEmpty,
                "washer rejected dirty slurry for no-power case",
            )
        } else if (machine is TileEntityChemicalCrystallizer) {
            machine.inputTank.insert(
                chemical("create_mekanism_zinc:clean_zinc", 200),
                Action.EXECUTE,
                AutomationType.INTERNAL,
            )
        }
        helper.runAfterDelay(260) {
            when (machine) {
                is TileEntityChemicalWasher ->
                    helper.assertTrue(
                        machine.inputTank.stack.amount == 1L && machine.outputTank.isEmpty,
                        "unpowered washer changed slurry",
                    )
                is TileEntityChemicalCrystallizer ->
                    helper.assertTrue(
                        machine.inputTank.stack.amount == 200L && outputSlot(machine).stack.isEmpty,
                        "unpowered crystallizer changed slurry",
                    )
                is TileEntityChemicalDissolutionChamber ->
                    helper.assertTrue(
                        inputSlot(machine).stack.`is`(item(requireNotNull(inputId))) && machine.outputTank.isEmpty,
                        "unpowered dissolution chamber processed input",
                    )
                is TileEntityAdvancedElectricMachine -> {
                    helper.assertTrue(
                        inputSlot(machine).stack.`is`(item(requireNotNull(inputId))),
                        "unpowered $machineId consumed input",
                    )
                    helper.assertTrue(outputSlot(machine).stack.isEmpty, "unpowered $machineId produced output")
                }
            }
            next()
        }
    }

    internal fun checkPerTickStarvation(
        helper: GameTestHelper,
        machineId: String,
        inputId: String,
        chemicalId: String,
        next: () -> Unit,
    ) {
        val machine = placeMachine(helper, machineId)
        val input = inputSlot(machine)
        input.stack = ItemStack(item(inputId), 1)
        val tank =
            if (machine is TileEntityAdvancedElectricMachine) {
                machine.chemicalTank
            } else {
                (machine as TileEntityChemicalDissolutionChamber)
                    .injectTank
            }
        giveEnergy(machine)
        helper.runAfterDelay(20) {
            helper.assertTrue(input.stack.`is`(item(inputId)), "$machineId consumed input before any $chemicalId")
            assertNoChemicalOutput(helper, machine, "$machineId produced output without $chemicalId")
            giveChemical(tank, chemicalId)
            val initial = tank.stored
            helper.runAfterDelay(10) {
                helper.assertTrue(tank.stored < initial, "$machineId did not consume $chemicalId while working")
                tank.setEmpty()
                helper.runAfterDelay(60) {
                    helper.assertTrue(tank.isEmpty, "$machineId chemical tank was refilled during starvation test")
                    helper.assertTrue(
                        input.stack.`is`(item(inputId)),
                        "$machineId consumed input with no remaining per-tick chemical",
                    )
                    assertNoChemicalOutput(helper, machine, "$machineId produced output after chemical starvation")
                    next()
                }
            }
        }
    }

    private fun assertNoChemicalOutput(
        helper: GameTestHelper,
        machine: TileEntityMekanism,
        message: String,
    ) {
        val empty =
            if (machine is TileEntityAdvancedElectricMachine) {
                outputSlot(
                    machine,
                ).stack.isEmpty
            } else {
                (machine as TileEntityChemicalDissolutionChamber).outputTank.isEmpty
            }
        helper.assertTrue(empty, message)
    }

    internal fun assertWrongChemical(
        helper: GameTestHelper,
        tank: IChemicalTank,
        wrongChemical: String,
        next: () -> Unit,
    ) {
        val sample = chemical(wrongChemical, 20)
        val remainder = tank.insert(sample, Action.EXECUTE, AutomationType.INTERNAL)
        helper.assertTrue(
            remainder.amount == sample.amount && remainder.`is`(sample.chemical),
            "input tank accepted wrong chemical $wrongChemical: ${tank.stack}",
        )
        next()
    }
}
