package com.soliddowant.gregtechenergistics.render;

import appeng.core.AppEng;
import codechicken.lib.texture.TextureUtils;
import gregtech.api.gui.resources.TextureArea;
import gregtech.client.renderer.texture.cube.SimpleOverlayRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class Textures {
    public static List<TextureUtils.IIconRegister> iconRegisters = new ArrayList<>();

    public static SimpleOverlayRenderer MACHINE_STATUS_OVERLAY = new ModOverlayRenderer("overlay/machine/overlay_status");
//    public static SimpleOverlayRenderer AE2_STOCKER = new ModOverlayRenderer("overlay/machine/overlay_status");
    public static SimpleOverlayRenderer STOCKER_OVERLAY = new ModOverlayRenderer("overlay/machine/overlay_stocker");

    public static ResourceLocation AE2SpriteMap = new ResourceLocation(AppEng.MOD_ID,"textures/guis/states.png");

    public static final TextureArea STOCK_OVERLAY = TextureArea.fullImage("textures/gui/widget/stock_overlay.png");
    public static final TextureArea STOCK_ARROW = TextureArea.fullImage("textures/gui/widget/arrow_stocker.png");
    public static final TextureArea STOCK_CONFIG = TextureArea.fullImage("textures/gui/widget/int_config_stocker.png");
    public static final TextureArea BUTTON_L = TextureArea.fullImage("textures/gui/widget/button_l.png");
    public static final TextureArea BUTTON_R = TextureArea.fullImage("textures/gui/widget/button_r.png");
    public static final TextureArea BUTTON_CONFIG = TextureArea.fullImage("textures/gui/widget/button_config.png");
    public static final TextureArea BACKGROUND = TextureArea.fullImage("textures/gui/ae2_stocker_gui.png");
    public static final TextureArea BACKGROUND_CONFIG = TextureArea.fullImage("textures/gui/ae2_stocker_config_gui.png");
    public static final TextureArea PATTERN_OVERLAY = TextureArea.fullImage("textures/gui/widget/pattern_overlay.png");
    public static final TextureArea AE2_STOCKER = TextureArea.fullImage("textures/items/metaitems/ae2_stocker.png");
    public static final TextureArea STOCKER_ONLINE = TextureArea.fullImage("textures/gui/widget/ae2_stocker_online.png");
    public static final TextureArea STOCKER_OFFLINE = TextureArea.fullImage("textures/gui/widget/ae2_stocker_offline.png");

    @SideOnly(Side.CLIENT)
    public static void register(TextureMap textureMap) {
        for (TextureUtils.IIconRegister iconRegister : iconRegisters)
            iconRegister.registerIcons(textureMap);
    }

    public static TextureArea getAE2Sprite(int x, int y) {
        assert x <= 16 && x >= 0 && y <= 16 && y >= 0 : "The sprite map coordinates are out of bounds";
        return new TextureArea(AE2SpriteMap, ((float) x / 16.0), ((float) y / 16.0), (1.0 / 16.0), (1.0 / 16.0));
    }
}
