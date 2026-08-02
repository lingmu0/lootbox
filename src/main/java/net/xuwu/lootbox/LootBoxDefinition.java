package net.xuwu.lootbox;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 一个可被物品组件引用的奖励箱定义。 */
public record LootBoxDefinition(ResourceLocation id, Component displayName, int rolls, List<Entry> entries, int color,
                                String displayNameKey) {
    public LootBoxDefinition(ResourceLocation id, Component displayName, int rolls, List<Entry> entries) {
        this(id, displayName, rolls, entries, 0xFFFFFF, null);
    }

    public LootBoxDefinition(ResourceLocation id, Component displayName, int rolls, List<Entry> entries, int color) {
        this(id, displayName, rolls, entries, color, null);
    }

    public LootBoxDefinition {
        rolls = Math.max(1, rolls);
        entries = List.copyOf(entries);
        color &= 0xFFFFFF;
        displayNameKey = displayNameKey == null || displayNameKey.isBlank() ? null : displayNameKey;
    }

        public record Entry(ItemStack stack, int min, int max, double weight, double luckWeight,
                        LootBoxCondition condition, String conditionText, Component conditionComponent, Float luckMinimum,
                        List<ItemStack> possibleStacks, String tagId) {
        public Entry(ItemStack stack, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, String conditionText) {
                    this(stack, min, max, weight, luckWeight, condition,
                    conditionText, conditionText == null || conditionText.isBlank() ? Component.empty() : Component.literal(conditionText), null,
                    List.of(stack), null);
        }

        public Entry(ItemStack stack, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, Component conditionText) {
            this(stack, min, max, weight, luckWeight, condition,
                    conditionText == null ? "" : conditionText.getString(), conditionText, null, List.of(stack), null);
        }

        public Entry(ItemStack stack, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, Component conditionText, Float luckMinimum) {
            this(stack, min, max, weight, luckWeight, condition,
                    conditionText == null ? "" : conditionText.getString(), conditionText, luckMinimum, List.of(stack), null);
        }

        public Entry(List<ItemStack> possibleStacks, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, Component conditionText, Float luckMinimum) {
            this(possibleStacks.get(0), min, max, weight, luckWeight, condition,
                    conditionText == null ? "" : conditionText.getString(), conditionText, luckMinimum, possibleStacks, null);
        }

        /** Creates an entry that resolves an item tag when the entry is used, not when KJS is evaluated. */
        public Entry(String tagId, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, Component conditionText) {
            this(tagId, min, max, weight, luckWeight, condition, conditionText, null);
        }

        public Entry(String tagId, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, Component conditionText, Float luckMinimum) {
            this(new ItemStack(Items.BARRIER), min, max, weight, luckWeight, condition,
                    conditionText == null ? "" : conditionText.getString(), conditionText, luckMinimum,
                    List.of(new ItemStack(Items.BARRIER)), normalizeTagId(tagId));
        }

        public Entry {
            min = Math.max(1, min);
            max = Math.max(min, max);
            weight = Math.max(0, weight);
            luckWeight = Math.max(0, luckWeight);
            conditionText = conditionText == null ? "" : conditionText;
            conditionComponent = conditionComponent == null ? Component.empty() : conditionComponent;
            possibleStacks = possibleStacks == null || possibleStacks.isEmpty()
                    ? List.of(stack.copy())
                    : possibleStacks.stream().map(ItemStack::copy).toList();
            tagId = normalizeTagId(tagId);
        }

        /** Resolves the current tag contents; normal item and box entries simply return their stored stacks. */
        public List<ItemStack> resolvedStacks() {
            if (tagId == null) return possibleStacks;
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
            HolderSet.Named<Item> taggedItems = BuiltInRegistries.ITEM.getTag(tag).orElse(null);
            if (taggedItems == null || taggedItems.size() == 0) {
                return List.of(new ItemStack(Items.BARRIER));
            }
            return taggedItems.stream().map(holder -> new ItemStack(holder.value())).toList();
        }

        public ItemStack displayStack() {
            return resolvedStacks().get(0).copy();
        }

        public ItemStack createStack(java.util.Random random) {
            List<ItemStack> stacks = resolvedStacks();
            ItemStack result = stacks.get(random.nextInt(stacks.size())).copy();
            result.setCount(min == max ? min : min + random.nextInt(max - min + 1));
            return result;
        }

        private static String normalizeTagId(String id) {
            if (id == null || id.isBlank()) return null;
            return id.startsWith("#") ? id.substring(1) : id;
        }
    }

    /** Expands tag entries for read-only displays such as JEI and tooltips. */
    public static List<Entry> expandForDisplay(List<Entry> source) {
        java.util.ArrayList<Entry> expanded = new java.util.ArrayList<>();
        for (Entry entry : source) {
            List<ItemStack> stacks = entry.resolvedStacks();
            if (stacks.size() <= 1) {
                expanded.add(entry);
                continue;
            }
            double divisor = stacks.size();
            for (ItemStack stack : stacks) {
                expanded.add(new Entry(stack, entry.min(), entry.max(), entry.weight() / divisor,
                        entry.luckWeight() / divisor, entry.condition(), entry.conditionComponent(), entry.luckMinimum()));
            }
        }
        return List.copyOf(expanded);
    }
}
