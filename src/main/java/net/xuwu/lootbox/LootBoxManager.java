package net.xuwu.lootbox;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 读取 data/<namespace>/loot_boxes/*.json 的服务端重载监听器。 */
public final class LootBoxManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, LootBoxDefinition> DEFINITIONS = new ConcurrentHashMap<>();

    static {
        // 客户端 JEI 在服务器数据包重载前也能显示两个随 mod 附带的示例箱。
        LootBoxCondition always = context -> true;
        LootBoxCondition luckTwo = context -> context.luck() >= 2;
        ResourceLocation common = ResourceLocation.fromNamespaceAndPath(LootBoxMod.MODID, "common");
        ResourceLocation rare = ResourceLocation.fromNamespaceAndPath(LootBoxMod.MODID, "rare");
        DEFINITIONS.put(common, new LootBoxDefinition(common, Component.translatable("lootbox.box.common"), 1, List.of(
                new LootBoxDefinition.Entry(new ItemStack(Items.IRON_INGOT), 2, 8, 50, 0, always, ""),
                new LootBoxDefinition.Entry(new ItemStack(Items.GOLD_INGOT), 1, 4, 25, 2, always, ""),
                new LootBoxDefinition.Entry(new ItemStack(Items.DIAMOND), 1, 1, 2, 1, luckTwo,
                        Component.translatable("condition.lootbox.luck", formatNumber(2.0F)), 2.0F)), 0xFFFFFF));
        DEFINITIONS.put(rare, new LootBoxDefinition(rare, Component.translatable("lootbox.box.rare"), 2, List.of(
                new LootBoxDefinition.Entry(new ItemStack(Items.EMERALD), 2, 8, 35, 3, always, ""),
                new LootBoxDefinition.Entry(new ItemStack(Items.DIAMOND), 1, 3, 12, 2, always, ""),
                new LootBoxDefinition.Entry(new ItemStack(Items.NETHERITE_SCRAP), 1, 1, 1, 1, context -> context.luck() >= 3,
                        Component.translatable("condition.lootbox.luck", formatNumber(3.0F)), 3.0F)), 0x7C4DFF));
    }

    public LootBoxManager() {
        super(GSON, "loot_boxes");
    }

    public static LootBoxDefinition get(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }

    public static Map<ResourceLocation, LootBoxDefinition> definitions() {
        Map<ResourceLocation, LootBoxDefinition> result = new HashMap<>(DEFINITIONS);
        result.putAll(LootBoxApi.scriptedDefinitions());
        return Map.copyOf(result);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, LootBoxDefinition> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> resource : jsons.entrySet()) {
            try {
                LootBoxDefinition definition = parse(resource.getKey(), resource.getValue().getAsJsonObject());
                loaded.put(resource.getKey(), definition);
            } catch (Exception exception) {
                LootBoxMod.LOGGER.error("无法读取战利品箱 {}", resource.getKey(), exception);
            }
        }
        DEFINITIONS.clear();
        DEFINITIONS.putAll(loaded);
        LootBoxMod.LOGGER.info("Loaded {} loot box definitions", loaded.size());
    }

    private static LootBoxDefinition parse(ResourceLocation id, JsonObject root) {
        Component name = root.has("display_name_key")
                ? Component.translatable(GsonHelper.getAsString(root, "display_name_key"))
                : Component.literal(GsonHelper.getAsString(root, "display_name", id.getPath()));
        int rolls = GsonHelper.getAsInt(root, "rolls", 1);
        int color = parseColor(root.get("color"));
        List<LootBoxDefinition.Entry> entries = new ArrayList<>();
        if (root.has("entries")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(root, "entries")) {
                JsonObject json = element.getAsJsonObject();
                ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(json, "item"));
                Item item = BuiltInRegistries.ITEM.get(itemId);
                if (item == null || item == Items.AIR) {
                    LootBoxMod.LOGGER.warn("战利品箱 {} 引用了不存在的物品 {}", id, itemId);
                    continue;
                }
                int min = GsonHelper.getAsInt(json, "min", GsonHelper.getAsInt(json, "count", 1));
                int max = GsonHelper.getAsInt(json, "max", min);
                double weight = GsonHelper.getAsDouble(json, "weight", 1.0D);
                double luckWeight = GsonHelper.getAsDouble(json, "luck_weight", 0.0D);
                JsonObject conditionJson = json.has("condition") ? GsonHelper.getAsJsonObject(json, "condition") : null;
                LootBoxCondition condition = LootBoxApi.condition("always");
                Component conditionText = Component.empty();
                Float luckMinimum = null;
                if (conditionJson != null) {
                    String type = GsonHelper.getAsString(conditionJson, "type", "always");
                    if ("custom".equals(type)) {
                        type = GsonHelper.getAsString(conditionJson, "id", "custom");
                    }
                    condition = LootBoxApi.condition(type);
                    if (condition == null) {
                        condition = context -> false;
                        conditionText = Component.translatable("condition.lootbox.unknown", type);
                    } else if ("luck".equals(type)) {
                        float minimum = GsonHelper.getAsFloat(conditionJson, "min", 0.0F);
                        condition = new LootBoxCondition() {
                            @Override public boolean test(LootBoxContext context) { return context.luck() >= minimum; }
                            @Override public String description(LootBoxContext context) {
                                return Component.translatable("condition.lootbox.luck", formatNumber(minimum)).getString();
                            }
                        };
                        conditionText = Component.translatable("condition.lootbox.luck", formatNumber(minimum));
                        luckMinimum = minimum;
                    } else {
                        conditionText = conditionJson.has("display_key")
                                ? Component.translatable(GsonHelper.getAsString(conditionJson, "display_key"))
                                : conditionJson.has("display")
                                ? Component.literal(GsonHelper.getAsString(conditionJson, "display"))
                                : Component.empty();
                    }
                }
                entries.add(new LootBoxDefinition.Entry(new ItemStack(item), min, max, weight, luckWeight,
                        condition, conditionText, luckMinimum));
            }
        }
        return new LootBoxDefinition(id, name, rolls, entries, color);
    }

    private static String formatNumber(float value) {
        return value == (int) value ? Integer.toString((int) value) : Float.toString(value);
    }

    private static int parseColor(JsonElement element) {
        if (element == null || element.isJsonNull()) return 0xFFFFFF;
        try {
            String value = element.getAsString().trim();
            if (value.startsWith("#")) value = value.substring(1);
            if (value.startsWith("0x") || value.startsWith("0X")) value = value.substring(2);
            return Integer.parseUnsignedInt(value, 16) & 0xFFFFFF;
        } catch (RuntimeException ignored) {
            try {
                return element.getAsInt() & 0xFFFFFF;
            } catch (RuntimeException ignoredAgain) {
                return 0xFFFFFF;
            }
        }
    }
}
