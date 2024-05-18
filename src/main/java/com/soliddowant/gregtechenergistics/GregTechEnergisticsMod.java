package com.soliddowant.gregtechenergistics;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Tags.MODID, name = Tags.MODNAME, version = Tags.VERSION,
		dependencies = "required-after:gregtech;required-after:appliedenergistics2;required-after:ae2fc;after:jei;")
public class GregTechEnergisticsMod {

	public static final String MODID = Tags.MODID;
	public static final boolean IS_AE2FC_LOADED = Loader.isModLoaded("ae2fc");

	@SidedProxy(modId = Tags.MODID, clientSide = "com.soliddowant.gregtechenergistics.ClientProxy",
			serverSide = "com.soliddowant.gregtechenergistics.CommonProxy")
	public static CommonProxy proxy;
	
	@Mod.Instance
    public static GregTechEnergisticsMod instance;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		proxy.preInit(event);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init(event);
	}

	@EventHandler
	public void onPostInit(FMLPostInitializationEvent event) {
		proxy.postInit(event);
	}
}
