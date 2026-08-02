package net.xuwu.lootbox;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-authoritative loot-box definitions sent to a physical client. */
public record LootBoxSyncPacket(CompoundTag data) {
    public LootBoxSyncPacket {
        data = data == null ? new CompoundTag() : data;
    }

    public static void encode(LootBoxSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data());
    }

    public static LootBoxSyncPacket decode(FriendlyByteBuf buffer) {
        CompoundTag data = buffer.readNbt();
        return new LootBoxSyncPacket(data == null ? new CompoundTag() : data);
    }

    public static void handle(LootBoxSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> LootBoxManager.applyClientSync(packet.data()));
        context.setPacketHandled(true);
    }
}
