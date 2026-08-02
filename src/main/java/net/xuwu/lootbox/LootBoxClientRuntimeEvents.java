package net.xuwu.lootbox;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Applies server definition changes to client UI after a join or /reload. */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = LootBoxMod.MODID, bus = EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LootBoxClientRuntimeEvents {
    private static long lastAppliedRevision = Long.MIN_VALUE;

    private LootBoxClientRuntimeEvents() {}

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            if (minecraft.getConnection() == null) {
                LootBoxManager.clearClientSync();
                lastAppliedRevision = LootBoxManager.clientSyncRevision();
            }
            return;
        }
        long revision = LootBoxManager.clientSyncRevision();
        if (revision == lastAppliedRevision) return;
        lastAppliedRevision = revision;
        LootBoxJeiPlugin.refreshRuntimeRecipes();
        if (minecraft.level != null) {
            CreativeModeTabs.tryRebuildTabContents(minecraft.level.enabledFeatures(), true,
                    minecraft.level.registryAccess());
        }
    }
}
