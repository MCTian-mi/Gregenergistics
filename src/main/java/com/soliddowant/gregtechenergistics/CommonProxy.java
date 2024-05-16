package com.soliddowant.gregtechenergistics;

import com.soliddowant.gregtechenergistics.covers.CoverBehaviors;
import com.soliddowant.gregtechenergistics.items.GEMetaItems;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod.EventBusSubscriber(modid = GregTechEnergisticsMod.MODID)
public class CommonProxy {
	public void preInit(FMLPreInitializationEvent e) {
		GEMetaItems.preInit();
	}

	public void init(FMLInitializationEvent e) {
		CoverBehaviors.init();
	}

//	@SubscribeEvent
//	public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
//		GEMetaItems.registerRecipes();
//	}
}
