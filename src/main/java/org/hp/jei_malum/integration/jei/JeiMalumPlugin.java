package org.hp.jei_malum.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.hp.jei_malum.Jei_malum;
import org.hp.jei_malum.reaping.MalumReapingDataCollector;
import org.hp.jei_malum.reaping.ReapingDropsRecipe;
import org.hp.jei_malum.spirit.MalumSpiritDataCollector;
import org.hp.jei_malum.spirit.SpiritHarvestRecipe;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JeiPlugin
public class JeiMalumPlugin implements IModPlugin {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Jei_malum.MODID, "malum_spirit_harvest");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        ItemStack spiritIconStack = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("malum", "sacred_spirit")).getDefaultInstance();
        ItemStack reapingIconStack = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("malum", "warp_flux")).getDefaultInstance();
        registration.addRecipeCategories(
                new SpiritHarvestCategory(registration.getJeiHelpers().getGuiHelper(), spiritIconStack),
                new ReapingDropsCategory(registration.getJeiHelpers().getGuiHelper(), reapingIconStack)
        );
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addTypedRecipeManagerPlugin(SpiritHarvestCategory.RECIPE_TYPE, new SpiritHarvestRecipeLookup(collectSpiritRecipes()));
        registration.addTypedRecipeManagerPlugin(ReapingDropsCategory.RECIPE_TYPE, new ReapingDropsRecipeLookup(collectReapingRecipes()));
    }

    private static List<SpiritHarvestRecipe> collectSpiritRecipes() {
        return MalumSpiritDataCollector.collectRecipes().stream()
                .filter(recipe -> !recipe.spiritStack().isEmpty())
                .sorted(Comparator.comparing(recipe -> recipe.spiritId().toString()))
                .toList();
    }

    private static List<ReapingDropsRecipe> collectReapingRecipes() {
        return MalumReapingDataCollector.collectRecipes().stream()
                .filter(recipe -> !recipe.dropStack().isEmpty())
                .sorted(Comparator.comparing(recipe -> recipe.dropId().toString()))
                .toList();
    }

    private static final class SpiritHarvestRecipeLookup implements ISimpleRecipeManagerPlugin<SpiritHarvestRecipe> {
        private final List<SpiritHarvestRecipe> recipes;
        private final Map<ResourceLocation, SpiritHarvestRecipe> recipesBySpirit;

        private SpiritHarvestRecipeLookup(List<SpiritHarvestRecipe> recipes) {
            this.recipes = recipes;
            this.recipesBySpirit = recipes.stream()
                    .collect(Collectors.toMap(SpiritHarvestRecipe::spiritId, recipe -> recipe));
        }

        @Override
        public boolean isHandledInput(ITypedIngredient<?> input) {
            return getItemId(input) != null;
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> output) {
            return getItemId(output) != null;
        }

        @Override
        public List<SpiritHarvestRecipe> getRecipesForInput(ITypedIngredient<?> input) {
            return getRecipe(input);
        }

        @Override
        public List<SpiritHarvestRecipe> getRecipesForOutput(ITypedIngredient<?> output) {
            return getRecipe(output);
        }

        @Override
        public List<SpiritHarvestRecipe> getAllRecipes() {
            return recipes;
        }

        private List<SpiritHarvestRecipe> getRecipe(ITypedIngredient<?> ingredient) {
            ResourceLocation itemId = getItemId(ingredient);
            SpiritHarvestRecipe recipe = itemId == null ? null : recipesBySpirit.get(itemId);
            if (recipe == null) {
                return List.of();
            }
            return List.of(recipe);
        }

        private ResourceLocation getItemId(ITypedIngredient<?> ingredient) {
            return ingredient.getItemStack()
                    .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                    .filter(recipesBySpirit::containsKey)
                    .orElse(null);
        }
    }

    private static final class ReapingDropsRecipeLookup implements ISimpleRecipeManagerPlugin<ReapingDropsRecipe> {
        private final List<ReapingDropsRecipe> recipes;
        private final Map<ResourceLocation, ReapingDropsRecipe> recipesByDrop;

        private ReapingDropsRecipeLookup(List<ReapingDropsRecipe> recipes) {
            this.recipes = recipes;
            this.recipesByDrop = recipes.stream()
                    .collect(Collectors.toMap(ReapingDropsRecipe::dropId, recipe -> recipe));
        }

        @Override
        public boolean isHandledInput(ITypedIngredient<?> input) {
            return getItemId(input) != null;
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> output) {
            return getItemId(output) != null;
        }

        @Override
        public List<ReapingDropsRecipe> getRecipesForInput(ITypedIngredient<?> input) {
            return getRecipe(input);
        }

        @Override
        public List<ReapingDropsRecipe> getRecipesForOutput(ITypedIngredient<?> output) {
            return getRecipe(output);
        }

        @Override
        public List<ReapingDropsRecipe> getAllRecipes() {
            return recipes;
        }

        private List<ReapingDropsRecipe> getRecipe(ITypedIngredient<?> ingredient) {
            ResourceLocation itemId = getItemId(ingredient);
            ReapingDropsRecipe recipe = itemId == null ? null : recipesByDrop.get(itemId);
            if (recipe == null) {
                return List.of();
            }
            return List.of(recipe);
        }

        private ResourceLocation getItemId(ITypedIngredient<?> ingredient) {
            return ingredient.getItemStack()
                    .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()))
                    .filter(recipesByDrop::containsKey)
                    .orElse(null);
        }
    }
}


