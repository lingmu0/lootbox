package net.xuwu.lootbox;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        ResourceLocation location = new ResourceLocation(id.contains(":") ? id : LootBoxMod.MODID + ":" + id);
        registerDefinition(location, Component.literal(displayName), null, rolls, entries, color, null);
    }

    /** Registers a box and adds one translated JEI information line. */
    public static void register(String id, String displayName, int rolls, List<LootBoxDefinition.Entry> entries,
                                int color, String jeiInfoKey) {
        ResourceLocation location = new ResourceLocation(id.contains(":") ? id : LootBoxMod.MODID + ":" + id);
        registerDefinition(location, Component.literal(displayName), null, rolls, entries, color, jeiInfoKey);
    }

    /** Registers a box using a localization key, matching data-pack display_name_key. */
    public static void registerTranslated(String id, String displayNameKey, int rolls,
                                          List<LootBoxDefinition.Entry> entries) {
        registerTranslated(id, displayNameKey, rolls, entries, 0xFFFFFF);
    }

    /** Registers a box using a localization key and RGB tint. */
    public static void registerTranslated(String id, String displayNameKey, int rolls,
                                          List<LootBoxDefinition.Entry> entries, int color) {
        ResourceLocation location = new ResourceLocation(id.contains(":") ? id : LootBoxMod.MODID + ":" + id);
        registerDefinition(location, Component.translatable(displayNameKey), displayNameKey, rolls, entries, color, null);
    }

    /** Registers a translated-name box and adds one translated JEI information line. */
    public static void registerTranslated(String id, String displayNameKey, int rolls,
                                          List<LootBoxDefinition.Entry> entries, int color, String jeiInfoKey) {
        ResourceLocation location = new ResourceLocation(id.contains(":") ? id : LootBoxMod.MODID + ":" + id);
        registerDefinition(location, Component.translatable(displayNameKey), displayNameKey, rolls, entries, color, jeiInfoKey);
    }

    private static void registerDefinition(ResourceLocation location, Component displayName, String displayNameKey,
                                           int rolls, List<LootBoxDefinition.Entry> entries, int color,
                                           String jeiInfoKey) {
        List<LootBoxDefinition.Entry> finalEntries = LootBoxManager.isDefaultBox(location)
                ? LootBoxOptionalRewards.append(location.getPath(), entries)
                : List.copyOf(entries);
        List<Component> jeiInfo = jeiInfoKey == null || jeiInfoKey.isBlank()
                ? List.of() : List.of(Component.translatable(jeiInfoKey));
        SCRIPT_DEFINITIONS.put(location, new LootBoxDefinition(location, displayName, rolls, finalEntries, color,
                displayNameKey, jeiInfo));
    }

    public static LootBoxDefinition getDefinition(ResourceLocation id) {
        LootBoxDefinition scripted = SCRIPT_DEFINITIONS.get(id);
        return scripted != null ? scripted : LootBoxManager.get(id);
    }

    /** Clears definitions from the previous KJS server-script evaluation before registering the new set. */
    public static void clearScriptedDefinitions() {
        SCRIPT_DEFINITIONS.clear();
    }

    public static ItemStack createBox(String id) {
        return LootBoxItem.createStack(id);
    }

    /** 方便 KJS 构建奖励项的工厂，组件可由脚本在返回的 ItemStack 上继续设置。 */
    public static LootBoxDefinition.Entry entry(String itemId, int min, int max, double weight,
                                                 double luckWeight, String conditionId, String conditionText) {
        return entry(itemId, min, max, weight, luckWeight, conditionId, literalConditionText(conditionText));
    }

    private static LootBoxDefinition.Entry entry(String itemId, int min, int max, double weight,
                                                  double luckWeight, String conditionId, Component conditionText) {
        ResourceLocation id = new ResourceLocation(itemId);
        var item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) item = Items.BARRIER;
        LootBoxCondition condition = resolveCondition(conditionId);
        return new LootBoxDefinition.Entry(new ItemStack(item), min, max, weight, luckWeight, condition, conditionText);
    }

    /** Item reward whose condition description is a translation key, matching data-pack display_key. */
    public static LootBoxDefinition.Entry entryWithConditionKey(String itemId, int min, int max, double weight,
                                                                  double luckWeight, String conditionId,
                                                                  String conditionKey) {
        return entry(itemId, min, max, weight, luckWeight, conditionId,
                Component.translatable(conditionKey));
    }

    public static LootBoxDefinition.Entry entry(ItemStack stack, int min, int max, double weight,
                                                 double luckWeight, String conditionId, String conditionText) {
        LootBoxCondition condition = resolveCondition(conditionId);
        return new LootBoxDefinition.Entry(stack.copy(), min, max, weight, luckWeight, condition, conditionText);
    }

    /** ItemStack reward whose condition description is a translation key. */
    public static LootBoxDefinition.Entry entryStackWithConditionKey(ItemStack stack, int min, int max,
                                                                       double weight, double luckWeight,
                                                                       String conditionId, String conditionKey) {
        return new LootBoxDefinition.Entry(stack.copy(), min, max, weight, luckWeight, resolveCondition(conditionId),
                Component.translatable(conditionKey));
    }

    /** Reward that gives another loot box, matching the data-pack box entry. */
    public static LootBoxDefinition.Entry entryBox(String boxId, int min, int max, double weight,
                                                    double luckWeight, String conditionId, String conditionText) {
        return new LootBoxDefinition.Entry(LootBoxItem.createReferenceStack(boxId), min, max, weight, luckWeight,
                resolveCondition(conditionId), conditionText);
    }

    /** Nested loot-box reward whose condition description is a translation key. */
    public static LootBoxDefinition.Entry entryBoxWithConditionKey(String boxId, int min, int max, double weight,
                                                                     double luckWeight, String conditionId,
                                                                     String conditionKey) {
        return new LootBoxDefinition.Entry(LootBoxItem.createReferenceStack(boxId), min, max, weight, luckWeight,
                resolveCondition(conditionId), Component.translatable(conditionKey));
    }

    /** Creates a reward that randomly selects one item from the tag with equal probability. */
    public static LootBoxDefinition.Entry entryTag(String tagId, int min, int max, double weight,
                                                    double luckWeight, String conditionId, String conditionText) {
        String normalizedTagId = tagId != null && tagId.startsWith("#") ? tagId.substring(1) : tagId;
        return new LootBoxDefinition.Entry(normalizedTagId, min, max, weight, luckWeight,
                resolveCondition(conditionId), literalConditionText(conditionText));
    }

    /** Tag reward whose condition description is a translation key. */
    public static LootBoxDefinition.Entry entryTagWithConditionKey(String tagId, int min, int max, double weight,
                                                                     double luckWeight, String conditionId,
                                                                     String conditionKey) {
        String normalizedTagId = tagId != null && tagId.startsWith("#") ? tagId.substring(1) : tagId;
        return new LootBoxDefinition.Entry(normalizedTagId, min, max, weight, luckWeight,
                resolveCondition(conditionId), Component.translatable(conditionKey));
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

    private static Component literalConditionText(String conditionText) {
        return conditionText == null || conditionText.isBlank() ? Component.empty() : Component.literal(conditionText);
    }

    private static void registerBuiltinConditionsGuard() {
        if (!builtinConditionsRegistered) {
            registerBuiltinConditions();
        }
    }
}
