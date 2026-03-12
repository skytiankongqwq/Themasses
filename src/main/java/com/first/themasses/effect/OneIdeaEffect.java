package com.first.themasses.effect;

import com.first.themasses.TheMasses;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
//我有一个点子buff
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID)
public class OneIdeaEffect extends MobEffect {

    public OneIdeaEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFEB3B);
    }

    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player && !player.level().isClientSide) {

            // --- 核心修改：检测到 ONE_IDEA 时，清除并禁止 THINKING_QUIETLY ---
            if (player.hasEffect(ModEffects.ONE_IDEA.get())) {
                if (player.hasEffect(ModEffects.THINKING_QUIETLY.get())) {
                    player.removeEffect(ModEffects.THINKING_QUIETLY.get());
                    // 重置饰品的计时器，防止 Buff 瞬间跳变
                    player.getPersistentData().putInt("laowang_stand_ticks", 0);
                }

                // 原有的逻辑：检测周围怪物并转化为保镖 (每 5 tick 执行一次以节省性能)
                if (player.tickCount % 5 == 0) {
                    AABB area = player.getBoundingBox().inflate(5.0D);
                    List<Mob> nearbyMobs = player.level().getEntitiesOfClass(Mob.class, area,
                            mob -> !mob.getUUID().equals(player.getUUID()) && mob.isAlive());

                    if (nearbyMobs.size() >= 2) {
                        for (int i = 0; i < 2; i++) {
                            Mob helper = nearbyMobs.get(i);
                            helper.setTarget(null);
                            helper.setLastHurtByMob(null);
                            helper.getNavigation().stop();

                            helper.getPersistentData().putUUID("OneIdeaOwner", player.getUUID());
                            helper.getPersistentData().putBoolean("WaitingForFirstTarget", true);
                        }
                        player.removeEffect(ModEffects.ONE_IDEA.get());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            LivingEntity target = event.getEntity();
            List<Mob> helpers = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20.0D),
                    mob -> mob.getPersistentData().contains("OneIdeaOwner") &&
                            mob.getPersistentData().getUUID("OneIdeaOwner").equals(player.getUUID()) &&
                            mob.getPersistentData().getBoolean("WaitingForFirstTarget"));

            for (Mob helper : helpers) {
                helper.getNavigation().stop();
                helper.getPersistentData().putUUID("OneIdeaTarget", target.getUUID()); // 先存入目标
                helper.getPersistentData().putBoolean("WaitingForFirstTarget", false);
                helper.setTarget(target); // 后设置目标
                helper.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onTargetDeath(LivingDeathEvent event) {
        LivingEntity deceased = event.getEntity();
        List<Mob> helpers = deceased.level().getEntitiesOfClass(Mob.class, deceased.getBoundingBox().inflate(25.0D),
                mob -> mob.getPersistentData().contains("OneIdeaTarget") &&
                        mob.getPersistentData().getUUID("OneIdeaTarget").equals(deceased.getUUID()));

        for (Mob helper : helpers) {
            helper.getPersistentData().remove("OneIdeaOwner");
            helper.getPersistentData().remove("WaitingForFirstTarget");
            helper.getPersistentData().remove("OneIdeaTarget");
            helper.setTarget(null);
        }
    }

    // 中立
    @SubscribeEvent
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.getPersistentData().contains("OneIdeaOwner")) {
            LivingEntity newTarget = event.getNewTarget();

            // 1. 如果新目标是主人（玩家），强制拦截，保持中立
            if (newTarget instanceof Player && newTarget.getUUID().equals(mob.getPersistentData().getUUID("OneIdeaOwner"))) {
                event.setCanceled(true);
                mob.setTarget(null);
                return;
            }

            // 2. 如果已经分配了攻击目标，且新目标不是该目标，则拦截（防止保镖中途去打路人）
            if (mob.getPersistentData().contains("OneIdeaTarget")) {
                java.util.UUID assignedUUID = mob.getPersistentData().getUUID("OneIdeaTarget");
                if (newTarget == null || !newTarget.getUUID().equals(assignedUUID)) {
                    // 如果尝试锁定的不是分配的目标，拦截
                    event.setCanceled(true);
                    // 尝试手动重定向回分配的目标（可选）
                }
            }
        }
    }
}