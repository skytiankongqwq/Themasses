package com.first.themasses.effect;

import com.first.themasses.TheMasses;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//别动，我在思考buff
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID)
public class ThinkingQuietlyEffect extends MobEffect {

    public ThinkingQuietlyEffect(MobEffectCategory harmful) {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance().getEffect() == ModEffects.THINKING_QUIETLY.get()) {
            LivingEntity entity = event.getEntity();
            entity.getPersistentData().putDouble("thinking_origin_x", entity.getX());
            entity.getPersistentData().putDouble("thinking_origin_y", entity.getY());
            entity.getPersistentData().putDouble("thinking_origin_z", entity.getZ());
        }
    }

    @SubscribeEvent
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player) {
            if (player.hasEffect(ModEffects.THINKING_QUIETLY.get())) {
                if (event.getEntity() instanceof Mob mob) {
                    event.setCanceled(true);
                    mob.setTarget(null);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity instanceof Player player && player.hasEffect(ModEffects.THINKING_QUIETLY.get())) {

            // 每秒 (20 ticks) 10% 概率获得 ONE_IDEA ---
            if (player.tickCount % 20 == 0) {
                if (player.getRandom().nextFloat() < 0.10f) {
                    // 添加 30 秒的 ONE_IDEA 效果
                    player.addEffect(new MobEffectInstance(ModEffects.ONE_IDEA.get(), 600, 0));
                }
            }

            // 1. 怪物中立判定 (针对正在追逐玩家的怪物)
            if (player.tickCount % 10 == 0) {
                player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16.0D)).forEach(mob -> {
                    if (mob.getTarget() == player) {
                        mob.setTarget(null);
                        mob.setLastHurtByMob(null);
                        mob.getNavigation().stop();
                    }
                });
            }

            // 2. 极致灵敏检测：比对原点坐标
            boolean hasInput = player.xxa != 0 || player.zza != 0;
            double originX = player.getPersistentData().getDouble("thinking_origin_x");
            double originY = player.getPersistentData().getDouble("thinking_origin_y");
            double originZ = player.getPersistentData().getDouble("thinking_origin_z");

            // 增加极小容差 0.001 防止浮点数抖动误判
            boolean posChanged = Math.abs(player.getX() - originX) > 0.001D ||
                    Math.abs(player.getY() - originY) > 0.001D ||
                    Math.abs(player.getZ() - originZ) > 0.001D;

            if (hasInput || posChanged) {
                player.removeEffect(ModEffects.THINKING_QUIETLY.get());
                player.getPersistentData().putInt("laowang_stand_ticks", 0);
                player.getPersistentData().remove("thinking_origin_x");
                player.getPersistentData().remove("thinking_origin_y");
                player.getPersistentData().remove("thinking_origin_z");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && player.hasEffect(ModEffects.THINKING_QUIETLY.get())) {
            player.removeEffect(ModEffects.THINKING_QUIETLY.get());
            player.getPersistentData().putInt("laowang_stand_ticks", 0);
        }
    }
}