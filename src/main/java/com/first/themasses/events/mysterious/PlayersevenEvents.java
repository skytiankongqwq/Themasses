package com.first.themasses.events.mysterious;
//p7饰品事件
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "themasses")
public class PlayersevenEvents {

    private static final UUID BOSS_EVOLUTION_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    // 修正 Tag 引用
    private static final TagKey<EntityType<?>> BOSS_TAG = Tags.EntityTypes.BOSSES;

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.getPersistentData().getInt("PlayerSevenActive") <= 0) return;

            // 1. 无视无敌帧
            event.getEntity().invulnerableTime = 0;

            float damage = event.getAmount();
            // 5. 剑类 50% 加成
            if (player.getMainHandItem().getItem() instanceof SwordItem) damage *= 1.5f;

            //满血和1滴血的时候增伤
            float currentHealth = player.getHealth();
            float maxHealth = player.getMaxHealth();
            float berserkMult = 1.0f; // 默认倍率

            if (currentHealth >= maxHealth) {
                // 满血状态：伤害 * 2
                berserkMult = 2.0f;
            } else if (currentHealth <= 1.0f || (currentHealth / maxHealth) <= 0.01f) {
                // 1滴血或血量低于1%：伤害 * 7
                berserkMult = 7.0f;
            }
            damage *= berserkMult;

            // 4. Boss 连击逻辑
            if (event.getEntity().getType().is(BOSS_TAG)) {
                // 从被攻击的 Boss 身上获取连击数
                int combo = event.getEntity().getPersistentData().getInt("P7_BossCombo");
                damage *= (1.0f + (0.1f * combo));

                // 增加该 Boss 的连击数
                event.getEntity().getPersistentData().putInt("P7_BossCombo", combo + 1);
            }

            event.setAmount(damage);
        }

        // --- 逻辑 B：玩家被攻击（重置对应 Boss 的连击） ---
        // 如果受击者是玩家，且攻击者是带有 Boss 标签的生物
        if (event.getEntity() instanceof Player player && event.getSource().getEntity() instanceof LivingEntity attacker) {
            if (attacker.getType().is(BOSS_TAG)) {
                // 精准重置：只清除“打了玩家的那个 Boss”的连击数
                attacker.getPersistentData().putInt("P7_BossCombo", 0);

                // 可选：添加一个粒子效果或提示，反馈连击已重置
                player.displayClientMessage(Component.literal("§c连击已中断！"), true);
            }
        }
    }

    // --- 2. 正午之炎：核心逻辑重写 ---
    @SubscribeEvent
    public static void onCombatEffect(LivingHurtEvent event) {
        // A. 处理攻击者（施加着火标记）
        if (event.getSource().getEntity() instanceof Player player) {
            int fireTicks = player.getPersistentData().getInt("P7_NoonFire");
            if (fireTicks > 0) {
                LivingEntity target = event.getEntity();

                // 1. 强制设置原版着火（渲染外观）
                target.setSecondsOnFire(48);

                // 2. 注入“劫火”标记：剩余伤害次数(48秒共计48次)，以及伤害来源
                CompoundTag targetData = target.getPersistentData();
                targetData.putInt("P7_BurnTicks", 48); // 持续48次伤害
                targetData.putUUID("P7_BurnSource", player.getUUID());

                // 每次攻击消耗正午之炎的持续时间
                player.getPersistentData().putInt("P7_NoonFire", Math.max(0, fireTicks - 20));
            }
        }

        // B. 处理火焰伤害增强（确保每秒烧10点）
        // 判定伤害源是否为火
        if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)) {

            if (event.getEntity().getPersistentData().contains("P7_BurnTicks")) {
                event.setAmount(10.0f); // 强行设置为10点伤害
            }
        }
    }

    // C. 强制着火兜底逻辑（防止某些实体免疫着火）
    @SubscribeEvent
    public static void onLivingUpdate(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        CompoundTag data = entity.getPersistentData();

        if (data.contains("P7_BurnTicks")) {
            int remaining = data.getInt("P7_BurnTicks");

            // 每秒执行一次 (20 ticks)
            if (entity.tickCount % 20 == 0) {
                if (remaining > 0) {
                    // 即使 entity.setSecondsOnFire 失效，这里也会强制扣血
                    entity.hurt(entity.damageSources().onFire(), 10.0f);
                    entity.setRemainingFireTicks(40); // 强行维持外观火焰
                    data.putInt("P7_BurnTicks", remaining - 1);
                } else {
                    data.remove("P7_BurnTicks");
                    data.remove("P7_BurnSource");
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.player.level().isClientSide) return;
        Player player = event.player;

        int activeTicks = player.getPersistentData().getInt("PlayerSevenActive");
        if (activeTicks <= 0) return;
        player.getPersistentData().putInt("PlayerSevenActive", activeTicks - 1);

        // 服务端同步标记：确保认可客户端的瞬移位置
        if (player.getMainHandItem().is(Items.STONE_SWORD)) {
            if (player.getDeltaMovement().horizontalDistance() > 0.01) {
                player.hurtMarked = true;
            }
        }

        // 6. 原石回血
        if (player.level().getBlockState(player.blockPosition().below()).is(Blocks.COBBLESTONE)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 2, false, false));
        }
    }

    @SubscribeEvent
    public static void onBossKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player && event.getEntity().getType().is(BOSS_TAG)) {
            if (player.getPersistentData().getInt("PlayerSevenActive") <= 0) return;

            String entityKey = "P7_Killed_" + event.getEntity().getType().getDescriptionId();
            if (!player.getPersistentData().getBoolean(entityKey)) {
                evolvePlayer(player);
                player.getPersistentData().putBoolean(entityKey, true);
                player.sendSystemMessage(Component.literal("§6§l宿敌已逝，进化开始..."));
            }
        }
    }

    private static void evolvePlayer(Player player) {
        applyAttr(player, Attributes.MAX_HEALTH, 0.5);
        applyAttr(player, Attributes.ATTACK_DAMAGE, 0.5);
        applyAttr(player, Attributes.ARMOR, 0.5);
        player.heal(player.getMaxHealth());
    }

    private static void applyAttr(Player player, net.minecraft.world.entity.ai.attributes.Attribute attr, double val) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null) {
            double currentMod = 0;
            if (inst.getModifier(BOSS_EVOLUTION_ID) != null) currentMod = inst.getModifier(BOSS_EVOLUTION_ID).getAmount();
            inst.removeModifier(BOSS_EVOLUTION_ID);
            inst.addPermanentModifier(new AttributeModifier(BOSS_EVOLUTION_ID, "P7 Evolution", currentMod + val, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }
}