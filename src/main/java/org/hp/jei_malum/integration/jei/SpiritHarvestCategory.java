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
import org.hp.jei_malum.spirit.SpiritDropEntry;
import org.hp.jei_malum.spirit.SpiritHarvestRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SpiritHarvestCategory implements IRecipeCategory<SpiritHarvestRecipe> {
    public static final RecipeType<SpiritHarvestRecipe> RECIPE_TYPE = RecipeType.create(JeiMalumPlugin.UID.getNamespace(), "spirit_harvest", SpiritHarvestRecipe.class);
    private static final int WIDTH = 172;
    private static final int HEIGHT = 136;
    private static final int SPIRIT_X = 8;
    private static final int SPIRIT_Y = 8;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 34;
    private static final int GRID_COLUMNS = 8;
    private static final int GRID_VISIBLE_ROWS = 5;
    private static final String ENTITY_SLOT_PREFIX = "entity_";

    private final IDrawable background;
    private final IDrawable icon;

    public SpiritHarvestCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(iconStack);
    }

    @Override
    public RecipeType<SpiritHarvestRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei_malum.category.spirit_harvest");
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
    public void setRecipe(IRecipeLayoutBuilder builder, SpiritHarvestRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, SPIRIT_X, SPIRIT_Y)
                .setOutputSlotBackground()
                .addItemStack(recipe.spiritStack())
                .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable("jei_malum.spirit_harvest.entity_count", recipe.entries().size()).withStyle(ChatFormatting.GRAY)));

        if (SoulShatterWeaponSlots.hasWeapons()) {
            IRecipeSlotBuilder weaponSlot = builder.addSlot(RecipeIngredientRole.CATALYST, SoulShatterWeaponSlots.WEAPON_X, SoulShatterWeaponSlots.WEAPON_Y);
            SoulShatterWeaponSlots.addWeaponSlot(weaponSlot, "jei_malum.spirit_harvest.soul_shatter_weapon");
        }

        for (int i = 0; i < recipe.entries().size(); i++) {
            SpiritDropEntry entry = recipe.entries().get(i);
            if (entry.spawnEggs().isEmpty()) {
                continue;
            }
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT)
                    .setStandardSlotBackground()
                    .setSlotName(ENTITY_SLOT_PREFIX + i)
                    .addItemStacks(entry.spawnEggs())
                    .addRichTooltipCallback((slotView, tooltip) -> {
                        tooltip.add(Component.translatable("jei_malum.spirit_harvest.count", entry.count()).withStyle(ChatFormatting.GRAY));
                        tooltip.add(Component.literal(entry.entityId().toString()).withStyle(ChatFormatting.DARK_GRAY));
                        if (entry.fallback()) {
                            tooltip.add(Component.translatable("jei_malum.spirit_harvest.default").withStyle(ChatFormatting.DARK_GRAY));
                        }
                    });
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(entry.spawnEggs());
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, SpiritHarvestRecipe recipe, IFocusGroup focuses) {
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
    public void draw(SpiritHarvestRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        guiGraphics.drawString(font, Component.translatable("jei_malum.spirit_harvest.entity_count", recipe.entries().size()), 38, 13, 0xFF404040, false);
        if (recipe.entries().stream().allMatch(entry -> entry.spawnEggs().isEmpty())) {
            guiGraphics.drawWordWrap(font, Component.translatable("jei_malum.spirit_harvest.no_spawn_eggs").withStyle(ChatFormatting.GRAY), GRID_X, GRID_Y + 4, WIDTH - GRID_X * 2, 0xFF707070);
        }
    }

    @Override
    public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip, SpiritHarvestRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
    }
}
