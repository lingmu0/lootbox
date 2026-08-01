package net.xuwu.lootbox;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 可选模组奖励池。这里只使用注册表 ID，不引用任何模组的 Java 类；未安装模组或物品不存在时会自动跳过。
 */
public final class LootBoxOptionalRewards {
    private static final List<OptionalReward> REWARDS = List.of(
            // Mekanism
            reward("common", "mekanism:ingot_osmium", 1, 3, 3),
            reward("rare", "mekanism:basic_control_circuit", 1, 2, 2),
            reward("unusual", "mekanism:alloy_infused", 1, 2, 2),
            reward("epic", "mekanism:alloy_reinforced", 1, 2, 1),
            reward("legendary", "mekanism:pellet_polonium", 1, 2, 0.5),

            // Create
            reward("common", "create:zinc_ingot", 2, 5, 3),
            reward("rare", "create:brass_ingot", 1, 3, 2),
            reward("unusual", "create:electron_tube", 1, 2, 2),
            reward("epic", "create:precision_mechanism", 1, 2, 1),
            reward("legendary", "create:refined_radiance", 1, 2, 0.5),

            // Iron's Spells 'n Spellbooks
            reward("common", "irons_spellbooks:arcane_essence", 2, 6, 3),
            reward("rare", "irons_spellbooks:magic_cloth", 1, 3, 2),
            reward("unusual", "irons_spellbooks:scroll", 1, 2, 1.5),
            reward("epic", "irons_spellbooks:ancient_ink", 1, 2, 1),
            reward("legendary", "irons_spellbooks:arcane_debris", 1, 2, 0.5),

            // Goety - The Dark Arts（诡厄巫法）
            reward("common", "goety:dark_ingot", 1, 3, 2),
            reward("rare", "goety:cursed_ingot", 1, 2, 1.5),
            reward("unusual", "goety:dark_alloy_ingot", 1, 2, 1),
            reward("epic", "goety:unholy_blood", 1, 2, 0.75),
            reward("legendary", "goety:dark_metal_ingot", 1, 2, 0.5),

            // Applied Energistics 2
            reward("common", "ae2:certus_quartz_crystal", 2, 6, 3),
            reward("rare", "ae2:fluix_crystal", 1, 3, 2),
            reward("unusual", "ae2:calculation_processor", 1, 2, 1.5),
            reward("epic", "ae2:engineering_processor", 1, 2, 1),
            reward("legendary", "ae2:quantum_entangled_singularity", 1, 2, 0.25),

            // Terra Entity（泰拉生物；这些物品在没有该模组时会自动忽略）
            reward("common", "terra_entity:finch_staff", 1, 1, 1),
            reward("rare", "terra_entity:hornet_staff", 1, 1, 1),
            reward("unusual", "terra_entity:imp_staff", 1, 1, 0.75),
            reward("epic", "terra_entity:terriprisma", 1, 1, 0.5),
            reward("legendary", "terra_entity:summon_sword", 1, 1, 0.25),

            // The Twilight Forest
            reward("common", "twilightforest:naga_scale", 1, 3, 2),
            reward("rare", "twilightforest:steeleaf_ingot", 1, 3, 1.5),
            reward("unusual", "twilightforest:fiery_ingot", 1, 2, 1),
            reward("epic", "twilightforest:knightmetal_ingot", 1, 2, 0.75),
            reward("legendary", "twilightforest:carminite", 1, 2, 0.5),

            // The Aether（天境）
            reward("common", "aether:ambrosium_shard", 2, 6, 2),
            reward("rare", "aether:zanite_gemstone", 1, 3, 1.5),
            reward("unusual", "aether:gravitite_ingot", 1, 2, 1),
            reward("epic", "aether:phoenix_tear", 1, 2, 0.75),
            reward("legendary", "aether:enchanted_gravitite", 1, 2, 0.25)
    );

    private LootBoxOptionalRewards() {}

    public static List<LootBoxDefinition.Entry> append(String tier, List<LootBoxDefinition.Entry> baseEntries) {
        List<LootBoxDefinition.Entry> result = new ArrayList<>(baseEntries);
        LootBoxCondition always = LootBoxApi.condition("always");
        for (OptionalReward reward : REWARDS) {
            if (!reward.tier().equals(tier)) continue;
            Item item = BuiltInRegistries.ITEM.get(reward.itemId());
            if (item == null || item == Items.AIR) continue;
            result.add(new LootBoxDefinition.Entry(new ItemStack(item), reward.min(), reward.max(), reward.weight(), 0,
                    always, ""));
        }
        return List.copyOf(result);
    }

    private static OptionalReward reward(String tier, String itemId, int min, int max, double weight) {
        return new OptionalReward(tier, ResourceLocation.parse(itemId), min, max, weight);
    }

    private record OptionalReward(String tier, ResourceLocation itemId, int min, int max, double weight) {}
}
