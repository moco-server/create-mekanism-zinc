package org.mocoserver.create_mekanism_zinc;

import java.util.concurrent.CompletableFuture;
import mekanism.api.MekanismAPI;
import mekanism.api.datamaps.IMekanismDataMapTypes;
import mekanism.api.datamaps.chemical.ChemicalSolidTag;
import mekanism.api.datagen.recipe.builder.ItemStackToItemStackRecipeBuilder;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
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
                var outputItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(recipe.outputId()));
                var result = new ItemStack(outputItem, recipe.outputCount());
                if (recipe.kind() == ZincProcessing.Kind.ENRICHING || recipe.kind() == ZincProcessing.Kind.CRUSHING) {
                    var inputId = tagId(recipe.inputId());
                    var tag = TagKey.create(Registries.ITEM, inputId);
                    var input = IngredientCreatorAccess.item().from(tag, recipe.inputCount());
                    var builder = recipe.kind() == ZincProcessing.Kind.ENRICHING
                            ? ItemStackToItemStackRecipeBuilder.enriching(input, result)
                            : ItemStackToItemStackRecipeBuilder.crushing(input, result);
                    builder.build(output, id);
                } else {
                    var inputItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(recipe.inputId()));
                    var input = Ingredient.of(inputItem);
                    var builder = recipe.kind() == ZincProcessing.Kind.SMELTING
                            ? SimpleCookingRecipeBuilder.smelting(input, RecipeCategory.MISC, result, recipe.experience(), recipe.cookingTicks())
                            : SimpleCookingRecipeBuilder.blasting(input, RecipeCategory.MISC, result, recipe.experience(), recipe.cookingTicks());
                    builder.unlockedBy("has_zinc_dust", has(inputItem)).save(output, id);
                }
            }
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
