package com.soliddowant.gregtechenergistics;

import appeng.api.config.Upgrades;
import com.soliddowant.gregtechenergistics.covers.CoverBehaviors;
import com.soliddowant.gregtechenergistics.items.GEMetaItem;
import com.soliddowant.gregtechenergistics.items.GEMetaItems;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod.EventBusSubscriber(modid = GregTechEnergisticsMod.MODID)
public class CommonProxy {
	public void preInit(FMLPreInitializationEvent e) {
		GEMetaItems.preInit();
	}

	public void init(FMLInitializationEvent e) {
		CoverBehaviors.init();
	}

	public void postInit(FMLPostInitializationEvent event) {
		Upgrades.CRAFTING.registerItem(GEMetaItem.AE2_STOCKER.getStackForm(), 1);
		Upgrades.CAPACITY.registerItem(GEMetaItem.AE2_STOCKER.getStackForm(), 2);
	}

//	@SubscribeEvent
//	public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
//		GEMetaItems.registerRecipes();
//	}
}
