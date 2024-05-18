package com.soliddowant.gregtechenergistics.gui.widgets;

import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.soliddowant.gregtechenergistics.covers.CoverAE2Stocker;
import com.soliddowant.gregtechenergistics.render.GETextures;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.impl.ModularUIGui;
import gregtech.api.gui.resources.IGuiTexture;
import gregtech.api.gui.widgets.*;
import gregtech.api.util.FluidTooltipUtil;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import gregtech.client.utils.RenderUtil;
import gregtech.client.utils.TooltipHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class AE2StockPatternSlotListWidget extends AbstractWidgetGroup {

    protected CoverAE2Stocker parentCover;
    protected NonNullList<AE2StockPatternSlotWidget> childSlots;
    protected NonNullList<PatternConfigWidgetGroup> childConfigs;
    protected int currentConfigSlotIndex = -1;

    public AE2StockPatternSlotListWidget(CoverAE2Stocker parentCover, IItemHandlerModifiable patternHandler,
                                         int x, int y, NonNullList<GhostCircuitItemStackHandler> circuitInventorys, ItemStackHandler ghostItemHandler) {
        super(new Position(x, y), new Size(54, 54));
        this.parentCover = parentCover;

        // Create pattern slot widgets
        this.childSlots = NonNullList.create();
        for (int i = 0; i < 9; i++) {
            AE2StockPatternSlotWidget patternSlotWidget = new AE2StockPatternSlotWidget(this, patternHandler, i, i % 3 * 18, i / 3 * 18);
            this.childSlots.add(patternSlotWidget);
            this.addWidget(patternSlotWidget);
        }

        // Create pattern config widget groups
        this.childConfigs = NonNullList.create();
        for (int i = 0; i < 9; i++) {
            int finalI = i;
            PatternConfigWidgetGroup patternConfigWidgetGroupSlotWidget = new PatternConfigWidgetGroup(i, this, i % 3 * 18, i / 3 * 18,
                    var -> adjustUpperBoundAt(finalI, var), () -> this.getUpperBoundAt(finalI), val -> this.setUpperBoundAt(finalI, val),
                    var -> adjustLowerBoundAt(finalI, var), () -> this.getLowerBoundAt(finalI), val -> this.setLowerBoundAt(finalI, val),
                    circuitInventorys.get(i), ghostItemHandler);
            patternConfigWidgetGroupSlotWidget.setVisible(false);
            this.childConfigs.add(patternConfigWidgetGroupSlotWidget);
            this.addWidget(patternConfigWidgetGroupSlotWidget);

            for (AE2StockPatternSlotWidget childSlot : this.childSlots) {
                childSlot.addLayeredWidget(patternConfigWidgetGroupSlotWidget);
            }
        }
    }

    // Unique methods

    public void notifySlotChangedAt(int i) {
        this.childSlots.get(i).notifySlotChanged();
    }

    public boolean getShouldBlockSlotAt(int i) {
        return this.parentCover.getShouldBlockSlotAt(i);
    }

    public int getCurrentConfigSlotIndex() {
        return this.currentConfigSlotIndex;
    }

    public void setCurrentConfigSlotIndex(int currentConfigSlotIndex) {
        this.currentConfigSlotIndex = currentConfigSlotIndex;
    }

    public int getUpperBoundAt(int i) {
        return this.parentCover.getUpperBoundAt(i);
    }

    public int getLowerBoundAt(int i) {
        return this.parentCover.getLowerBoundAt(i);
    }

    public void setUpperBoundAt(int i, int upperBound) {
        this.parentCover.setUpperBoundAt(i, upperBound);
    }

    public void setLowerBoundAt(int i, int lowerBound) {
        this.parentCover.setLowerBoundAt(i, lowerBound);
    }

    public void adjustUpperBoundAt(int i, int delta) {
        this.parentCover.adjustUpperBoundAt(i, delta);
    }

    public void adjustLowerBoundAt(int i, int delta) {
        this.parentCover.adjustLowerBoundAt(i, delta);
    }

    public void setConfigVisible(boolean visible, int i) {
        if (i == -1) {
            return;
        }
        this.childConfigs.get(i).setVisible(visible);
    }

    public boolean hasPattern(int i) {
        return this.childSlots.get(i).getHasStack();
    }

    public void clearData(int index) {
        this.parentCover.clearDataAt(index);
    }

    @Nonnull
    public ItemStack getPattern(int i) {
        if(!hasPattern(i))
            return ItemStack.EMPTY;
        return this.childSlots.get(i).getStack();
    }

    // Overrides

    private static class PatternConfigWidgetGroup extends AbstractWidgetGroup {

        protected AE2StockPatternSlotListWidget parentWidget;
        protected final GhostCircuitSlotWidget circuitSlotWidget;
        protected final PhantomSlotWidget stockSlotWidget;
        protected final BoundaryConfigWidgetGroup upperBondConfigWidgetGroup;
        protected final BoundaryConfigWidgetGroup lowerBondConfigWidgetGroup;
        protected final int index;

        public PatternConfigWidgetGroup(int i, AE2StockPatternSlotListWidget parentWidget, int x, int y,
                                        IntConsumer upperBoundConsumer, IntSupplier upperBoundSupplier, IntConsumer upperBoundSetter,
                                        IntConsumer lowerBoundConsumer, IntSupplier lowerBoundSupplier, IntConsumer lowerBoundSetter,
                                        GhostCircuitItemStackHandler circuitInventory,
                                        IItemHandlerModifiable ghostItemHandler) {
            super(new Position(x - 31, y - 31), new Size(80, 80));
            this.index = i;
            this.parentWidget = parentWidget;


            //
            this.addWidget(new ImageWidget(0, 0, 80, 80, GETextures.MUI2_PATTERN_CONFIG_BACKGROUND));
            this.addWidget(new ImageWidget(24, 33, 7, 14, GETextures.STOCK_CONFIG_ARROW));
            this.addWidget(new ImageWidget(50, 37, 5, 6, GETextures.STOCK_ARROW));

            //
            this.circuitSlotWidget = new GhostCircuitSlotWidget(circuitInventory, 0, 6, 31);
            this.circuitSlotWidget.setBackgroundTexture(GuiTextures.SLOT, GuiTextures.INT_CIRCUIT_OVERLAY);
            this.circuitSlotWidget.setConsumer(widget -> widget.setTooltipText("gregtech.gui.pattern_configurator_slot.tooltip", getCircuitSlotTooltip(circuitInventory)));
            this.addWidget(this.circuitSlotWidget);

            //
            this.stockSlotWidget = new FixedPhantomSlotWidget(ghostItemHandler, i, 56, 31);
            this.stockSlotWidget.setBackgroundTexture(GuiTextures.SLOT, GETextures.STOCK_OVERLAY).setTooltipText("gregtech.gui.pattern_stock_item.tooltip");
            this.addWidget(this.stockSlotWidget);

            //
            this.upperBondConfigWidgetGroup = new BoundaryConfigWidgetGroup(this, 6, 6, upperBoundConsumer, upperBoundSupplier, upperBoundSetter);
            this.addWidget(this.upperBondConfigWidgetGroup);

            //
            this.lowerBondConfigWidgetGroup = new BoundaryConfigWidgetGroup(this, 6, 56, lowerBoundConsumer, lowerBoundSupplier, lowerBoundSetter);
            this.addWidget(this.lowerBondConfigWidgetGroup);
        }

        protected String getCircuitSlotTooltip(GhostCircuitItemStackHandler circuitInventory) {
            String configString;
            if (circuitInventory == null || circuitInventory.getCircuitValue() == GhostCircuitItemStackHandler.NO_CONFIG) {
                configString = new TextComponentTranslation("gregtech.gui.configurator_slot.no_value").getFormattedText();
            } else {
                configString = String.valueOf(circuitInventory.getCircuitValue());
            }
           return configString;
        }

        @Override
        public void drawInBackground(int mouseX, int mouseY, float partialTicks, IRenderContext context) {
            GlStateManager.translate(0, 0, 200);
            super.drawInBackground(mouseX, mouseY, partialTicks, context);
            GlStateManager.translate(0, 0, -200);
        }

//        @Override
//        @SideOnly(Side.CLIENT)
//        public boolean mouseClicked(int mouseX, int mouseY, int button) {
//            if (super.mouseClicked(mouseX, mouseY, button)) {
//                return true;
//            }
//            boolean result = this.isMouseOverElement(mouseX, mouseY);
//            if (!result) {
//                this.parentWidget.setConfigVisible(false, this.parentWidget.currentConfigSlotIndex);
//            }
//            return result;
//        }

        @Override
        public boolean isMouseOverElement(int mouseX, int mouseY) {
            Position position = getPosition();
            return super.isMouseOverElement(mouseX, mouseY) && !isMouseOver(position.x + 31, position.y + 31, 18, 18, mouseX, mouseY);
        }

        private static class FixedPhantomSlotWidget extends PhantomSlotWidget {

            public FixedPhantomSlotWidget(IItemHandlerModifiable itemHandler, int slotIndex, int xPosition, int yPosition) {
                super(itemHandler, slotIndex, xPosition, yPosition);
            }

            @Override
            public boolean mouseClicked(int mouseX, int mouseY, int button) {
                if (isMouseOverElement(mouseX, mouseY) && gui != null) {
                    if (button == 1 && !slotReference.getStack().isEmpty()) {
                        slotReference.putStack(ItemStack.EMPTY);
                        writeClientAction(2, buf -> {
                        });
                        return true;
                    } else {
                        ItemStack is = gui.entityPlayer.inventory.getItemStack().copy();
                        is.setCount(1);
                        if (is.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
                            IFluidHandlerItem itemTank = Objects.requireNonNull(
                                    is.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null));
                            is = FakeFluids.packFluid2Drops(itemTank.drain(1000, true));
                        }
                        slotReference.putStack(is);
                        writeClientAction(1, buffer -> {
                            buffer.writeItemStack(slotReference.getStack());
                            int mouseButton = Mouse.getEventButton();
                            boolean shiftDown = TooltipHelper.isShiftDown();
                            buffer.writeVarInt(mouseButton);
                            buffer.writeBoolean(shiftDown);
                        });
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void drawInForeground(int mouseX, int mouseY) {
                ItemStack itemStack = slotReference.getStack();
                if (itemStack.getItem() instanceof ItemFluidDrop) {
                    FluidStack fluidStack = FakeItemRegister.getStack(itemStack);
                    if (!gui.isJEIHandled && isMouseOverElement(mouseX, mouseY)) {
                        List<String> tooltips = new ArrayList<>();
                        if (fluidStack != null) {
                            Fluid fluid = fluidStack.getFluid();
                            tooltips.add(fluid.getLocalizedName(fluidStack));

                            // Add various tooltips from the material
                            List<String> formula = FluidTooltipUtil.getFluidTooltip(fluidStack);
                            if (formula != null) {
                                for (String s : formula) {
                                    if (s.isEmpty()) continue;
                                    tooltips.add(s);
                                }
                            }
                            drawHoveringText(ItemStack.EMPTY, tooltips, 300, mouseX, mouseY);
                            GlStateManager.color(1.0f, 1.0f, 1.0f);
                            return;
                        }
                    }
                }
                super.drawInForeground(mouseX, mouseY);
            }

            @Override
            @SideOnly(Side.CLIENT)
            public void drawInBackground(int mouseX, int mouseY, float partialTicks, IRenderContext context) {
                Position pos = getPosition();
                Size size = getSize();
                if (backgroundTexture != null) {
                    for (IGuiTexture backgroundTexture : this.backgroundTexture) {
                        backgroundTexture.draw(pos.x, pos.y, size.width, size.height);
                    }
                }
                ItemStack itemStack = slotReference.getStack();
                ModularUIGui modularUIGui = gui == null ? null : gui.getModularUIGui();
                if (itemStack.isEmpty() && modularUIGui != null && modularUIGui.getDragSplitting() &&
                        modularUIGui.getDragSplittingSlots().contains(slotReference)) { // draw split
                    int splitSize = modularUIGui.getDragSplittingSlots().size();
                    itemStack = gui.entityPlayer.inventory.getItemStack();
                    if (!itemStack.isEmpty() && splitSize > 1 && Container.canAddItemToSlot(slotReference, itemStack, true)) {
                        itemStack = itemStack.copy();
                        Container.computeStackSize(modularUIGui.getDragSplittingSlots(), modularUIGui.dragSplittingLimit,
                                itemStack, slotReference.getStack().isEmpty() ? 0 : slotReference.getStack().getCount());
                        int k = Math.min(itemStack.getMaxStackSize(), slotReference.getItemStackLimit(itemStack));
                        if (itemStack.getCount() > k) {
                            itemStack.setCount(k);
                        }
                    }
                }
                if (!itemStack.isEmpty()) {
                    if (itemStack.getItem() instanceof ItemFluidDrop) {
                        FluidStack fluidStack = FakeItemRegister.getStack(itemStack);
                        if (fluidStack != null) {
                            GlStateManager.disableBlend();
                            RenderUtil.drawFluidForGui(fluidStack, fluidStack.amount, pos.x + 1, pos.y + 1, size.width - 1,
                                    size.height - 1);
                            GlStateManager.enableBlend();
                        }
                    } else {
                        GlStateManager.enableBlend();
                        GlStateManager.enableDepth();
                        GlStateManager.disableRescaleNormal();
                        GlStateManager.disableLighting();
                        RenderHelper.disableStandardItemLighting();
                        RenderHelper.enableStandardItemLighting();
                        RenderHelper.enableGUIStandardItemLighting();
                        GlStateManager.pushMatrix();
                        RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();
                        itemRender.renderItemAndEffectIntoGUI(itemStack, pos.x + 1, pos.y + 1);
                        itemRender.renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRenderer, itemStack, pos.x + 1, pos.y + 1,
                                null);
                        GlStateManager.enableAlpha();
                        GlStateManager.popMatrix();
                        RenderHelper.disableStandardItemLighting();
                    }
                }
                if (isActive()) {
                    if (slotReference instanceof ISlotWidget) {
                        if (isMouseOverElement(mouseX, mouseY)) {
                            GlStateManager.disableDepth();
                            GlStateManager.colorMask(true, true, true, false);
                            drawSolidRect(getPosition().x + 1, getPosition().y + 1, 16, 16, -2130706433);
                            GlStateManager.colorMask(true, true, true, true);
                            GlStateManager.enableDepth();
                            GlStateManager.enableBlend();
                        }
                    }
                } else {
                    GlStateManager.disableDepth();
                    GlStateManager.colorMask(true, true, true, false);
                    drawSolidRect(getPosition().x + 1, getPosition().y + 1, 16, 16, 0xbf000000);
                    GlStateManager.colorMask(true, true, true, true);
                    GlStateManager.enableDepth();
                    GlStateManager.enableBlend();
                }
            }
        }

        private static class BoundaryConfigWidgetGroup extends AbstractWidgetGroup {

            protected Widget parentWidget;
            protected IncrementButtonWidget decreaseButton;
            protected IncrementButtonWidget increaseButton;
            protected TextFieldWidget2 amountTextField;

            public BoundaryConfigWidgetGroup(Widget parentWidget, int x, int y, IntConsumer boundaryConsumer, IntSupplier boundarySupplier, IntConsumer boundarySetter) {
                super(new Position(x, y), new Size(68, 18));
                this.parentWidget = parentWidget;

                //
                this.decreaseButton = new TextlessIncrementButtonWidget(0, 0, 9, 18, false, boundaryConsumer);
                this.addWidget(this.decreaseButton);

                //
                this.increaseButton = new TextlessIncrementButtonWidget(59, 0, 9, 18, true, boundaryConsumer);
                this.addWidget(this.increaseButton);

                //
                this.amountTextField = new TextFieldWidget2(11, 5, 46, 18, () -> Integer.toString(boundarySupplier.getAsInt()), val -> {
                    if (val != null && !val.isEmpty()) {
                        boundarySetter.accept(Integer.parseInt(val));
                    }
                });
                this.amountTextField.setCentered(true).setNumbersOnly(0, 640000).setMaxLength(6);
                this.addWidget(new ImageWidget(10, 0, 48, 18, GuiTextures.DISPLAY));
                this.addWidget(this.amountTextField);
            }

            private static class TextlessIncrementButtonWidget extends IncrementButtonWidget {

                public TextlessIncrementButtonWidget(int x, int y, int width, int height, boolean isIncreaseButton, IntConsumer updater) {
                    super(x, y, width, height, isIncreaseButton ? 1 : -1, isIncreaseButton ? 16 : -16, isIncreaseButton ? 32 : -32, isIncreaseButton ? 64 : -64, updater);
                    this.setButtonTexture(isIncreaseButton ? GETextures.BUTTON_ANGLE_R : GETextures.BUTTON_ANGLE_L).setTextScale(0F).setDefaultTooltip().setShouldClientCallback(false);
                }
            }
        }
    }
}
