package net.xuwu.lootbox;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 一个可被物品组件引用的奖励箱定义。 */
public record LootBoxDefinition(ResourceLocation id, Component displayName, int rolls, List<Entry> entries, int color) {
    public LootBoxDefinition(ResourceLocation id, Component displayName, int rolls, List<Entry> entries) {
        this(id, displayName, rolls, entries, 0xFFFFFF);
    }

    public LootBoxDefinition {
        rolls = Math.max(1, rolls);
        entries = List.copyOf(entries);
        color &= 0xFFFFFF;
    }

    public record Entry(ItemStack stack, int min, int max, double weight, double luckWeight,
                        LootBoxCondition condition, String conditionText, Component conditionComponent, Float luckMinimum) {
        public Entry(ItemStack stack, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, String conditionText) {
            this(stack, min, max, weight, luckWeight, condition,
                    conditionText, conditionText == null || conditionText.isBlank() ? Component.empty() : Component.literal(conditionText), null);
        }

        public Entry(ItemStack stack, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, Component conditionText) {
            this(stack, min, max, weight, luckWeight, condition,
                    conditionText == null ? "" : conditionText.getString(), conditionText, null);
        }

        public Entry(ItemStack stack, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, Component conditionText, Float luckMinimum) {
            this(stack, min, max, weight, luckWeight, condition,
                    conditionText == null ? "" : conditionText.getString(), conditionText, luckMinimum);
        }

        public Entry {
            min = Math.max(1, min);
            max = Math.max(min, max);
            weight = Math.max(0, weight);
            luckWeight = Math.max(0, luckWeight);
            conditionText = conditionText == null ? "" : conditionText;
            conditionComponent = conditionComponent == null ? Component.empty() : conditionComponent;
        }

        public ItemStack createStack(java.util.Random random) {
            ItemStack result = stack.copy();
            result.setCount(min == max ? min : min + random.nextInt(max - min + 1));
            return result;
        }
    }
}
