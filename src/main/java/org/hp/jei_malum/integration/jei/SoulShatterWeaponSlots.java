package org.hp.jei_malum.integration.jei;

import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class SoulShatterWeaponSlots {
    static final int WEAPON_X = 144;
    static final int WEAPON_Y = 8;
    private static final TagKey<Item> SOUL_SHATTER_WEAPONS = TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath("malum", "soul_shatter_capable_weapon"));

    private SoulShatterWeaponSlots() {
    }

    static void addWeaponSlot(IRecipeSlotBuilder slot, String tooltipKey) {
        slot.setStandardSlotBackground()
                .addItemStacks(getSoulShatterWeapons())
                .addRichTooltipCallback((slotView, tooltip) -> {
                    var tagComponents = tooltip.getLines().stream()
                            .filter(line -> line.right().isPresent())
                            .toList();
                    tooltip.clear();
                    slotView.getDisplayedItemStack()
                            .map(ItemStack::getHoverName)
                            .ifPresent(tooltip::add);
                    tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
                    tooltip.add(Component.translatable("jei_malum.soul_shatter_weapon.accepted_tag").withStyle(ChatFormatting.GRAY));
                    tooltip.add(Component.literal("#malum:soul_shatter_capable_weapon").withStyle(ChatFormatting.GRAY));
                    tooltip.getLines().addAll(tagComponents);
                });
    }

    static boolean hasWeapons() {
        return !getSoulShatterWeapons().isEmpty();
    }

    private static List<ItemStack> getSoulShatterWeapons() {
        return BuiltInRegistries.ITEM.getTag(SOUL_SHATTER_WEAPONS)
                .stream()
                .flatMap(tag -> tag.stream()
                        .map(holder -> holder.value().getDefaultInstance())
                        .filter(stack -> !stack.isEmpty()))
                .toList();
    }
}
