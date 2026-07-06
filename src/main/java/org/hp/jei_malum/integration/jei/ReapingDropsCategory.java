package org.hp.jei_malum.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.hp.jei_malum.reaping.ReapingDropEntry;
import org.hp.jei_malum.reaping.ReapingDropsRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ReapingDropsCategory implements IRecipeCategory<ReapingDropsRecipe> {
    public static final RecipeType<ReapingDropsRecipe> RECIPE_TYPE = RecipeType.create(JeiMalumPlugin.UID.getNamespace(), "reaping_drops", ReapingDropsRecipe.class);
    private static final int WIDTH = 172;
    private static final int HEIGHT = 136;
    private static final int DROP_X = 8;
    private static final int DROP_Y = 8;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 34;
    private static final int GRID_COLUMNS = 8;
    private static final int GRID_VISIBLE_ROWS = 5;
    private static final String ENTITY_SLOT_PREFIX = "reaping_entity_";

    private final IDrawable background;
    private final IDrawable icon;

    public ReapingDropsCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(iconStack);
    }

    @Override
    public RecipeType<ReapingDropsRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei_malum.category.reaping_drops");
    }

    @Override
    public @Nullable IDrawable getBackground() {
        return background;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ReapingDropsRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, DROP_X, DROP_Y)
                .setOutputSlotBackground()
                .addItemStack(recipe.dropStack())
                .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable("jei_malum.reaping_drops.entity_count", recipe.entries().size()).withStyle(ChatFormatting.GRAY)));

        if (SoulShatterWeaponSlots.hasWeapons()) {
            IRecipeSlotBuilder weaponSlot = builder.addSlot(RecipeIngredientRole.CATALYST, SoulShatterWeaponSlots.WEAPON_X, SoulShatterWeaponSlots.WEAPON_Y);
            SoulShatterWeaponSlots.addWeaponSlot(weaponSlot, "jei_malum.reaping_drops.soul_shatter_weapon");
        }

        for (int i = 0; i < recipe.entries().size(); i++) {
            ReapingDropEntry entry = recipe.entries().get(i);
            if (entry.spawnEggs().isEmpty()) {
                continue;
            }
            builder.addSlot(RecipeIngredientRole.INPUT)
                    .setStandardSlotBackground()
                    .setSlotName(ENTITY_SLOT_PREFIX + i)
                    .addItemStacks(entry.spawnEggs())
                    .addRichTooltipCallback((slotView, tooltip) -> {
                        tooltip.add(Component.translatable("jei_malum.reaping_drops.count", entry.min(), entry.max()).withStyle(ChatFormatting.GRAY));
                        tooltip.add(Component.translatable("jei_malum.reaping_drops.chance", formatChance(entry.chance())).withStyle(ChatFormatting.GRAY));
                        tooltip.add(Component.literal(entry.entityId().toString()).withStyle(ChatFormatting.DARK_GRAY));
                    });
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(entry.spawnEggs());
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, ReapingDropsRecipe recipe, IFocusGroup focuses) {
        List<IRecipeSlotDrawable> entitySlots = new ArrayList<>();
        for (int i = 0; i < recipe.entries().size(); i++) {
            builder.getRecipeSlots().findSlotByName(ENTITY_SLOT_PREFIX + i).ifPresent(entitySlots::add);
        }
        if (!entitySlots.isEmpty()) {
            builder.addScrollGridWidget(entitySlots, GRID_COLUMNS, GRID_VISIBLE_ROWS)
                    .setPosition(GRID_X, GRID_Y);
        }
    }

    @Override
    public void draw(ReapingDropsRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, Component.translatable("jei_malum.reaping_drops.entity_count", recipe.entries().size()), 38, 13, 0xFF404040, false);
        if (recipe.entries().stream().allMatch(entry -> entry.spawnEggs().isEmpty())) {
            guiGraphics.drawWordWrap(font, Component.translatable("jei_malum.reaping_drops.no_spawn_eggs").withStyle(ChatFormatting.GRAY), GRID_X, GRID_Y + 4, WIDTH - GRID_X * 2, 0xFF707070);
        }
    }

    @Override
    public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip, ReapingDropsRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
    }

    private static String formatChance(float chance) {
        float percent = chance * 100F;
        if (percent == (int) percent) {
            return Integer.toString((int) percent);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", percent);
    }
}
