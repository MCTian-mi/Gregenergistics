package com.soliddowant.gregtechenergistics.gui.widgets;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.items.misc.ItemEncodedPattern;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.LinkedList;

public class PatternHandler extends ItemStackHandler {

    private NonNullList<PatternInfo> patternInfos;

    public PatternHandler(int size) {
        super(size);
        this.patternInfos = NonNullList.withSize(size, new PatternInfo());
    }

    @Override
    public void setSize(int size) {
        super.setSize(size);
        this.patternInfos = NonNullList.withSize(size, new PatternInfo());
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        validateSlotIndex(slot);
        this.stacks.set(slot, stack);
        this.patternInfos.set(slot, new PatternInfo(stack));
        onContentsChanged(slot);
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        boolean hasInsertItem = !stack.isEmpty();
        ItemStack stackOut = super.insertItem(slot, stack, simulate);
        if (!simulate && hasInsertItem && stackOut.isEmpty()) {
            this.patternInfos.set(slot, new PatternInfo(stack));
        }
        return stackOut;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        boolean hasItemInSlot = !getStackInSlot(slot).isEmpty();
        ItemStack stackOut = super.extractItem(slot, amount, simulate);
        if (!simulate && hasItemInSlot && !stackOut.isEmpty()) {
            this.patternInfos.set(slot, new PatternInfo());
        }
        return stackOut;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
        return 1;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = super.serializeNBT();
        NBTTagList patternInfosTagList = new NBTTagList();
        for (PatternInfo patternInfo : patternInfos) {
            patternInfosTagList.appendTag(patternInfo.serializeNBT());
        }
        nbt.setTag("PatternInfos", patternInfosTagList);
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        NBTTagList patternInfosTagList = nbt.getTagList("PatternInfos", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < patternInfosTagList.tagCount(); i++) {
            patternInfos.set(i, new PatternInfo(patternInfosTagList.getCompoundTagAt(i)));
        }
        super.deserializeNBT(nbt);
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        this.patternInfos.set(slot, new PatternInfo(getStackInSlot(slot)));
    }

    public PatternInfo getPatternInfoInSlot(int slot) {
        validateSlotIndex(slot);
        return this.patternInfos.get(slot);
    }

    public int getCircuitValueInSlot(int slot) {
        return getPatternInfoInSlot(slot).getCircuitValue();
    }

    public long getUpperBoundInSlot(int slot) {
        return getPatternInfoInSlot(slot).getUpperBound();
    }

    public long getLowerBoundInSlot(int slot) {
        return getPatternInfoInSlot(slot).getLowerBound();
    }

    public boolean hasPatternInSlot(int slot) {
        return !getStackInSlot(slot).isEmpty();
    }

    public boolean hasValidPatternInSlot(int slot) {
        return hasPatternInSlot(slot) && getPatternInfoInSlot(slot).isValid();
    }

    public boolean hasCircuitInSlot(int slot) {
        return getPatternInfoInSlot(slot).hasCircuit();
    }

    public boolean hasStockInSlot(int slot) {
        return getPatternInfoInSlot(slot).hasStock();
    }

    public LinkedList<IAEItemStack> getInputItemsInSlot(int slot) {
        return new LinkedList<>(Arrays.asList(getPatternInfoInSlot(slot).getInputItems()));
    }

    public LinkedList<IAEItemStack> getOutputItemsInSlot(int slot) {
        return new LinkedList<>(Arrays.asList(getPatternInfoInSlot(slot).getOutputItems()));
    }


    public class PatternInfo implements INBTSerializable<NBTTagCompound> {

        private long upperBound = 0;
        private long lowerBound = 0;
        private int circuitValue = -1;

        private ICraftingPatternDetails craftingDetails;
        private boolean isValid = false;
        private IAEItemStack[] inputItems;
        private IAEItemStack[] outputItems;
        private IAEFluidStack[] inputFluids;
        private IAEFluidStack[] outputFluids;
        private ItemStack stockItem = ItemStack.EMPTY;

        public PatternInfo(long upperBound, long lowerBound, int circuitValue, ItemStack pattern, World world) {
            this.upperBound = upperBound;
            this.lowerBound = lowerBound;
            this.circuitValue = circuitValue;
            if (!pattern.isEmpty()) {
                this.craftingDetails = ((ItemEncodedPattern) pattern.getItem()).getPatternForItem(pattern, world);
                if (craftingDetails != null) {
                    this.isValid = true;
                    this.inputItems = craftingDetails.getCondensedInputs();
                    this.outputItems = craftingDetails.getCondensedOutputs();
                    this.stockItem = outputItems[0].getCachedItemStack(1);
//                    if (craftingDetails instanceof FluidCraftingPatternDetails fluidCraftingPatternDetails) {
//                        this.inputFluids = fluidCraftingPatternDetails.getInputs();
//                    }
                }
            }
        }

        public PatternInfo(ItemStack pattern) {
            this(0, 0, -1, pattern, null);
        }

        public PatternInfo(NBTTagCompound nbt) {
            this.deserializeNBT(nbt);
        }

        public PatternInfo() {
            this(ItemStack.EMPTY);
        }

        public boolean hasCircuit() {
            return circuitValue != -1;
        }

        public boolean hasStock() {
            return stockItem != ItemStack.EMPTY;
        }

        public boolean isValid() {
            return isValid;
        }

        @Override
        public NBTTagCompound serializeNBT() {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setBoolean("isValid", isValid);
            nbt.setInteger("circuitValue", circuitValue);
            nbt.setLong("upperBound", upperBound);
            nbt.setLong("lowerBound", lowerBound);
            stockItem.writeToNBT(nbt);

            return nbt;
        }

        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            isValid = nbt.getBoolean("isValid");
            circuitValue = nbt.getInteger("circuitValue");
            upperBound = nbt.getLong("upperBound");
            lowerBound = nbt.getLong("lowerBound");
            stockItem = new ItemStack(nbt);
        }

        public int getCircuitValue() {
            return circuitValue;
        }

        public long getUpperBound() {
            return upperBound;
        }

        public long getLowerBound() {
            return lowerBound;
        }

        public ItemStack getStockItem() {
            return stockItem;
        }

        public ICraftingPatternDetails getCraftingDetails() {
            return craftingDetails;
        }

        public IAEItemStack[] getInputItems() {
            return inputItems;
        }

        public IAEItemStack[] getOutputItems() {
            return outputItems;
        }

        public void setCircuitValue(int circuitValue) {
            this.circuitValue = (circuitValue >= 0 && circuitValue <= 24) ? circuitValue : -1;
        }

        public void nextCircuitValue() {
            setCircuitValue(circuitValue + 1);
        }

        public void setUpperBound(long upperBound) {
            this.upperBound = Math.max(upperBound, lowerBound);
        }

        public void setLowerBound(long lowerBound) {
            this.lowerBound = lowerBound < 0 ? 0 : (Math.min(lowerBound, upperBound));
        }
    }
}
