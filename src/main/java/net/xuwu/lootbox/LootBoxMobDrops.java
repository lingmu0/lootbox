package net.xuwu.lootbox;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.List;

/** 为玩家击杀的原版或模组生物投掷战利品箱。 */
public final class LootBoxMobDrops {
    private static final List<DropRule> RULES = List.of(
            new DropRule("lootbox:endurance", LootBoxConfig.ENDURANCE_DROP_CHANCE),
            new DropRule("lootbox:legendary", LootBoxConfig.LEGENDARY_DROP_CHANCE),
            new DropRule("lootbox:epic", LootBoxConfig.EPIC_DROP_CHANCE),
            new DropRule("lootbox:rare", LootBoxConfig.RARE_DROP_CHANCE),
            new DropRule("lootbox:unusual", LootBoxConfig.UNUSUAL_DROP_CHANCE),
            new DropRule("lootbox:common", LootBoxConfig.COMMON_DROP_CHANCE)
    );

    private LootBoxMobDrops() {}

    public static void handle(LivingDeathEvent event) {
        if (!LootBoxConfig.MOB_DROPS_ENABLED.get()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide()) return;

        if (findPlayerAttacker(event.getSource()) == null) return;

        for (DropRule rule : RULES) {
            if (mob.getRandom().nextDouble() >= rule.chance().get()) continue;
            ItemStack stack = LootBoxItem.createStack(rule.id().toString());
            mob.spawnAtLocation(stack, 0.5F);
            return;
        }
    }

    private static ServerPlayer findPlayerAttacker(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) return player;
        if (attacker instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private record DropRule(ResourceLocation id, net.minecraftforge.common.ForgeConfigSpec.DoubleValue chance) {
        private DropRule(String id, net.minecraftforge.common.ForgeConfigSpec.DoubleValue chance) {
            this(new ResourceLocation(id), chance);
        }
    }
}
