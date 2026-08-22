package net.xuwu.lootbox;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** JEI 的只读展示对象，不参与 Minecraft 配方系统。 */
public record LootBoxJeiRecipe(ItemStack box, int rolls, List<LootBoxDefinition.Entry> entries,
                               List<Component> info, List<LootBoxDefinition.Entry> probabilityEntries) {
    public LootBoxJeiRecipe {
        entries = List.copyOf(entries);
        info = List.copyOf(info);
        probabilityEntries = List.copyOf(probabilityEntries);
    }
}
