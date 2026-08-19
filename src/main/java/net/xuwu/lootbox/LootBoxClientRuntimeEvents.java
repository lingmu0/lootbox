package net.xuwu.lootbox;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Applies server definition changes to client UI after a join or /reload. */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = LootBoxMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class LootBoxClientRuntimeEvents {
    private LootBoxClientRuntimeEvents() {}

    /** Refreshes only the lootbox tab after a new server definition snapshot arrives. */
    public static void refreshClientUi() {
        LootBoxJeiPlugin.refreshRuntimeRecipes();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(
                minecraft.level.enabledFeatures(), true, minecraft.level.registryAccess());
        LootBoxMod.TAB.get().buildContents(parameters);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LootBoxManager.clearClientSync();
    }
}
