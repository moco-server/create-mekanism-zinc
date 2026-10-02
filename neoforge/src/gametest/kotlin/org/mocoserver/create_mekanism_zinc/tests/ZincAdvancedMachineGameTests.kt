// packageと、加工数量・化学物質・待機tickの独立した期待値を維持する。
@file:Suppress("ktlint:standard:package-name", "PackageNaming", "MagicNumber")

package org.mocoserver.create_mekanism_zinc.tests

import mekanism.api.Action
import mekanism.api.AutomationType
import mekanism.common.recipe.MekanismRecipeType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import org.mocoserver.create_mekanism_zinc.content.ZincContent
import org.mocoserver.create_mekanism_zinc.tests.ZincMachineFailureChecks.assertWrongChemical
import org.mocoserver.create_mekanism_zinc.tests.ZincMachineFailureChecks.checkPerTickStarvation
import org.mocoserver.create_mekanism_zinc.tests.ZincMachineFailureChecks.checkPowerGate
import org.mocoserver.create_mekanism_zinc.tests.ZincMachineFailureChecks.checkRawBatchGate
import org.mocoserver.create_mekanism_zinc.tests.ZincProcessingLineFixture.pipeline

// 同じholderで従来20件のテストIDを維持する。非同期補助は責務別のfixtureへ分離する。
@Suppress("TooManyFunctions")
@GameTestHolder(ZincContent.MOD_ID)
@PrefixGameTestTemplate(false)
object ZincAdvancedMachineGameTests {
    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 1800)
    fun threeTimesNormalOre(helper: GameTestHelper) = pipeline(helper, 3, "create:zinc_ore", 1, 3)

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 1800)
    fun threeTimesDeepslateOre(helper: GameTestHelper) = pipeline(helper, 3, "create:deepslate_zinc_ore", 1, 3)

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 1800)
    fun threeTimesRawBatch(helper: GameTestHelper) = pipeline(helper, 3, "create:raw_zinc", 1, 2)

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 1800)
    fun threeTimesRawBlock(helper: GameTestHelper) = pipeline(helper, 3, "create:raw_zinc_block", 1, 18)

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 1800)
    fun fourTimesNormalOre(helper: GameTestHelper) = pipeline(helper, 4, "create:zinc_ore", 1, 4)

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 1800)
    fun fourTimesDeepslateOre(helper: GameTestHelper) = pipeline(helper, 4, "create:deepslate_zinc_ore", 1, 4)

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 1800)
    fun fourTimesRawBatch(helper: GameTestHelper) = pipeline(helper, 4, "create:raw_zinc", 3, 8)

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 1800)
    fun fourTimesRawBlock(helper: GameTestHelper) = pipeline(helper, 4, "create:raw_zinc_block", 1, 24)

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 2400)
    fun fiveTimesNormalOre(helper: GameTestHelper) = pipeline(helper, 5, "create:zinc_ore", 1, 5)

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 2400)
    fun fiveTimesDeepslateOre(helper: GameTestHelper) = pipeline(helper, 5, "create:deepslate_zinc_ore", 1, 5)

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 2400)
    fun fiveTimesRawBatch(helper: GameTestHelper) = pipeline(helper, 5, "create:raw_zinc", 3, 10)

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 8000)
    fun fiveTimesRawBlock(helper: GameTestHelper) = pipeline(helper, 5, "create:raw_zinc_block", 1, 30)

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 1400)
    fun fourAndFiveTimesInputsRequireThreeRawZinc(helper: GameTestHelper) {
        checkRawBatchGate(helper, 4, 1) {
            checkRawBatchGate(helper, 4, 2) {
                checkRawBatchGate(helper, 5, 1) { checkRawBatchGate(helper, 5, 2, helper::succeed) }
            }
        }
    }

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 1600)
    fun newPoweredMachineTypesStopWithoutEnergy(helper: GameTestHelper) {
        checkPowerGate(helper, "mekanism:purification_chamber", "create:zinc_ore", "mekanism:oxygen", 1) {
            checkPowerGate(
                helper,
                "mekanism:chemical_injection_chamber",
                "create:zinc_ore",
                "mekanism:hydrogen_chloride",
                1,
            ) {
                checkPowerGate(
                    helper,
                    "mekanism:chemical_dissolution_chamber",
                    "create:zinc_ore",
                    "mekanism:sulfuric_acid",
                    1,
                ) {
                    checkPowerGate(helper, "mekanism:chemical_washer", null, null, 0) {
                        checkPowerGate(helper, "mekanism:chemical_crystallizer", null, null, 0, helper::succeed)
                    }
                }
            }
        }
    }

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 900)
    fun missingPerTickChemicalsRetainItemsAndStallAfterDepletion(helper: GameTestHelper) {
        checkPerTickStarvation(helper, "mekanism:purification_chamber", "create:zinc_ore", "mekanism:oxygen") {
            checkPerTickStarvation(
                helper,
                "mekanism:chemical_injection_chamber",
                "create:zinc_ore",
                "mekanism:hydrogen_chloride",
            ) {
                checkPerTickStarvation(
                    helper,
                    "mekanism:chemical_dissolution_chamber",
                    "create:zinc_ore",
                    "mekanism:sulfuric_acid",
                    helper::succeed,
                )
            }
        }
    }

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 700)
    fun washerNeedsFiveWaterPerDirtySlurryAndMatchingChemical(helper: GameTestHelper) {
        val washer = washer(helper)
        val dirty = chemical("create_mekanism_zinc:dirty_zinc", 1)
        helper.assertTrue(
            washer.inputTank.insert(dirty, Action.EXECUTE, AutomationType.INTERNAL).isEmpty,
            "washer rejected dirty zinc slurry",
        )
        fillWater(washer, 4)
        giveEnergy(washer)
        helper.runAfterDelay(80) {
            helper.assertTrue(washer.inputTank.stack.amount == 1L, "washer consumed slurry with only 4 mB water")
            helper.assertTrue(washer.outputTank.isEmpty, "washer produced clean slurry with insufficient water")
            helper.succeed()
        }
    }

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 700)
    fun crystallizerRetainsLessThanTwoHundredCleanSlurry(helper: GameTestHelper) {
        val crystallizer = crystallizer(helper)
        val clean = chemical("create_mekanism_zinc:clean_zinc", 199)
        helper.assertTrue(
            crystallizer.inputTank.insert(clean, Action.EXECUTE, AutomationType.INTERNAL).isEmpty,
            "crystallizer rejected valid clean zinc slurry",
        )
        giveEnergy(crystallizer)
        helper.runAfterDelay(240) {
            val remaining = crystallizer.inputTank.stack
            helper.assertTrue(
                remaining.`is`(clean.chemical) && remaining.amount == 199L,
                "crystallizer consumed less than 200 mB: $remaining",
            )
            helper.assertTrue(outputSlot(crystallizer).stack.isEmpty, "crystallizer produced a crystal from 199 mB")
            helper.succeed()
        }
    }

    @JvmStatic
    @GameTest(template = "processing_line")
    // 4つの異なるMekanism recipe型に同じ無効入力の契約を確認する。
    @Suppress("LongMethod")
    fun addonEntranceRecipesRejectCrushedRawAndZincIngots(helper: GameTestHelper) {
        val crushed = ItemStack(item("create:crushed_raw_zinc"), 64)
        val ingot = ItemStack(item("create:zinc_ingot"), 64)
        val oxygen = chemical("mekanism:oxygen", 100)
        val acid = chemical("mekanism:sulfuric_acid", 100)
        val manager = helper.level.recipeManager
        val enriching =
            manager.getAllRecipesFor(MekanismRecipeType.ENRICHING.get()).filter {
                it.id().namespace ==
                    ZincContent.MOD_ID
            }
        for (holder in enriching) {
            helper.assertTrue(!holder.value().test(crushed), "${holder.id()} accepts a 64-stack of crushed raw zinc")
            helper.assertTrue(!holder.value().test(ingot), "${holder.id()} accepts a 64-stack of zinc ingots")
        }
        val purifying =
            manager.getAllRecipesFor(MekanismRecipeType.PURIFYING.get()).filter {
                it.id().namespace ==
                    ZincContent.MOD_ID
            }
        for (holder in purifying) {
            helper.assertTrue(
                !holder.value().test(crushed, oxygen),
                "${holder.id()} accepts a 64-stack of crushed raw zinc",
            )
            helper.assertTrue(!holder.value().test(ingot, oxygen), "${holder.id()} accepts a 64-stack of zinc ingots")
        }
        val injecting =
            manager.getAllRecipesFor(MekanismRecipeType.INJECTING.get()).filter {
                it.id().namespace ==
                    ZincContent.MOD_ID
            }
        for (holder in injecting) {
            val chloride = chemical("mekanism:hydrogen_chloride", 100)
            helper.assertTrue(
                !holder.value().test(crushed, chloride),
                "${holder.id()} accepts a 64-stack of crushed raw zinc",
            )
            helper.assertTrue(!holder.value().test(ingot, chloride), "${holder.id()} accepts a 64-stack of zinc ingots")
        }
        val dissolution =
            manager.getAllRecipesFor(MekanismRecipeType.DISSOLUTION.get()).filter {
                it.id().namespace ==
                    ZincContent.MOD_ID
            }
        for (holder in dissolution) {
            helper.assertTrue(
                !holder.value().test(crushed, acid),
                "${holder.id()} accepts a 64-stack of crushed raw zinc",
            )
            helper.assertTrue(!holder.value().test(ingot, acid), "${holder.id()} accepts a 64-stack of zinc ingots")
        }
        helper.assertTrue(
            enriching.isNotEmpty() && purifying.isNotEmpty() && injecting.isNotEmpty() && dissolution.isNotEmpty(),
            (
                "expected addon recipe families, got " +
                    "${enriching.size}/${purifying.size}/${injecting.size}/${dissolution.size}"
            ),
        )
        helper.succeed()
    }

    @JvmStatic
    @GameTest(template = "processing_line", timeoutTicks = 1000)
    fun wrongChemicalsAreRejectedByAllChemicalRecipeInputs(helper: GameTestHelper) {
        val purifying = advanced(helper, "mekanism:purification_chamber", "create:zinc_ore", 1)
        assertWrongChemical(helper, purifying.chemicalTank, "mekanism:hydrogen_chloride") {
            val injecting = advanced(helper, "mekanism:chemical_injection_chamber", "create:zinc_ore", 1)
            assertWrongChemical(helper, injecting.chemicalTank, "mekanism:oxygen") {
                val chamber = dissolution(helper, "create:zinc_ore", 1)
                assertWrongChemical(helper, chamber.injectTank, "mekanism:oxygen") {
                    val washer = washer(helper)
                    fillWater(washer, 5)
                    assertWrongChemical(helper, washer.inputTank, "create_mekanism_zinc:clean_zinc") {
                        val crystallizer = crystallizer(helper)
                        assertWrongChemical(
                            helper,
                            crystallizer.inputTank,
                            "create_mekanism_zinc:dirty_zinc",
                            helper::succeed,
                        )
                    }
                }
            }
        }
    }

    @JvmStatic
    @GameTest(template = "processing_line")
    fun exactZincOreItemsAreInTheirExpectedInputTags(helper: GameTestHelper) {
        val inputs =
            mapOf(
                "create:zinc_ore" to "c:ores/zinc",
                "create:deepslate_zinc_ore" to "c:ores/zinc",
                "create:raw_zinc" to "c:raw_materials/zinc",
                "create:raw_zinc_block" to "c:storage_blocks/raw_zinc",
                "create:zinc_ingot" to "c:ingots/zinc",
            )
        for ((input, tag) in inputs) {
            val key = TagKey.create(Registries.ITEM, ResourceLocation.parse(tag))
            helper.assertTrue(
                BuiltInRegistries.ITEM.getTagOrEmpty(key).any { it.value() == item(input) },
                "$input missing $tag",
            )
        }
        helper.succeed()
    }
}
