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

    public static ResourceLocation AE2SpriteMap = new ResourceLocation(AppEng.MOD_ID,"textures/guis/states.png");

    public static final TextureArea MUI2_BACKGROUND = TextureArea.fullImage("textures/gui/base/background_mui2.png");
    public static final TextureArea MUI2_PATTERN_CONFIG_BACKGROUND = TextureArea.fullImage("textures/gui/base/background_mui2_pattern_config.png");
    public static final TextureArea BUTTON_ANGLE_L = TextureArea.fullImage("textures/gui/widget/button_angle_l.png");
    public static final TextureArea BUTTON_ANGLE_R = TextureArea.fullImage("textures/gui/widget/button_angle_r.png");
    public static final TextureArea BUTTON_OPEN_PATTERN_CONFIG = TextureArea.fullImage("textures/gui/widget/button_open_pattern_config.png");
    public static final TextureArea PATTERN_OVERLAY = TextureArea.fullImage("textures/gui/overlay/pattern_overlay.png");
    public static final TextureArea STOCK_OVERLAY = TextureArea.fullImage("textures/gui/overlay/stock_overlay.png");
    public static final TextureArea AE2_UPGRADE_OVERLAY = TextureArea.fullImage("textures/gui/overlay/ae2_upgrade_overlay.png");
    public static final TextureArea STOCK_ARROW = TextureArea.fullImage("textures/gui/arrows/arrow_stocker.png");
    public static final TextureArea STOCK_CONFIG_ARROW = TextureArea.fullImage("textures/gui/arrows/int_config_stocker.png");
    public static final TextureArea AE2_STOCKER = TextureArea.fullImage("textures/items/metaitems/ae2_stocker.png");
    public static final TextureArea STOCKER_ONLINE = TextureArea.fullImage("textures/gui/widget/stocker_online.png");
    public static final TextureArea STOCKER_OFFLINE = TextureArea.fullImage("textures/gui/widget/stocker_offline.png");
    public static final SimpleOverlayRenderer STOCKER_COVER_ACTIVE = new SimpleOverlayRenderer("overlay/appeng/cover_ae2_stocker_active");
    public static final SimpleOverlayRenderer STOCKER_COVER_INACTIVE = new SimpleOverlayRenderer("overlay/appeng/cover_ae2_stocker_inactive");

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
