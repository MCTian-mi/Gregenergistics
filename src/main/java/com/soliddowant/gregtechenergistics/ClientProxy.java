package com.soliddowant.gregtechenergistics;

import codechicken.lib.texture.TextureUtils;
import com.soliddowant.gregtechenergistics.render.GETextures;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	public void init(FMLInitializationEvent e) {
		super.init(e);
	}

	@Override
	public void preInit(FMLPreInitializationEvent e) {
		super.preInit(e);
		TextureUtils.addIconRegister(GETextures::register);
	}
}
