package org.hp.jei_malum.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.hp.jei_malum.Jei_malum;
import org.hp.jei_malum.spirit.MalumSpiritDataCollector;
import org.hp.jei_malum.spirit.SpiritHarvestRecipe;

import java.util.Comparator;
import java.util.List;

@JeiPlugin
public class JeiMalumPlugin implements IModPlugin {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Jei_malum.MODID, "malum_spirit_harvest");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        ItemStack iconStack = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("malum", "sacred_spirit")).getDefaultInstance();
        registration.addRecipeCategories(new SpiritHarvestCategory(registration.getJeiHelpers().getGuiHelper(), iconStack));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<SpiritHarvestRecipe> recipes = MalumSpiritDataCollector.collectRecipes().stream()
                .filter(recipe -> !recipe.spiritStack().isEmpty())
                .sorted(Comparator.comparing(recipe -> recipe.spiritId().toString()))
                .toList();
        Jei_malum.LOGGER.info("Registering {} Malum spirit harvest recipes in JEI", recipes.size());
        registration.addRecipes(SpiritHarvestCategory.RECIPE_TYPE, recipes);
    }
}
