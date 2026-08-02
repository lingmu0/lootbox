package net.xuwu.lootbox;

import net.neoforged.neoforge.common.ModConfigSpec;

/** 服务端战利品箱掉落配置。配置文件位于世界的 serverconfig/lootbox-server.toml。 */
public final class LootBoxConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HIDE_DEFAULT_BOXES;
    public static final ModConfigSpec.BooleanValue ENABLE_MEKANISM_REWARDS;
    public static final ModConfigSpec.BooleanValue ENABLE_CREATE_REWARDS;
    public static final ModConfigSpec.BooleanValue ENABLE_IRONS_SPELLBOOKS_REWARDS;
    public static final ModConfigSpec.BooleanValue ENABLE_GOETY_REWARDS;
    public static final ModConfigSpec.BooleanValue ENABLE_AE2_REWARDS;
    public static final ModConfigSpec.BooleanValue ENABLE_TERRA_ENTITY_REWARDS;
    public static final ModConfigSpec.BooleanValue ENABLE_TWILIGHT_FOREST_REWARDS;
    public static final ModConfigSpec.BooleanValue ENABLE_AETHER_REWARDS;
    public static final ModConfigSpec.BooleanValue MOB_DROPS_ENABLED;
    public static final ModConfigSpec.DoubleValue COMMON_DROP_CHANCE;
    public static final ModConfigSpec.DoubleValue RARE_DROP_CHANCE;
    public static final ModConfigSpec.DoubleValue UNUSUAL_DROP_CHANCE;
    public static final ModConfigSpec.DoubleValue EPIC_DROP_CHANCE;
    public static final ModConfigSpec.DoubleValue LEGENDARY_DROP_CHANCE;
    public static final ModConfigSpec.DoubleValue ENDURANCE_DROP_CHANCE;

    static {
        ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
        HIDE_DEFAULT_BOXES = commonBuilder
                .comment("Whether the built-in loot boxes are hidden from the creative tab and JEI.")
                .translation("config.lootbox.hide_default_boxes")
                .define("hide_default_boxes", false);
        COMMON_SPEC = commonBuilder.build();

        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENABLE_MEKANISM_REWARDS = integration(builder, "mekanism", "config.lootbox.enable_mekanism_rewards");
        ENABLE_CREATE_REWARDS = integration(builder, "create", "config.lootbox.enable_create_rewards");
        ENABLE_IRONS_SPELLBOOKS_REWARDS = integration(builder, "irons_spellbooks", "config.lootbox.enable_irons_spellbooks_rewards");
        ENABLE_GOETY_REWARDS = integration(builder, "goety", "config.lootbox.enable_goety_rewards");
        ENABLE_AE2_REWARDS = integration(builder, "ae2", "config.lootbox.enable_ae2_rewards");
        ENABLE_TERRA_ENTITY_REWARDS = integration(builder, "terra_entity", "config.lootbox.enable_terra_entity_rewards");
        ENABLE_TWILIGHT_FOREST_REWARDS = integration(builder, "twilightforest", "config.lootbox.enable_twilight_forest_rewards");
        ENABLE_AETHER_REWARDS = integration(builder, "aether", "config.lootbox.enable_aether_rewards");
        MOB_DROPS_ENABLED = builder
                .comment("Whether mobs killed by a player can drop loot boxes.")
                .translation("config.lootbox.mob_drops_enabled")
                .define("mob_drops_enabled", true);

        builder.comment("Chance for each tier. Values are between 0 and 1.",
                "Tiers are checked from the rarest to the commonest; one kill drops at most one box.")
                .push("mob_drop_chances");
        COMMON_DROP_CHANCE = chance(builder, "common", "config.lootbox.common_drop_chance", 0.05D);
        UNUSUAL_DROP_CHANCE = chance(builder, "unusual", "config.lootbox.unusual_drop_chance", 0.02D);
        RARE_DROP_CHANCE = chance(builder, "rare", "config.lootbox.rare_drop_chance", 0.01D);
        EPIC_DROP_CHANCE = chance(builder, "epic", "config.lootbox.epic_drop_chance", 0.005D);
        LEGENDARY_DROP_CHANCE = chance(builder, "legendary", "config.lootbox.legendary_drop_chance", 0.001D);
        ENDURANCE_DROP_CHANCE = chance(builder, "endurance", "config.lootbox.endurance_drop_chance", 0.0001D);
        builder.pop();
        SPEC = builder.build();
    }

    private LootBoxConfig() {}

    private static ModConfigSpec.BooleanValue integration(ModConfigSpec.Builder builder, String modId,
                                                           String translationKey) {
        return builder
                .comment("Whether rewards from " + modId + " are added to built-in loot boxes.")
                .translation(translationKey)
                .define("enable_" + modId + "_rewards", true);
    }

    private static ModConfigSpec.DoubleValue chance(ModConfigSpec.Builder builder, String path,
                                                     String translationKey, double defaultValue) {
        return builder.comment("Default: " + defaultValue + " (" + (defaultValue * 100.0D) + "%).")
                .translation(translationKey)
                .defineInRange(path, defaultValue, 0.0D, 1.0D);
    }
}
