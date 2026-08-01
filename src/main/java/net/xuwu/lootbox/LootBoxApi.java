package net.xuwu.lootbox;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * KJS/Java 扩展入口。KubeJS 可以直接调用静态方法，例如
 * {@code LootBoxApi.register("my_pack", "我的奖励箱", 1, ...)}。
 */
public final class LootBoxApi {
    private static final Map<String, LootBoxCondition> CONDITIONS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, LootBoxDefinition> SCRIPT_DEFINITIONS = new ConcurrentHashMap<>();
    private static volatile boolean builtinConditionsRegistered;

    private LootBoxApi() {}

    public static void registerBuiltinConditions() {
        if (builtinConditionsRegistered) return;
        builtinConditionsRegistered = true;
        CONDITIONS.put("luck", new LootBoxCondition() {
            @Override public boolean test(LootBoxContext context) { return context.luck() >= 0; }
            @Override public String description(LootBoxContext context) {
                return context == null ? "" : Component.translatable("condition.lootbox.luck", context.luck()).getString();
            }
        });
        CONDITIONS.put("always", context -> true);
    }

    public static void registerCondition(String id, LootBoxCondition condition, Function<LootBoxContext, String> display) {
        registerBuiltinConditionsGuard();
        CONDITIONS.put(id, new LootBoxCondition() {
            @Override public boolean test(LootBoxContext context) { return condition.test(context); }
            @Override public String description(LootBoxContext context) { return display.apply(context); }
        });
    }

    public static LootBoxCondition condition(String id) {
        registerBuiltinConditionsGuard();
        return CONDITIONS.get(id);
    }

    public static void register(String id, String displayName, int rolls, List<LootBoxDefinition.Entry> entries) {
        register(id, displayName, rolls, entries, 0xFFFFFF);
    }

    /** 注册箱子并使用 RGB 色值给原版箱子贴图染色，例如 0x7C4DFF。 */
    public static void register(String id, String displayName, int rolls, List<LootBoxDefinition.Entry> entries, int color) {
        ResourceLocation location = ResourceLocation.parse(id.contains(":") ? id : LootBoxMod.MODID + ":" + id);
        SCRIPT_DEFINITIONS.put(location, new LootBoxDefinition(location, Component.literal(displayName), rolls, entries, color));
    }

    public static LootBoxDefinition getDefinition(ResourceLocation id) {
        LootBoxDefinition scripted = SCRIPT_DEFINITIONS.get(id);
        return scripted != null ? scripted : LootBoxManager.get(id);
    }

    public static ItemStack createBox(String id) {
        return LootBoxItem.createStack(id);
    }

    /** 方便 KJS 构建奖励项的工厂，组件可由脚本在返回的 ItemStack 上继续设置。 */
    public static LootBoxDefinition.Entry entry(String itemId, int min, int max, double weight,
                                                 double luckWeight, String conditionId, String conditionText) {
        ResourceLocation id = ResourceLocation.parse(itemId);
        var item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) item = Items.BARRIER;
        LootBoxCondition condition = conditionId == null || conditionId.isBlank()
                ? LootBoxApi.condition("always") : LootBoxApi.condition(conditionId);
        if (condition == null) condition = context -> false;
        return new LootBoxDefinition.Entry(new ItemStack(item), min, max, weight, luckWeight, condition, conditionText);
    }

    public static LootBoxDefinition.Entry entry(ItemStack stack, int min, int max, double weight,
                                                 double luckWeight, String conditionId, String conditionText) {
        LootBoxCondition condition = resolveCondition(conditionId);
        return new LootBoxDefinition.Entry(stack.copy(), min, max, weight, luckWeight, condition, conditionText);
    }

    /** Creates a reward that randomly selects one item from the tag with equal probability. */
    public static LootBoxDefinition.Entry entryTag(String tagId, int min, int max, double weight,
                                                    double luckWeight, String conditionId, String conditionText) {
        String normalizedTagId = tagId != null && tagId.startsWith("#") ? tagId.substring(1) : tagId;
        TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(normalizedTagId));
        HolderSet.Named<Item> taggedItems = BuiltInRegistries.ITEM.getTag(tag).orElse(null);
        List<ItemStack> stacks = taggedItems == null
                ? List.of(new ItemStack(Items.BARRIER))
                : taggedItems.stream().map(holder -> new ItemStack(holder.value())).toList();
        if (stacks.isEmpty()) stacks = List.of(new ItemStack(Items.BARRIER));
        if (taggedItems == null || taggedItems.size() == 0) {
            LootBoxMod.LOGGER.warn("KJS loot box entry references missing or empty item tag {}", tag);
        }
        return new LootBoxDefinition.Entry(stacks, min, max, weight, luckWeight,
                resolveCondition(conditionId), conditionText == null ? Component.empty() : Component.literal(conditionText), null);
    }

    public static List<LootBoxDefinition.Entry> entries(LootBoxDefinition.Entry... entries) {
        return new ArrayList<>(List.of(entries));
    }

    static Map<ResourceLocation, LootBoxDefinition> scriptedDefinitions() {
        return Map.copyOf(SCRIPT_DEFINITIONS);
    }

    private static LootBoxCondition resolveCondition(String conditionId) {
        LootBoxCondition condition = conditionId == null || conditionId.isBlank()
                ? LootBoxApi.condition("always") : LootBoxApi.condition(conditionId);
        return condition == null ? context -> false : condition;
    }

    private static void registerBuiltinConditionsGuard() {
        if (!builtinConditionsRegistered) {
            registerBuiltinConditions();
        }
    }
}
