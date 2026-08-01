package net.xuwu.lootbox;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
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

/** Loads data/<namespace>/loot_boxes/*.json definitions. */
public final class LootBoxManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final Map<ResourceLocation, LootBoxDefinition> DEFINITIONS = new ConcurrentHashMap<>();

    static {
        LootBoxCondition always = context -> true;
        LootBoxCondition luckTwo = context -> context.luck() >= 2;
        LootBoxCondition luckThree = context -> context.luck() >= 3;
        ResourceLocation common = boxId("common");
        ResourceLocation rare = boxId("rare");
        ResourceLocation unusual = boxId("unusual");
        ResourceLocation epic = boxId("epic");
        ResourceLocation legendary = boxId("legendary");
        ResourceLocation endurance = boxId("endurance");

        DEFINITIONS.put(common, new LootBoxDefinition(common, Component.translatable("lootbox.box.common"), 1,
                optionalEntries("common",
                        new LootBoxDefinition.Entry(new ItemStack(Items.IRON_INGOT), 2, 8, 50, 0, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.GOLD_INGOT), 1, 4, 25, 2, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.DIAMOND), 1, 1, 2, 1, luckTwo,
                                Component.translatable("condition.lootbox.luck", formatNumber(2.0F)), 2.0F)), 0xFFFFFF));
        DEFINITIONS.put(rare, new LootBoxDefinition(rare, Component.translatable("lootbox.box.rare"), 2,
                optionalEntries("rare",
                        new LootBoxDefinition.Entry(new ItemStack(Items.EMERALD), 2, 8, 35, 3, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.DIAMOND), 1, 3, 12, 2, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.NETHERITE_SCRAP), 1, 1, 1, 1, luckThree,
                                Component.translatable("condition.lootbox.luck", formatNumber(3.0F)), 3.0F)), 0x7C4DFF));
        DEFINITIONS.put(unusual, new LootBoxDefinition(unusual, Component.translatable("lootbox.box.unusual"), 1,
                optionalEntries("unusual",
                        new LootBoxDefinition.Entry(new ItemStack(Items.REDSTONE), 8, 16, 30, 0, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.LAPIS_LAZULI), 4, 10, 20, 0, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.ENDER_PEARL), 1, 2, 8, 0, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.DIAMOND), 1, 2, 4, 0, always, "")), 0x26A69A));
        DEFINITIONS.put(epic, new LootBoxDefinition(epic, Component.translatable("lootbox.box.epic"), 1,
                optionalEntries("epic",
                        new LootBoxDefinition.Entry(new ItemStack(Items.DIAMOND), 2, 5, 25, 0, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.EMERALD), 2, 5, 20, 0, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.NETHERITE_SCRAP), 1, 2, 8, 0, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.GOLDEN_APPLE), 1, 1, 3, 0, always, "")), 0x9C27B0));
        DEFINITIONS.put(legendary, new LootBoxDefinition(legendary, Component.translatable("lootbox.box.legendary"), 1,
                optionalEntries("legendary",
                        new LootBoxDefinition.Entry(new ItemStack(Items.DIAMOND_BLOCK), 1, 3, 25, 0, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.EMERALD_BLOCK), 1, 3, 15, 0, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.NETHERITE_INGOT), 1, 2, 8, 0, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 1, 1, 1, 0, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.ELYTRA), 1, 1, 0.25D, 0, always, "")), 0xFFB300));
        DEFINITIONS.put(endurance, new LootBoxDefinition(endurance, Component.translatable("lootbox.box.endurance"), 1,
                List.of(
                        new LootBoxDefinition.Entry(new ItemStack(Items.DIAMOND_BLOCK), 100, 100, 8, 0, always, ""),
                        new LootBoxDefinition.Entry(new ItemStack(Items.EMERALD_BLOCK), 100, 100, 2, 0, always, ""),
                        new LootBoxDefinition.Entry(LootBoxItem.createReferenceStack(endurance.toString()), 1, 1, 99990, 0, always, "")),
                0xF44336));
    }

    private static ResourceLocation boxId(String path) {
        return ResourceLocation.fromNamespaceAndPath(LootBoxMod.MODID, path);
    }

    private static List<LootBoxDefinition.Entry> optionalEntries(String tier, LootBoxDefinition.Entry... entries) {
        return LootBoxOptionalRewards.append(tier, List.of(entries));
    }

    public LootBoxManager() {
        super(GSON, "loot_boxes");
    }

    public static LootBoxDefinition get(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }

    public static boolean isDefaultBox(ResourceLocation id) {
        if (!LootBoxMod.MODID.equals(id.getNamespace())) return false;
        return switch (id.getPath()) {
            case "common", "rare", "unusual", "epic", "legendary", "endurance" -> true;
            default -> false;
        };
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
                LootBoxMod.LOGGER.error("Unable to read loot box {}", resource.getKey(), exception);
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
                List<ItemStack> stacks;
                if (json.has("box")) {
                    stacks = List.of(LootBoxItem.createReferenceStack(GsonHelper.getAsString(json, "box")));
                } else if (json.has("tag")) {
                    String tagId = GsonHelper.getAsString(json, "tag");
                    if (tagId.startsWith("#")) tagId = tagId.substring(1);
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
                    HolderSet.Named<Item> taggedItems = BuiltInRegistries.ITEM.getTag(tag).orElse(null);
                    if (taggedItems == null || taggedItems.size() == 0) {
                        LootBoxMod.LOGGER.warn("Loot box {} references missing or empty item tag {}", id, tag);
                        continue;
                    }
                    stacks = taggedItems.stream().map(holder -> new ItemStack(holder.value())).toList();
                } else {
                    ResourceLocation itemId = ResourceLocation.parse(GsonHelper.getAsString(json, "item"));
                    Item item = BuiltInRegistries.ITEM.get(itemId);
                    if (item == null || item == Items.AIR) {
                        LootBoxMod.LOGGER.warn("Loot box {} references missing item {}", id, itemId);
                        continue;
                    }
                    stacks = List.of(new ItemStack(item));
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
                    if ("custom".equals(type)) type = GsonHelper.getAsString(conditionJson, "id", "custom");
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
                double itemWeight = weight / stacks.size();
                double itemLuckWeight = luckWeight / stacks.size();
                for (ItemStack stack : stacks) {
                    entries.add(new LootBoxDefinition.Entry(stack, min, max, itemWeight, itemLuckWeight,
                            condition, conditionText, luckMinimum));
                }
            }
        }
        if (isDefaultBox(id)) entries = LootBoxOptionalRewards.append(id.getPath(), entries);
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
