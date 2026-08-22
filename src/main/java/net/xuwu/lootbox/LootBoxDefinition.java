package net.xuwu.lootbox;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.List;

/** 一个可被物品组件引用的奖励箱定义。 */
public record LootBoxDefinition(ResourceLocation id, Component displayName, int rolls, List<Entry> entries, int color,
                                String displayNameKey, List<Component> jeiInfo) {
    public LootBoxDefinition(ResourceLocation id, Component displayName, int rolls, List<Entry> entries) {
        this(id, displayName, rolls, entries, 0xFFFFFF, null, List.of());
    }

    public LootBoxDefinition(ResourceLocation id, Component displayName, int rolls, List<Entry> entries, int color) {
        this(id, displayName, rolls, entries, color, null, List.of());
    }

    public LootBoxDefinition(ResourceLocation id, Component displayName, int rolls, List<Entry> entries,
                             int color, String displayNameKey) {
        this(id, displayName, rolls, entries, color, displayNameKey, List.of());
    }

    public LootBoxDefinition {
        rolls = Math.max(1, rolls);
        entries = List.copyOf(entries);
        color &= 0xFFFFFF;
        displayNameKey = displayNameKey == null || displayNameKey.isBlank() ? null : displayNameKey;
        jeiInfo = jeiInfo == null ? List.of() : List.copyOf(jeiInfo);
    }

    /** Returns the spawn egg, or structure void when this entity has no registered egg. */
    public static ItemStack summonDisplayStack(ResourceLocation summonEntity) {
        if (summonEntity == null) return ItemStack.EMPTY;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(summonEntity).orElse(null);
        SpawnEggItem egg = type == null ? null : SpawnEggItem.byId(type);
        if (egg == null && type != null) {
            egg = ForgeSpawnEggItem.fromEntityType(type);
        }
        ItemStack display = egg == null ? new ItemStack(Items.STRUCTURE_VOID) : new ItemStack(egg);
        display.setHoverName(summonDisplayName(summonEntity));
        return display;
    }

    public static Component summonDisplayName(ResourceLocation summonEntity) {
        if (summonEntity == null) return Component.empty();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(summonEntity).orElse(null);
        return type == null ? Component.literal(summonEntity.toString()) : type.getDescription();
    }

    public record Entry(ItemStack stack, int min, int max, double weight, double luckWeight,
                        LootBoxCondition condition, String conditionText, Component conditionComponent, Float luckMinimum,
                        List<ItemStack> possibleStacks, String tagId, ResourceLocation summonEntity) {
        public Entry(ItemStack stack, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, String conditionText) {
            this(stack, min, max, weight, luckWeight, condition,
                    conditionText, conditionText == null || conditionText.isBlank() ? Component.empty() : Component.literal(conditionText), null,
                    List.of(stack), null, null);
        }

        public Entry(ItemStack stack, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, Component conditionText) {
            this(stack, min, max, weight, luckWeight, condition,
                    conditionText == null ? "" : conditionText.getString(), conditionText, null, List.of(stack), null, null);
        }

        public Entry(ItemStack stack, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, Component conditionText, Float luckMinimum) {
            this(stack, min, max, weight, luckWeight, condition,
                    conditionText == null ? "" : conditionText.getString(), conditionText, luckMinimum, List.of(stack), null, null);
        }

        public Entry(List<ItemStack> possibleStacks, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, Component conditionText, Float luckMinimum) {
            this(possibleStacks.get(0), min, max, weight, luckWeight, condition,
                    conditionText == null ? "" : conditionText.getString(), conditionText, luckMinimum, possibleStacks, null, null);
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
                    List.of(new ItemStack(Items.BARRIER)), normalizeTagId(tagId), null);
        }

        /** Creates an entry that summons the selected entity instead of giving an item. */
        public Entry(ResourceLocation summonEntity, int min, int max, double weight, double luckWeight,
                     LootBoxCondition condition, Component conditionText, Float luckMinimum) {
            this(ItemStack.EMPTY, min, max, weight, luckWeight, condition,
                    conditionText == null ? "" : conditionText.getString(), conditionText, luckMinimum,
                    List.of(), null, summonEntity);
        }

        public Entry {
            min = Math.max(1, min);
            max = Math.max(min, max);
            weight = Math.max(0, weight);
            luckWeight = Math.max(0, luckWeight);
            conditionText = conditionText == null ? "" : conditionText;
            conditionComponent = conditionComponent == null ? Component.empty() : conditionComponent;
            possibleStacks = summonEntity != null ? List.of()
                    : possibleStacks == null || possibleStacks.isEmpty()
                    ? List.of(stack.copy())
                    : possibleStacks.stream().map(ItemStack::copy).toList();
            tagId = normalizeTagId(tagId);
        }

        /** Resolves the current tag contents; normal item and box entries simply return their stored stacks. */
        public List<ItemStack> resolvedStacks() {
            if (summonEntity != null) return List.of();
            if (tagId == null) return possibleStacks;
            TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(tagId));
            HolderSet.Named<Item> taggedItems = BuiltInRegistries.ITEM.getTag(tag).orElse(null);
            if (taggedItems == null || taggedItems.size() == 0) {
                return List.of(new ItemStack(Items.BARRIER));
            }
            return taggedItems.stream().map(holder -> new ItemStack(holder.value())).toList();
        }

        public ItemStack displayStack() {
            if (summonEntity != null) return summonDisplayStack(summonEntity);
            return resolvedStacks().get(0).copy();
        }

        public ItemStack createStack(java.util.Random random) {
            if (summonEntity != null) return ItemStack.EMPTY;
            List<ItemStack> stacks = resolvedStacks();
            ItemStack result = stacks.get(random.nextInt(stacks.size())).copy();
            result.setCount(min == max ? min : min + random.nextInt(max - min + 1));
            return result;
        }

        public int randomCount(java.util.Random random) {
            return min == max ? min : min + random.nextInt(max - min + 1);
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
