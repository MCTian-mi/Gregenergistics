package com.soliddowant.gregtechenergistics.covers;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.Upgrades;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.events.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.fluids.util.AEFluidStack;
import appeng.items.misc.ItemEncodedPattern;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.me.helpers.MachineSource;
import appeng.parts.automation.StackUpgradeInventory;
import appeng.parts.automation.UpgradeInventory;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import appeng.util.item.AEItemStack;
import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.google.common.collect.ImmutableSet;
import com.soliddowant.gregtechenergistics.capability.impl.ItemHandlerListFixed;
import com.soliddowant.gregtechenergistics.gui.widgets.AE2StockPatternSlotListWidget;
import com.soliddowant.gregtechenergistics.helpers.CraftingTracker;
import com.soliddowant.gregtechenergistics.render.GETextures;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IControllable;
import gregtech.api.capability.IGhostSlotConfigurable;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.cover.*;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.common.ConfigHolder;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockPart;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static gregtech.api.capability.GregtechDataCodes.UPDATE_ONLINE_STATUS;

public class CoverAE2Stocker extends CoverBase
        implements CoverWithUI, ITickable, IControllable, IGridHost, IGridProxyable, IActionHost, ICraftingRequester, IAEAppEngInventory {

    protected EntityPlayer placingPlayer;
    public final long maxItemsStocked = 64000;
    protected final IActionSource machineActionSource;
    protected final IItemStorageChannel itemChannel;
    protected final IFluidStorageChannel fluidChannel;
    protected IItemHandler machineItemInputHandler;
    protected IItemHandler machineItemExportHandler;
    protected IFluidHandler machineFluidInputHandler;
    protected IFluidHandler machineFluidExportHandler;
    protected boolean doesOtherAllowsWorking = true;
    protected long stockCount = 64; // AE2 uses 'long's for most stack sizes, so we will too
//    protected IGridNode node;
    protected IMEMonitor<IAEItemStack> attachedAE2ItemInventory;
    protected IMEMonitor<IAEFluidStack> attachedAE2FluidInventory;
    protected boolean shouldInsert = true;

    protected boolean useFluids = false;
    protected List<IAEFluidStack> patternInputFluids;
    protected List<IAEItemStack> patternInputItems;
    protected List<IAEFluidStack> remainingInputFluids;
    protected List<IAEItemStack> remainingInputItems;
    protected List<IAEFluidStack> patternOutputFluids;
    protected List<IAEItemStack> patternOutputItems;
    protected CoverStatus currentStatus;
    protected CraftingTracker craftingTracker;
    protected List<IAEItemStack> missingInputItems;
    protected MultiblockControllerBase controller;


    private boolean hasCircuit;
    private int circuitConfiguration;


//    private final PatternHandler patternHandler = new PatternHandler(27);

    private int meUpdateTick;
    protected boolean isOnline = false;
    private AENetworkProxy aeProxy;

    private final PatternStackHandler patternInventory = new PatternStackHandler(9);
    private final ItemStackHandler phantomStockItemInventory = new ItemStackHandler(9);
    private final NonNullList<GhostCircuitItemStackHandler> ghostCircuitInventory = NonNullList.create();
    private final NonNullList<Integer> upperBounds= NonNullList.withSize(9, 0);
    private final NonNullList<Integer> lowerBounds= NonNullList.withSize(9, 0);
    private final NonNullList<Boolean> hasFullyStocked= NonNullList.withSize(9, false);

    private final UpgradeInventory upgradeInventory;

    protected int currentPatternIndex = 0;

    private AE2StockPatternSlotListWidget slots;

    public CoverAE2Stocker(CoverDefinition definition, CoverableView coverable, EnumFacing attachedSide) {
        super(definition, coverable, attachedSide);
        this.upgradeInventory = new StackUpgradeInventory(this.getPickItem(), this, 4);
        this.machineActionSource = new MachineSource(this);
        this.itemChannel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        this.fluidChannel = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
        this.craftingTracker = new CraftingTracker(this, machineActionSource);
        this.meUpdateTick = 0;

        for (int i = 0; i < 9; i++) {
            ghostCircuitInventory.add(new GhostCircuitItemStackHandler((MetaTileEntity) this.getCoverableView()));
        }

        // In the case of non-multiblocks this only needs to be done once and can be done on instantiation
        registerSingleBlockHandlers();
    }

    public AENetworkProxy getProxy() {
        if (this.aeProxy == null) {
            this.aeProxy = this.createProxy();
        }
        if (!this.aeProxy.isReady() && this.getWorld() != null) {
            this.aeProxy.onReady();
        }
        return this.aeProxy;
    }

    private AENetworkProxy createProxy() {
//        AENetworkProxy proxy = new AENetworkProxy((IGridProxyable) ((MetaTileEntity) getCoverableView()).getHolder(), "cover_proxy", this.getPickItem(), true);
        AENetworkProxy proxy = new AENetworkProxy(this, "cover_proxy", this.getPickItem(), true);
        proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
        proxy.setIdlePowerUsage(ConfigHolder.compat.ae2.meHatchEnergyUsage); // TODO
        proxy.setValidSides(EnumSet.of(this.getAttachedSide()));
        return proxy;
    }

    public void notifySlotChangedAt(int i) {
        this.slots.notifySlotChangedAt(i);
    }

    public void dropPatternAt(int i) {
        Block.spawnAsEntity(this.getWorld(), this.getPos(), this.patternInventory.extractItem(i, 1, false));
        notifySlotChangedAt(i);
    }

    public boolean getShouldBlockSlotAt(int i) {
        return switch (this.upgradeInventory.getInstalledUpgrades(Upgrades.CAPACITY)) {
            case 0 -> i != 4;
            case 1 -> i != 4 && i % 2 == 0;
            default -> false;
        };
    }

    protected boolean hasValidPattern() {
        return hasValidPatternAt(currentPatternIndex);
    }

    protected boolean hasValidPatternAt(int i) {
        ItemStack patternStack = patternInventory.getStackInSlot(i);
        if (!patternStack.isEmpty()) {
            return ((ItemEncodedPattern) patternStack.getItem()).getPatternForItem(patternStack, getWorld()) != null
                    && !phantomStockItemInventory.getStackInSlot(i).isEmpty()
                    && upperBounds.get(i) != 0;
        }
        return false;
    }

    protected boolean hasPatternAt(int i) {
        ItemStack patternStack = patternInventory.getStackInSlot(i);
        return !patternStack.isEmpty();
    }

    protected boolean canWork() {
        return hasValidPattern();
    }

    public void clearDataAt(int i) {
        phantomStockItemInventory.setStackInSlot(i, ItemStack.EMPTY);
        ghostCircuitInventory.get(i).setCircuitValue(GhostCircuitItemStackHandler.NO_CONFIG);
        upperBounds.set(i, 0);
        lowerBounds.set(i, 0);
    }

    public boolean getHasFullyStockedAt(int i) {
        return hasFullyStocked.get(i);
    }

    public void setHasFullyStockedAt(int i, boolean fullyStocked) {
        hasFullyStocked.set(i, fullyStocked);
    }

    public int getUpperBoundAt(int i) {
        return upperBounds.get(i);
    }

    public int getLowerBoundAt(int i) {
        return lowerBounds.get(i);
    }

    public void setUpperBoundAt(int i, int upperBound) {
        upperBounds.set(i, MathHelper.clamp(upperBound, getLowerBoundAt(i), 64000));
        setHasFullyStockedAt(i, false);
    }

    public void setLowerBoundAt(int i, int lowerBound) {
        lowerBounds.set(i, MathHelper.clamp(lowerBound, 0, getUpperBoundAt(i)));
    }

    public void adjustUpperBoundAt(int i, int amount) {
        setUpperBoundAt(i, getUpperBoundAt(i) + amount);
        setHasFullyStockedAt(i, false);
    }

    public void adjustLowerBoundAt(int i, int amount) {
        setLowerBoundAt(i, getLowerBoundAt(i) + amount);
    }

    protected int getCurrentCircuitConfiguration() {
        return ghostCircuitInventory.get(currentPatternIndex).getCircuitValue();
    }


    protected void updateInputs() {
        LinkedList<IAEItemStack> inputItems = new LinkedList<IAEItemStack>();
        LinkedList<IAEFluidStack> inputFluids = new LinkedList<IAEFluidStack>();
        IAEItemStack[] rawInputs = ((ItemEncodedPattern) patternInventory.getStackInSlot(currentPatternIndex).getItem())
                .getPatternForItem(patternInventory.getStackInSlot(currentPatternIndex), getWorld())
                .getCondensedInputs();
        for (IAEItemStack inputStack : rawInputs) {
            if (inputStack.getDefinition().getItem() instanceof ItemFluidDrop) {
                inputFluids.add(AEFluidStack.fromFluidStack(FakeItemRegister.getStack(inputStack.createItemStack())));
            } else {
                inputItems.add(inputStack);
            }
        }
        patternInputItems = inputItems;
        remainingInputItems = copyAEStackList(patternInputItems);
        patternInputFluids = inputFluids;
        remainingInputFluids = copyAEStackList(patternInputFluids);
    }

    protected void updateOutputs() {
        LinkedList<IAEItemStack> outputItems = new LinkedList<IAEItemStack>();
        LinkedList<IAEFluidStack> outputFluids = new LinkedList<IAEFluidStack>();
        IAEItemStack[] rawOutputs = ((ItemEncodedPattern) patternInventory.getStackInSlot(currentPatternIndex).getItem())
                .getPatternForItem(patternInventory.getStackInSlot(currentPatternIndex), getWorld())
                .getCondensedOutputs();
        for (IAEItemStack outputStack : rawOutputs) {
            if (outputStack.getDefinition().getItem() instanceof ItemFluidDrop) {
                outputFluids.add(AEFluidStack.fromFluidStack(FakeItemRegister.getStack(outputStack.createItemStack())));
            } else {
                outputItems.add(outputStack);
            }
        }
        patternOutputItems = outputItems;
        patternOutputFluids = outputFluids;
    }

    protected LinkedList<IAEItemStack> getInputItems() {
        return new LinkedList<>(patternInputItems);
    }

    protected LinkedList<IAEItemStack> getOutputItems() {
        return new LinkedList<>(patternOutputItems);
    }

    protected ItemStack getStockItemStack() {
        return phantomStockItemInventory.getStackInSlot(currentPatternIndex);
    }

    protected int getStockedAmount() {
        int count = 0;
        for (IAEItemStack aeStack : attachedAE2ItemInventory.getStorageList()) {
            if (aeStack.isSameType(getStockItemStack())) {
                count += (int) aeStack.getStackSize();
            }
        }
        return count;
    }

//    protected boolean shouldWork() { // TODO
//        return canWork() && getStockedAmount() < stockCount;
//    }


    protected static boolean checkIfICoveraebleContainsCover(CoverHolder coverHolder) {
        for (EnumFacing side : EnumFacing.VALUES)
            if (coverHolder.getCoverAtSide(side) instanceof CoverAE2Stocker)
                return true;
        return false;
    }

//    protected void patternChangeCallback(boolean patternInserted) {
//        updatePatternCaches();
//
//        if (patternInserted)
//            shouldInsert = true;
//        else
//            craftingTracker.cancelAll();
//
//        getCoverableView().markDirty();
//    }

    @Override
    public boolean canAttach(@NotNull CoverableView coverable, @NotNull EnumFacing side) {
        CoverableView coverHolder = getCoverableView();
        if (isHolderMultiblock()) {
            // Cover holder is a multiblock partis
            // Unfortunately this has to check specifically for a MetaTileEntityMultiblockPart here
            // (rather than IMultiblockPart) because this needs to get the controller from the part.
            MetaTileEntityMultiblockPart castedHolder = (MetaTileEntityMultiblockPart) coverHolder;
            MultiblockControllerBase controller = castedHolder.getController();

            if (controller == null)
                return false;

            if (!controller.isStructureFormed())
                return false;

            for (IMultiblockPart part : controller.getMultiblockParts())
                if (part instanceof CoverHolder)
                    if (checkIfICoveraebleContainsCover((CoverHolder) part))
                        return false;

            // Check to make sure the multiblock has at least one input and export capability
            if (controller.getAbilities(MultiblockAbility.IMPORT_ITEMS).isEmpty() &&
                    controller.getAbilities(MultiblockAbility.IMPORT_FLUIDS).isEmpty())
                return false;

            //noinspection RedundantIfStatement // This is easier to read than the alternative
            if (controller.getAbilities(MultiblockAbility.EXPORT_ITEMS).isEmpty() &&
                    controller.getAbilities(MultiblockAbility.EXPORT_FLUIDS).isEmpty())
                return false;

            return true;
        } else {
            if (checkIfICoveraebleContainsCover((CoverHolder) coverHolder))
                return false;

            return coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, getAttachedSide()) != null ||
                    coverHolder.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, getAttachedSide()) != null;
        }
    }

    public EntityPlayer getPlacingPlayer() {
        return placingPlayer;
    }

    public void setPlacingPlayer(EntityPlayer player) {
        this.placingPlayer = player;
        this.getProxy().setOwner(getPlacingPlayer());
    }

    protected void updateMultiblockInformation() {
        if (!isHolderMultiblock())
            return;

        controller = ((MetaTileEntityMultiblockPart) getCoverableView()).getController();
        if (controller == null)
            return;

        if (controller instanceof RecipeMapMultiblockController)
            registerRecipeMapMultiblockControllerHandlers();
        else
            registerGenericMetaTileEntityMultiblockPartHandlers();
    }

    public boolean isHolderMultiblock() {
        return getCoverableView() instanceof MetaTileEntityMultiblockPart;
    }

    protected void registerSingleBlockHandlers() {
        CoverableView coverHolder = getCoverableView();
        machineItemInputHandler = coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                this.getAttachedSide());
        machineItemExportHandler = machineItemInputHandler;
        machineFluidInputHandler = coverHolder.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
                this.getAttachedSide());
        machineFluidExportHandler = machineFluidInputHandler;
    }

    // Will remove these warnings after ItemHandlerList bug is fixed
    @SuppressWarnings({"CommentedOutCode", "DuplicatedCode"})
    protected void registerRecipeMapMultiblockControllerHandlers() {
        RecipeMapMultiblockController castedController = (RecipeMapMultiblockController) controller;

        // Temporary fix for ItemHandlerList bug
        List<IItemHandlerModifiable> itemInputHandlers = controller.getAbilities(MultiblockAbility.IMPORT_ITEMS);
        if (!itemInputHandlers.isEmpty())
            machineItemInputHandler = new ItemHandlerListFixed(itemInputHandlers);

        List<IItemHandlerModifiable> itemExportHandlers = controller.getAbilities(MultiblockAbility.EXPORT_ITEMS);
        if (!itemExportHandlers.isEmpty())
            machineItemExportHandler = new ItemHandlerListFixed(itemExportHandlers);
//        machineItemInputHandler = castedController.getInputInventory();
//        machineItemExportHandler = castedController.getOutputInventory();

        machineFluidInputHandler = castedController.getInputFluidInventory();
        machineFluidExportHandler = castedController.getOutputFluidInventory();
    }

    // Will remove these warnings after ItemHandlerList bug is fixed
    @SuppressWarnings("DuplicatedCode")
    protected void registerGenericMetaTileEntityMultiblockPartHandlers() {
        List<IItemHandlerModifiable> itemInputHandlers = controller.getAbilities(MultiblockAbility.IMPORT_ITEMS);
        if (!itemInputHandlers.isEmpty())
            machineItemInputHandler = new ItemHandlerListFixed(itemInputHandlers);

        List<IItemHandlerModifiable> itemExportHandlers = controller.getAbilities(MultiblockAbility.EXPORT_ITEMS);
        if (!itemExportHandlers.isEmpty())
            machineItemExportHandler = new ItemHandlerListFixed(itemExportHandlers);

        List<IFluidTank> fluidInputHandlers = controller.getAbilities(MultiblockAbility.IMPORT_FLUIDS);
        if (!fluidInputHandlers.isEmpty())
            machineFluidInputHandler = new FluidTankList(false, fluidInputHandlers);

        List<IFluidTank> fluidExportHandlers = controller.getAbilities(MultiblockAbility.EXPORT_FLUIDS);
        if (!fluidExportHandlers.isEmpty())
            machineFluidExportHandler = new FluidTankList(false, fluidExportHandlers);
    }

    @Override
    public void onRemoval() {
        this.aeProxy.invalidate();
        if (doesOtherAllowsWorking)
            getControllable().setWorkingEnabled(true);
        super.dropInventoryContents(patternInventory);
    }

    public boolean updateMEStatus() {
        if (this.aeProxy != null) {
            this.isOnline = this.aeProxy.isActive()  && this.aeProxy.isPowered();
        } else {
            this.isOnline = false;
        }
        if (!getWorld().isRemote) {
            writeCustomData(UPDATE_ONLINE_STATUS, buf -> buf.writeBoolean(this.isOnline));
        }
        return this.isOnline;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderCover(@NotNull CCRenderState renderState, @NotNull Matrix4 translation,
                            IVertexOperation[] pipeline, @NotNull Cuboid6 plateBox, @NotNull BlockRenderLayer layer) {
        if (this.isOnline) {
            GETextures.STOCKER_COVER_ACTIVE.renderSided(this.getAttachedSide(), plateBox, renderState, pipeline, translation);
        } else {
            GETextures.STOCKER_COVER_INACTIVE.renderSided(this.getAttachedSide(), plateBox, renderState, pipeline, translation);
        }
    }

    @Override
    public @NotNull EnumActionResult onScrewdriverClick(@NotNull EntityPlayer playerIn, @NotNull EnumHand hand,
                                                        @NotNull CuboidRayTraceResult hitResult) {
        if (!getWorld().isRemote) {
            openUI((EntityPlayerMP) playerIn);
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, T defaultValue) {
        if (capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE)
            return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
        return defaultValue;
    }

    protected String getUITitle() {
        return "cover.stocker.title";
    }

    protected ModularUI buildUI(ModularUI.Builder builder, EntityPlayer player) {
        return builder.build(this, player);
    }


    @Override
    public ModularUI createUI(EntityPlayer player) {
        ModularUI.Builder builder = ModularUI
                .builder(GETextures.MUI2_BACKGROUND, 211, 192);

        WidgetGroup labelWithIcon = new WidgetGroup();

        labelWithIcon.addWidget(new ImageWidget(4, 4, 16, 16, GETextures.AE2_STOCKER));
        labelWithIcon.addWidget(new LabelWidget(24, 8, I18n.format("metaitem.ae2_stocker.name")));
        labelWithIcon.addWidget(new SuppliedImageWidget(156, 4, 16, 16, () -> this.isOnline ? (GETextures.STOCKER_ONLINE) : (GETextures.STOCKER_OFFLINE))
                .setTooltip(this.isOnline ? I18n.format("gregtech.gui.me_network.online") :
                        I18n.format("gregtech.gui.me_network.offline")));  // TODO: does this work?

        WidgetGroup upgradeSlots = new WidgetGroup();

        upgradeSlots.addWidget(new AE2UpgradeSlotWidget(this, upgradeInventory, 0, 186, 7));
        upgradeSlots.addWidget(new AE2UpgradeSlotWidget(this, upgradeInventory, 1, 186, 25));
        upgradeSlots.addWidget(new AE2UpgradeSlotWidget(this, upgradeInventory, 2, 186, 43));
        upgradeSlots.addWidget(new AE2UpgradeSlotWidget(this, upgradeInventory, 3, 186, 61));

        this.slots = new AE2StockPatternSlotListWidget(this, patternInventory, 61, 29, ghostCircuitInventory, phantomStockItemInventory);

        builder.bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, 109)
                .widget(labelWithIcon)
                .widget(upgradeSlots)
                .label(8, 99, "container.inventory")
                .widget(this.slots);


        return builder.build(this, player);
    }



//    @Override
//    public ModularUI createUI(EntityPlayer player) {
//        WidgetGroup primaryGroup = new WidgetGroup();
//
//        // Title
//        primaryGroup.addWidget(new LabelWidget(6, 5, getUITitle()));
//
//        // Stock amount
//        long incrementSize = maxItemsStocked / 100;
//        String readableIncrementSize = ReadableNumberConverter.INSTANCE.toWideReadableForm(incrementSize);
//        primaryGroup.addWidget(new ClickButtonWidget(10, 20, 40, 20,
//                "-" + readableIncrementSize,
//                data -> adjustStockCount(data.isShiftClick ? -10 * incrementSize : -incrementSize)));
//        primaryGroup.addWidget(new ClickButtonWidget(126, 20, 40, 20,
//                "+" + readableIncrementSize,
//                data -> adjustStockCount(data.isShiftClick ? 10 * incrementSize : incrementSize)));
//        primaryGroup.addWidget(new ImageWidget(50, 20, 76, 20, GuiTextures.DISPLAY));
//        primaryGroup.addWidget(new SimpleTextWidget(88, 30, "cover.stocker.stock_count",
//                0xFFFFFF, () -> ReadableNumberConverter.INSTANCE.toWideReadableForm(stockCount)));
//
//        // Pattern
//        primaryGroup.addWidget(new LabelWidget(32, 45 + 5, "cover.stocker.pattern.title"));
//        this.patternSlotWidget.initUI(11, 45, primaryGroup::addWidget);
//
//        // Upgrade
//        primaryGroup.addWidget(new LabelWidget(32, 68, "cover.stocker.upgrade.label"));
//        this.upgradeSlotWidget.initUI(11, 63, primaryGroup::addWidget);
//
//        // Fluids
//        primaryGroup.addWidget(new CycleButtonWidget(10, 86, 156, 20,
//                this::shouldUseFluids, this::setUseFluids, "cover.stocker.fluids.disable",
//                "cover.stocker.fluids.enable"));
//
//        // Status
//        primaryGroup.addWidget(new NestedTextWidget(88, 120, "cover.stocker.status",
//                () -> getCurrentStatus().toString()));
//
//        ModularUI.Builder builder = ModularUI.extendedBuilder().widget(primaryGroup)
//                .bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, 216 - 84);
//        return buildUI(builder, player);
//    }

    protected boolean shouldUseFluids() {
        return true;
    }

//    protected void setUseFluids(boolean useFluids) {
//        this.useFluids = useFluids;
//        updatePatternCaches();
//        shouldInsert = true;
//    }

    protected void updatePatternCaches() {
        if (hasValidPattern()) {
            updatePatternCircuit();
            updateInputs();
            updateOutputs();
        }
    }

    protected void updatePatternCircuit() {
        circuitConfiguration = getCurrentCircuitConfiguration();
        hasCircuit = circuitConfiguration != GhostCircuitItemStackHandler.NO_CONFIG;
    }

    @Override
    public boolean isWorkingEnabled() {
        return getCurrentStatus() == CoverStatus.RUNNING;
    }

    @Override
    public void setWorkingEnabled(boolean isActivationAllowed) {
        this.doesOtherAllowsWorking = isActivationAllowed;
    }

    public CoverStatus getCurrentStatus() {
        return currentStatus;
    }

    protected CoverStatus getPartialStatus() {
        // Checks are in order of what's easy to check, then what (IMO) is likely to
        // fail first.
        if (!doesOtherAllowsWorking)
            return CoverStatus.OTHER_DISABLED;
        if (!isPatternAvailable())
            return CoverStatus.PATTERN_NOT_INSERTED;
        if (!isGridConnected())
            return CoverStatus.GRID_DISCONNECTED;
        if (isFullyStocked())
            return CoverStatus.FULLY_STOCKED;
        if (!areAllInputsAvailable())
            return CoverStatus.MISSING_INPUTS;
//        if (isHolderMultiblock() && (controller == null || !controller.isStructureFormed()))
//            return CoverStatus.INVALID_MULTIBLOCK;
        if (!isInputSpaceAvailable())
            return CoverStatus.MISSING_INPUT_SPACE;
        if (!isOutputSpaceAvailable())
            return CoverStatus.MISSING_OUTPUT_SPACE;

        return CoverStatus.RUNNING;
    }

    // This runs through all the checks that are possible, making it potentially
    // much slower than a partial check. This should be used for in-game
    // information, not checking if the machine should be running.
    @SuppressWarnings("unused")
    protected EnumSet<CoverStatus> getFullStatus() {
        EnumSet<CoverStatus> status = EnumSet.noneOf(CoverStatus.class);

        boolean patternAvailable = true; // Some checks rely on a pattern being available
        if (!doesOtherAllowsWorking)
            status.add(CoverStatus.OTHER_DISABLED);
        if (!hasValidPattern()) {
            status.add(CoverStatus.PATTERN_NOT_INSERTED);
            patternAvailable = false;
        }
        if (!isOnline)
            status.add(CoverStatus.GRID_DISCONNECTED);
        else if (patternAvailable) {
            // These checks all rely on a grid connection, as well as a pattern
            if (isFullyStocked())
                status.add(CoverStatus.FULLY_STOCKED);
            if (!areAllInputsAvailable())
                status.add(CoverStatus.MISSING_INPUTS);
            if (!isOutputSpaceAvailable())
                status.add(CoverStatus.MISSING_OUTPUT_SPACE);
        }
        if (patternAvailable && !isInputSpaceAvailable())
            status.add(CoverStatus.MISSING_INPUT_SPACE);

        if (status.isEmpty())
            return EnumSet.of(CoverStatus.RUNNING);

        return status;
    }

    protected boolean isPatternAvailable() {
        return hasValidPattern();
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("HasCircuit", hasCircuit);
        tagCompound.setInteger("CircuitConfiguration", circuitConfiguration);
        tagCompound.setBoolean("OtherAllowsWorking", doesOtherAllowsWorking);
        tagCompound.setLong("StockCount", stockCount);
        tagCompound.setTag("PatternHandler", patternInventory.serializeNBT());
//        tagCompound.setInteger("Status", currentStatus.ordinal());
        tagCompound.setBoolean("ShouldInsert", shouldInsert);
        tagCompound.setBoolean("UseFluids", useFluids);
        tagCompound.setTag("UpgradeInventory", upgradeInventory.serializeNBT());
        tagCompound.setTag("CraftingTag", craftingTracker.serializeNBT());
        tagCompound.setTag("RemainingItems", serializeRemainingInputItems());
        tagCompound.setTag("RemainingFluids", serializeRemainingInputFluids());

        tagCompound.setInteger("MeUpdateTick", meUpdateTick);
        tagCompound.setTag("PatternHandler", patternInventory.serializeNBT());
        tagCompound.setTag("PhantomStock", phantomStockItemInventory.serializeNBT());
        tagCompound.setInteger("CurrentPatternIndex", currentPatternIndex);
        for (int i = 0; i < 9; i++) {
            tagCompound.setByte("GhostCircuit" + i, (byte) ghostCircuitInventory.get(i).getCircuitValue());
            tagCompound.setInteger("UpperBound" + i, upperBounds.get(i));
            tagCompound.setInteger("LowerBound" + i, lowerBounds.get(i));
        }
    }

    protected NBTTagCompound serializeRemainingInputItems() {
        NBTTagCompound remainingItems = new NBTTagCompound();
        if (remainingInputItems != null) {
            int i = 0;
            for (IAEItemStack remainingInputItem : remainingInputItems) {
                NBTTagCompound remainingItem = new NBTTagCompound();
                remainingInputItem.writeToNBT(remainingItem);
                remainingItems.setTag(String.valueOf(i++), remainingItem);
            }
        }

        return remainingItems;
    }

    protected NBTTagCompound serializeRemainingInputFluids() {
        NBTTagCompound remainingFluids = new NBTTagCompound();
        if (remainingInputFluids != null) {
            int i = 0;
            for (IAEFluidStack remainingInputFluid : remainingInputFluids) {
                NBTTagCompound remainingFluid = new NBTTagCompound();
                remainingInputFluid.writeToNBT(remainingFluid);
                remainingFluids.setTag(String.valueOf(i++), remainingFluid);
            }
        }

        return remainingFluids;
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        this.hasCircuit = tagCompound.getBoolean("HasCircuit");
        this.circuitConfiguration = tagCompound.getInteger("CircuitConfiguration");
        this.useFluids = tagCompound.getBoolean("UseFluids");
        this.patternInventory.deserializeNBT(tagCompound.getCompoundTag("PatternHandler"));
        this.doesOtherAllowsWorking = tagCompound.getBoolean("OtherAllowsWorking");
        this.stockCount = tagCompound.getLong("StockCount");
        this.shouldInsert = tagCompound.getBoolean("ShouldInsert");
//        this.currentStatus = CoverStatus.values()[tagCompound.getInteger("Status")];
        this.upgradeInventory.deserializeNBT(tagCompound.getCompoundTag("UpgradeInventory"));
        this.craftingTracker.deserializeNBT(tagCompound.getCompoundTag("CraftingTracker"));

        this.meUpdateTick = tagCompound.getInteger("MeUpdateTick");
        this.patternInventory.deserializeNBT(tagCompound.getCompoundTag("PatternHandler"));
        this.phantomStockItemInventory.deserializeNBT(tagCompound.getCompoundTag("PhantomStock"));
        this.currentPatternIndex = tagCompound.getInteger("CurrentPatternIndex");
        for (int i = 0; i < 9; i++) {
            this.ghostCircuitInventory.get(i).setCircuitValue(tagCompound.getByte("GhostCircuit" + i));
            upperBounds.set(i, tagCompound.getInteger("UpperBound" + i));
            lowerBounds.set(i, tagCompound.getInteger("LowerBound" + i));
        }

        if (tagCompound.hasKey("RemainingItems")) {
            NBTTagCompound remainingItemsTag = tagCompound.getCompoundTag("RemainingItems");
            this.remainingInputItems = tagCompound.getCompoundTag("RemainingItems").getKeySet().stream()
                    .map(key -> AEItemStack.fromNBT(remainingItemsTag.getCompoundTag(key)))
                    .collect(Collectors.toList());
        } else
            this.remainingInputItems = new LinkedList<>();
        if (tagCompound.hasKey("RemainingFluids")) {
            NBTTagCompound remainingFluidsTag = tagCompound.getCompoundTag("RemainingFluids");
            this.remainingInputFluids = remainingFluidsTag.getKeySet().stream()
                    .map(key -> AEFluidStack.fromNBT(remainingFluidsTag.getCompoundTag(key)))
                    .collect(Collectors.toList());
        } else {
            remainingInputFluids = new LinkedList<>();
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        if (this.aeProxy != null) {
            buf.writeBoolean(true);
            NBTTagCompound proxy = new NBTTagCompound();
            this.aeProxy.writeToNBT(proxy);
            buf.writeCompoundTag(proxy);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeInt(this.meUpdateTick);
        buf.writeBoolean(this.isOnline);
    }

    @Override
    public void readInitialSyncData(PacketBuffer buf) {
        super.readInitialSyncData(buf);
        if (buf.readBoolean()) {
            NBTTagCompound nbtTagCompound;
            try {
                nbtTagCompound = buf.readCompoundTag();
            } catch (IOException ignored) {
                nbtTagCompound = null;
            }

            if (this.aeProxy != null && nbtTagCompound != null) {
                this.aeProxy.readFromNBT(nbtTagCompound);
            }
        }
        this.meUpdateTick = buf.readInt();
        this.isOnline = buf.readBoolean();
    }

    @Override
    public void readCustomData(int dataId, PacketBuffer buf) {
        super.readCustomData(dataId, buf);
        if (dataId == UPDATE_ONLINE_STATUS) {
            this.isOnline = buf.readBoolean();
            scheduleRenderUpdate();
        }
    }

    @Override
    public void update() {
        if (!this.getWorld().isRemote) {
            this.meUpdateTick++;
        }

        if (getCoverableView().getWorld() != null) {
            updateMEStatus();
        }

        if (meUpdateTick % ConfigHolder.compat.ae2.updateIntervals == 0) {
            getProxy().getNode().updateState();
            getAttachedAEInverntory();
//            GTLog.logger.info(this.currentStatus.toString());
            if (this.isOnline && (this.currentStatus == CoverStatus.PATTERN_NOT_INSERTED
                    || this.currentStatus == CoverStatus.FULLY_STOCKED
                    || this.currentStatus == CoverStatus.MISSING_INPUTS
                    || this.currentStatus == CoverStatus.MISSING_INPUT_SPACE)) {
                for (int i = 0; i < 9; i++) {
                    this.currentPatternIndex = (this.currentPatternIndex + 1) % 9;
                    if (hasValidPattern()) {
                        break;
                    }
                }
            }
        }

        // Covers cannot currently tell when a neighboring block changes. This is a
        // workaround so that when a new cable is placed the cover can connect to it.
        // This is currently the highest cost operation in the update loop.



        // If the cover holder is/should be a part of a multiblock and the multiblock changed, this will deal with the
        // new/changed handlers.
//        updateMultiblockInformation();

        // Upon checking this we should know several things and don't have to check
        // them:
        // * There is a pattern available
        // * The machine is attached to the grid
        // * The stock count of all pattern output items have not been met
        // * All input items are available for another pattern's worth of crafts
        // * There is at least some space (though may not be all) for another pattern's
        // worth of inputs
        // * There is at least some space (though may not be all) for another pattern's
        // worth of outputs
        // Therefore for only space available for insertion and extraction needs to be
        // checked
        currentStatus = getPartialStatus();
        if (!isWorkingEnabled()) {
            // If the cover holder is/should be a part of a multiblock and the current status is missing input or output
            // space, then it's likely that the multiblock needs to be reformed with new hatches/busses. If working is
            // disabled then the multiblock cannot reform, resulting in the status to always return the initial missing
            // input/output space. While working is enabled, the method is exited afterwords as not all checks have been
            // passed.
            //noinspection RedundantIfStatement // This is easier to read than the alternative
            if (!isHolderMultiblock() ||
                    (currentStatus != CoverStatus.MISSING_INPUT_SPACE && currentStatus != CoverStatus.MISSING_OUTPUT_SPACE))
                setWorkingStatus(false);
            else
                setWorkingStatus(true);

            // Order missing items
            if (currentStatus == CoverStatus.MISSING_INPUTS && upgradeInventory.getInstalledUpgrades(Upgrades.CRAFTING) != 0)
                orderMissingItems();

            return;
        }
        setWorkingStatus(true);

        // Structure not formed, so don't do anything.
        if (isHolderMultiblock() && controller == null)
            return;

        // Do the insert/extract operations.
        if (shouldInsert)
            shouldInsert = !doInsert();

        if (!shouldInsert)
            shouldInsert = doExtract();
    }

    // Returns true if at least one item or fluid was extracted, and there was no missing output space.
    // Extract produced items
    // This will extract all items, not just what's in the pattern.
    // Good for things like electrolyzing clay dust where there are 4 outputs but
    // the pattern only supports 3.
    // This may extract up to one update()'s worth of outputs more than the targeted
    // amount.
    // This is something that could be fixed/changed but IMO it's not worth the
    // extra per tick cost.
    protected boolean doExtract() {
        boolean missingOutputSpace = false;
        boolean hasRemovedSomething = false;
        if (machineItemExportHandler != null)
            for (int slot = 0; slot < (machineItemExportHandler == null ? 0 : machineItemExportHandler.getSlots()); slot++) {
                ItemStack slotStack = machineItemExportHandler.getStackInSlot(slot);
                if (slotStack.isEmpty())
                    continue;

                int availableCount = slotStack.getCount();

                // Test to see how many items can be removed. Some slots (i.e. inputs) might not
                // be removable.
                int amountAvailableToRemove = machineItemExportHandler.extractItem(slot, availableCount, true).getCount();

                if (amountAvailableToRemove == 0)
                    continue;

                hasRemovedSomething = true;

                // Insert into AE2 grid and extract the actual amount removed from the machine
                // inventory
                ItemStack insertionStack = slotStack.copy();
                insertionStack.setCount(amountAvailableToRemove);
                IAEItemStack remainingItemStack = attachedAE2ItemInventory
                        .injectItems(AEItemStack.fromItemStack(slotStack), Actionable.MODULATE, machineActionSource);
                long missingSpace = remainingItemStack == null ? 0 : remainingItemStack.getStackSize();
                int insertedAmount = (int) (availableCount - missingSpace);

                machineItemExportHandler.extractItem(slot, insertedAmount, false);

                if (missingSpace > 0)
                    missingOutputSpace = true;
            }

        if (shouldUseFluids() && machineFluidExportHandler != null)
            for (IFluidTankProperties tankProperties : machineFluidExportHandler.getTankProperties()) {
                if (!tankProperties.canDrain())
                    continue;

                // Test to see how much of what fluid can be removed
                FluidStack availableFluidStack = machineFluidExportHandler.drain(tankProperties.getContents(), false);
                if (availableFluidStack == null)
                    continue;

                int availableFluidAmount = availableFluidStack.amount;
                if (availableFluidAmount == 0)
                    continue;

                hasRemovedSomething = true;

                // Attempt to add the fluid to AE2
                IAEFluidStack remainingItemStack = attachedAE2FluidInventory.injectItems(
                        AEFluidStack.fromFluidStack(availableFluidStack), Actionable.MODULATE, machineActionSource);

                // Calculate how much was actually inserted
                long missingSpace = remainingItemStack == null ? 0 : remainingItemStack.getStackSize();

                // Remove the amount actually inserted
                availableFluidStack.amount = (int) (availableFluidAmount - missingSpace);
                machineFluidExportHandler.drain(availableFluidStack, true);

                if (missingSpace > 0)
                    missingOutputSpace = true;
            }

        // Update the machine state to show that all items have been extracted
        return !missingOutputSpace && hasRemovedSomething;
    }

    // Returns true if all items were inserted.
    // This will track how much of the recipe is actually inserted so that multiple sets
    // of the same recipe aren't inserted when input space runs out.
    protected boolean doInsert() {
        LinkedList<IAEItemStack> newRemainingInputItems = insertItems();
        LinkedList<IAEFluidStack> newRemainingInputFluids = insertFluids();

        if (newRemainingInputItems.isEmpty() && newRemainingInputFluids.isEmpty()) {
            remainingInputItems = copyAEStackList(patternInputItems);
            remainingInputFluids = copyAEStackList(patternInputFluids);
            return true;
        }

        remainingInputItems = newRemainingInputItems;
        remainingInputFluids = newRemainingInputFluids;
        return false;
    }

    protected LinkedList<IAEFluidStack> insertFluids() {
        LinkedList<IAEFluidStack> newRemainingInputFluids = new LinkedList<>();
        for (IAEFluidStack inputFluid : getRemainingInputFluids()) {
            FluidStack insertingStack = inputFluid.getFluidStack();
            final int insertedCount = machineFluidInputHandler.fill(insertingStack, true);

            // Create a stack to extract from AE2 grid that matches only what was inserted.
            IAEFluidStack extractionStack;
            int remainingCount = insertingStack.amount - insertedCount;
            if (remainingCount != 0) {
                // Haven't inserted a full pattern, likely due to missing input space.
                // Save whatever is left over for the next round.
                extractionStack = AEFluidStack.fromFluidStack(insertingStack);
                extractionStack.setStackSize(remainingCount);
                newRemainingInputFluids.add(extractionStack);

                // Can't insert into this slot for whatever reason. No need to try to extract 0 fluid from grid
                if (insertedCount == 0)
                    continue;
            } else {
                // No need to make a new item stack if they're identical
                extractionStack = inputFluid;
            }

            attachedAE2FluidInventory.extractItems(extractionStack, Actionable.MODULATE, machineActionSource);
        }

        return newRemainingInputFluids;
    }

    protected LinkedList<IAEItemStack>  insertItems() {
        LinkedList<IAEItemStack> newRemainingInputItems = new LinkedList<>();
        for (IAEItemStack inputItem : getRemainingInputItems()) {
            IAEItemStack remainingStack = insertItem(inputItem);
            if(remainingStack != null)
                newRemainingInputItems.add(remainingStack);
        }

        return newRemainingInputItems;
    }

    protected IAEItemStack insertItem(IAEItemStack inputItem) {
        ItemStack insertingStack = inputItem.createItemStack();
        for (int slot = 0; slot < (machineItemInputHandler == null ? 0 : machineItemInputHandler.getSlots()); slot++) {
            final ItemStack remainingStack = machineItemInputHandler.insertItem(slot, insertingStack, false);
            final int insertedCount = insertingStack.getCount() - remainingStack.getCount();

            // Can't insert into this slot for whatever reason
            if (insertedCount == 0)
                continue;

            // Extract only what was inserted
            IAEItemStack extractionStack;
            if (insertedCount != inputItem.getStackSize()) {
                extractionStack = AEItemStack.fromItemStack(insertingStack);
                // extractionStack is only null if insertingStack.isEmpty(), which is checked elsewhere
                //noinspection ConstantConditions
                extractionStack.setStackSize(insertedCount);
            } else {
                // No need to make a new item stack if they're identical
                extractionStack = inputItem;
            }
            attachedAE2ItemInventory.extractItems(extractionStack, Actionable.MODULATE, machineActionSource);

            // Update the next inserting stack with whatever is left
            insertingStack = remainingStack;

            if (remainingStack.isEmpty())
                break;
        }

        // insertingStack is now remainingStack. Add the remainder to the remaining items list
        return insertingStack.isEmpty() ? null : AEItemStack.fromItemStack(insertingStack);
    }

    protected void orderMissingItems() {
        IGrid grid = this.getProxy().getNode().getGrid();
        ICraftingGrid craftingGrid = grid.getCache(ICraftingGrid.class);
        for (IAEItemStack missingInputItem : missingInputItems)
            craftingTracker.handleCrafting(missingInputItem, craftingGrid, getCoverableView().getWorld(), grid);
    }

    public void setWorkingStatus(boolean shouldWork) {
        IControllable machine = getControllable();
        if (machine != null) {
            machine.setWorkingEnabled(shouldWork);
            if (hasCircuit && getCoverableView() instanceof IGhostSlotConfigurable configurableMachine && configurableMachine.hasGhostCircuitInventory()) {
                configurableMachine.setGhostCircuitConfig(shouldWork ? getCurrentCircuitConfiguration() : -1);
            }
        }
    }

    @MENetworkEventSubscribe
    public void channelUpdated(final MENetworkChannelChanged c) {
        updateMEStatus();
    }

    @MENetworkEventSubscribe
    public void controllerChanged(final MENetworkControllerChange c) {
        updateMEStatus();
    }

    @MENetworkEventSubscribe
    public void bootingStatusUpdated(final MENetworkBootingStatusChange c) {
        updateMEStatus();
    }

    @MENetworkEventSubscribe
    public void channelsUpdated(final MENetworkChannelsChanged c) {
        updateMEStatus();
    }

    @MENetworkEventSubscribe
    public void powerChanged(final MENetworkPowerStatusChange c) {
        updateMEStatus();
    }

//    protected void updateGridConnectionState() {
//        if (getProxy().isActive())
//            connectToGrid();
//        else
//            disconnectFromGrid();
//    }
//
//    protected void connectToGrid() {
//        IStorageGrid storageGrid = getProxy().getNode().getGrid().getCache(IStorageGrid.class);
//
//        attachedAE2ItemInventory = storageGrid.getInventory(itemChannel);
//        attachedAE2FluidInventory = storageGrid.getInventory(fluidChannel);
//
//        if (attachedAE2ItemInventory == null || attachedAE2FluidInventory == null) {
//            disconnectFromGrid();
//            return;
//        }
//
//        isOnline = true;
//    }

//    protected void disconnectFromGrid() {
//        attachedAE2ItemInventory = null;
//        attachedAE2FluidInventory = null;
//        isOnline = false;
//    }

    public void getAttachedAEInverntory() {
        IStorageGrid storageGrid = getProxy().getNode().getGrid().getCache(IStorageGrid.class);
        attachedAE2ItemInventory = storageGrid.getInventory(itemChannel);
        attachedAE2FluidInventory = storageGrid.getInventory(fluidChannel);
    }



    public boolean isGridConnected() {
        return isOnline;
    }

    @Override
    public IGridNode getGridNode(@Nonnull AEPartLocation dir) {
        return getActionableNode();
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Override
    public void securityBreak() {
        getCoverableView().getWorld().destroyBlock(getCoverableView().getPos(), true);
    }

//    @Override
//    public double getIdlePowerUsage() {
//        return 0; // TODO
//    }
//
//    @Nonnull
//    @Override
//    public EnumSet<GridFlags> getFlags() {
//        return EnumSet.of(GridFlags.REQUIRE_CHANNEL);
//    }
//
//    @Override
//    public boolean isWorldAccessible() {
//        return true;
//    }

    @Nonnull
    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(getCoverableView().getWorld(), getCoverableView().getPos());
    }

//    @Nonnull
//    @Override
//    public AEColor getGridColor() {
//        return AEColor.TRANSPARENT;
//    }
//
//    @Override
//    public void onGridNotification(@Nonnull GridNotification notification) {
//    }
//
//    @Override
//    public void setNetworkStatus(IGrid grid, int channelsInUse) {
//    }
//
//    @Nonnull
//    @Override
//    public EnumSet<EnumFacing> getConnectableSides() {
//        return EnumSet.of(this.getAttachedSide());
//    }
//
//    @Nonnull
//    @Override
//    public IGridHost getMachine() {
//        return this;
//    }

    @Override
    public void gridChanged() {
    }

//    @Nonnull
//    @Override
//    public ItemStack getMachineRepresentation() {
//        if (isHolderMultiblock() && controller != null)
//            return controller.getStackForm();
//
//        return ((MetaTileEntity) getCoverableView()).getStackForm();
//    }

    @Nonnull
    @Override
    public IGridNode getActionableNode() {
        return this.getProxy().getNode();
    }

    /// Check if output items have been fully stocked.
    /// Returns true if there are at least stockCount items in the AE2 grid for each
    /// output.
    protected boolean isFullyStocked() {
        int storagedAmount = getStockedAmount();
        if (getHasFullyStockedAt(currentPatternIndex)) {
            if (storagedAmount <= getLowerBoundAt(currentPatternIndex)) {
                setHasFullyStockedAt(currentPatternIndex, false);
                return false;
            }
            return true;
        } else if (storagedAmount >= getUpperBoundAt(currentPatternIndex)) {
            setHasFullyStockedAt(currentPatternIndex, true);
            return true;
        }
        return false;
    }

    protected <T extends IAEStack<T>> long getStorageCount(T item, IItemList<T> storedItems) {
        T testStack = item.copy();
        testStack.setStackSize(stockCount);
        // This behavior is very similar to a level emitter.
        // The idea is to check for stored items, not extractable items.
        // I _think_ this is also more efficient than attempted a simulated extraction.
        T availableItems = storedItems.findPrecise(testStack);
        return availableItems == null ? 0 : availableItems.getStackSize();
    }

    public LinkedList<Long> getOutputAvailableCounts() {
        IItemList<IAEItemStack> storedItems = attachedAE2ItemInventory.getStorageList();
        IItemList<IAEFluidStack> storedFluids = attachedAE2FluidInventory.getStorageList();

        LinkedList<Long> outputAvailableCounts = new LinkedList<>();

        for (IAEItemStack outputItem : patternOutputItems)
            outputAvailableCounts.add(getStorageCount(outputItem, storedItems));

        if (shouldUseFluids())
            for (IAEFluidStack outputFluid : patternOutputFluids)
                outputAvailableCounts.add(getStorageCount(outputFluid, storedFluids));

        return outputAvailableCounts;
    }

    public long getOutputLeastAvailableCount() {
        return (isGridConnected() && isPatternAvailable()) ?
                getOutputAvailableCounts().stream().reduce((x, y) -> x < y ? x : y).orElse(0L) : 0;
    }

    /// Check if all items in a pattern are available to insert.
    /// If there is not enough for another craft in AE2, stop working
    /// There is a corner case here where there are no new items available but the
    /// machine is still processing.
    /// In this case, the machine will stop after consuming the input. AFAIK there
    /// isn't a way to tell
    /// if the machine is still running from a cover.
    protected boolean areAllInputsAvailable() {
        updatePatternCaches();
        if (upgradeInventory.getInstalledUpgrades(Upgrades.CRAFTING) == 0) {
            for (IAEItemStack inputItem : getRemainingInputItems())
                if (!isItemAvailableForExtraction(inputItem))
                    return false;
        } else {
            missingInputItems = new LinkedList<>();
            for (IAEItemStack inputItem : getRemainingInputItems()) {
                long availableCount = getItemAvailableCount(inputItem);
                long requiredCount = inputItem.getStackSize();

                if (availableCount >= requiredCount)
                    continue;

                IAEItemStack missingItemStack = inputItem.copy();
                missingItemStack.setStackSize(requiredCount - availableCount);
                missingInputItems.add(missingItemStack);
            }

            if (!missingInputItems.isEmpty())
                return false;
        }

        if (shouldUseFluids())
            for (IAEFluidStack inputFluid : getRemainingInputFluids())
                if (!isFluidAvailableForExtraction(inputFluid))
                    return false;

        return true;
    }

    protected boolean isFluidAvailableForExtraction(IAEFluidStack fluid) {
        IAEFluidStack availableFluid = attachedAE2FluidInventory.extractItems(fluid, Actionable.SIMULATE,
                machineActionSource);

        return availableFluid != null && availableFluid.getStackSize() == fluid.getStackSize();
    }

    protected boolean isItemAvailableForExtraction(IAEItemStack items) {
        return getItemAvailableCount(items) == items.getStackSize();
    }

    protected long getItemAvailableCount(IAEItemStack item) {
        IAEItemStack availableItems = attachedAE2ItemInventory.extractItems(item, Actionable.SIMULATE,
                machineActionSource);

        return availableItems == null ? 0 : availableItems.getStackSize();
    }

    /// Check if there is room to insert another batch of inputs
    /// There is a corner case here that's not checked: If there is one empty slot
    /// but two non-compatible item stacks
    /// need to be inserted into it, this will pass despite being a conflict.
    protected boolean isInputSpaceAvailable() {
        // If items need to be inserted but the machine can't handle items, fail
        updatePatternCaches();
        if (!patternInputItems.isEmpty() && machineItemInputHandler == null)
            return false;

        if (isMissingItemInputSpace())
            return false;

        //noinspection RedundantIfStatement
//        if (shouldUseFluids() && isMissingFluidInputSpace())
//            return false;

        return true;
    }

    protected boolean isMissingFluidInputSpace() {
        // If fluids need to be inserted but the machine can't handle fluids, fail
        if (!patternInputFluids.isEmpty() && machineFluidInputHandler == null)
            return true;

        for (IAEFluidStack aeFluidStack : patternInputFluids) {
            FluidStack fluidStack = aeFluidStack.getFluidStack();
            // If a full set of fluid inputs couldn't be inserted, fail
            if (fluidStack.amount != machineFluidInputHandler.fill(fluidStack, false))
                return true;
        }

        return false;
    }

    protected boolean isMissingItemInputSpace() {
        for (IAEItemStack iaeItemStack : patternInputItems) {
            int targetInsertingCount = (int) iaeItemStack.getStackSize();
            int neededSpace = targetInsertingCount;
            for (int slot = 0; slot < machineItemInputHandler.getSlots(); slot++) {
                int missingSpace = machineItemInputHandler.insertItem(slot, iaeItemStack.createItemStack(), true).getCount();
                int spaceAvailableToInsert = targetInsertingCount - missingSpace;
                neededSpace -= spaceAvailableToInsert;

                // No need to keep checking slots if there is enough space in the previously checked slots
                if (neededSpace <= 0)
                    break;
            }

            if (neededSpace > 0)
                return true;
        }

        return false;
    }

    /// Check if there is room to store another batch of outputs
    /// If there is no more room to insert, stop working
    /// There is a corner case here with multiple output stacks the first inserted
    /// stack will fill up all remaining space
    protected boolean isOutputSpaceAvailable() {
        if (isMissingItemOutputSpace())
            return false;

        //noinspection RedundantIfStatement
        if (shouldUseFluids() && isMissingFluidOutputSpace())
            return false;

        return true;
    }

    protected boolean isMissingFluidOutputSpace() {
        for (IAEFluidStack outputFluid : patternOutputFluids) {
            IAEFluidStack remainingFluid = attachedAE2FluidInventory.injectItems(outputFluid, Actionable.SIMULATE,
                    machineActionSource);

            if (remainingFluid != null && remainingFluid.getStackSize() > 0)
                return true;
        }
        return false;
    }

    protected boolean isMissingItemOutputSpace() {
        for (IAEItemStack outputItem : patternOutputItems) {
            IAEItemStack remainingItems = attachedAE2ItemInventory.injectItems(outputItem, Actionable.SIMULATE,
                    machineActionSource);

            if (remainingItems != null && remainingItems.getStackSize() > 0)
                return true;
        }

        return false;
    }

    protected IControllable getControllable() {
        CoverHolder capabilityProvider = controller == null ? (CoverHolder) getCoverableView() : controller;
        return capabilityProvider.getCapability(GregtechTileCapabilities.CAPABILITY_CONTROLLABLE, null);
    }

    public String getHolderName() {
        if (isHolderMultiblock() && controller != null)
            return controller.getMetaFullName();

        return ((MetaTileEntity) this.getCoverableView()).getMetaFullName();
    }

    public IItemHandler getPatternHandler() {
        return this.patternInventory;
    }

    public long getStockCount() {
        return stockCount;
    }

    protected void setStockCount(long itemsStocked) {
        this.stockCount = itemsStocked;
        getCoverableView().markDirty();
    }

    protected List<IAEItemStack> getRemainingInputItems() {
        return remainingInputItems;
    }

    protected List<IAEFluidStack> getRemainingInputFluids() {
        return remainingInputFluids;
    }

    protected <T extends IAEStack<T>> List<T> copyAEStackList(List<T> sourceList) {
        return sourceList == null ?
                null : sourceList.stream().map(T::copy).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return craftingTracker.getRequestedJobs();
    }

    @Override
    public IAEItemStack injectCraftedItems(ICraftingLink link, IAEItemStack items, Actionable mode) {
        return items;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        craftingTracker.jobStateChange(link);
    }

    @Override
    public void saveChanges() {

    }

    @Override
    public void onChangeInventory(IItemHandler iItemHandler, int i, InvOperation invOperation, ItemStack itemStack, ItemStack itemStack1) {

    }

    private static class PatternStackHandler extends ItemStackHandler {

        public PatternStackHandler(int size) {
            super(size);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return 1;
        }
    }

    public static class AE2UpgradeSlotWidget extends SlotWidget {

        private final CoverAE2Stocker parentCover;

        public AE2UpgradeSlotWidget(CoverAE2Stocker parentCover, UpgradeInventory upgradeInventory, int slotIndex, int xPosition, int yPosition) {
            super(upgradeInventory, slotIndex, xPosition, yPosition);
            this.setBackgroundTexture(GuiTextures.SLOT, GETextures.AE2_UPGRADE_OVERLAY);
            this.parentCover = parentCover;
        }
        // TODO: modify transferStackInSlot method in ModularUIContainer

        @Override
        public void onSlotChanged() {
            super.onSlotChanged();
            if (this.slotReference.getHasStack()) {
                return;
            }
            for (int i = 0; i < 9; i++) {
                if (this.parentCover.hasPatternAt(i) && parentCover.getShouldBlockSlotAt(i)) {
                    parentCover.dropPatternAt(i);
                }
            }
        }
    }
}