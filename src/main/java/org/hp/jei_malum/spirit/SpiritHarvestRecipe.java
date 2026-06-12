package org.hp.jei_malum.spirit;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record SpiritHarvestRecipe(ResourceLocation spiritId, ItemStack spiritStack, List<SpiritDropEntry> entries) {
}
