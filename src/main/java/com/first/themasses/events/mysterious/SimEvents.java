package com.first.themasses.events.mysterious;

import com.first.themasses.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;
@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = "themasses")
public class SimEvents {

    private static final UUID SIM_STATS_UUID = UUID.fromString("11223344-5566-7788-9900-aabbccddeeff");

    private static Item getSimItem() {
        return ModItems.SIM.get();
    }

    private static boolean hasSim(Player player) {
        if (player == null || getSimItem() == null) return false;
        if (CuriosApi.getCuriosHelper().findFirstCurio(player, getSimItem()).isPresent()) return true;
        return player.getInventory().contains(new ItemStack(getSimItem()));
    }

    // ================== 核心算法：寻找距离指针(准星)最近的生物 ==================
    private static LivingEntity findBestTarget(Player player, double maxRange) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.getEyePosition();
        AABB searchBox = player.getBoundingBox().inflate(maxRange);

        LivingEntity bestTarget = null;
        double closestDistToRay = Double.MAX_VALUE;

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, searchBox)) {
            if (target == player || !target.isAlive() || target.isAlliedTo(player)) continue;

            Vec3 toTarget = target.position().subtract(startPos);
            // 确保怪物在玩家视角的前半球（防背后锁定）
            if (toTarget.dot(lookVec) < 0) continue;

            // 叉乘计算距离准星射线的垂直距离（实现“距离指针最近”）
            double distToRay = toTarget.cross(lookVec).length();
            if (distToRay < closestDistToRay && toTarget.length() <= maxRange) {
                closestDistToRay = distToRay;
                bestTarget = target;
            }
        }
        return bestTarget;
    }


    // ================== 被动：怠战 (周围怪物属性变化) ==================
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        // 如果该实体是敌对怪物
        if (entity instanceof Enemy) {
            // 搜索 32 格内是否有带 Sim 饰品的玩家
            Player nearbySimPlayer = entity.level().getNearestPlayer(entity, 32.0D);
            boolean hasAura = nearbySimPlayer != null && hasSim(nearbySimPlayer);

            // 将属性变化应用给怪物（提升生存，降低攻击）
            manageAttribute(entity, Attributes.MAX_HEALTH, 0.1, hasAura);
            manageAttribute(entity, Attributes.ARMOR, 0.1, hasAura);
            manageAttribute(entity, Attributes.ARMOR_TOUGHNESS, 0.1, hasAura);
            manageAttribute(entity, Attributes.MOVEMENT_SPEED, 0.1, hasAura);
            manageAttribute(entity, Attributes.ATTACK_DAMAGE, -0.15, hasAura); // 降低 15% 攻击力

            // 怪物生命回复：如果没有到达上限，每 2 秒回复 1 点血量
            if (hasAura && entity.tickCount % 40 == 0 && entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(1.0F);
            }
        }
    }

    private static void manageAttribute(LivingEntity entity, Attribute attribute, double value, boolean enable) {
        var inst = entity.getAttribute(attribute);
        if (inst != null) {
            boolean hasModifier = inst.hasModifier(new AttributeModifier(SIM_STATS_UUID, "SimStat", value, AttributeModifier.Operation.MULTIPLY_TOTAL));
            if (enable && !hasModifier) {
                inst.addTransientModifier(new AttributeModifier(SIM_STATS_UUID, "SimStat", value, AttributeModifier.Operation.MULTIPLY_TOTAL));
            } else if (!enable && hasModifier) {
                inst.removeModifier(SIM_STATS_UUID);
            }
        }
    }

    // ================== 被动：怠战 (降低仇恨 & 受击弹飞) ==================
    @SubscribeEvent
    public static void onLivingVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player && hasSim(player)) {
            if (event.getLookingEntity() instanceof Enemy && !event.getLookingEntity().getType().is(Tags.EntityTypes.BOSSES)) {
                event.modifyVisibility(0.1);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && hasSim(player)) {
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                // 修复弹飞方向：用 玩家坐标 减去 攻击者坐标，底层 knockback 取反后即为向外推
                double d0 = player.getX() - attacker.getX();
                double d1 = player.getZ() - attacker.getZ();
                attacker.knockback(1.5F, d0, d1);
            }
        }
    }

    // ================== 被动：洞察 (近战/弹射物 绝对锁死) ==================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide || !hasSim(player)) return;

        // 玩家挥动武器的第一帧，触发近战自瞄
        if (player.swingTime == 1) {
            // 搜索 8 格内的最佳目标
            LivingEntity target = findBestTarget(player, 8.0);
            if (target != null) {
                player.attack(target); // 强制执行伤害判定
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        // 当箭矢、雪球、火球等弹射物生成的瞬间
        if (event.getEntity() instanceof Projectile projectile && !projectile.level().isClientSide) {
            if (projectile.getOwner() instanceof Player player && hasSim(player)) {
                // 搜索 64 格内的最佳目标
                LivingEntity target = findBestTarget(player, 64.0);

                if (target != null) {
                    // 获取弹射物原本的飞行速度
                    double speed = projectile.getDeltaMovement().length();
                    if (speed < 0.1) speed = 1.5; // 给个保底速度

                    // 计算瞄准目标胸口的方向
                    Vec3 targetPos = target.getEyePosition().subtract(0, target.getBbHeight() * 0.5, 0);
                    Vec3 shootDir = targetPos.subtract(projectile.position()).normalize();

                    // 调用底层 shoot 方法，瞬间重写弹射物的轨道和朝向
                    projectile.shoot(shootDir.x, shootDir.y, shootDir.z, (float)speed, 0.0F);
                }
            }
        }
    }

    // ================== 被动：跑路啦兄弟跑路啦 ==================
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFatalDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && hasSim(player)) {
            Item simItem = getSimItem();
            if (simItem != null && !player.getCooldowns().isOnCooldown(simItem)) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth() * 0.2F);

                for (int i = 0; i < 16; i++) {
                    double x = player.getX() + (player.getRandom().nextDouble() - 0.5D) * 32.0D;
                    double y = net.minecraft.util.Mth.clamp(
                            player.getY() + (double)(player.getRandom().nextInt(16) - 8),
                            (double)player.level().getMinBuildHeight(),
                            (double)(player.level().getMinBuildHeight() + ((net.minecraft.server.level.ServerLevel)player.level()).getLogicalHeight() - 1)
                    );
                    double z = player.getZ() + (player.getRandom().nextDouble() - 0.5D) * 32.0D;

                    if (player.randomTeleport(x, y, z, true)) {
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                        break;
                    }
                }

                player.getCooldowns().addCooldown(simItem, 2000);
                player.displayClientMessage(Component.literal("§d[Sim] 触发被动：跑路啦兄弟跑路啦！"), true);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 60, 4));
            }
        }
    }

    // ================== 主动技能：仿真 (视野标示与推演) ==================
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        @SuppressWarnings("resource")
        Player localPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (localPlayer == null) return;

        long endTime = localPlayer.getPersistentData().getLong("SimActiveEndTime");
        if (localPlayer.level().getGameTime() < endTime) {
            AABB sightBox = localPlayer.getBoundingBox().inflate(32.0);

            for (LivingEntity entity : localPlayer.level().getEntitiesOfClass(LivingEntity.class, sightBox)) {
                if (entity == localPlayer) continue;

                Vec3 motion = entity.getDeltaMovement();
                Vec3 predictedPos = entity.position().add(motion.x * 40, motion.y * 40, motion.z * 40);

                if (localPlayer.level().getGameTime() % 2 == 0) {
                    localPlayer.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + entity.getBbHeight() + 0.5, entity.getZ(), 0, 0, 0);
                    if (motion.lengthSqr() > 0.01) {
                        localPlayer.level().addParticle(ParticleTypes.PORTAL, predictedPos.x, predictedPos.y, predictedPos.z, 0, 0, 0);
                    }
                }
            }
        }
    }
}