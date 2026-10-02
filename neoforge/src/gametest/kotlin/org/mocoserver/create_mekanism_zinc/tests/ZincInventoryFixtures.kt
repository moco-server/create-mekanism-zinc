// 既存packageと、独立した加工量・待機tickの期待値を維持する。
@file:Suppress("ktlint:standard:package-name", "PackageNaming", "MagicNumber")

package org.mocoserver.create_mekanism_zinc.tests

import mekanism.api.MekanismAPI
import mekanism.api.chemical.ChemicalStack
import mekanism.common.inventory.slot.InputInventorySlot
import mekanism.common.inventory.slot.OutputInventorySlot
import mekanism.common.tile.base.TileEntityMekanism
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

internal fun inputSlot(machine: TileEntityMekanism): InputInventorySlot =
    machine.getInventorySlots(null).filterIsInstance<InputInventorySlot>().first()

internal fun outputSlot(machine: TileEntityMekanism): OutputInventorySlot =
    machine.getInventorySlots(null).filterIsInstance<OutputInventorySlot>().first()

internal fun takeOutput(slot: OutputInventorySlot): ItemStack {
    val actual = slot.stack.copy()
    slot.stack = ItemStack.EMPTY
    return actual
}

internal fun assertItem(
    helper: GameTestHelper,
    stack: ItemStack,
    itemId: String,
    count: Int,
    stage: String,
) {
    helper.assertTrue(
        stack.`is`(item(itemId)) && stack.count == count,
        "$stage expected $itemId x$count, got $stack",
    )
}

internal fun item(id: String): Item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))

internal fun chemical(
    id: String,
    amount: Long,
): ChemicalStack =
    ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.getHolder(ResourceLocation.parse(id)).orElseThrow(), amount)
