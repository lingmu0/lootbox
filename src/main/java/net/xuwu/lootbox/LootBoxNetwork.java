package net.xuwu.lootbox;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** NeoForge payload helpers for the server-to-client definition snapshot. */
public final class LootBoxNetwork {
    private LootBoxNetwork() {}

    public static void sendToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new LootBoxSyncPayload(LootBoxManager.createSyncPayload()));
    }
}
