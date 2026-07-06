package org.hp.jei_malum.reaping;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ReapingDropsRecipe(ResourceLocation dropId, ItemStack dropStack, List<ReapingDropEntry> entries) {
}
