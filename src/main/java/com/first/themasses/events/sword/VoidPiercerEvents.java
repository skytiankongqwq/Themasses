package com.first.themasses.events.sword;

import com.first.themasses.curios.mysterious.laowang237;
import com.first.themasses.item.sword.VoidPiercer;
import com.first.themasses.util.BuffBlacklist;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

@Mod.EventBusSubscriber(modid = "themasses")
public class VoidPiercerEvents {

    // 🛡️ 创建一个线程本地锁，专门用来标记是否正在执行“前刺伤害”
    private static final ThreadLocal<Boolean> IS_DASH_HURTING = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        // 1. 如果当前线程正在执行前刺伤害，直接跳过，防止死循环
        if (IS_DASH_HURTING.get()) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof VoidPiercer)) return;

        if (player.isSprinting()) {
            Level level = player.level();
            Vec3 lookVec = player.getLookAngle().normalize();
            AABB area = player.getBoundingBox().expandTowards(lookVec.scale(5.0)).inflate(1.0);
            List<Entity> list = level.getEntities(player, area, e -> e instanceof LivingEntity);

            // 2. 准备执行前刺，加上锁
            IS_DASH_HURTING.set(true);
            try {
                int count = 0;
                for (Entity target : list) {
                    if (count >= 3) break;
                    // 这里直接使用最标准的玩家伤害
                    target.hurt(player.damageSources().playerAttack(player), 15.0F);
                    count++;
                }
            } finally {
                // 3. 无论是否报错，最后必须解锁，否则玩家后续攻击都会失效
                IS_DASH_HURTING.set(false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            Level world = player.level();
            BlockPos pos = player.blockPosition();

            // --- 1. 基础环境判定 (所有人都有) ---
            float envMultiplier = 0.0F;
            float envLifeSteal = 0.0F;

            boolean hasCelestialCycle = world.dimensionType().hasSkyLight();
            boolean canSeeSky = world.canSeeSky(pos);
            int brightness = world.getMaxLocalRawBrightness(pos);

            if (hasCelestialCycle && canSeeSky) {
                envMultiplier = 1.0F; // 地表 +100%
                envLifeSteal = 0.10F;  // 地表 +10%
            } else if (!hasCelestialCycle || brightness < 7) {
                envMultiplier = 0.5F; // 异界/阴暗 +50%
                envLifeSteal = 0.05F;  // 异界/阴暗 +5%
            }

            // --- 2. 饰品额外加成 (检测 laowang237) ---
            // 先定义饰品提供的额外变量
            final float[] extraStats = {0.0F, 0.0F, 1.0F}; // {伤害增量, 吸血增量, 穿透倍率}

            CuriosApi.getCuriosHelper().findFirstCurio(player, stack ->
                    stack.getItem().toString().contains("laowang237")
            ).ifPresent(slot -> {
                extraStats[0] = 0.02F; // 额外 2% 伤害
                extraStats[1] = 0.07F; // 额外 7% 吸血
                extraStats[2] = 1.03F; // 3% 护甲穿透
            });

            // --- 3. 综合计算 ---
            // 总倍率 = 1.0(原伤) + 环境加成 + 饰品加成
            float totalMultiplier = 1.0F + envMultiplier + extraStats[0];
            float totalLifeSteal = envLifeSteal + extraStats[1];
            float armorPen = extraStats[2];

            if (totalMultiplier > 1.0F || armorPen > 1.0F) {
                float finalDamage = event.getAmount() * totalMultiplier * armorPen;
                event.setAmount(finalDamage);

                if (totalLifeSteal > 0) {
                    player.heal(finalDamage * totalLifeSteal);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onHitEffect(LivingHurtEvent event) {
        // 第一个 player 声明已经涵盖了后续所有逻辑，不需要再次判断
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        LivingEntity victim = event.getEntity();

        // --- 2. 攻击提升 Buff (如果是持有虚空刺杀者攻击) ---
        if (player.getMainHandItem().getItem() instanceof VoidPiercer) {
            for (MobEffectInstance effect : victim.getActiveEffects()) {
                if (!BuffBlacklist.isBlacklisted(effect.getEffect())) {
                    int newDuration = effect.getDuration() + 3200;
                    int newAmp = Math.min(effect.getAmplifier() + 1, 254);
                    victim.addEffect(new MobEffectInstance(effect.getEffect(), newDuration, newAmp));
                }
            }
        }
    }


    @SubscribeEvent
    public static void onPerfectBlock(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // 1. 检查是否正在使用虚空刺杀者格挡
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof VoidPiercer) {

            // 获取玩家已经格挡的时长 (ticks，1秒 = 20 ticks)
            int useDuration = player.getTicksUsingItem();

            // 2. 判定：1秒内 (20 ticks) 为完美格挡
            if (useDuration > 0 && useDuration <= 20) {
                // --- 【完美格挡逻辑】 ---
                event.setAmount(0); // 100% 减伤

                Entity attacker = event.getSource().getEntity();
                if (attacker instanceof LivingEntity target && attacker != player) {
                    // A. 瞬间冲刺到目标面前 (偏移 1 格防止重叠)
                    Vec3 dashPos = target.position().add(target.getLookAngle().reverse().scale(1.0));
                    player.teleportTo(dashPos.x, dashPos.y, dashPos.z);

                    // B. 瞬间伤害并挑起 (击飞)
                    IS_DASH_HURTING.set(true); // 同样需要防递归锁
                    try {
                        // 刺一下 (基础伤害)
                        target.hurt(player.damageSources().playerAttack(player), 8.0F);

                        // 挑起：设置向上速度 (约 0.5~0.6 对应 3 格高度)
                        target.setDeltaMovement(target.getDeltaMovement().x, 0.65, target.getDeltaMovement().z);
                        target.hurtMarked = true; // 强制同步给客户端
                    } finally {
                        IS_DASH_HURTING.set(false);
                    }
                    player.displayClientMessage(Component.literal("§b⚡ 完美格挡：虚空反击！"), true);
                }
            } else {
                // --- 【普通格挡逻辑】 ---
                // 1秒外减伤 20%
                event.setAmount(event.getAmount() * 0.8F);
            }
        }
    }

    @SubscribeEvent
    public static void onSneakAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.isShiftKeyDown() && player.getMainHandItem().getItem() instanceof VoidPiercer) {
            Entity target = event.getTarget();
            if (target instanceof LivingEntity livingTarget) {
                player.teleportTo(target.getX(), target.getY() + 3.0, target.getZ());
                player.setDeltaMovement(0, -1.5, 0);
                // 这里建议同步改为 Action Bar 显示，效果更好
                player.displayClientMessage(Component.literal("§d⚡ 虚空坠击！"), true);
            }
        }
    }
}