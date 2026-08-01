package net.xuwu.lootbox;

import net.minecraftforge.common.ForgeConfigSpec;

/** 服务端战利品箱掉落配置。配置文件位于世界的 serverconfig/lootbox-server.toml。 */
public final class LootBoxConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue HIDE_DEFAULT_BOXES;
    public static final ForgeConfigSpec.BooleanValue MOB_DROPS_ENABLED;
    public static final ForgeConfigSpec.DoubleValue COMMON_DROP_CHANCE;
    public static final ForgeConfigSpec.DoubleValue RARE_DROP_CHANCE;
    public static final ForgeConfigSpec.DoubleValue UNUSUAL_DROP_CHANCE;
    public static final ForgeConfigSpec.DoubleValue EPIC_DROP_CHANCE;
    public static final ForgeConfigSpec.DoubleValue LEGENDARY_DROP_CHANCE;
    public static final ForgeConfigSpec.DoubleValue ENDURANCE_DROP_CHANCE;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        HIDE_DEFAULT_BOXES = commonBuilder
                .comment("Whether the built-in loot boxes are hidden from the creative tab and JEI.")
                .translation("config.lootbox.hide_default_boxes")
                .define("hide_default_boxes", false);
        COMMON_SPEC = commonBuilder.build();

        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
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

    private static ForgeConfigSpec.DoubleValue chance(ForgeConfigSpec.Builder builder, String path,
                                                      String translationKey, double defaultValue) {
        return builder.comment("Default: " + defaultValue + " (" + (defaultValue * 100.0D) + "%).")
                .translation(translationKey)
                .defineInRange(path, defaultValue, 0.0D, 1.0D);
    }
}
