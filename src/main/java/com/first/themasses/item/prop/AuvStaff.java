package com.first.themasses.item.prop;
//auv法杖（聚形之杖)
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
//对着生物右键可以将它变成幼年体（如果是幼年体则永远不会变大）
//（对着僵尸右键有1%概率将它变成巨人僵尸，生命和伤害为原先10000倍，在不收到伤害的5s后自动消失）
//对同一个生物只能用1次变成可以用三次，属性加成分别为变成初始的5倍，15倍，30倍
//第一次使用只需要右键一次，cd为5s，第二次使用需要右键两次，cd为20s和30s，同时给予他永久的力量2
//第三次使用需要右键4次，cd为60s，120s，720s和1s，同时在完成了第三次对生物的变化之后，给这个生物永久的生命恢复3
//如果对象为玩家，cd时间不变，但属性加成变为初始的2倍，3倍，5倍，buff变成力量1和生命恢复1
//攻击产生掉落物第一次使用为10%掉落，第二次为30%，第四次为70%，如果是带有boss标签的生物掉落概率缩减为原先的一半，也就是10%变成5%这样，30%变成15%，70%变成35%
//cd是计算在生物身上而不是法杖，法杖cd只有5s

public class AuvStaff extends Item {
    public AuvStaff(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
        if (!player.level().isClientSide && player.level() instanceof ServerLevel serverLevel) {

            // --- 1. 生物冷却检查 (核心修改) ---
            long currentTime = serverLevel.getGameTime();
            long cooldownEnd = target.getPersistentData().getLong("auv_cooldown_end");

            if (currentTime < cooldownEnd) {
                // 如果还在冷却中，给玩家发送一个提示信息
                long remainingTicks = cooldownEnd - currentTime;
                player.displayClientMessage(Component.literal("§c该生物正在进化适应中... (剩余 " + (remainingTicks / 20) + "秒)"), true);
                return InteractionResult.FAIL;
            }

            // --- 2. 特殊逻辑：1% 概率将僵尸变成超级巨人僵尸 ---
            if (target instanceof Zombie && !(target instanceof Giant)) {
                if (player.getRandom().nextFloat() < 0.01F) {
                    if (transformToGiant(serverLevel, (Zombie) target)) {
                        return InteractionResult.SUCCESS;
                    }
                }
            }

            // --- 3. 多阶段进化逻辑 ---
            int stage = target.getPersistentData().getInt("auv_staff_stage");
            int clickCount = target.getPersistentData().getInt("auv_staff_clicks");

            if (stage >= 3) return InteractionResult.FAIL;

            clickCount++;
            target.getPersistentData().putInt("auv_staff_clicks", clickCount);

            int cdTicks = 100; // 默认 5s

            // 阶段 0 -> 1
            if (stage == 0) {
                applyUpgrade(target, 1);
                target.getPersistentData().putInt("auv_staff_stage", 1);
                target.getPersistentData().putInt("auv_staff_clicks", 0);
                cdTicks = 100;
                spawnUpgradeEffects(serverLevel, target, 1);
            }
            // 阶段 1 -> 2
            else if (stage == 1) {
                if (clickCount == 1) {
                    cdTicks = 400; // 20s
                } else {
                    applyUpgrade(target, 2);
                    target.getPersistentData().putInt("auv_staff_stage", 2);
                    target.getPersistentData().putInt("auv_staff_clicks", 0);
                    cdTicks = 600; // 30s
                    spawnUpgradeEffects(serverLevel, target, 2);
                }
            }
            // 阶段 2 -> 3
            else if (stage == 2) {
                if (clickCount == 1) cdTicks = 1200;  // 60s
                else if (clickCount == 2) cdTicks = 2400; // 120s
                else if (clickCount == 3) cdTicks = 14400; // 720s
                else {
                    applyUpgrade(target, 3);
                    target.getPersistentData().putInt("auv_staff_stage", 3);
                    target.getPersistentData().putInt("auv_staff_clicks", 0);
                    cdTicks = 20; // 1s
                    spawnUpgradeEffects(serverLevel, target, 3);
                }
            }

            // --- 4. 设置生物身上的冷却截止时间 ---
            target.getPersistentData().putLong("auv_cooldown_end", currentTime + cdTicks);
            player.getCooldowns().addCooldown(this, 100);

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    public static void applyUpgrade(LivingEntity target, int toStage) {
        boolean isPlayer = target instanceof Player;
        AttributeInstance hpAttr = target.getAttribute(Attributes.MAX_HEALTH);

        // 初始化/记录原始血量
        if (!target.getPersistentData().contains("auv_original_hp") && hpAttr != null) {
            target.getPersistentData().putDouble("auv_original_hp", hpAttr.getBaseValue());
        }
        double originalHp = target.getPersistentData().getDouble("auv_original_hp");

        // 1. 属性倍率
        if (hpAttr != null) {
            double multiplier = 1.0;
            if (toStage == 1) multiplier = isPlayer ? 2.0 : 5.0;
            else if (toStage == 2) multiplier = isPlayer ? 3.0 : 15.0;
            else if (toStage == 3) multiplier = isPlayer ? 5.0 : 30.0;
            else multiplier = 1.0; // Stage 0

            hpAttr.setBaseValue(originalHp * multiplier);
            // 只有当血量调高时才回满，防止掉级时强制回满血
            if (target.getHealth() < (float)(originalHp * multiplier)) {
                target.setHealth((float) hpAttr.getBaseValue());
            }
        }

        // 2. Buff 管理 (先清除旧的，再加新的)
        target.removeEffect(MobEffects.DAMAGE_BOOST);
        target.removeEffect(MobEffects.REGENERATION);
        if (toStage == 2) {
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, isPlayer ? 0 : 1, false, false));
        } else if (toStage == 3) {
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, isPlayer ? 0 : 1, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, isPlayer ? 0 : 2, false, false));
        }

        // 3. 掉落率设定
        float dropChance = (toStage == 1) ? 0.1F : (toStage == 2 ? 0.3F : 0.7F);
        if (toStage <= 0) dropChance = 0.0F;
        target.getPersistentData().putFloat("auv_drop_chance", dropChance);
        target.getPersistentData().putBoolean("auv_staff_affected", toStage > 0);

        // 4. 幼年锁定 (仅限非玩家)
        if (!isPlayer) {
            if (target instanceof AgeableMob ageable) {
                ageable.setAge(-24000);
                target.getPersistentData().putBoolean("auv_age_locked", true);
            } else if (target instanceof Zombie zombie) {
                zombie.setBaby(true);
                target.getPersistentData().putBoolean("auv_age_locked", true);
            }
        }
    }

    private boolean transformToGiant(ServerLevel level, Zombie target) {
        Giant giant = EntityType.GIANT.create(level);
        if (giant != null) {
            giant.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            AttributeInstance hp = giant.getAttribute(Attributes.MAX_HEALTH);
            AttributeInstance dmg = giant.getAttribute(Attributes.ATTACK_DAMAGE);
            if (hp != null && dmg != null) {
                hp.setBaseValue(hp.getBaseValue() * 10000);
                dmg.setBaseValue(dmg.getBaseValue() * 10000);
                giant.setHealth((float) hp.getBaseValue());
            }
            giant.getPersistentData().putBoolean("auv_is_giant", true);
            giant.getPersistentData().putInt("auv_giant_timer", 100);
            giant.getPersistentData().putBoolean("auv_staff_affected", true);
            giant.getPersistentData().putFloat("auv_drop_chance", 0.7F);
            level.addFreshEntity(giant);
            target.discard();
            spawnUpgradeEffects(level, giant, 3);
            return true;
        }
        return false;
    }

    private void spawnUpgradeEffects(ServerLevel level, LivingEntity entity, int stage) {
        if (stage == 3) {
            EntityType.LIGHTNING_BOLT.spawn(level, (ItemStack) null, null, entity.blockPosition(),
                    net.minecraft.world.entity.MobSpawnType.TRIGGERED, true, true);
        }
        level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1, entity.getZ(), stage * 50, 0.5, 0.5, 0.5, 0.2);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.themasses.auvstaff.line1").withStyle(Style.EMPTY.withColor(0x9933FF)));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}