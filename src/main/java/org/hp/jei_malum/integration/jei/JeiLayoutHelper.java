package org.hp.jei_malum.integration.jei;

import mezz.jei.common.Internal;
import mezz.jei.common.config.IClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

final class JeiLayoutHelper {
    private static final int SCREEN_MARGIN = 58;
    private static final int CENTER_SEARCH_BAR_SCREEN_MARGIN = 76;
    private static final int RECIPE_AREA_HEADER = 40;
    private static final int RECIPE_BORDER = 8;
    private static final int SLOT_SIZE = 18;

    private JeiLayoutHelper() {
    }

    static int fitRows(int heightWithoutRows, int maxRows) {
        int maxHeight = heightWithoutRows + maxRows * SLOT_SIZE;
        int minHeight = heightWithoutRows + SLOT_SIZE;
        return (Mth.clamp(maxRecipeHeight(), minHeight, maxHeight) - heightWithoutRows) / SLOT_SIZE;
    }

    private static int maxRecipeHeight() {
        IClientConfig config = Internal.getJeiClientConfigs().getClientConfig();
        int screenMargin = config.isCenterSearchBarEnabled() ? CENTER_SEARCH_BAR_SCREEN_MARGIN : SCREEN_MARGIN;
        int guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight() - screenMargin;
        guiHeight = Math.min(Math.max(guiHeight, IClientConfig.minRecipeGuiHeight), config.getMaxRecipeGuiHeight());
        return guiHeight - RECIPE_AREA_HEADER - RECIPE_BORDER;
    }
}
