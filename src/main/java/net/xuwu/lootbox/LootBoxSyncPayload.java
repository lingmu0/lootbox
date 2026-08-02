package net.xuwu.lootbox;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.codec.StreamCodec;

/** Server-authoritative loot-box definitions sent to a physical client. */
public record LootBoxSyncPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<LootBoxSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(LootBoxMod.MODID, "loot_box_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LootBoxSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, LootBoxSyncPayload payload) {
            buffer.writeNbt(payload.data());
        }

        @Override
        public LootBoxSyncPayload decode(RegistryFriendlyByteBuf buffer) {
            CompoundTag data = buffer.readNbt();
            return new LootBoxSyncPayload(data == null ? new CompoundTag() : data);
        }
    };

    public LootBoxSyncPayload {
        data = data == null ? new CompoundTag() : data;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
