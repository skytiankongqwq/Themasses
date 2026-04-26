package com.first.themasses.events.mysterious;

import com.first.themasses.curios.mysterious.SweetToothRelicItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Random;

@Mod.EventBusSubscriber(modid = "themasses")
public class SweetToothEvents {

    // 辅助方法：判断怪物是否处于 5s 休战期
    private static boolean isTamed(LivingEntity entity) {
        return entity.level().getGameTime() < entity.getPersistentData().getLong("SugarTameEnd");
    }

    // 1. 食用奖励
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            if (findRelic(player).isEmpty()) return;
            ItemStack food = event.getItem();
            if (food.is(Items.CAKE) || food.is(Items.COOKIE) || food.is(Items.PUMPKIN_PIE) || food.is(Items.BEETROOT_SOUP)) {
                Random rand = new Random();
                player.giveExperiencePoints(rand.nextInt(1000) + 1);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 800, 1)); // 速度 II
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 2400, 1)); // 跳跃提升 II
            }
        }
    }

    // 2. 核心 Tick 逻辑 (整合了你提供的方法)
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide) return;

        // --- 怪物中立判定 (采用你提供的方法进行强化) ---
        if (entity instanceof Player player && player.tickCount % 10 == 0) {
            if (!findRelic(player).isEmpty()) {
                // 扫描周围 16 格内正在追逐玩家且处于“休战期”的怪物
                player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16.0D)).forEach(mob -> {
                    if (isTamed(mob) && (mob.getTarget() == player || mob.getLastHurtByMob() == player)) {
                        mob.setTarget(null);
                        mob.setLastHurtByMob(null);
                        mob.getNavigation().stop(); // 停止寻路，强制原地发呆
                    }
                });
            }
        }

        // 蜜蜂逻辑：回血 + 协同
        if (entity instanceof Bee bee) {
            Player owner = bee.level().getNearestPlayer(bee, 16.0D);
            if (owner != null && !findRelic(owner).isEmpty()) {
                if (bee.tickCount % 20 == 0) bee.heal(2.0F);
                LivingEntity playerTarget = owner.getLastHurtMob();
                if (playerTarget != null && playerTarget.isAlive() && owner.getLastHurtMobTimestamp() > owner.tickCount - 100) {
                    bee.setTarget(playerTarget);
                }
            }
        }

        // 动物跟随逻辑
        if (entity instanceof Animal animal && animal.tickCount % 10 == 0) {
            if (animal.getTarget() != null) return;
            Player closestPlayer = animal.level().getNearestPlayer(animal, 10.0D);
            if (closestPlayer != null && !findRelic(closestPlayer).isEmpty()) {
                animal.getNavigation().moveTo(closestPlayer, 1.2D);
            }
        }
    }

    // 拦截怪物在休战期试图重新锁定目标
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMonsterChangeTarget(LivingChangeTargetEvent event) {
        if (isTamed(event.getEntity())) {
            event.setNewTarget(null);
            event.setCanceled(true);
        }
    }

    // 3. 右键交互逻辑 (降价、开启休战、繁殖)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        ItemStack relic = findRelic(player);
        if (relic.isEmpty()) return;

        if (!(event.getTarget() instanceof LivingEntity target)) return;

        CompoundTag tag = relic.getOrCreateTag();
        int sugarCount = tag.getInt("SugarCount");

        // A. 交易降价 (所有 AbstractVillager 子类，如流浪商人)
        if (target instanceof AbstractVillager merchant && player.isShiftKeyDown()) {
            if (sugarCount >= 10) {
                tag.putInt("SugarCount", sugarCount - 10);
                for (MerchantOffer offer : merchant.getOffers()) {
                    int originalPrice = offer.getBaseCostA().getCount();
                    int discount = (int)Math.max(1, originalPrice * 0.1);
                    offer.addToSpecialPriceDiff(-discount);
                }
                player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.PLAYERS, 1.0F, 1.0F);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }

        // B. 怪物消失敌意 (5s 效果, 10s CD)
        if (target instanceof Monster monster) {
            long currentTime = player.level().getGameTime();
            long cdEnd = monster.getPersistentData().getLong("SugarTameCD");

            if (sugarCount >= 5 && currentTime >= cdEnd) {
                tag.putInt("SugarCount", sugarCount - 5);

                // 标记休战时间戳
                monster.getPersistentData().putLong("SugarTameEnd", currentTime + 100); // 5秒
                monster.getPersistentData().putLong("SugarTameCD", currentTime + 200);  // 10秒

                // 立即执行一次你提供的方法
                monster.setTarget(null);
                monster.setLastHurtByMob(null);
                monster.getNavigation().stop();

                player.level().playSound(null, target.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);

                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.5, target.getZ(), 5, 0.3, 0.3, 0.3, 0.1);
                }

                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }

        // C. 动物繁殖
        if (target instanceof Animal animal) {
            if (sugarCount >= 1 && !animal.isInLove() && animal.getAge() == 0) {
                tag.putInt("SugarCount", sugarCount - 1);
                animal.setInLove(player);
                player.level().broadcastEntityEvent(animal, (byte)18);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    public static ItemStack findRelic(Player player) {
        if (player == null) return ItemStack.EMPTY;
        try {
            var curioOpt = CuriosApi.getCuriosHelper().findFirstCurio(player, s -> s.getItem() instanceof SweetToothRelicItem);
            if (curioOpt.isPresent()) return curioOpt.get().stack();
        } catch (Exception ignored) {}

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof SweetToothRelicItem) return stack;
        }
        return ItemStack.EMPTY;
    }
}