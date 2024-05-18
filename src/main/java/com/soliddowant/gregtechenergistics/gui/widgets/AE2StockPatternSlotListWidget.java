package com.soliddowant.gregtechenergistics.gui.widgets;

import com.soliddowant.gregtechenergistics.covers.CoverAE2Stocker;
import com.soliddowant.gregtechenergistics.render.Textures;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.IRenderContext;
import gregtech.api.gui.Widget;
import gregtech.api.gui.widgets.*;
import gregtech.api.util.Position;
import gregtech.api.util.Size;
import gregtech.client.utils.TooltipHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
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
            this.addWidget(new ImageWidget(0, 0, 80, 80, Textures.MUI2_PATTERN_CONFIG_BACKGROUND));
            this.addWidget(new ImageWidget(24, 33, 7, 14, Textures.STOCK_CONFIG_ARROW));
            this.addWidget(new ImageWidget(50, 37, 5, 6, Textures.STOCK_ARROW));

            //
            this.circuitSlotWidget = new GhostCircuitSlotWidget(circuitInventory, 0, 6, 31);
            this.circuitSlotWidget.setBackgroundTexture(GuiTextures.SLOT, GuiTextures.INT_CIRCUIT_OVERLAY);
            this.circuitSlotWidget.setConsumer(widget -> widget.setTooltipText("gregtech.gui.pattern_configurator_slot.tooltip", getCircuitSlotTooltip(circuitInventory)));
            this.addWidget(this.circuitSlotWidget);

            //
            this.stockSlotWidget = new FixedPhantomSlotWidget(ghostItemHandler, i, 56, 31);
            this.stockSlotWidget.setBackgroundTexture(GuiTextures.SLOT, Textures.STOCK_OVERLAY).setTooltipText("gregtech.gui.pattern_stock_item.tooltip");
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

        @Override
        @SideOnly(Side.CLIENT)
        public boolean mouseClicked(int mouseX, int mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            boolean result = this.isMouseOverElement(mouseX, mouseY);
            if (!result) {
                this.parentWidget.setConfigVisible(false, this.parentWidget.currentConfigSlotIndex);
            }
            return result;
        }

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
                        writeClientAction(2, buf -> {});
                    } else {
                        ItemStack is = gui.entityPlayer.inventory.getItemStack().copy();
                        is.setCount(1);
                        slotReference.putStack(is);
                        writeClientAction(1, buffer -> {
                            buffer.writeItemStack(slotReference.getStack());
                            int mouseButton = Mouse.getEventButton();
                            boolean shiftDown = TooltipHelper.isShiftDown();
                            buffer.writeVarInt(mouseButton);
                            buffer.writeBoolean(shiftDown);
                        });
                    }
                }
                return false;
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
                    this.setButtonTexture(isIncreaseButton ? Textures.BUTTON_ANGLE_R : Textures.BUTTON_ANGLE_L).setTextScale(0F).setDefaultTooltip().setShouldClientCallback(false);
                }
            }
        }
    }
}
