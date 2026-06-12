package org.hp.jei_malum.spirit;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record SpiritDropEntry(
        ResourceLocation entityId,
        EntityType<?> entityType,
        Component entityName,
        List<ItemStack> spawnEggs,
        int count,
        boolean fallback
) {
}
