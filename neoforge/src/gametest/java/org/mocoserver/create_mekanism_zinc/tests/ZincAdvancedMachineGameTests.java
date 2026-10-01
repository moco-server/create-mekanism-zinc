package org.mocoserver.create_mekanism_zinc.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.Upgrade;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.MekanismAPI;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.machine.TileEntityChemicalCrystallizer;
import mekanism.common.tile.machine.TileEntityChemicalDissolutionChamber;
import mekanism.common.tile.machine.TileEntityChemicalWasher;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import mekanism.common.tile.prefab.TileEntityElectricMachine;
import mekanism.common.recipe.MekanismRecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.mocoserver.create_mekanism_zinc.content.ZincContent;

@GameTestHolder(ZincContent.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ZincAdvancedMachineGameTests {
    private static final BlockPos MACHINE = new BlockPos(3, 1, 3);
    private static final TagKey<Item> ZINC_ORES = TagKey.create(Registries.ITEM, id("c", "ores/zinc"));
    private static final TagKey<Item> RAW_ZINC = TagKey.create(Registries.ITEM, id("c", "raw_materials/zinc"));
    private static final TagKey<Item> RAW_ZINC_BLOCK = TagKey.create(Registries.ITEM, id("c", "storage_blocks/raw_zinc"));
    private static final TagKey<Item> ZINC_INGOTS = TagKey.create(Registries.ITEM, id("c", "ingots/zinc"));
    private static final long STAGE_TIMEOUT = 800;
    private static final int POSITIVE_SPEED_UPGRADES = 8;
    private static final int POSITIVE_ENERGY_UPGRADES = 8;

    @GameTest(template = "processing_line", timeoutTicks = 1800)
    public static void threeTimesNormalOre(GameTestHelper helper) { pipeline(helper, 3, "create:zinc_ore", 1, 3); }

    @GameTest(template = "processing_line", timeoutTicks = 1800)
    public static void threeTimesDeepslateOre(GameTestHelper helper) { pipeline(helper, 3, "create:deepslate_zinc_ore", 1, 3); }

    @GameTest(template = "processing_line", timeoutTicks = 1800)
    public static void threeTimesRawBatch(GameTestHelper helper) { pipeline(helper, 3, "create:raw_zinc", 1, 2); }

    @GameTest(template = "processing_line", timeoutTicks = 1800)
    public static void threeTimesRawBlock(GameTestHelper helper) { pipeline(helper, 3, "create:raw_zinc_block", 1, 18); }

    @GameTest(template = "processing_line", timeoutTicks = 1800)
    public static void fourTimesNormalOre(GameTestHelper helper) { pipeline(helper, 4, "create:zinc_ore", 1, 4); }

    @GameTest(template = "processing_line", timeoutTicks = 1800)
    public static void fourTimesDeepslateOre(GameTestHelper helper) { pipeline(helper, 4, "create:deepslate_zinc_ore", 1, 4); }

    @GameTest(template = "processing_line", timeoutTicks = 1800)
    public static void fourTimesRawBatch(GameTestHelper helper) { pipeline(helper, 4, "create:raw_zinc", 3, 8); }

    @GameTest(template = "processing_line", timeoutTicks = 1800)
    public static void fourTimesRawBlock(GameTestHelper helper) { pipeline(helper, 4, "create:raw_zinc_block", 1, 24); }

    @GameTest(template = "processing_line", timeoutTicks = 2400)
    public static void fiveTimesNormalOre(GameTestHelper helper) { pipeline(helper, 5, "create:zinc_ore", 1, 5); }

    @GameTest(template = "processing_line", timeoutTicks = 2400)
    public static void fiveTimesDeepslateOre(GameTestHelper helper) { pipeline(helper, 5, "create:deepslate_zinc_ore", 1, 5); }

    @GameTest(template = "processing_line", timeoutTicks = 2400)
    public static void fiveTimesRawBatch(GameTestHelper helper) { pipeline(helper, 5, "create:raw_zinc", 3, 10); }

    @GameTest(template = "processing_line", timeoutTicks = 8000)
    public static void fiveTimesRawBlock(GameTestHelper helper) { pipeline(helper, 5, "create:raw_zinc_block", 1, 30); }

    @GameTest(template = "processing_line", timeoutTicks = 1400)
    public static void fourAndFiveTimesInputsRequireThreeRawZinc(GameTestHelper helper) {
        checkRawBatchGate(helper, 4, 1, () -> checkRawBatchGate(helper, 4, 2,
                () -> checkRawBatchGate(helper, 5, 1, () -> checkRawBatchGate(helper, 5, 2, helper::succeed))));
    }

    @GameTest(template = "processing_line", timeoutTicks = 1600)
    public static void newPoweredMachineTypesStopWithoutEnergy(GameTestHelper helper) {
        checkPowerGate(helper, "mekanism:purification_chamber", "create:zinc_ore", "mekanism:oxygen", 1,
                () -> checkPowerGate(helper, "mekanism:chemical_injection_chamber", "create:zinc_ore", "mekanism:hydrogen_chloride", 1,
                () -> checkPowerGate(helper, "mekanism:chemical_dissolution_chamber", "create:zinc_ore", "mekanism:sulfuric_acid", 1,
                () -> checkPowerGate(helper, "mekanism:chemical_washer", null, null, 0,
                () -> checkPowerGate(helper, "mekanism:chemical_crystallizer", null, null, 0, helper::succeed)))));
    }

    @GameTest(template = "processing_line", timeoutTicks = 900)
    public static void missingPerTickChemicalsRetainItemsAndStallAfterDepletion(GameTestHelper helper) {
        checkPerTickStarvation(helper, "mekanism:purification_chamber", "create:zinc_ore", "mekanism:oxygen",
                () -> checkPerTickStarvation(helper, "mekanism:chemical_injection_chamber", "create:zinc_ore", "mekanism:hydrogen_chloride",
                () -> checkPerTickStarvation(helper, "mekanism:chemical_dissolution_chamber", "create:zinc_ore", "mekanism:sulfuric_acid", helper::succeed)));
    }

    @GameTest(template = "processing_line", timeoutTicks = 700)
    public static void washerNeedsFiveWaterPerDirtySlurryAndMatchingChemical(GameTestHelper helper) {
        TileEntityChemicalWasher washer = washer(helper);
        ChemicalStack dirty = chemical("create_mekanism_zinc:dirty_zinc", 1);
        helper.assertTrue(washer.inputTank.insert(dirty, Action.EXECUTE, AutomationType.INTERNAL).isEmpty(), "washer rejected dirty zinc slurry");
        washer.fluidTank.fill(new FluidStack(Fluids.WATER, 4), FluidAction.EXECUTE);
        giveEnergy(washer);
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(washer.inputTank.getStack().getAmount() == 1, "washer consumed slurry with only 4 mB water");
            helper.assertTrue(washer.outputTank.isEmpty(), "washer produced clean slurry with insufficient water");
            helper.succeed();
        });
    }

    @GameTest(template = "processing_line", timeoutTicks = 700)
    public static void crystallizerRetainsLessThanTwoHundredCleanSlurry(GameTestHelper helper) {
        TileEntityChemicalCrystallizer crystallizer = crystallizer(helper);
        ChemicalStack clean = chemical("create_mekanism_zinc:clean_zinc", 199);
        helper.assertTrue(crystallizer.inputTank.insert(clean, Action.EXECUTE, AutomationType.INTERNAL).isEmpty(), "crystallizer rejected valid clean zinc slurry");
        giveEnergy(crystallizer);
        helper.runAfterDelay(240, () -> {
            helper.assertTrue(crystallizer.inputTank.getStack().is(clean.getChemical()) && crystallizer.inputTank.getStack().getAmount() == 199,
                    "crystallizer consumed less than 200 mB: " + crystallizer.inputTank.getStack());
            OutputInventorySlot out = outputSlot(crystallizer);
            helper.assertTrue(out.getStack().isEmpty(), "crystallizer produced a crystal from 199 mB");
            helper.succeed();
        });
    }

    @GameTest(template = "processing_line")
    public static void addonEntranceRecipesRejectCrushedRawAndZincIngots(GameTestHelper helper) {
        Item crushed = item("create:crushed_raw_zinc");
        Item ingot = item("create:zinc_ingot");
        ChemicalStack oxygen = chemical("mekanism:oxygen", 100);
        ChemicalStack acid = chemical("mekanism:sulfuric_acid", 100);
        int purifying = 0;
        for (var holder : helper.getLevel().getRecipeManager().getAllRecipesFor(MekanismRecipeType.PURIFYING.get())) {
            if (holder.id().getNamespace().equals(ZincContent.MOD_ID)) {
                purifying++;
                var recipe = holder.value();
                helper.assertTrue(!recipe.test(new ItemStack(crushed), oxygen), holder.id() + " accepts crushed raw zinc");
                helper.assertTrue(!recipe.test(new ItemStack(ingot), oxygen), holder.id() + " accepts a zinc ingot");
            }
        }
        int injecting = 0;
        for (var holder : helper.getLevel().getRecipeManager().getAllRecipesFor(MekanismRecipeType.INJECTING.get())) {
            if (holder.id().getNamespace().equals(ZincContent.MOD_ID)) {
                injecting++;
                var recipe = holder.value();
                helper.assertTrue(!recipe.test(new ItemStack(crushed), chemical("mekanism:hydrogen_chloride", 100)), holder.id() + " accepts crushed raw zinc");
                helper.assertTrue(!recipe.test(new ItemStack(ingot), chemical("mekanism:hydrogen_chloride", 100)), holder.id() + " accepts a zinc ingot");
            }
        }
        int dissolution = 0;
        for (var holder : helper.getLevel().getRecipeManager().getAllRecipesFor(MekanismRecipeType.DISSOLUTION.get())) {
            if (holder.id().getNamespace().equals(ZincContent.MOD_ID)) {
                dissolution++;
                var recipe = holder.value();
                helper.assertTrue(!recipe.test(new ItemStack(crushed), acid), holder.id() + " accepts crushed raw zinc");
                helper.assertTrue(!recipe.test(new ItemStack(ingot), acid), holder.id() + " accepts a zinc ingot");
            }
        }
        helper.assertTrue(purifying > 0 && injecting > 0 && dissolution > 0,
                "expected addon purifying/injecting/dissolution recipe families, got " + purifying + "/" + injecting + "/" + dissolution);
        helper.succeed();
    }

    @GameTest(template = "processing_line", timeoutTicks = 1000)
    public static void wrongChemicalsAreRejectedByAllChemicalRecipeInputs(GameTestHelper helper) {
        TileEntityAdvancedElectricMachine purifying = advanced(helper, "mekanism:purification_chamber", "create:zinc_ore", 1);
        assertWrongChemical(helper, purifying.chemicalTank, "mekanism:hydrogen_chloride", () -> {
            TileEntityAdvancedElectricMachine injecting = advanced(helper, "mekanism:chemical_injection_chamber", "create:zinc_ore", 1);
            assertWrongChemical(helper, injecting.chemicalTank, "mekanism:oxygen", () -> {
                TileEntityChemicalDissolutionChamber dissolution = dissolution(helper, "create:zinc_ore", 1);
                assertWrongChemical(helper, dissolution.injectTank, "mekanism:oxygen", () -> {
                    TileEntityChemicalWasher washer = washer(helper);
                    washer.fluidTank.fill(new FluidStack(Fluids.WATER, 5), FluidAction.EXECUTE);
                    assertWrongChemical(helper, washer.inputTank, "create_mekanism_zinc:clean_zinc", () -> {
                        TileEntityChemicalCrystallizer crystallizer = crystallizer(helper);
                        assertWrongChemical(helper, crystallizer.inputTank, "create_mekanism_zinc:dirty_zinc", helper::succeed);
                    });
                });
            });
        });
    }

    @GameTest(template = "processing_line")
    public static void exactZincOreItemsAreInTheirExpectedInputTags(GameTestHelper helper) {
        helper.assertTrue(member(ZINC_ORES, item("create:zinc_ore")), "create:zinc_ore missing c:ores/zinc");
        helper.assertTrue(member(ZINC_ORES, item("create:deepslate_zinc_ore")), "create:deepslate_zinc_ore missing c:ores/zinc");
        helper.assertTrue(member(RAW_ZINC, item("create:raw_zinc")), "create:raw_zinc missing c:raw_materials/zinc");
        helper.assertTrue(member(RAW_ZINC_BLOCK, item("create:raw_zinc_block")), "create:raw_zinc_block missing c:storage_blocks/raw_zinc");
        helper.assertTrue(member(ZINC_INGOTS, item("create:zinc_ingot")), "create:zinc_ingot missing c:ingots/zinc");
        helper.succeed();
    }

    private static void pipeline(GameTestHelper helper, int multiplier, String inputId, int inputCount, int expectedIngots) {
        ItemStack input = new ItemStack(item(inputId), inputCount);
        switch (multiplier) {
            case 3 -> runItemStage(helper, "mekanism:purification_chamber", input, "mekanism:oxygen",
                    stack -> { assertItem(helper, stack, "create_mekanism_zinc:clump_zinc", expectedIngots, "3x purifying"); runDustLine(helper, stack, expectedIngots); });
            case 4 -> runItemStage(helper, "mekanism:chemical_injection_chamber", input, "mekanism:hydrogen_chloride",
                    stack -> { assertItem(helper, stack, "create_mekanism_zinc:shard_zinc", expectedIngots, "4x injecting");
                        runItemStage(helper, "mekanism:purification_chamber", stack, "mekanism:oxygen",
                                clumps -> { assertItem(helper, clumps, "create_mekanism_zinc:clump_zinc", expectedIngots, "shard purifying"); runDustLine(helper, clumps, expectedIngots); }); });
            case 5 -> runDissolutionLine(helper, input, expectedIngots);
            default -> throw new IllegalArgumentException("Unknown multiplier " + multiplier);
        }
    }

    private static void runDissolutionLine(GameTestHelper helper, ItemStack input, int expectedIngots) {
        int dirtyAmount = expectedIngots == 5 ? 1000 : expectedIngots == 10 ? 2000 : 6000;
        TileEntityChemicalDissolutionChamber chamber = dissolution(helper, null, 0);
        InputInventorySlot in = inputSlot(chamber);
        in.setStack(input.copy());
        configurePositiveMachine(chamber);
        giveChemical(chamber.injectTank, "mekanism:sulfuric_acid");
        final long[] acidInitial = {chamber.injectTank.getStored()};
        pollMachine(helper, chamber, "mekanism:sulfuric_acid", 300, () -> {
            ChemicalStack dirty = chamber.outputTank.getStack().copy();
            helper.assertTrue(dirty.is(chemical("create_mekanism_zinc:dirty_zinc", 1).getChemical()) && dirty.getAmount() == dirtyAmount,
                    "dissolution expected dirty_zinc " + dirtyAmount + ", got " + dirty);
            helper.assertTrue(in.getStack().isEmpty(), "dissolution retained input " + in.getStack());
            helper.assertTrue(chamber.injectTank.getStored() < acidInitial[0], "sulfuric acid was not consumed during dissolution");
            long acidConsumed = acidInitial[0] - chamber.injectTank.getStored();
            helper.assertTrue(acidConsumed > 0, "dissolution consumed no sulfuric acid");
            TileEntityChemicalWasher washer = washer(helper);
            int initialWater = washer.fluidTank.fill(new FluidStack(Fluids.WATER, washer.fluidTank.getCapacity()), FluidAction.EXECUTE);
            helper.assertTrue(initialWater == washer.fluidTank.getCapacity(), "could not fill washer's actual water capacity");
            helper.assertTrue(washer.inputTank.insert(dirty.copy(), Action.EXECUTE, AutomationType.INTERNAL).isEmpty(), "washer rejected actual dirty slurry output");
            runWasher(helper, washer, dirtyAmount, totalWater -> {
            helper.assertTrue(totalWater == dirtyAmount * 5L, "washer consumed " + totalWater + " water; expected " + (dirtyAmount * 5L));
            ChemicalStack clean = washer.outputTank.getStack().copy();
            helper.assertTrue(clean.is(chemical("create_mekanism_zinc:clean_zinc", 1).getChemical()) && clean.getAmount() == dirtyAmount,
                    "washing expected clean slurry " + dirtyAmount + ", got " + clean);
            TileEntityChemicalCrystallizer crystallizer = crystallizer(helper);
            helper.assertTrue(crystallizer.inputTank.insert(clean.copy(), Action.EXECUTE, AutomationType.INTERNAL).isEmpty(), "crystallizer rejected washed slurry");
            configurePositiveMachine(crystallizer);
            pollMachine(helper, crystallizer, null, 6500, () -> {
                ChemicalStack remaining = crystallizer.inputTank.getStack();
                long crystals = dirtyAmount / 200L;
                helper.assertTrue(remaining.getAmount() == dirtyAmount % 200L, "crystallizer remainder should be " + (dirtyAmount % 200L) + ", got " + remaining);
                ItemStack crystalOutput = outputSlot(crystallizer).getStack().copy();
                assertItem(helper, crystalOutput, "create_mekanism_zinc:crystal_zinc", (int) crystals, "crystallizing");
                ItemStack crystalsInput = takeOutput(outputSlot(crystallizer));
                runItemStage(helper, "mekanism:chemical_injection_chamber", crystalsInput, "mekanism:hydrogen_chloride",
                        shards -> { assertItem(helper, shards, "create_mekanism_zinc:shard_zinc", (int) crystals, "crystal injecting");
                            runItemStage(helper, "mekanism:purification_chamber", shards, "mekanism:oxygen",
                                    clumps -> { assertItem(helper, clumps, "create_mekanism_zinc:clump_zinc", (int) crystals, "shard purifying"); runDustLine(helper, clumps, (int) crystals); }); });
            });
            });
        });
    }

    private static void runWasher(GameTestHelper helper, TileEntityChemicalWasher washer, int expectedOutputAmount,
                                  Consumer<Long> onComplete) {
        configurePositiveMachine(washer);
        int[] elapsed = {0};
        long[] supplied = {washer.fluidTank.getFluidAmount()};
        Runnable[] poll = new Runnable[1];
        poll[0] = () -> {
            giveEnergy(washer);
            int needed = washer.fluidTank.getCapacity() - washer.fluidTank.getFluidAmount();
            if (needed > 0) {
                int filled = washer.fluidTank.fill(new FluidStack(Fluids.WATER, needed), FluidAction.EXECUTE);
                supplied[0] += filled;
            }
            helper.runAfterDelay(1, () -> {
                elapsed[0]++;
                if (washer.inputTank.isEmpty() && washer.outputTank.getStack().getAmount() == expectedOutputAmount) {
                    long used = supplied[0] - washer.fluidTank.getFluidAmount();
                    helper.assertTrue(used == expectedOutputAmount * 5L,
                            "water used was " + used + " mB for " + expectedOutputAmount + " slurry");
                    helper.assertTrue(supplied[0] > 0, "washer was not supplied with water");
                    onComplete.accept(used);
                    return;
                }
                if (elapsed[0] > STAGE_TIMEOUT) {
                    helper.fail("washer timed out: input=" + washer.inputTank.getStack() + " water=" + washer.fluidTank.getFluid() + " output=" + washer.outputTank.getStack());
                    return;
                }
                poll[0].run();
            });
        };
        poll[0].run();
    }

    private static void runDustLine(GameTestHelper helper, ItemStack clumps, int expectedIngots) {
        runItemStage(helper, "mekanism:crusher", clumps, null, dirty -> {
            assertItem(helper, dirty, "create_mekanism_zinc:dirty_dust_zinc", expectedIngots, "clump crushing");
            runItemStage(helper, "mekanism:enrichment_chamber", dirty, null, dust -> {
                assertItem(helper, dust, "create_mekanism_zinc:dust_zinc", expectedIngots, "dirty dust enriching");
                smelt(helper, dust, expectedIngots);
            });
        });
    }

    private static void smelt(GameTestHelper helper, ItemStack dust, int expected) {
        helper.assertTrue(dust.is(item("create_mekanism_zinc:dust_zinc")) && dust.getCount() == expected,
                "smelting input was not the actual expected zinc dust: " + dust);
        List<AbstractFurnaceBlockEntity> furnaces = new ArrayList<>();
        int index = 0;
        for (int x = 1; x <= 5 && index < expected; x++) {
            for (int z = 1; z <= 6 && index < expected; z++) {
                BlockPos pos = new BlockPos(x, 1, z);
                helper.setBlock(pos, Blocks.BLAST_FURNACE);
                BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
                helper.assertTrue(entity instanceof AbstractFurnaceBlockEntity, "blast furnace did not create its vanilla block entity");
                AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) entity;
                furnace.setItem(0, new ItemStack(dust.getItem(), 1));
                furnace.setItem(1, new ItemStack(item("minecraft:coal"), 1));
                furnaces.add(furnace);
                dust.shrink(1);
                index++;
            }
        }
        helper.assertTrue(dust.isEmpty(), "not all actual dust output was split across real blast furnaces: " + dust);
        helper.runAfterDelay(99, () -> {
            for (AbstractFurnaceBlockEntity furnace : furnaces) {
                helper.assertTrue(furnace.getItem(2).isEmpty(), "blasting finished before 100 ticks");
            }
        });
        helper.runAfterDelay(180, () -> {
            int total = 0;
            for (AbstractFurnaceBlockEntity furnace : furnaces) {
                helper.assertTrue(furnace.getItem(0).isEmpty(), "blast furnace did not consume one dust");
                helper.assertTrue(furnace.getItem(2).is(item("create:zinc_ingot")), "blast furnace output was " + furnace.getItem(2));
                total += furnace.getItem(2).getCount();
            }
            helper.assertTrue(total == expected, "expected exactly " + expected + " create:zinc_ingot, got " + total);
            helper.succeed();
        });
    }

    private static void runItemStage(GameTestHelper helper, String machineId, ItemStack actualInput, String chemicalId, Consumer<ItemStack> onOutput) {
        TileEntityMekanism raw = placeMachine(helper, machineId);
        helper.assertTrue(raw instanceof TileEntityElectricMachine || raw instanceof TileEntityAdvancedElectricMachine,
                machineId + " is not an actual item-processing machine: " + raw);
        InputInventorySlot in = inputSlot(raw);
        OutputInventorySlot out = outputSlot(raw);
        helper.assertTrue(out.getStack().isEmpty(), machineId + " output was not empty at stage start: " + out.getStack());
        in.setStack(actualInput.copy());
        configurePositiveMachine(raw);
        IChemicalTank tank = null;
        if (raw instanceof TileEntityAdvancedElectricMachine advanced && chemicalId != null) {
            tank = advanced.chemicalTank;
            giveChemical(tank, chemicalId);
        }
        final IChemicalTank stageTank = tank;
        final long[] chemicalBefore = {stageTank == null ? 0 : stageTank.getStored()};
        pollMachine(helper, raw, chemicalId, 800, () -> {
            ItemStack actualOutput = out.getStack().copy();
            helper.assertTrue(in.getStack().isEmpty(), machineId + " retained input after producing output: " + in.getStack());
            if (stageTank != null) helper.assertTrue(stageTank.getStored() < chemicalBefore[0], machineId + " did not consume its per-tick chemical");
            out.setStack(ItemStack.EMPTY);
            onOutput.accept(actualOutput);
        });
    }

    private static void pollMachine(GameTestHelper helper, TileEntityMekanism machine, String chemicalId, int maxTicks, Runnable complete) {
        int[] elapsed = {0};
        Runnable[] poll = new Runnable[1];
        poll[0] = () -> {
            replenish(machine, chemicalId);
            helper.runAfterDelay(1, () -> {
                elapsed[0]++;
                if (machine instanceof TileEntityElectricMachine electric) {
                    if (!outputSlot(electric).getStack().isEmpty() && inputSlot(electric).getStack().isEmpty()) { complete.run(); return; }
                } else if (machine instanceof TileEntityAdvancedElectricMachine advanced) {
                    if (!outputSlot(advanced).getStack().isEmpty() && inputSlot(advanced).getStack().isEmpty()) { complete.run(); return; }
                } else if (machine instanceof TileEntityChemicalDissolutionChamber chamber) {
                    if (!chamber.outputTank.isEmpty() && inputSlot(chamber).getStack().isEmpty()) { complete.run(); return; }
                } else if (machine instanceof TileEntityChemicalCrystallizer crystallizer) {
                    if (crystallizer.inputTank.isEmpty() && !outputSlot(crystallizer).getStack().isEmpty()) { complete.run(); return; }
                }
                if (elapsed[0] > maxTicks) {
                    helper.fail("machine timed out: " + machine.getClass().getSimpleName() + " after " + elapsed[0] + " ticks; " + machineState(machine));
                    return;
                }
                poll[0].run();
            });
        };
        poll[0].run();
    }

    private static String machineState(TileEntityMekanism machine) {
        StringBuilder state = new StringBuilder("active=").append(machine.getActive()).append(" energy=");
        state.append(machine.getEnergyContainers(null).stream().mapToLong(c -> c.getEnergy()).sum());
        if (machine instanceof TileEntityAdvancedElectricMachine advanced) {
            state.append(" input=").append(inputSlot(advanced).getStack()).append(" output=").append(outputSlot(advanced).getStack())
                    .append(" chemical=").append(advanced.chemicalTank.getStack()).append(" ticks=").append(advanced.getOperatingTicks()).append('/').append(advanced.getTicksRequired());
        } else if (machine instanceof TileEntityChemicalDissolutionChamber chamber) {
            state.append(" input=").append(inputSlot(chamber).getStack()).append(" output=").append(chamber.outputTank.getStack())
                    .append(" chemical=").append(chamber.injectTank.getStack()).append(" ticks=").append(chamber.getOperatingTicks()).append('/').append(chamber.getTicksRequired());
        } else if (machine instanceof TileEntityChemicalCrystallizer crystallizer) {
            state.append(" inputChemical=").append(crystallizer.inputTank.getStack()).append(" output=").append(outputSlot(crystallizer).getStack())
                    .append(" ticks=").append(crystallizer.getOperatingTicks()).append('/').append(crystallizer.getTicksRequired())
                    .append(" speedUpgrades=").append(machine.getComponent().getUpgrades(Upgrade.SPEED))
                    .append(" energyUpgrades=").append(machine.getComponent().getUpgrades(Upgrade.ENERGY));
        } else if (machine instanceof TileEntityChemicalWasher washer) {
            state.append(" slurryIn=").append(washer.inputTank.getStack()).append(" fluid=").append(washer.fluidTank.getFluid())
                    .append(" slurryOut=").append(washer.outputTank.getStack());
        } else if (machine instanceof TileEntityElectricMachine electric) {
            state.append(" input=").append(inputSlot(electric).getStack()).append(" output=").append(outputSlot(electric).getStack()).append(" ticks=").append(electric.getOperatingTicks()).append('/').append(electric.getTicksRequired());
        }
        return state.toString();
    }

    private static void replenish(TileEntityMekanism machine, String chemicalId) {
        giveEnergy(machine);
        if (chemicalId == null) return;
        if (machine instanceof TileEntityAdvancedElectricMachine advanced) giveChemical(advanced.chemicalTank, chemicalId);
        if (machine instanceof TileEntityChemicalDissolutionChamber chamber) giveChemical(chamber.injectTank, chemicalId);
    }

    private static void configurePositiveMachine(TileEntityMekanism machine) {
        var component = machine.getComponent();
        if (POSITIVE_SPEED_UPGRADES > 0 && machine.getSupportedUpgrade().contains(Upgrade.SPEED)) {
            int alreadyInstalled = component.getUpgrades(Upgrade.SPEED);
            int needed = Math.max(0, POSITIVE_SPEED_UPGRADES - alreadyInstalled);
            int count = component.addUpgrades(Upgrade.SPEED, needed);
            if (alreadyInstalled + count != POSITIVE_SPEED_UPGRADES) throw new IllegalStateException("Installed " + (alreadyInstalled + count) + " of " + POSITIVE_SPEED_UPGRADES + " SPEED upgrades on " + machine);
        }
        if (POSITIVE_ENERGY_UPGRADES > 0 && machine.getSupportedUpgrade().contains(Upgrade.ENERGY)) {
            int alreadyInstalled = component.getUpgrades(Upgrade.ENERGY);
            int needed = Math.max(0, POSITIVE_ENERGY_UPGRADES - alreadyInstalled);
            int count = component.addUpgrades(Upgrade.ENERGY, needed);
            if (alreadyInstalled + count != POSITIVE_ENERGY_UPGRADES) throw new IllegalStateException("Installed " + (alreadyInstalled + count) + " of " + POSITIVE_ENERGY_UPGRADES + " ENERGY upgrades on " + machine);
        }
        giveEnergy(machine);
    }

    private static void giveEnergy(TileEntityMekanism machine) {
        for (var container : machine.getEnergyContainers(null)) container.setEnergy(container.getMaxEnergy());
    }

    private static void giveChemical(IChemicalTank tank, String chemicalId) {
        if (tank.getNeeded() <= 0) return;
        ChemicalStack fill = chemical(chemicalId, tank.getNeeded());
        ChemicalStack remainder = tank.insert(fill, Action.EXECUTE, AutomationType.INTERNAL);
        if (!remainder.isEmpty() && !tank.getStack().isEmpty() && tank.getStack().is(remainder.getChemical())) {
            // A per-tick input is replenished from its actual remaining contents; tanks reject foreign chemicals.
        }
    }

    private static void checkRawBatchGate(GameTestHelper helper, int multiplier, int count, Runnable next) {
        String machineId = multiplier == 4 ? "mekanism:chemical_injection_chamber" : "mekanism:chemical_dissolution_chamber";
        TileEntityMekanism machine = placeMachine(helper, machineId);
        InputInventorySlot input = inputSlot(machine);
        input.setStack(new ItemStack(item("create:raw_zinc"), count));
        configurePositiveMachine(machine);
        if (machine instanceof TileEntityAdvancedElectricMachine advanced) giveChemical(advanced.chemicalTank, "mekanism:hydrogen_chloride");
        else if (machine instanceof TileEntityChemicalDissolutionChamber chamber) giveChemical(chamber.injectTank, "mekanism:sulfuric_acid");
        helper.runAfterDelay(240, () -> {
            helper.assertTrue(input.getStack().is(item("create:raw_zinc")) && input.getStack().getCount() == count,
                    multiplier + "x raw input " + count + " unexpectedly changed");
            if (machine instanceof TileEntityAdvancedElectricMachine advanced) helper.assertTrue(advanced.getInventorySlots(null).stream().filter(OutputInventorySlot.class::isInstance).map(OutputInventorySlot.class::cast).allMatch(slot -> slot.getStack().isEmpty()), "4x raw produced a shard from fewer than three raw zinc");
            if (machine instanceof TileEntityChemicalDissolutionChamber chamber) helper.assertTrue(chamber.outputTank.isEmpty(), "5x raw produced slurry from fewer than three raw zinc");
            next.run();
        });
    }

    private static void checkPowerGate(GameTestHelper helper, String machineId, String inputId, String chemicalId, int inputCount, Runnable next) {
        TileEntityMekanism machine = placeMachine(helper, machineId);
        if (machine instanceof TileEntityElectricMachine electric && inputId != null) inputSlot(electric).setStack(new ItemStack(item(inputId), inputCount));
        if (machine instanceof TileEntityAdvancedElectricMachine advanced && inputId != null) inputSlot(advanced).setStack(new ItemStack(item(inputId), inputCount));
        if (machine instanceof TileEntityChemicalDissolutionChamber chamber && inputId != null) inputSlot(chamber).setStack(new ItemStack(item(inputId), inputCount));
        if (chemicalId != null) {
            IChemicalTank tank = machine instanceof TileEntityAdvancedElectricMachine advanced ? advanced.chemicalTank : ((TileEntityChemicalDissolutionChamber) machine).injectTank;
            giveChemical(tank, chemicalId);
        } else if (machine instanceof TileEntityChemicalWasher washer) {
            washer.fluidTank.fill(new FluidStack(Fluids.WATER, 5), FluidAction.EXECUTE);
            helper.assertTrue(washer.inputTank.insert(chemical("create_mekanism_zinc:dirty_zinc", 1), Action.EXECUTE, AutomationType.INTERNAL).isEmpty(), "washer rejected dirty slurry for no-power case");
        } else if (machine instanceof TileEntityChemicalCrystallizer crystallizer) {
            crystallizer.inputTank.insert(chemical("create_mekanism_zinc:clean_zinc", 200), Action.EXECUTE, AutomationType.INTERNAL);
        }
        helper.runAfterDelay(260, () -> {
            if (machine instanceof TileEntityChemicalWasher washer) {
                helper.assertTrue(washer.inputTank.getStack().getAmount() == 1 && washer.outputTank.isEmpty(), "unpowered washer changed slurry");
            } else if (machine instanceof TileEntityChemicalCrystallizer crystallizer) {
                helper.assertTrue(crystallizer.inputTank.getStack().getAmount() == 200 && outputSlot(crystallizer).getStack().isEmpty(), "unpowered crystallizer changed slurry");
            } else if (machine instanceof TileEntityChemicalDissolutionChamber chamber) {
                helper.assertTrue(inputSlot(chamber).getStack().is(item(inputId)) && chamber.outputTank.isEmpty(), "unpowered dissolution chamber processed input");
            } else if (machine instanceof TileEntityAdvancedElectricMachine advanced) {
                helper.assertTrue(inputSlot(advanced).getStack().is(item(inputId)), "unpowered " + machineId + " consumed input");
                helper.assertTrue(outputSlot(advanced).getStack().isEmpty(), "unpowered " + machineId + " produced output");
            }
            next.run();
        });
    }

    private static void checkPerTickStarvation(GameTestHelper helper, String machineId, String inputId, String chemicalId, Runnable next) {
        TileEntityMekanism machine = placeMachine(helper, machineId);
        InputInventorySlot input = inputSlot(machine);
        input.setStack(new ItemStack(item(inputId), 1));
        IChemicalTank tank = machine instanceof TileEntityAdvancedElectricMachine advanced ? advanced.chemicalTank : ((TileEntityChemicalDissolutionChamber) machine).injectTank;
        giveEnergy(machine);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(input.getStack().is(item(inputId)), machineId + " consumed input before any " + chemicalId);
            if (machine instanceof TileEntityAdvancedElectricMachine advanced) helper.assertTrue(outputSlot(advanced).getStack().isEmpty(), machineId + " produced output without " + chemicalId);
            else helper.assertTrue(((TileEntityChemicalDissolutionChamber) machine).outputTank.isEmpty(), machineId + " produced slurry without sulfuric acid");
            giveChemical(tank, chemicalId);
            long initial = tank.getStored();
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(tank.getStored() < initial, machineId + " did not consume " + chemicalId + " while working");
                tank.setEmpty();
                helper.runAfterDelay(60, () -> {
                    helper.assertTrue(tank.isEmpty(), machineId + " chemical tank was refilled during starvation test");
                    helper.assertTrue(input.getStack().is(item(inputId)), machineId + " consumed input with no remaining per-tick chemical");
                    if (machine instanceof TileEntityAdvancedElectricMachine advanced) helper.assertTrue(outputSlot(advanced).getStack().isEmpty(), machineId + " produced output after chemical starvation");
                    else helper.assertTrue(((TileEntityChemicalDissolutionChamber) machine).outputTank.isEmpty(), machineId + " produced slurry after acid starvation");
                    next.run();
                });
            });
        });
    }

    private static void assertWrongChemical(GameTestHelper helper, IChemicalTank tank, String wrongChemical, Runnable next) {
        ChemicalStack sample = chemical(wrongChemical, 20);
        ChemicalStack remainder = tank.insert(sample, Action.EXECUTE, AutomationType.INTERNAL);
        helper.assertTrue(remainder.getAmount() == sample.getAmount() && remainder.is(sample.getChemical()),
                "input tank accepted wrong chemical " + wrongChemical + ": " + tank.getStack());
        next.run();
    }

    private static TileEntityAdvancedElectricMachine advanced(GameTestHelper helper, String machineId, String inputId, int count) {
        TileEntityMekanism tile = placeMachine(helper, machineId);
        helper.assertTrue(tile instanceof TileEntityAdvancedElectricMachine, machineId + " is not an advanced electric machine: " + tile);
        TileEntityAdvancedElectricMachine advanced = (TileEntityAdvancedElectricMachine) tile;
        inputSlot(advanced).setStack(new ItemStack(item(inputId), count));
        return advanced;
    }

    private static TileEntityChemicalDissolutionChamber dissolution(GameTestHelper helper, String inputId, int count) {
        TileEntityMekanism tile = placeMachine(helper, "mekanism:chemical_dissolution_chamber");
        helper.assertTrue(tile instanceof TileEntityChemicalDissolutionChamber, "dissolution block did not create its real machine tile: " + tile);
        TileEntityChemicalDissolutionChamber chamber = (TileEntityChemicalDissolutionChamber) tile;
        if (inputId != null) inputSlot(chamber).setStack(new ItemStack(item(inputId), count));
        return chamber;
    }

    private static TileEntityChemicalWasher washer(GameTestHelper helper) {
        TileEntityMekanism tile = placeMachine(helper, "mekanism:chemical_washer");
        helper.assertTrue(tile instanceof TileEntityChemicalWasher, "washer block did not create its real machine tile: " + tile);
        return (TileEntityChemicalWasher) tile;
    }

    private static TileEntityChemicalCrystallizer crystallizer(GameTestHelper helper) {
        TileEntityMekanism tile = placeMachine(helper, "mekanism:chemical_crystallizer");
        helper.assertTrue(tile instanceof TileEntityChemicalCrystallizer, "crystallizer block did not create its real machine tile: " + tile);
        return (TileEntityChemicalCrystallizer) tile;
    }

    private static TileEntityMekanism placeMachine(GameTestHelper helper, String machineId) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(machineId));
        helper.setBlock(MACHINE, block);
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE));
        helper.assertTrue(entity instanceof TileEntityMekanism, machineId + " did not create a real Mekanism tile: " + entity);
        return (TileEntityMekanism) entity;
    }

    private static InputInventorySlot inputSlot(TileEntityMekanism machine) {
        return machine.getInventorySlots(null).stream().filter(InputInventorySlot.class::isInstance).map(InputInventorySlot.class::cast).findFirst().orElseThrow();
    }

    private static OutputInventorySlot outputSlot(TileEntityMekanism machine) {
        return machine.getInventorySlots(null).stream().filter(OutputInventorySlot.class::isInstance).map(OutputInventorySlot.class::cast).findFirst().orElseThrow();
    }

    private static ItemStack takeOutput(OutputInventorySlot slot) {
        ItemStack actual = slot.getStack().copy();
        slot.setStack(ItemStack.EMPTY);
        return actual;
    }

    private static void assertItem(GameTestHelper helper, ItemStack stack, String itemId, int count, String stage) {
        helper.assertTrue(stack.is(item(itemId)) && stack.getCount() == count,
                stage + " expected " + itemId + " x" + count + ", got " + stack);
    }

    private static boolean member(TagKey<Item> tag, Item item) {
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) if (holder.value() == item) return true;
        return false;
    }

    private static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }

    private static ChemicalStack chemical(String id, long amount) {
        return new ChemicalStack(MekanismAPI.CHEMICAL_REGISTRY.getHolder(ResourceLocation.parse(id)).orElseThrow(), amount);
    }

    private static ResourceLocation id(String namespace, String path) { return ResourceLocation.fromNamespaceAndPath(namespace, path); }
}
