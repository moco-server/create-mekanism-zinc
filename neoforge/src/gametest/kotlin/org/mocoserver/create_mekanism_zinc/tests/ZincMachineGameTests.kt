// 既存packageと、独立した加工数量・待機tickの期待値を維持する。
@file:Suppress("ktlint:standard:package-name", "PackageNaming", "MagicNumber")

package org.mocoserver.create_mekanism_zinc.tests

import mekanism.api.MekanismAPI
import mekanism.api.datamaps.IMekanismDataMapTypes
import mekanism.common.inventory.slot.InputInventorySlot
import mekanism.common.inventory.slot.OutputInventorySlot
import mekanism.common.recipe.MekanismRecipeType
import mekanism.common.tile.prefab.TileEntityElectricMachine
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import org.mocoserver.create_mekanism_zinc.content.ZincContent

// 同一のGameTest holderに従来の12件を保持し、テストIDを維持する。
@Suppress("TooManyFunctions")
@GameTestHolder(ZincContent.MOD_ID)
@PrefixGameTestTemplate(false)
object ZincMachineGameTests {
    private val machinePos = BlockPos(1, 2, 1)
    private val zincOres = TagKey.create(Registries.ITEM, id("c:ores/zinc"))
    private val rawZinc = TagKey.create(Registries.ITEM, id("c:raw_materials/zinc"))
    private val rawZincBlock = TagKey.create(Registries.ITEM, id("c:storage_blocks/raw_zinc"))
    private val zincIngots = TagKey.create(Registries.ITEM, id("c:ingots/zinc"))

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 700)
    fun normalOreEnrichesToTwoDust(helper: GameTestHelper) {
        val ore = item("create:zinc_ore")
        helper.assertTrue(isMember(zincOres, ore), "create:zinc_ore is absent from c:ores/zinc")
        process(helper, "mekanism:enrichment_chamber", ore, 1, 2, "normal ore")
    }

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 700)
    fun deepslateOreEnrichesToTwoDust(helper: GameTestHelper) {
        val ore = item("create:deepslate_zinc_ore")
        helper.assertTrue(isMember(zincOres, ore), "create:deepslate_zinc_ore is absent from c:ores/zinc")
        process(helper, "mekanism:enrichment_chamber", ore, 1, 2, "deepslate ore")
    }

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 700)
    fun threeRawZincEnrichesToFourDust(helper: GameTestHelper) {
        val raw = item("create:raw_zinc")
        helper.assertTrue(isMember(rawZinc, raw), "create:raw_zinc is absent from c:raw_materials/zinc")
        process(helper, "mekanism:enrichment_chamber", raw, 3, 4, "3 raw zinc")
    }

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 700)
    fun rawZincBlockEnrichesToTwelveDust(helper: GameTestHelper) {
        val block = item("create:raw_zinc_block")
        helper.assertTrue(
            isMember(rawZincBlock, block),
            "create:raw_zinc_block is absent from c:storage_blocks/raw_zinc",
        )
        process(helper, "mekanism:enrichment_chamber", block, 1, 12, "raw zinc block")
    }

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 700)
    fun oneRawZincIsRetained(helper: GameTestHelper) =
        remainsBlocked(helper, item("create:raw_zinc"), 1, true, "one raw zinc")

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 700)
    fun twoRawZincAreRetained(helper: GameTestHelper) =
        remainsBlocked(helper, item("create:raw_zinc"), 2, true, "two raw zinc")

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 700)
    fun oreDoesNotProcessWithoutPower(helper: GameTestHelper) =
        remainsBlocked(helper, item("create:zinc_ore"), 1, false, "no power")

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 700)
    fun ingotCrushesToOneDust(helper: GameTestHelper) {
        val ingot = item("create:zinc_ingot")
        helper.assertTrue(isMember(zincIngots, ingot), "create:zinc_ingot is absent from c:ingots/zinc")
        process(helper, "mekanism:crusher", ingot, 1, 1, "zinc ingot crushing")
    }

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 700)
    fun dustSmeltsToCreateIngot(helper: GameTestHelper) = furnace(helper, Blocks.FURNACE, 200, "smelting")

    @JvmStatic
    @GameTest(template = "empty", timeoutTicks = 700)
    fun dustBlastsToCreateIngot(helper: GameTestHelper) = furnace(helper, Blocks.BLAST_FURNACE, 100, "blasting")

    @JvmStatic
    @GameTest(template = "empty")
    fun crushedRawZincIsRejected(helper: GameTestHelper) {
        val recipes = helper.level.recipeManager.getAllRecipesFor(MekanismRecipeType.ENRICHING.get())
        val accepted =
            recipes
                .filter {
                    it.id().namespace == ZincContent.MOD_ID
                }.any { it.value().test(ItemStack(item("create:crushed_raw_zinc"))) }
        helper.assertTrue(!accepted, "Addon enrichment recipes must not accept create:crushed_raw_zinc")
        helper.succeed()
    }

    @JvmStatic
    @GameTest(template = "empty")
    fun cleanSlurrySolidTagLoadsWithBaseMapping(helper: GameTestHelper) {
        val access = helper.level.registryAccess()
        val dataType = IMekanismDataMapTypes.INSTANCE.chemicalSolidTag()
        val clean = MekanismAPI.CHEMICAL_REGISTRY.getHolder(id("${ZincContent.MOD_ID}:clean_zinc")).orElseThrow()
        val iron = MekanismAPI.CHEMICAL_REGISTRY.getHolder(id("mekanism:clean_iron")).orElseThrow()
        val cleanData =
            IMekanismDataMapTypes.INSTANCE.getData(
                access,
                MekanismAPI.CHEMICAL_REGISTRY.key(),
                clean,
                dataType,
            )
        val ironData =
            IMekanismDataMapTypes.INSTANCE.getData(
                access,
                MekanismAPI.CHEMICAL_REGISTRY.key(),
                iron,
                dataType,
            )
        helper.assertTrue(
            cleanData?.solidRepresentation()?.location() == id("c:ores/zinc"),
            "clean_zinc lacks c:ores/zinc data map entry: $cleanData",
        )
        helper.assertTrue(
            ironData?.solidRepresentation()?.location() == id("c:ores/iron"),
            "Base clean_iron data map entry was lost: $ironData",
        )
        helper.succeed()
    }

    // 実機入力と期待する出力量を独立した引数として保持する。
    @Suppress("LongParameterList")
    private fun process(
        helper: GameTestHelper,
        machineId: String,
        input: Item,
        inputCount: Int,
        outputCount: Int,
        label: String,
    ) {
        val machine = placeMachine(helper, machineId)
        val inputSlot = machine.getInventorySlots(null).filterIsInstance<InputInventorySlot>().first()
        val outputSlot = machine.getInventorySlots(null).filterIsInstance<OutputInventorySlot>().first()
        inputSlot.stack = ItemStack(input, inputCount)
        machine.energyContainer.energy = machine.energyContainer.maxEnergy
        helper.runAfterDelay(240) {
            val result = outputSlot.stack
            helper.assertTrue(
                result.`is`(item("${ZincContent.MOD_ID}:dust_zinc")) && result.count == outputCount,
                "$label expected exactly $outputCount dust, got $result",
            )
            helper.assertTrue(inputSlot.stack.isEmpty, "$label input was not consumed: ${inputSlot.stack}")
            helper.succeed()
        }
    }

    private fun remainsBlocked(
        helper: GameTestHelper,
        input: Item,
        count: Int,
        powered: Boolean,
        label: String,
    ) {
        val machine = placeMachine(helper, "mekanism:enrichment_chamber")
        val inputSlot = machine.getInventorySlots(null).filterIsInstance<InputInventorySlot>().first()
        val outputSlot = machine.getInventorySlots(null).filterIsInstance<OutputInventorySlot>().first()
        inputSlot.stack = ItemStack(input, count)
        if (powered) machine.energyContainer.energy = machine.energyContainer.maxEnergy
        helper.runAfterDelay(260) {
            helper.assertTrue(
                inputSlot.stack.`is`(input) && inputSlot.stack.count == count,
                "$label unexpectedly changed input: ${inputSlot.stack}",
            )
            helper.assertTrue(outputSlot.stack.isEmpty, "$label unexpectedly produced output: ${outputSlot.stack}")
            helper.succeed()
        }
    }

    private fun placeMachine(
        helper: GameTestHelper,
        machineId: String,
    ): TileEntityElectricMachine {
        helper.setBlock(machinePos, BuiltInRegistries.BLOCK.get(id(machineId)))
        val entity = helper.level.getBlockEntity(helper.absolutePos(machinePos))
        helper.assertTrue(
            entity is TileEntityElectricMachine,
            "$machineId did not create an electric machine tile: $entity",
        )
        return entity as TileEntityElectricMachine
    }

    private fun furnace(
        helper: GameTestHelper,
        block: Block,
        cookingTicks: Int,
        mode: String,
    ) {
        helper.setBlock(machinePos, block)
        val raw = helper.level.getBlockEntity(helper.absolutePos(machinePos))
        helper.assertTrue(raw is AbstractFurnaceBlockEntity, "$mode block did not create a furnace block entity")
        val furnace = raw as AbstractFurnaceBlockEntity
        val ingot = item("create:zinc_ingot")
        furnace.setItem(0, ItemStack(item("${ZincContent.MOD_ID}:dust_zinc")))
        furnace.setItem(1, ItemStack(item("minecraft:coal"), 1))
        helper.runAfterDelay((cookingTicks - 1).toLong()) {
            helper.assertTrue(furnace.getItem(2).isEmpty, "$mode completed before $cookingTicks ticks")
        }
        helper.runAfterDelay((cookingTicks + 80).toLong()) {
            helper.assertTrue(furnace.getItem(0).isEmpty, "$mode did not consume dust")
            helper.assertTrue(
                furnace.getItem(2).`is`(ingot) && furnace.getItem(2).count == 1,
                "$mode expected one create:zinc_ingot, got ${furnace.getItem(2)}",
            )
            helper.succeed()
        }
    }

    private fun isMember(
        tag: TagKey<Item>,
        item: Item,
    ): Boolean =
        BuiltInRegistries.ITEM.getTagOrEmpty(tag).any {
            it.value() ==
                item
        }

    private fun item(location: String): Item = BuiltInRegistries.ITEM.get(id(location))

    private fun id(location: String): ResourceLocation = ResourceLocation.parse(location)
}
