package org.hp.jei_malum.reaping;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ReapingDropEntry(
        ResourceLocation entityId,
        EntityType<?> entityType,
        Component entityName,
        List<ItemStack> spawnEggs,
        float chance,
        int min,
        int max
) {
}
