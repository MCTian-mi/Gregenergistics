package com.soliddowant.gregtechenergistics.items;

import gregtech.api.items.metaitem.MetaItem;

public class GEMetaItem extends MetaItem<GEMetaItem.GEMetaValueItem> {

    public static MetaItem<?>.MetaValueItem AE2_STOCKER;

    public GEMetaItem() {
        super((short) 0);
    }

    @Override
    protected GEMetaValueItem constructMetaValueItem(short metaValue, String unlocalizedName) {
        return new GEMetaValueItem(metaValue, unlocalizedName);
    }

    @Override
    public void registerSubItems() {
        AE2_STOCKER = addItem(8, "ae2_stocker");
    }

//    @Override
//    public IPart createPartFromItemStack(ItemStack is) {
//        T metaValueItem = getItem(is);
//        if(metaValueItem == null)
//            return null;
//
//        IPartProvider partProvider = metaValueItem.getPartProvider();
//        if(partProvider == null)
//            return null;
//
//        return partProvider.getPart(is);
//    }
//
//    @Override
//    @SideOnly(Side.CLIENT)
//    public void registerModels() {
//        for (short itemMetaKey : metaItems.keySet()) {
//            T metaValueItem = metaItems.get(itemMetaKey);
//
//            IModelProvider itemModelProvider = metaValueItem.getModelProvider();
//            if(itemModelProvider != null) {
//                ModelResourceLocation resourceLocation = itemModelProvider.getModel();
//                ModelBakery.registerItemVariants(this, resourceLocation);
//                metaItemsModels.put((short) (metaItemOffset + itemMetaKey), resourceLocation);
//            } else {
//                ResourceLocation resourceLocation = new ResourceLocation(GregTechEnergisticsMod.MODID, formatModelPath(metaValueItem));
//                ModelBakery.registerItemVariants(this, resourceLocation);
//                metaItemsModels.put((short) (metaItemOffset + itemMetaKey),
//                    new ModelResourceLocation(resourceLocation, "inventory"));
//            }
//        }
//
//        ModelLoader.setCustomMeshDefinition(this, itemStack -> {
//            short itemDamage = formatRawItemDamage((short) itemStack.getItemDamage());
//
//            if (specialItemsModels.containsKey(itemDamage))
//                return specialItemsModels.get(itemDamage)[getModelIndex(itemStack)];
//
//            if (metaItemsModels.containsKey(itemDamage))
//                return metaItemsModels.get(itemDamage);
//
//            return MISSING_LOCATION;
//        });
//    }

    public class GEMetaValueItem extends MetaItem<?>.MetaValueItem {

//        protected IPartProvider partProvider;
//        protected IModelProvider modelProvider;

        protected GEMetaValueItem(int metaValue, String unlocalizedName) {
            super(metaValue, unlocalizedName);
        }

//        @SuppressWarnings("deprecation")
//        @Override
//        protected void addItemComponentsInternal(IItemComponent... stats) {
//            super.addItemComponentsInternal(stats);
//            for (IItemComponent stat : stats) {
//                if (stat instanceof CoverMachineStatus) {
//                    try {
//                        Class clazz = Class.forName("gregtech.api.items.metaitem.MetaItem$MetaValueItem");
//                        Field field = clazz.getDeclaredField("useManager");
//                        field.setAccessible(true);
//                        field.set(this, new GTFOFoodUseManager((GTFOFoodStats) stat));
//                    } catch (Exception e) {
//                        GTFOLog.logger.error("Failed to add GTFOFoodStats to GTFOMetaValueItem", e);
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//        @Nullable
//        public IPartProvider getPartProvider() {
//            return partProvider;
//        }
//
//        @Nullable
//        public IModelProvider getModelProvider() {
//            return modelProvider;
//        }
    }
}
