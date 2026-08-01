package net.xuwu.lootbox;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = LootBoxMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LootBoxClientEvents {
    private LootBoxClientEvents() {}

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((ItemStack stack, int tintIndex) -> {
            LootBoxDefinition definition = LootBoxItem.getDefinition(stack);
            return tintIndex == 0 && definition != null ? 0xFF000000 | definition.color() : 0xFFFFFFFF;
        }, LootBoxMod.LOOT_BOX.get());
    }
}
