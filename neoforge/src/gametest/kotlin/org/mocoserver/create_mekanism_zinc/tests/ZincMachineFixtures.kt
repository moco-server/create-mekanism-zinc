// 既存packageと、独立した加工量・待機tickの期待値を維持する。
@file:Suppress("ktlint:standard:package-name", "PackageNaming", "MagicNumber")

package org.mocoserver.create_mekanism_zinc.tests

import mekanism.api.Action
import mekanism.api.AutomationType
import mekanism.api.Upgrade
import mekanism.api.chemical.IChemicalTank
import mekanism.common.tile.base.TileEntityMekanism
import mekanism.common.tile.machine.TileEntityChemicalCrystallizer
import mekanism.common.tile.machine.TileEntityChemicalDissolutionChamber
import mekanism.common.tile.machine.TileEntityChemicalWasher
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack

private val machinePos = BlockPos(3, 1, 3)
private const val POSITIVE_SPEED_UPGRADES = 8
private const val POSITIVE_ENERGY_UPGRADES = 8

internal fun replenish(
    machine: TileEntityMekanism,
    chemicalId: String?,
) {
    giveEnergy(machine)
    if (chemicalId == null) return
    if (machine is TileEntityAdvancedElectricMachine) giveChemical(machine.chemicalTank, chemicalId)
    if (machine is TileEntityChemicalDissolutionChamber) giveChemical(machine.injectTank, chemicalId)
}

internal fun configurePositiveMachine(machine: TileEntityMekanism) {
    val component = machine.component
    for ((upgrade, expected) in mapOf(
        Upgrade.SPEED to POSITIVE_SPEED_UPGRADES,
        Upgrade.ENERGY to POSITIVE_ENERGY_UPGRADES,
    )) {
        if (machine.supportedUpgrade.contains(upgrade)) {
            val alreadyInstalled = component.getUpgrades(upgrade)
            val needed = maxOf(0, expected - alreadyInstalled)
            val count = component.addUpgrades(upgrade, needed)
            check(alreadyInstalled + count == expected) {
                "Installed ${alreadyInstalled + count} of $expected $upgrade upgrades on $machine"
            }
        }
    }
    giveEnergy(machine)
}

internal fun giveEnergy(machine: TileEntityMekanism) {
    for (container in machine.getEnergyContainers(null)) container.energy = container.maxEnergy
}

/** Mekanism 10.7.19のfill委譲と同じEXTERNAL挿入を使い、拒否量を差し引く。 */

internal fun fillWater(
    washer: TileEntityChemicalWasher,
    amount: Int,
): Int {
    val remainder =
        washer.fluidTank.insert(
            FluidStack(Fluids.WATER, amount),
            Action.EXECUTE,
            AutomationType.EXTERNAL,
        )
    return amount - remainder.amount
}

internal fun giveChemical(
    tank: IChemicalTank,
    chemicalId: String,
) {
    if (tank.needed > 0) tank.insert(chemical(chemicalId, tank.needed), Action.EXECUTE, AutomationType.INTERNAL)
}

internal fun advanced(
    helper: GameTestHelper,
    machineId: String,
    inputId: String,
    count: Int,
): TileEntityAdvancedElectricMachine {
    val tile = placeMachine(helper, machineId)
    helper.assertTrue(
        tile is TileEntityAdvancedElectricMachine,
        "$machineId is not an advanced electric machine: $tile",
    )
    val advanced = tile as TileEntityAdvancedElectricMachine
    inputSlot(advanced).stack = ItemStack(item(inputId), count)
    return advanced
}

internal fun dissolution(
    helper: GameTestHelper,
    inputId: String?,
    count: Int,
): TileEntityChemicalDissolutionChamber {
    val tile = placeMachine(helper, "mekanism:chemical_dissolution_chamber")
    helper.assertTrue(
        tile is TileEntityChemicalDissolutionChamber,
        "dissolution block did not create its real machine tile: $tile",
    )
    val chamber = tile as TileEntityChemicalDissolutionChamber
    if (inputId != null) inputSlot(chamber).stack = ItemStack(item(inputId), count)
    return chamber
}

internal fun washer(helper: GameTestHelper): TileEntityChemicalWasher {
    val tile = placeMachine(helper, "mekanism:chemical_washer")
    helper.assertTrue(tile is TileEntityChemicalWasher, "washer block did not create its real machine tile: $tile")
    return tile as TileEntityChemicalWasher
}

internal fun crystallizer(helper: GameTestHelper): TileEntityChemicalCrystallizer {
    val tile = placeMachine(helper, "mekanism:chemical_crystallizer")
    helper.assertTrue(
        tile is TileEntityChemicalCrystallizer,
        "crystallizer block did not create its real machine tile: $tile",
    )
    return tile as TileEntityChemicalCrystallizer
}

internal fun placeMachine(
    helper: GameTestHelper,
    machineId: String,
): TileEntityMekanism {
    helper.setBlock(machinePos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(machineId)))
    val entity = helper.level.getBlockEntity(helper.absolutePos(machinePos))
    helper.assertTrue(entity is TileEntityMekanism, "$machineId did not create a real Mekanism tile: $entity")
    return entity as TileEntityMekanism
}
