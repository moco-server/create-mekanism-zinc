package org.mocoserver.create_mekanism_zinc;

import java.util.concurrent.CompletableFuture;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.datamaps.IMekanismDataMapTypes;
import mekanism.api.datamaps.chemical.ChemicalSolidTag;
import mekanism.api.datagen.recipe.builder.ChemicalCrystallizerRecipeBuilder;
import mekanism.api.datagen.recipe.builder.ChemicalDissolutionRecipeBuilder;
import mekanism.api.datagen.recipe.builder.FluidChemicalToChemicalRecipeBuilder;
import mekanism.api.datagen.recipe.builder.ItemStackChemicalToItemStackRecipeBuilder;
import mekanism.api.datagen.recipe.builder.ItemStackToItemStackRecipeBuilder;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.mocoserver.create_mekanism_zinc.content.ZincContent;
import org.mocoserver.create_mekanism_zinc.processing.ZincProcessing;

/** Native NeoForge providers built from the common pure Java definitions. */
public final class ZincDataGenerators {
    private ZincDataGenerators() {}

    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var lookup = event.getLookupProvider();
        var existing = event.getExistingFileHelper();
        generator.addProvider(event.includeServer(), new ZincRecipes(output, lookup));
        generator.addProvider(event.includeServer(), new ZincItemTags(output, lookup,
                CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()), existing));
        generator.addProvider(event.includeServer(), new ZincDataMaps(output, lookup));
    }

    private static final class ZincRecipes extends RecipeProvider {
        private ZincRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            for (var recipe : ZincProcessing.recipes()) {
                var id = ResourceLocation.fromNamespaceAndPath(ZincContent.MOD_ID, "processing/zinc/" + recipe.path());
                switch (recipe.kind()) {
                    case ENRICHING, CRUSHING -> {
                        var input = itemInput(recipe);
                        var result = new ItemStack(item(recipe.outputId()), recipe.outputCount());
                        var builder = recipe.kind() == ZincProcessing.Kind.ENRICHING
                                ? ItemStackToItemStackRecipeBuilder.enriching(input, result)
                                : ItemStackToItemStackRecipeBuilder.crushing(input, result);
                        builder.build(output, id);
                    }
                    case PURIFYING, INJECTING -> {
                        var input = itemInput(recipe);
                        var chemical = chemicalInput(recipe.chemicalInput());
                        var result = new ItemStack(item(recipe.outputId()), recipe.outputCount());
                        var builder = recipe.kind() == ZincProcessing.Kind.PURIFYING
                                ? ItemStackChemicalToItemStackRecipeBuilder.purifying(input, chemical, result, recipe.perTickUsage())
                                : ItemStackChemicalToItemStackRecipeBuilder.injecting(input, chemical, result, recipe.perTickUsage());
                        builder.build(output, id);
                    }
                    case DISSOLUTION -> {
                        var input = itemInput(recipe);
                        var acid = chemicalInput(recipe.chemicalInput());
                        var slurry = new ChemicalStack(chemical(recipe.outputId()), recipe.outputCount());
                        ChemicalDissolutionRecipeBuilder.dissolution(input, acid, slurry, recipe.perTickUsage()).build(output, id);
                    }
                    case WASHING -> {
                        var slurry = chemicalInput(recipe.inputId(), recipe.inputCount());
                        var waterTag = TagKey.create(Registries.FLUID, ResourceLocation.parse(recipe.fluidInput().tag()));
                        var water = IngredientCreatorAccess.fluid().from(waterTag, recipe.fluidInput().amount());
                        var cleanSlurry = new ChemicalStack(chemical(recipe.outputId()), recipe.outputCount());
                        FluidChemicalToChemicalRecipeBuilder.washing(water, slurry, cleanSlurry).build(output, id);
                    }
                    case CRYSTALLIZING -> {
                        var cleanSlurry = chemicalInput(recipe.inputId(), recipe.inputCount());
                        var crystals = new ItemStack(item(recipe.outputId()), recipe.outputCount());
                        ChemicalCrystallizerRecipeBuilder.crystallizing(cleanSlurry, crystals).build(output, id);
                    }
                    case SMELTING, BLASTING -> {
                        var outputItem = item(recipe.outputId());
                        var result = new ItemStack(outputItem, recipe.outputCount());
                        var inputItem = item(recipe.inputId());
                        var input = Ingredient.of(inputItem);
                        var builder = recipe.kind() == ZincProcessing.Kind.SMELTING
                                ? SimpleCookingRecipeBuilder.smelting(input, RecipeCategory.MISC, result, recipe.experience(), recipe.cookingTicks())
                                : SimpleCookingRecipeBuilder.blasting(input, RecipeCategory.MISC, result, recipe.experience(), recipe.cookingTicks());
                        builder.unlockedBy("has_zinc_dust", has(inputItem)).save(output, id);
                    }
                }
            }
        }

        private static mekanism.api.recipes.ingredients.ItemStackIngredient itemInput(ZincProcessing.ProcessingRecipe recipe) {
            if (recipe.inputTag()) {
                return IngredientCreatorAccess.item().from(TagKey.create(Registries.ITEM, ResourceLocation.parse(recipe.inputId())), recipe.inputCount());
            }
            return IngredientCreatorAccess.item().from(item(recipe.inputId()), recipe.inputCount());
        }

        private static mekanism.api.recipes.ingredients.ChemicalStackIngredient chemicalInput(ZincProcessing.ChemicalInput input) {
            return IngredientCreatorAccess.chemicalStack().fromHolder(chemical(input.id()), input.amount());
        }

        private static mekanism.api.recipes.ingredients.ChemicalStackIngredient chemicalInput(String id, long amount) {
            return IngredientCreatorAccess.chemicalStack().fromHolder(chemical(id), amount);
        }

        private static net.minecraft.world.item.Item item(String id) {
            return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        }

        private static net.minecraft.core.Holder<Chemical> chemical(String id) {
            var location = ResourceLocation.parse(id);
            if (location.getNamespace().equals(ZincContent.MOD_ID)) {
                return ZincRegistries.REGISTERED_CHEMICALS.get(location.getPath());
            }
            var key = ResourceKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, location);
            return MekanismAPI.CHEMICAL_REGISTRY.getHolder(key).orElseThrow(() -> new IllegalStateException("Unknown Mekanism chemical " + id));
        }
    }

    private static final class ZincItemTags extends ItemTagsProvider {
        private ZincItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                             CompletableFuture<TagsProvider.TagLookup<Block>> blocks, ExistingFileHelper existing) {
            super(output, lookup, blocks, ZincContent.MOD_ID, existing);
        }

        @Override
        protected void addTags(HolderLookup.Provider lookup) {
            for (var definition : ZincContent.ITEMS) {
                var subtag = itemTag(definition.tag());
                tag(subtag).add(ZincRegistries.REGISTERED_ITEMS.get(definition.id()).get());
                var separator = definition.tag().lastIndexOf('/');
                var parent = itemTag(definition.tag().substring(0, separator));
                tag(parent).addTag(subtag);
            }
        }
    }

    private static final class ZincDataMaps extends DataMapProvider {
        private ZincDataMaps(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
            super(output, lookup);
        }

        @Override
        protected void gather(HolderLookup.Provider provider) {
            var id = ResourceLocation.fromNamespaceAndPath(ZincContent.MOD_ID, "clean_zinc");
            var key = net.minecraft.resources.ResourceKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, id);
            var holder = provider.lookupOrThrow(MekanismAPI.CHEMICAL_REGISTRY_NAME).getOrThrow(key);
            var ores = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ores/zinc"));
            builder(IMekanismDataMapTypes.INSTANCE.chemicalSolidTag())
                    .add(holder, new ChemicalSolidTag(ores), false);
        }
    }

    private static ResourceLocation tagId(String id) {
        return ResourceLocation.parse(id);
    }

    private static TagKey<Item> itemTag(String id) {
        return TagKey.create(Registries.ITEM, tagId(id));
    }
}
