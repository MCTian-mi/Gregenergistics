package com.soliddowant.gregtechenergistics.items;

public final class GEMetaItems {
    public static GEMetaItem META_ITEM;

    private GEMetaItems() {
    }

    public static void preInit() {
        META_ITEM = new GEMetaItem();
        META_ITEM.setRegistryName("gregtechenergistics_meta_item");
    }
}
