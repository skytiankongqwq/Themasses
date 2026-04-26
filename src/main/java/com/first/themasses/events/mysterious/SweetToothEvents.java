package com.first.themasses.events.mysterious;

import com.first.themasses.curios.mysterious.SweetToothRelicItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
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

    // 1. 食用奖励逻辑
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            if (findRelic(player).isEmpty()) return;
            ItemStack food = event.getItem();
            // 判定特定食物
            if (food.is(Items.CAKE) || food.is(Items.COOKIE) || food.is(Items.PUMPKIN_PIE) || food.is(Items.BEETROOT_SOUP)) {
                Random rand = new Random();
                player.giveExperiencePoints(rand.nextInt(1000) + 1);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 800, 1)); // 速度 II (40秒)
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 2400, 1)); // 跳跃提升 II (120秒)
            }
        }
    }

    // 2. 核心 Tick 逻辑：蜜蜂协同、动物跟随、怪物休战
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide) return;

        // A. 蜜蜂逻辑：回血 + 协同攻击
        if (entity instanceof Bee bee) {
            Player owner = bee.level().getNearestPlayer(bee, 16.0D);
            if (owner != null && !findRelic(owner).isEmpty()) {
                // 每秒回 2hp
                if (bee.tickCount % 20 == 0) bee.heal(2.0F);

                // 协同攻击：锁定玩家最后攻击的对象
                LivingEntity playerTarget = owner.getLastHurtMob();
                if (playerTarget != null && playerTarget.isAlive() && owner.getLastHurtMobTimestamp() > owner.tickCount - 20) {
                    bee.setRemainingPersistentAngerTime(100);
                    bee.setPersistentAngerTarget(playerTarget.getUUID());
                    bee.setTarget(playerTarget);
                }
            }
        }

        // B. 动物跟随逻辑
        if (entity instanceof Animal animal && animal.tickCount % 10 == 0) {
            Player closestPlayer = animal.level().getNearestPlayer(animal, 10.0D);
            if (closestPlayer != null && !findRelic(closestPlayer).isEmpty()) {
                animal.getLookControl().setLookAt(closestPlayer, 10.0F, (float)animal.getMaxHeadXRot());
                if (animal.distanceToSqr(closestPlayer) > 4.0D) {
                    animal.getNavigation().moveTo(closestPlayer, 1.2D);
                }
            }
        }

        // C. 怪物休战判定
        if (entity instanceof Monster monster) {
            if (monster.level().getGameTime() < monster.getPersistentData().getLong("SugarTameEnd")) {
                monster.setTarget(null);
            }
        }
    }

    // 强制拦截怪物目标转换（休战期间）
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMonsterChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Monster monster) {
            if (monster.level().getGameTime() < monster.getPersistentData().getLong("SugarTameEnd")) {
                event.setCanceled(true);
            }
        }
    }

    // 3. 右键交互逻辑：消耗饰品内的糖进行操作
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        ItemStack relic = findRelic(player);
        if (relic.isEmpty()) return;

        CompoundTag tag = relic.getOrCreateTag();
        int sugarCount = tag.getInt("SugarCount");
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        // A. 村民降价 (必须蹲下 Shift + 右键，消耗 10 糖)
        if (target instanceof Villager villager && player.isShiftKeyDown()) {
            if (sugarCount >= 10) {
                tag.putInt("SugarCount", sugarCount - 10);
                for (MerchantOffer offer : villager.getOffers()) {
                    int originalPrice = offer.getBaseCostA().getCount();
                    // 降低 10%
                    offer.addToSpecialPriceDiff(-(int)Math.max(1, originalPrice * 0.1));
                }
                player.level().playSound(null, player.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.PLAYERS, 1.0F, 1.0F);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return; // 处理完村民逻辑直接返回，防止触发后续动物/怪物逻辑
        }

        // B. 怪物消失敌意 (右键，消耗 5 糖)
        if (target instanceof Monster monster && !player.isShiftKeyDown()) {
            long currentTime = player.level().getGameTime();
            long cdEnd = monster.getPersistentData().getLong("SugarTameCD");

            if (sugarCount >= 5 && currentTime >= cdEnd) {
                tag.putInt("SugarCount", sugarCount - 5);
                monster.setTarget(null);
                // 效果 5s，CD 10s
                monster.getPersistentData().putLong("SugarTameEnd", currentTime + 100);
                monster.getPersistentData().putLong("SugarTameCD", currentTime + 200);

                player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }

        // C. 动物繁殖 (右键，消耗 1 糖)
        else if (target instanceof Animal animal && !player.isShiftKeyDown()) {
            if (sugarCount >= 1 && !animal.isInLove()) {
                tag.putInt("SugarCount", sugarCount - 1);
                animal.setInLove(player);
                player.level().broadcastEntityEvent(animal, (byte)18);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    // 通用饰品查找方法
    public static ItemStack findRelic(Player player) {
        if (player == null) return ItemStack.EMPTY;
        // 优先检查背包
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof SweetToothRelicItem) return stack;
        }
        // 检查 Curios 槽位
        try {
            return CuriosApi.getCuriosHelper().findFirstCurio(player, s -> s.getItem() instanceof SweetToothRelicItem)
                    .map(res -> res.stack()).orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}