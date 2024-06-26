package com.soliddowant.gregtechenergistics.gui.widgets;

import appeng.items.misc.ItemEncodedPattern;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.soliddowant.gregtechenergistics.render.GETextures;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.impl.ModularUIGui;
import gregtech.api.gui.resources.IGuiTexture;
import gregtech.api.gui.widgets.AbstractWidgetGroup;
import gregtech.api.gui.widgets.BlockableSlotWidget;
import gregtech.api.gui.widgets.ClickButtonWidget;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import gregtech.api.util.TextFormattingUtil;
import gregtech.client.utils.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class AE2StockPatternSlotWidget extends AbstractWidgetGroup {

    protected AE2StockPatternSlotListWidget parentWidget;
    protected ClickButtonWidget configButton;
    protected PatternSlotWidget patternSlotWidget;
    protected int index;
    protected boolean configVisible = false;

    public AE2StockPatternSlotWidget(AE2StockPatternSlotListWidget parentWidget, IItemHandlerModifiable itemHandler,
                                     int slotIndex, int xPosition, int yPosition) {
        super(new Position(xPosition, yPosition), new Size(18, 18));
        this.parentWidget = parentWidget;
        this.index = slotIndex;

        // Create the pattern slot widget
        this.patternSlotWidget = new PatternSlotWidget(slotIndex, this, itemHandler, slotIndex, 0, 0, true, true); // TODO
        this.patternSlotWidget.setBackgroundTexture(GuiTextures.SLOT, GETextures.PATTERN_OVERLAY);
        this.addWidget(this.patternSlotWidget);

        // Create the config button
        this.configButton = new CustomClickButtonWidget(12, 0, 6, 6, "", clickData -> {
            this.toggleConfigVisible();
        }).setButtonTexture(GETextures.BUTTON_OPEN_PATTERN_CONFIG).setShouldClientCallback(true);
        this.configButton.setVisible(false);
        this.addWidget(this.configButton);

        //
        this.patternSlotWidget.addLayeredWidget(this.configButton);
    }

    // Unique methods

    public void notifySlotChanged() {
        this.patternSlotWidget.onSlotChanged();
    }

    public boolean getHasStack() {
        return this.patternSlotWidget.getHandle().getHasStack();
    }

    public ItemStack getStack() {
        return this.patternSlotWidget.getHandle().getStack();
    }

    public void toggleConfigVisible() {
        int openedConfigIndex = parentWidget.getCurrentConfigSlotIndex();
        if (openedConfigIndex == this.index) {
            parentWidget.setCurrentConfigSlotIndex(-1);
            parentWidget.setConfigVisible(false, this.index);
        } else {
            parentWidget.setCurrentConfigSlotIndex(this.index);
            parentWidget.setConfigVisible(true, this.index);
            parentWidget.setConfigVisible(false, openedConfigIndex); // TODO
        }
    }

    public boolean getShouldBlockSlot() {
        return this.parentWidget.getShouldBlockSlotAt(this.index);
    }

    public boolean hasPattern() {
        return this.patternSlotWidget.getHandle().getHasStack();
    }

    public void clearData() {
        this.parentWidget.clearData(this.index);
    }

    @Nonnull
    public ItemStack getPattern() {
        if (!hasPattern())
            return ItemStack.EMPTY;
        return this.patternSlotWidget.getHandle().getStack();
    }

    public void addLayeredWidget(Widget widget) {
        this.patternSlotWidget.addLayeredWidget(widget);
    }

    // Overrides

//    @Override
//    public boolean mouseClicked(int mouseX, int mouseY, int button) {
//        if (this.configVisible) {
//            if (this.patternConfigWidgetGroup.mouseClicked(mouseX, mouseY, button)) {
//                return true;
//            } else {
//                this.configVisible = false;
//                return true;
//            }
//        } else if (this.configButton.mouseClicked(mouseX, mouseY, button)) {
//            this.configVisible = true;
//            return true;
//        }
//        return super.mouseClicked(mouseX, mouseY, button);
//    }

    private class PatternSlotWidget extends BlockableSlotWidget {

        protected AE2StockPatternSlotWidget parentWidget;
        protected int index;

        protected NonNullList<Widget> layeredWidgets = NonNullList.create();

        public PatternSlotWidget(int i, AE2StockPatternSlotWidget parentWidget, IItemHandlerModifiable itemHandler,
                                 int slotIndex, int xPosition, int yPosition, boolean canTakeItems, boolean canPutItems) {
            super(itemHandler, slotIndex, xPosition, yPosition, canTakeItems, canPutItems);
            this.index = i;
            this.parentWidget = parentWidget;
            this.setBackgroundTexture(GuiTextures.SLOT, GETextures.PATTERN_OVERLAY);
            this.setIsBlocked(this::shouldBlockSlot);
        }

        public boolean shouldBlockSlot() {
            return this.parentWidget.getShouldBlockSlot();
        }

        public void addLayeredWidget(Widget widget) {
            this.layeredWidgets.add(widget);
        }

        @Override
        public boolean canPutStack(ItemStack stack) {
            return super.canPutStack(stack) && stack.getItem() instanceof ItemEncodedPattern;
        }

        @Override
        public boolean canMergeSlot(ItemStack stack) {
            return false;
        }

        @Override
        public void onSlotChanged() {
            ItemStack pattern = getPattern();
            if (pattern.isEmpty()) {
                clearData();
                parentWidget.configButton.setVisible(false);
                parentWidget.parentWidget.setConfigVisible(false, this.index);
            } else {
                parentWidget.configButton.setVisible(true);
            }
        }

        @Override
        public boolean isMouseOverElement(int mouseX, int mouseY) {
            return super.isMouseOverElement(mouseX, mouseY) && this.layeredWidgets.stream().noneMatch(widget -> widget.isVisible() && widget.isMouseOverElement(mouseX, mouseY));
        }


        @Override
        @SideOnly(Side.CLIENT)
        public void drawInBackground(int mouseX, int mouseY, float partialTicks, IRenderContext context) {
            Position pos = getPosition();
            Size size = getSize();
            for (IGuiTexture backgroundTexture : this.backgroundTexture) {
                backgroundTexture.draw(pos.x, pos.y, size.width, size.height);
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
                final ItemEncodedPattern pattern = (ItemEncodedPattern) itemStack.getItem();
                final ItemStack out = pattern.getOutput(itemStack);

                if (out.getItem() instanceof ItemFluidDrop) {
                    FluidStack fluidStack = FakeItemRegister.getStack(out);
                    if (fluidStack != null) {
                        GlStateManager.disableBlend();
                        RenderUtil.drawFluidForGui(fluidStack, fluidStack.amount, pos.x + 1, pos.y + 1, size.width - 1,
                                size.height - 1);
                        GlStateManager.pushMatrix();
                        GlStateManager.scale(0.5, 0.5, 1);
                        String s = TextFormattingUtil.formatLongToCompactString(fluidStack.amount, 4) + "L";
                        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
                        fontRenderer.drawStringWithShadow(s,
                                (pos.x + (size.width / 3F)) * 2 - fontRenderer.getStringWidth(s) + 21,
                                (pos.y + (size.height / 3F) + 6) * 2, 0xFFFFFF);
                        GlStateManager.popMatrix();
                        GlStateManager.enableBlend();
                    }
                } else {
                    final ItemStack renderStack = out.isEmpty() ? itemStack : out;
//                    renderStack.setCount(1);
                    GlStateManager.enableBlend();
                    GlStateManager.enableDepth();
                    GlStateManager.disableRescaleNormal();
                    GlStateManager.disableLighting();
                    RenderHelper.disableStandardItemLighting();
                    RenderHelper.enableStandardItemLighting();
                    RenderHelper.enableGUIStandardItemLighting();
                    GlStateManager.pushMatrix();
                    RenderItem itemRender = Minecraft.getMinecraft().getRenderItem();
                    itemRender.renderItemAndEffectIntoGUI(renderStack, pos.x + 1, pos.y + 1);
                    itemRender.renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRenderer, renderStack, pos.x + 1, pos.y + 1,
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
            if (shouldBlockSlot()) {
                GlStateManager.disableDepth();
                GlStateManager.colorMask(true, true, true, false);
                drawSolidRect(pos.getX(), pos.getY(), size.getWidth(), size.getHeight(), 0x8FA3ABC6);
                GlStateManager.colorMask(true, true, true, true);
                GlStateManager.enableDepth();
                GlStateManager.enableBlend();
            }
        }
    }

    public static class CustomClickButtonWidget extends ClickButtonWidget {

        public CustomClickButtonWidget(int xPosition, int yPosition, int width, int height, String displayText, Consumer<ClickData> onPressed) {
            super(xPosition, yPosition, width, height, displayText, onPressed);
        }

        @Override
        public void drawInBackground(int mouseX, int mouseY, float partialTicks, IRenderContext context) {
            GlStateManager.translate(0, 0, 190);
            super.drawInBackground(mouseX, mouseY, partialTicks, context);
            GlStateManager.translate(0, 0, -190);
        }
    }
}

//    private class PatternConfigWidgetGroup extends AbstractWidgetGroup {
//
//        protected AE2StockPatternSlotWidget parentWidget;
//        protected final GhostCircuitSlotWidget circuitSlotWidget;
//        protected final PhantomSlotWidget stockSlotWidget;
//        protected final BoundaryConfigWidgetGroup upperBondConfigWidgetGroup;
//        protected final BoundaryConfigWidgetGroup lowerBondConfigWidgetGroup;
//        protected final int index;
//
//        public PatternConfigWidgetGroup(int i, AE2StockPatternSlotWidget parentWidget, int x, int y,
//                                        IntConsumer upperBoundConsumer, IntSupplier upperBoundSupplier,
//                                        IntConsumer lowerBoundConsumer, IntSupplier lowerBoundSupplier,
//                                        GhostCircuitItemStackHandler circuitInventory,
//                                        IItemHandlerModifiable ghostItemHandler) {
//            super(new Position(7 - x, 24 - y), new Size(162, 18));
//            this.index = i;
//            this.parentWidget = parentWidget;
//
//            //
//            this.circuitSlotWidget = new GhostCircuitSlotWidget(circuitInventory, 0, 72, 0);
//            this.circuitSlotWidget.setBackgroundTexture(GuiTextures.SLOT, GuiTextures.INT_CIRCUIT_OVERLAY);
//            this.circuitSlotWidget.setConsumer(widget -> widget.setTooltipText("1")); // TODO: Add tooltip
//            this.addWidget(this.circuitSlotWidget);
//
//            //
//            this.stockSlotWidget = new PhantomSlotWidget(ghostItemHandler, i, x + 24, y);
//            this.stockSlotWidget.setBackgroundTexture(GuiTextures.SLOT, Textures.STOCK_OVERLAY);
//            this.addWidget(this.stockSlotWidget);
//
//            //
//            this.upperBondConfigWidgetGroup = new BoundaryConfigWidgetGroup(this, 90, 0, upperBoundConsumer, upperBoundSupplier);
//            this.addWidget(this.upperBondConfigWidgetGroup);
//
//            //
//            this.lowerBondConfigWidgetGroup = new BoundaryConfigWidgetGroup(this, 0, 0, lowerBoundConsumer, lowerBoundSupplier);
//            this.addWidget(this.lowerBondConfigWidgetGroup);
//        }
//
//        @Override
//        public void setVisible(boolean visible) {
//            if (this.isVisible() != visible) {
//                super.setVisible(visible);
//                for (Widget widget : this.widgets) {
//                    widget.setVisible(visible);
//                }
//            }
//        }
//
//        private class BoundaryConfigWidgetGroup extends AbstractWidgetGroup {
//
//            protected Widget parentWidget;
//            protected IncrementButtonWidget decreaseButton;
//            protected IncrementButtonWidget increaseButton;
//            protected TextFieldWidget2 amountTextField;
//
//
//            public BoundaryConfigWidgetGroup(Widget parentWidget, int x, int y, IntConsumer boundaryConsumer, IntSupplier boundarySupplier) {
//                super(new Position(x, y), new Size(56, 18));
//                this.parentWidget = parentWidget;
//
//                //
//                this.decreaseButton = new IncrementButtonWidget(1, 1, 10, 16, -1, -16, -32, -64, boundaryConsumer);
//                this.decreaseButton.setDefaultTooltip().setShouldClientCallback(false);
//                this.addWidget(this.decreaseButton);
//
//                //
//                this.increaseButton = new IncrementButtonWidget(61, 1, 10, 16, 1, 16, 32, 64, boundaryConsumer);
//                this.increaseButton.setDefaultTooltip().setShouldClientCallback(false);
//                this.addWidget(this.increaseButton);
//
//                //
//                this.amountTextField = new TextFieldWidget2(12, 6, 48, 16, () -> Integer.toString(boundarySupplier.getAsInt()), val -> {
//                    if (val != null && !val.isEmpty()) {
//                        boundaryConsumer.accept(Integer.parseInt(val));
//                    }
//                });
//                this.amountTextField.setCentered(true).setNumbersOnly(0, 640000).setMaxLength(6);
//                this.addWidget(new ImageWidget(12, 1, 48, 16, GuiTextures.DISPLAY));
//                this.addWidget(this.amountTextField);
//            }
//
//            @Override
//            public void setVisible(boolean visible) {
//                if (this.isVisible() != visible) {
//                    super.setVisible(visible);
//                    for (Widget widget : this.widgets) {
//                        widget.setVisible(visible);
//                    }
//                }
//            }
//        }
//    }
//}
