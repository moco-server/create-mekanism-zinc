// 既存packageを維持する。
@file:Suppress("ktlint:standard:package-name", "PackageNaming")

package org.mocoserver.create_mekanism_zinc

import mekanism.api.MekanismAPI
import mekanism.api.chemical.Chemical
import mekanism.api.chemical.ChemicalStack
import mekanism.api.datagen.recipe.builder.ChemicalCrystallizerRecipeBuilder
import mekanism.api.datagen.recipe.builder.ChemicalDissolutionRecipeBuilder
import mekanism.api.datagen.recipe.builder.FluidChemicalToChemicalRecipeBuilder
import mekanism.api.datagen.recipe.builder.ItemStackChemicalToItemStackRecipeBuilder
import mekanism.api.datagen.recipe.builder.ItemStackToItemStackRecipeBuilder
import mekanism.api.datamaps.IMekanismDataMapTypes
import mekanism.api.datamaps.chemical.ChemicalSolidTag
import mekanism.api.recipes.ingredients.ChemicalStackIngredient
import mekanism.api.recipes.ingredients.ItemStackIngredient
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess
import net.minecraft.core.Holder
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder
import net.minecraft.data.tags.ItemTagsProvider
import net.minecraft.data.tags.TagsProvider
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.common.data.DataMapProvider
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.data.event.GatherDataEvent
import org.mocoserver.create_mekanism_zinc.content.ZincContent
import org.mocoserver.create_mekanism_zinc.processing.ZincProcessing
import java.util.concurrent.CompletableFuture

/** ゲーム非依存の加工係数を対象版Mekanismのdatagen APIへ渡す。 */
object ZincDataGenerators {
    fun gatherData(event: GatherDataEvent) {
        val generator = event.generator
        val output = generator.packOutput
        val lookup = event.lookupProvider
        generator.addProvider(event.includeServer(), ZincRecipes(output, lookup))
        generator.addProvider(
            event.includeServer(),
            ZincItemTags(
                output,
                lookup,
                CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()),
                event.existingFileHelper,
            ),
        )
        generator.addProvider(event.includeServer(), ZincDataMaps(output, lookup))
    }

    private class ZincRecipes(
        output: PackOutput,
        lookup: CompletableFuture<HolderLookup.Provider>,
    ) : RecipeProvider(output, lookup) {
        override fun buildRecipes(output: RecipeOutput) {
            for (recipe in ZincProcessing.recipes()) {
                val id = ResourceLocation.fromNamespaceAndPath(ZincContent.MOD_ID, "processing/zinc/${recipe.path}")
                when (recipe.kind) {
                    ZincProcessing.Kind.ENRICHING, ZincProcessing.Kind.CRUSHING -> buildItemRecipe(recipe, output, id)
                    ZincProcessing.Kind.PURIFYING, ZincProcessing.Kind.INJECTING ->
                        buildChemicalItemRecipe(
                            recipe,
                            output,
                            id,
                        )
                    ZincProcessing.Kind.DISSOLUTION -> buildDissolution(recipe, output, id)
                    ZincProcessing.Kind.WASHING -> buildWashing(recipe, output, id)
                    ZincProcessing.Kind.CRYSTALLIZING -> buildCrystallizing(recipe, output, id)
                    ZincProcessing.Kind.SMELTING, ZincProcessing.Kind.BLASTING -> buildCooking(recipe, output, id)
                }
            }
        }

        private fun buildItemRecipe(
            recipe: ZincProcessing.ProcessingRecipe,
            output: RecipeOutput,
            id: ResourceLocation,
        ) {
            val input = itemInput(recipe)
            val result = ItemStack(item(recipe.outputId), recipe.outputCount)
            val builder =
                if (recipe.kind ==
                    ZincProcessing.Kind.ENRICHING
                ) {
                    ItemStackToItemStackRecipeBuilder.enriching(input, result)
                } else {
                    ItemStackToItemStackRecipeBuilder.crushing(input, result)
                }
            builder.build(output, id)
        }

        private fun buildChemicalItemRecipe(
            recipe: ZincProcessing.ProcessingRecipe,
            output: RecipeOutput,
            id: ResourceLocation,
        ) {
            val input = itemInput(recipe)
            val chemical = chemicalInput(requireNotNull(recipe.chemicalInput))
            val result = ItemStack(item(recipe.outputId), recipe.outputCount)
            val builder =
                if (recipe.kind ==
                    ZincProcessing.Kind.PURIFYING
                ) {
                    ItemStackChemicalToItemStackRecipeBuilder.purifying(
                        input,
                        chemical,
                        result,
                        recipe.perTickUsage,
                    )
                } else {
                    ItemStackChemicalToItemStackRecipeBuilder.injecting(
                        input,
                        chemical,
                        result,
                        recipe.perTickUsage,
                    )
                }
            builder.build(output, id)
        }

        private fun buildDissolution(
            recipe: ZincProcessing.ProcessingRecipe,
            output: RecipeOutput,
            id: ResourceLocation,
        ) {
            val input = itemInput(recipe)
            val acid = chemicalInput(requireNotNull(recipe.chemicalInput))
            val slurry = ChemicalStack(chemical(recipe.outputId), recipe.outputCount.toLong())
            ChemicalDissolutionRecipeBuilder
                .dissolution(
                    input,
                    acid,
                    slurry,
                    recipe.perTickUsage,
                ).build(output, id)
        }

        private fun buildWashing(
            recipe: ZincProcessing.ProcessingRecipe,
            output: RecipeOutput,
            id: ResourceLocation,
        ) {
            val slurry = chemicalInput(recipe.inputId, recipe.inputCount.toLong())
            val fluid = requireNotNull(recipe.fluidInput)
            val waterTag = TagKey.create(Registries.FLUID, ResourceLocation.parse(fluid.tag))
            val water = IngredientCreatorAccess.fluid().from(waterTag, fluid.amount)
            val cleanSlurry = ChemicalStack(chemical(recipe.outputId), recipe.outputCount.toLong())
            FluidChemicalToChemicalRecipeBuilder.washing(water, slurry, cleanSlurry).build(output, id)
        }

        private fun buildCrystallizing(
            recipe: ZincProcessing.ProcessingRecipe,
            output: RecipeOutput,
            id: ResourceLocation,
        ) {
            val slurry = chemicalInput(recipe.inputId, recipe.inputCount.toLong())
            val crystals = ItemStack(item(recipe.outputId), recipe.outputCount)
            ChemicalCrystallizerRecipeBuilder.crystallizing(slurry, crystals).build(output, id)
        }

        private fun buildCooking(
            recipe: ZincProcessing.ProcessingRecipe,
            output: RecipeOutput,
            id: ResourceLocation,
        ) {
            val result = ItemStack(item(recipe.outputId), recipe.outputCount)
            val inputItem = item(recipe.inputId)
            val input = Ingredient.of(inputItem)
            val builder =
                if (recipe.kind ==
                    ZincProcessing.Kind.SMELTING
                ) {
                    SimpleCookingRecipeBuilder.smelting(
                        input,
                        RecipeCategory.MISC,
                        result,
                        recipe.experience,
                        recipe.cookingTicks,
                    )
                } else {
                    SimpleCookingRecipeBuilder.blasting(
                        input,
                        RecipeCategory.MISC,
                        result,
                        recipe.experience,
                        recipe.cookingTicks,
                    )
                }
            builder.unlockedBy("has_zinc_dust", has(inputItem)).save(output, id)
        }
    }

    private fun itemInput(recipe: ZincProcessing.ProcessingRecipe): ItemStackIngredient =
        if (recipe.inputTag) {
            IngredientCreatorAccess.item().from(
                TagKey.create(Registries.ITEM, ResourceLocation.parse(recipe.inputId)),
                recipe.inputCount,
            )
        } else {
            IngredientCreatorAccess.item().from(item(recipe.inputId), recipe.inputCount)
        }

    private fun chemicalInput(input: ZincProcessing.ChemicalInput): ChemicalStackIngredient =
        chemicalInput(input.id, input.amount)

    private fun chemicalInput(
        id: String,
        amount: Long,
    ): ChemicalStackIngredient = IngredientCreatorAccess.chemicalStack().fromHolder(chemical(id), amount)

    private fun item(id: String): Item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))

    private fun chemical(id: String): Holder<Chemical> {
        val location = ResourceLocation.parse(id)
        if (location.namespace ==
            ZincContent.MOD_ID
        ) {
            return ZincRegistries.REGISTERED_CHEMICALS.getValue(location.path)
        }
        val key = ResourceKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, location)
        return MekanismAPI.CHEMICAL_REGISTRY.getHolder(key).orElseThrow {
            IllegalStateException("Unknown Mekanism chemical $id")
        }
    }

    private class ZincItemTags(
        output: PackOutput,
        lookup: CompletableFuture<HolderLookup.Provider>,
        blocks: CompletableFuture<TagsProvider.TagLookup<Block>>,
        existing: ExistingFileHelper,
    ) : ItemTagsProvider(output, lookup, blocks, ZincContent.MOD_ID, existing) {
        override fun addTags(lookup: HolderLookup.Provider) {
            for (definition in ZincContent.ITEMS) {
                val subtag = itemTag(definition.tag)
                tag(subtag).add(ZincRegistries.REGISTERED_ITEMS.getValue(definition.id).get())
                val parent = itemTag(definition.tag.substringBeforeLast('/'))
                tag(parent).addTag(subtag)
            }
        }
    }

    private class ZincDataMaps(
        output: PackOutput,
        lookup: CompletableFuture<HolderLookup.Provider>,
    ) : DataMapProvider(output, lookup) {
        override fun gather(provider: HolderLookup.Provider) {
            val id = ResourceLocation.fromNamespaceAndPath(ZincContent.MOD_ID, "clean_zinc")
            val key = ResourceKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, id)
            val holder = provider.lookupOrThrow(MekanismAPI.CHEMICAL_REGISTRY_NAME).getOrThrow(key)
            val ores = itemTag("c:ores/zinc")
            builder(IMekanismDataMapTypes.INSTANCE.chemicalSolidTag()).add(holder, ChemicalSolidTag(ores), false)
        }
    }

    private fun itemTag(id: String): TagKey<Item> = TagKey.create(Registries.ITEM, ResourceLocation.parse(id))
}
