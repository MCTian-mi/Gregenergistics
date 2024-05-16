package com.soliddowant.gregtechenergistics.covers;

import com.soliddowant.gregtechenergistics.items.GEMetaItem;

import static gregtech.api.util.GTUtility.gregtechId;
import static gregtech.common.covers.CoverBehaviors.registerBehavior;

public class CoverBehaviors {
	public static void init() {
		registerBehavior(gregtechId("ae2.interface"), GEMetaItem.AE2_STOCKER,
                CoverAE2Stocker::new);
	}
}
