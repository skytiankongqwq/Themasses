package com.first.themasses.events.mysterious;

import com.first.themasses.TheMasses;
import com.first.themasses.curios.mysterious.laowang237;
import com.first.themasses.curios.mysterious.wu1wu2;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID)
public class Laowang237Events {

    private static MobEffect ASTRAL_DRUNKENNESS = null;
    private static boolean SEARCHED_FOR_EFFECT = false;

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        // 只处理服务器端的玩家受伤
        if (victim.level().isClientSide || !(victim instanceof Player player)) return;

        // 1. 检查受伤玩家本人是否佩戴了 laowang237
        CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof laowang237).ifPresent(slot -> {

            float originalDamage = event.getAmount();

            // 2. 寻找半径 16 格内的猪
            List<Pig> nearbyPigs = player.level().getEntitiesOfClass(Pig.class, player.getBoundingBox().inflate(16.0D));

            if (!nearbyPigs.isEmpty()) {
                // 找到最近的一头活猪
                Pig scapegoat = nearbyPigs.stream()
                        .filter(LivingEntity::isAlive)
                        .min((p1, p2) -> Double.compare(p1.distanceToSqr(player), p2.distanceToSqr(player)))
                        .orElse(null);

                if (scapegoat != null) {
                    // --- 核心：拦截对玩家的伤害 ---
                    event.setAmount(0);
                    event.setCanceled(true);

                    // --- 核心：将伤害全额转移给猪 ---
                    // 伤害来源保持一致（如果是摔死的，猪也会显示摔死）
                    scapegoat.hurt(event.getSource(), originalDamage);

                    // 视觉反馈：在玩家和猪之间产生联系感
                    if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        // 猪身上冒黑烟/传送粒子
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                                scapegoat.getX(), scapegoat.getY() + 0.5, scapegoat.getZ(),
                                15, 0.1, 0.2, 0.1, 0.05);
                        // 玩家身上闪烁一些咒术粒子
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                                player.getX(), player.getY() + 1.0, player.getZ(),
                                10, 0.3, 0.5, 0.3, 0.1);
                    }

                    // 猪发出惨叫
                    scapegoat.playSound(net.minecraft.sounds.SoundEvents.PIG_HURT, 1.0F, 0.6F);
                }
            }
        });
    }

    // --- 1. 拦截夜视获得逻辑 ---
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        // 检查是否正在尝试添加夜视
        if (event.getEffectInstance().getEffect() == MobEffects.NIGHT_VISION && entity instanceof Player player) {
            // 检查玩家是否戴了饰品
            CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof laowang237).ifPresent(slot -> {
                MobEffect astralEffect = getAstralEffect();
                if (astralEffect != null) {
                    MobEffectInstance current = player.getEffect(astralEffect);
                    // 如果身上有该 Buff 且等级为 1 (Amplifier 为 0)
                    if (current != null && current.getAmplifier() == 0) {
                        // 核心操作：直接让该效果不可应用（拦截获得）
                        event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                    }
                }
            });
        }
    }

    // --- 2. 常驻 Buff 与 村民逻辑 ---
    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;

        CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof laowang237).ifPresent(slot -> {

            // 每秒处理一次村民
            if (player.tickCount % 20 == 0) {
                applyVillagerLogic(player);
                applyPigAssaultLogic(player);
            }

            // --- 优化后的 Buff 补充逻辑 ---
            if (player.tickCount % 20 == 5) {
                MobEffect astralEffect = getAstralEffect();
                if (astralEffect != null) {
                    MobEffectInstance current = player.getEffect(astralEffect);

                    // 判定条件：
                    // 1. 玩家完全没有这个效果
                    // 2. 或者 玩家身上只有 1 级效果 (Amplifier == 0) 且持续时间快到了 (小于 10 秒/200 ticks)
                    if (current == null || (current.getAmplifier() == 0 && current.getDuration() < 200)) {
                        // 补充 1 级效果，持续 20 秒
                        player.addEffect(new MobEffectInstance(astralEffect, 400, 0, false, false, true));
                    }

                    // 如果玩家身上已经是 2 级 (Amplifier >= 1)，逻辑会跳过，
                    // 这样你手动喝药水或通过其他方式提升的等级就不会被这个 1 级常驻给“挡住”或“重置”。
                }
            }
        });
    }
    /**
     * 附近的猪会获得 Buff 并攻击玩家最后攻击的目标
     */
    private static void applyPigAssaultLogic(Player player) {
        List<Pig> pigs = player.level().getEntitiesOfClass(Pig.class, player.getBoundingBox().inflate(16.0D));
        LivingEntity combatTarget = player.getLastHurtMob();

        if (combatTarget == null || !combatTarget.isAlive() || combatTarget.distanceTo(player) > 20) {
            combatTarget = player.getLastHurtByMob();
        }

        for (Pig pig : pigs) {
            // 1. 移除胆怯 AI
            pig.goalSelector.getAvailableGoals().removeIf(wrapped ->
                    wrapped.getGoal() instanceof net.minecraft.world.entity.ai.goal.PanicGoal
            );

            pig.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false));

            if (combatTarget != null && combatTarget.isAlive() && combatTarget != pig) {
                pig.setTarget(combatTarget);
                pig.getLookControl().setLookAt(combatTarget, 60.0F, 60.0F);

                double distSqr = pig.distanceToSqr(combatTarget);

                // --- 优化：冲锋加速 ---
                if (distSqr > 0.8D) { // 只要没贴脸，就维持高航速
                    pig.getNavigation().moveTo(combatTarget, 1.8D);
                }

                // --- 核心修复：改用 AABB 碰撞检测替代单纯的距离检测 ---
                // inflate(0.5) 相当于给猪的身体外围包了一层 0.5 格的感应区
                if (pig.getBoundingBox().inflate(0.5D).intersects(combatTarget.getBoundingBox())) {

                    // 提高攻击频率，从每 8 tick 改为每 5 tick (0.25秒)，防止擦肩而过没触发
                    if (pig.tickCount % 5 == 0) {
                        float damageAmount = 6.0F; // 稍微调高基础伤害
                        MobEffectInstance str = pig.getEffect(MobEffects.DAMAGE_BOOST);
                        if (str != null) damageAmount += (str.getAmplifier() + 1) * 2.5F;

                        // 强制执行伤害（来源为玩家）
                        // 如果普通攻击还是打不中，改用 generic 并在名称里标注玩家，或者直接用 playerAttack
                        boolean hurtSuccess = combatTarget.hurt(player.damageSources().playerAttack(player), damageAmount);

                        if (hurtSuccess) {
                            // 表现增强：撞击时产生一个小冲量，让猪反弹一点点，防止重叠导致的判定失效
                            net.minecraft.world.phys.Vec3 knockVec = pig.position().subtract(combatTarget.position()).normalize().scale(0.2D);
                            pig.setDeltaMovement(knockVec.x, 0.4D, knockVec.z);

                            pig.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                            pig.playSound(net.minecraft.sounds.SoundEvents.PIG_HURT, 1.0F, 1.5F);

                            // 强力击退目标
                            combatTarget.knockback(0.6F, pig.getX() - combatTarget.getX(), pig.getZ() - combatTarget.getZ());

                            // 极大幅度缩短无敌帧（关键：解决群猪围攻不掉血）
                            if (combatTarget.invulnerableTime > 2) {
                                combatTarget.invulnerableTime = 2;
                            }
                        }
                    }
                }
            } else {
                if (pig.distanceTo(player) > 6.0D) {
                    pig.getNavigation().moveTo(player, 1.3D);
                }
            }
        }
    }

    // 获取动态效果（不产生硬前置）
    private static MobEffect getAstralEffect() {
        if (!SEARCHED_FOR_EFFECT) {
            ASTRAL_DRUNKENNESS = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("enigmaticdelicacy", "astral_drunkenness"));
            SEARCHED_FOR_EFFECT = true;
        }
        return ASTRAL_DRUNKENNESS;
    }

    private static void applyVillagerLogic(Player player) {
        List<Villager> villagers = player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(12.0D));
        for (Villager villager : villagers) {
            for (MerchantOffer offer : villager.getOffers()) {
                offer.resetUses();
                try {
                    ObfuscationReflectionHelper.setPrivateValue(MerchantOffer.class, offer, 99999, "maxUses");
                } catch (Exception ignored) {}

                int baseCount = offer.getBaseCostA().getCount();
                if (baseCount > 1) {
                    int targetCount = Math.max(1, (int) Math.ceil(baseCount * 0.8));
                    offer.setSpecialPriceDiff(targetCount - baseCount);
                } else {
                    offer.setSpecialPriceDiff(0);
                }
            }
        }
    }
}