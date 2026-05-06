package com.first.themasses.events.mysterious;

import com.first.themasses.curios.mysterious.XiaoArin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
@SuppressWarnings("removal")
// 小梦樱饰品逻辑处理
@Mod.EventBusSubscriber(modid = "themasses")
public class XiaoArinEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();

        // 狐狸反伤逻辑：仅当持有者攻击狐狸时触发
        if (victim instanceof Fox fox && source.getEntity() instanceof Player attacker) {
            if (hasArin(attacker)) {
                attacker.hurt(fox.damageSources().generic(), event.getAmount() * 10);
            }
        }

        // 生物伤害倍率修正
        if (source.getEntity() instanceof Player player && hasArin(player)) {
            String name = victim.getType().getDescriptionId();
            // 攻击小型动物和鱼类：10倍伤害
            if (name.contains("chicken") || name.contains("rabbit") || name.contains("cod")
                    || name.contains("salmon") || name.contains("tropical_fish") || (name.contains("turtle") && victim.isBaby())) {
                event.setAmount(event.getAmount() * 10);
            }
            // 攻击狼和北极熊：伤害减半
            if (name.contains("wolf") || name.contains("polar_bear")) {
                event.setAmount(event.getAmount() * 0.5f);
            }
        }

        // 共死状态：受到伤害直接致死 (修改：从持久化NBT读取)
        if (victim instanceof Player player && hasArin(player)) {
            CompoundTag persistedData = getPersistedData(player);
            if (persistedData.getInt("Arin_GongSi") > 0) {
                // 这里不再清除，统一在 onLivingDeath 清除
                event.setAmount(1000000f);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        // 被动激怒：靠近 8 格激怒狼和北极熊
        if (!entity.level().isClientSide && entity instanceof Mob mob && entity instanceof NeutralMob neutral) {
            String name = entity.getType().getDescriptionId();
            if (name.contains("wolf") || name.contains("polar_bear")) {
                List<Player> nearbyPlayers = entity.level().getEntitiesOfClass(Player.class, entity.getBoundingBox().inflate(8.0D));
                for (Player player : nearbyPlayers) {
                    if (hasArin(player) && !player.isCreative() && !player.isSpectator()) {
                        if (mob.getTarget() == null && (mob.hasLineOfSight(player) || mob.distanceTo(player) < 3.0D)) {
                            mob.setTarget(player);
                            neutral.setPersistentAngerTarget(player.getUUID());
                            neutral.setRemainingPersistentAngerTime(800);
                            neutral.startPersistentAngerTimer();
                        }
                    }
                }
            }
        }

        // 实体停滞：冻结运动与逻辑
        if (entity.getPersistentData().contains("ArinFrozenTicks")) {
            int ticks = entity.getPersistentData().getInt("ArinFrozenTicks");
            if (ticks > 0) {
                entity.setDeltaMovement(0, 0, 0);
                entity.getPersistentData().putInt("ArinFrozenTicks", ticks - 1);
                if (entity.tickCount % 2 == 0) event.setCanceled(true);
            } else {
                entity.getPersistentData().remove("ArinFrozenTicks");
            }
        }

        //免疫细雪效果
        if (entity instanceof Player player && hasArin(player)) {
            if (player.isInPowderSnow) {
                player.setTicksFrozen(0);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();

        // 狐狸不死图腾：周围有持有者则恢复血量
        if (victim instanceof Fox fox) {
            List<Player> players = fox.level().getEntitiesOfClass(Player.class, fox.getBoundingBox().inflate(8.0D));
            if (players.stream().anyMatch(XiaoArinEvents::hasArin)) {
                event.setCanceled(true);
                fox.setHealth(8.0f);
                fox.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 5));
            }
        }

        // 见证死亡：樱花林内触发共死
        if (!victim.level().isClientSide && !(victim instanceof Player) && isShatura(victim)) {
            findNearbyArinPlayer(victim).ifPresent(player -> handleWitness(player, victim, "Death"));
        }

        // 持有者复活甲逻辑
        if (victim instanceof Player player && hasArin(player)) {
            CompoundTag persistedData = getPersistedData(player);

            // 判定共死：如果正处于共死状态，直接死亡，不触发复活甲，不消耗次数
            if (persistedData.getInt("Arin_GongSi") > 0) {
                persistedData.putInt("Arin_GongSi", 0);
                return; // 允许死亡
            }

            // 判定复活甲次数
            int lives = persistedData.getInt("Arin_Lives");
            if (lives > 0) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth());
                persistedData.putInt("Arin_Lives", lives - 1);
            }
        }
    }

    //免疫甜浆果伤害
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();

        // 在攻击阶段就拦截甜浆果伤害，彻底消除红色闪烁和抖动效果
        if (victim instanceof Player player && hasArin(player)) {
            if (source.is(DamageTypes.SWEET_BERRY_BUSH)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        // 见证诞生：樱花林内触发复活甲
        LivingEntity parent = event.getParentA();
        if (isShatura(parent)) {
            findNearbyArinPlayer(parent).ifPresent(player -> handleWitness(player, parent, "Birth"));
        }
    }

    private static boolean hasArin(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, com.first.themasses.item.ModItems.XIAOARIN.get()).isPresent();
    }

    private static java.util.Optional<Player> findNearbyArinPlayer(LivingEntity entity) {
        return entity.level().getEntitiesOfClass(Player.class, entity.getBoundingBox().inflate(16.0D))
                .stream().filter(XiaoArinEvents::hasArin).findFirst();
    }

    private static boolean isShatura(LivingEntity entity) {
        return entity.level().getBiome(entity.blockPosition()).is(Biomes.CHERRY_GROVE);
    }

    // 辅助方法：获取跨死亡持久化的 NBT
    private static CompoundTag getPersistedData(Player player) {
        CompoundTag forgeData = player.getPersistentData();
        if (!forgeData.contains(Player.PERSISTED_NBT_TAG)) {
            forgeData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return forgeData.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static void handleWitness(Player player, LivingEntity entity, String type) {
        // 1. 获取跨死亡持久化的 NBT 标签
        CompoundTag persistedData = getPersistedData(player);

        // 2. 构造唯一键：记录这个生物是否被见证过
        String entityKey = "Arin_Witnessed_" + entity.getType().getDescriptionId() + "_" + type;

        // 3. 检查是否已经见证过（由于存放在 PERSISTED_NBT_TAG，死后也不会消失）
        if (!persistedData.getBoolean(entityKey)) {
            persistedData.putBoolean(entityKey, true);

            if (type.equals("Birth")) {
                // 获得复活甲次数
                int currentLives = persistedData.getInt("Arin_Lives");
                persistedData.putInt("Arin_Lives", currentLives + 1);
            } else if (type.equals("Death")) {
                // 获得共死状态
                persistedData.putInt("Arin_GongSi", 1);
            }
        }
    }
}