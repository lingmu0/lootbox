package net.xuwu.lootbox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ContainerHelper;

import javax.annotation.Nullable;
import java.util.UUID;

/** 自动开箱器的可被漏斗访问的 10 格库存（0 输入，1-9 输出）。 */
public class LootBoxOpenerBlockEntity extends BlockEntity implements WorldlyContainer {
    private final NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY);
    @Nullable private UUID owner;

    public LootBoxOpenerBlockEntity(BlockPos pos, BlockState state) {
        super(LootBoxMod.LOOT_BOX_OPENER_ENTITY.get(), pos, state);
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        setChanged();
    }

    public boolean insertInput(ItemStack stack) {
        if (!(stack.getItem() instanceof LootBoxItem) || !items.get(0).isEmpty()) return false;
        ItemStack input = stack.copyWithCount(1);
        items.set(0, input);
        stack.shrink(1);
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LootBoxOpenerBlockEntity opener) {
        if (opener.items.get(0).isEmpty() || level.getServer() == null || opener.owner == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(opener.owner);
        if (player == null) return; // 保证自动开箱沿用放置者的幸运和自定义条件
        LootBoxContext context = new LootBoxContext(player, level, player.getLuck());
        if (!LootBoxItem.open(opener.items.get(0), context, opener::insertOutput)) return;
        opener.items.set(0, ItemStack.EMPTY);
        opener.setChanged();
    }

    private void insertOutput(ItemStack output) {
        ItemStack remaining = output.copy();
        for (int slot = 1; slot < items.size() && !remaining.isEmpty(); slot++) {
            ItemStack existing = items.get(slot);
            if (existing.isEmpty()) {
                items.set(slot, remaining.copy());
                remaining = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameTags(existing, remaining)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                remaining.shrink(moved);
            }
        }
        if (!remaining.isEmpty() && level != null) {
            BlockPos dropPos = worldPosition.above();
            net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(
                    level, dropPos.getX() + .5, dropPos.getY() + .5, dropPos.getZ() + .5, remaining);
            level.addFreshEntity(entity);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { ItemStack result = ContainerHelper.removeItem(items, slot, amount); if (!result.isEmpty()) setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); stack.setCount(Math.min(stack.getCount(), getMaxStackSize())); setChanged(); }
    @Override public void setChanged() { super.setChanged(); }
    @Override public boolean stillValid(Player player) { return level != null && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getCenter()) <= 64.0D; }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.UP ? new int[]{0} : new int[]{1,2,3,4,5,6,7,8,9}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return slot == 0 && stack.getItem() instanceof LootBoxItem && items.get(0).isEmpty(); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot > 0; }
}
