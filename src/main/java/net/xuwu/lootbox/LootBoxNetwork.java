package net.xuwu.lootbox;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Forge SimpleChannel helpers for the server-to-client definition snapshot. */
public final class LootBoxNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LootBoxMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);
    private static int packetId;

    private LootBoxNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(packetId++, LootBoxSyncPacket.class,
                LootBoxSyncPacket::encode, LootBoxSyncPacket::decode, LootBoxSyncPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new LootBoxSyncPacket(LootBoxManager.createSyncPayload()));
    }
}
