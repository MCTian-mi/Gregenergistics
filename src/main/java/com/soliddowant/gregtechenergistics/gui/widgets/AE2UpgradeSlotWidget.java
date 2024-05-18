package com.soliddowant.gregtechenergistics.gui.widgets;

import appeng.api.config.Upgrades;
import appeng.items.materials.ItemMaterial;
import appeng.parts.automation.UpgradeInventory;
import com.soliddowant.gregtechenergistics.render.Textures;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.widgets.SlotWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

public class AE2UpgradeSlotWidget extends SlotWidget {

    public AE2UpgradeSlotWidget(UpgradeInventory upgradeInventory, int slotIndex, int xPosition, int yPosition) {
        super(upgradeInventory, slotIndex, xPosition, yPosition);
        this.setBackgroundTexture(GuiTextures.SLOT, Textures.AE2_UPGRADE_OVERLAY);
    }
}
