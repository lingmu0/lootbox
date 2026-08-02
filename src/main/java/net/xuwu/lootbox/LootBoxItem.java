package net.xuwu.lootbox;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** 所有奖励箱共用的物品；箱子类型存储在 CUSTOM_DATA 组件中。 */
public class LootBoxItem extends Item {
    public static final String BOX_ID = "loot_box_id";
    private static final String SNAPSHOT = "loot_box_snapshot";
    private static final Random RANDOM = new Random();

    public LootBoxItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createStack(String id) {
        ItemStack stack = createReferenceStack(id);
        ResourceLocation location = ResourceLocation.parse(id.contains(":") ? id : LootBoxMod.MODID + ":" + id);
        LootBoxDefinition definition = LootBoxApi.getDefinition(location);
        if (definition != null) {
            CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            tag.put(SNAPSHOT, snapshot(definition));
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return stack;
    }

    public static ItemStack createReferenceStack(String id) {
        ItemStack stack = new ItemStack(LootBoxMod.LOOT_BOX.get());
        ResourceLocation location = ResourceLocation.parse(id.contains(":") ? id : LootBoxMod.MODID + ":" + id);
        CompoundTag tag = new CompoundTag();
        tag.putString(BOX_ID, location.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static ResourceLocation getDefinitionId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return ResourceLocation.fromNamespaceAndPath(LootBoxMod.MODID, "common");
        CompoundTag tag = data.copyTag();
        String id = tag.getString(BOX_ID);
        if (id.isBlank()) return ResourceLocation.fromNamespaceAndPath(LootBoxMod.MODID, "common");
        try {
            return ResourceLocation.parse(id);
        } catch (IllegalArgumentException ignored) {
            return ResourceLocation.fromNamespaceAndPath(LootBoxMod.MODID, "common");
        }
    }

    public static LootBoxDefinition getDefinition(ItemStack stack) {
        LootBoxDefinition definition = LootBoxApi.getDefinition(getDefinitionId(stack));
        if (definition != null) return definition;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag snapshot = data.copyTag().getCompound(SNAPSHOT);
        if (snapshot.isEmpty()) return null;
        return fromSnapshot(getDefinitionId(stack), snapshot);
    }

    private static CompoundTag snapshot(LootBoxDefinition definition) {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putString("name", definition.displayName().getString());
        if (definition.displayNameKey() != null) snapshot.putString("name_key", definition.displayNameKey());
        snapshot.putInt("rolls", definition.rolls());
        snapshot.putInt("color", definition.color());
        ListTag entries = new ListTag();
        for (LootBoxDefinition.Entry entry : definition.entries()) {
            CompoundTag json = new CompoundTag();
            if (entry.tagId() != null) {
                json.putString("tag", entry.tagId());
            } else if (entry.stack().getItem() instanceof LootBoxItem) {
                json.putString("box", getDefinitionId(entry.stack()).toString());
            } else if (entry.possibleStacks().size() > 1) {
                ListTag items = new ListTag();
                for (ItemStack possible : entry.possibleStacks()) {
                    CompoundTag item = new CompoundTag();
                    item.putString("id", BuiltInRegistries.ITEM.getKey(possible.getItem()).toString());
                    items.add(item);
                }
                json.put("items", items);
            } else {
                json.putString("item", BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString());
            }
            json.putInt("min", entry.min());
            json.putInt("max", entry.max());
            json.putDouble("weight", entry.weight());
            json.putDouble("luck_weight", entry.luckWeight());
            json.putString("condition", entry.conditionComponent().getString());
            if (entry.luckMinimum() != null) json.putFloat("luck_minimum", entry.luckMinimum());
            entries.add(json);
        }
        snapshot.put("entries", entries);
        return snapshot;
    }

    private static LootBoxDefinition fromSnapshot(ResourceLocation id, CompoundTag snapshot) {
        List<LootBoxDefinition.Entry> entries = new ArrayList<>();
        ListTag list = snapshot.getList("entries", Tag.TAG_COMPOUND);
        for (Tag value : list) {
            CompoundTag entry = (CompoundTag) value;
            List<ItemStack> stacks = new ArrayList<>();
            String tagId = null;
            if (entry.contains("tag", Tag.TAG_STRING)) {
                tagId = entry.getString("tag");
            } else if (entry.contains("box", Tag.TAG_STRING)) {
                stacks.add(createReferenceStack(entry.getString("box")));
            } else if (entry.contains("items", Tag.TAG_LIST)) {
                for (Tag itemValue : entry.getList("items", Tag.TAG_COMPOUND)) {
                    CompoundTag itemTag = (CompoundTag) itemValue;
                    var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemTag.getString("id")));
                    if (item != null && item != net.minecraft.world.item.Items.AIR) stacks.add(new ItemStack(item));
                }
                if (stacks.isEmpty()) continue;
            } else {
                var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getString("item")));
                if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
                stacks.add(new ItemStack(item));
            }
            Component conditionText = entry.contains("luck_minimum", Tag.TAG_FLOAT)
                    ? Component.translatable("condition.lootbox.luck", entry.getFloat("luck_minimum"))
                    : entry.getString("condition").isBlank()
                    ? Component.empty()
                    : Component.literal(entry.getString("condition"));
            Float luckMinimum = entry.contains("luck_minimum", Tag.TAG_FLOAT) ? entry.getFloat("luck_minimum") : null;
            entries.add(tagId == null
                    ? new LootBoxDefinition.Entry(stacks, entry.getInt("min"), entry.getInt("max"),
                    entry.getDouble("weight"), entry.getDouble("luck_weight"), context -> true,
                    conditionText, luckMinimum)
                    : new LootBoxDefinition.Entry(tagId, entry.getInt("min"), entry.getInt("max"),
                    entry.getDouble("weight"), entry.getDouble("luck_weight"), context -> true,
                    conditionText, luckMinimum));
        }
        String nameKey = snapshot.contains("name_key", Tag.TAG_STRING) ? snapshot.getString("name_key") : null;
        Component name = nameKey == null
                ? Component.literal(snapshot.getString("name"))
                : Component.translatable(nameKey);
        return new LootBoxDefinition(id, name, snapshot.getInt("rolls"), entries,
                snapshot.contains("color", Tag.TAG_INT) ? snapshot.getInt("color") : 0xFFFFFF, nameKey);
    }

    public static List<ItemStack> roll(ItemStack box, LootBoxContext context) {
        LootBoxDefinition definition = getDefinition(box);
        if (definition == null) return List.of();
        List<LootBoxDefinition.Entry> eligible = new ArrayList<>();
        for (LootBoxDefinition.Entry entry : definition.entries()) {
            if (entry.condition().test(context)) eligible.add(entry);
        }
        if (eligible.isEmpty()) return List.of();
        List<ItemStack> result = new ArrayList<>();
        for (int roll = 0; roll < definition.rolls(); roll++) {
            double total = eligible.stream().mapToDouble(entry ->
                    Math.max(0.0D, entry.weight() + context.luck() * entry.luckWeight())).sum();
            if (total <= 0.0D) break;
            double selected = RANDOM.nextDouble() * total;
            for (LootBoxDefinition.Entry entry : eligible) {
                selected -= Math.max(0.0D, entry.weight() + context.luck() * entry.luckWeight());
                if (selected <= 0.0D) {
                    result.add(entry.createStack(RANDOM));
                    break;
                }
            }
        }
        return result;
    }

    public static boolean open(ItemStack box, LootBoxContext context, java.util.function.Consumer<ItemStack> output) {
        List<ItemStack> result = roll(box, context);
        if (result.isEmpty()) return false;
        result.forEach(output);
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        LootBoxDefinition definition = getDefinition(stack);
        return definition == null ? Component.translatable("item.lootbox.loot_box")
                : definition.displayName();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);
        LootBoxContext context = new LootBoxContext(serverPlayer, level, serverPlayer.getLuck());
        if (!open(stack, context, result -> {
            if (!player.getInventory().add(result)) player.drop(result, false);
        })) {
            player.displayClientMessage(Component.translatable("message.lootbox.loot_box_no_reward").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        stack.shrink(1);
        player.displayClientMessage(Component.translatable("message.lootbox.loot_box_opened"), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LootBoxDefinition definition = getDefinition(stack);
        if (definition == null) {
            tooltip.add(Component.translatable("tooltip.lootbox.loot_box_unknown").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.lootbox.shift_for_details").withStyle(ChatFormatting.GRAY));
        if (!detailsRequested()) return;
        tooltip.add(Component.translatable("tooltip.lootbox.loot_box_rolls", definition.rolls()).withStyle(ChatFormatting.GRAY));
        for (LootBoxDefinition.Entry entry : LootBoxDefinition.expandForDisplay(definition.entries())) {
            var line = Component.literal("- ").append(entry.displayStack().getHoverName())
                    .append(" x" + entry.min() + (entry.max() == entry.min() ? "" : "-" + entry.max()))
                    .append("  ")
                    .append(Component.translatable("tooltip.lootbox.probability", probabilityText(definition, entry)));
            tooltip.add(line.withStyle(ChatFormatting.DARK_GRAY));
            if (!entry.conditionComponent().getString().isBlank()) {
                tooltip.add(Component.literal("  ").append(entry.conditionComponent()).withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    private static boolean detailsRequested() {
        return FMLEnvironment.dist == Dist.CLIENT && LootBoxClientHooks.isShiftDown();
    }

    private static String probabilityText(LootBoxDefinition definition, LootBoxDefinition.Entry selected) {
        float luck = FMLEnvironment.dist == Dist.CLIENT ? LootBoxClientHooks.currentLuck() : 0.0F;
        for (LootBoxDefinition.Entry entry : definition.entries()) {
            if (isUnknownCondition(entry)) return "?";
        }
        double total = definition.entries().stream()
                .filter(entry -> availableAtLuck(entry, luck))
                .mapToDouble(entry -> effectiveWeight(entry, luck))
                .sum();
        if (total <= 0.0D || !availableAtLuck(selected, luck)) return "0.00%";
        return String.format(Locale.ROOT, "%.2f%%", effectiveWeight(selected, luck) / total * 100.0D);
    }

    private static double effectiveWeight(LootBoxDefinition.Entry entry, float luck) {
        return Math.max(0.0D, entry.weight() + luck * entry.luckWeight());
    }

    private static boolean availableAtLuck(LootBoxDefinition.Entry entry, float luck) {
        return entry.luckMinimum() == null || luck >= entry.luckMinimum();
    }

    private static boolean isUnknownCondition(LootBoxDefinition.Entry entry) {
        return !entry.conditionComponent().getString().isBlank() && entry.luckMinimum() == null;
    }
}
