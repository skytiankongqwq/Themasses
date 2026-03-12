package com.first.themasses.events.mysterious;

import com.first.themasses.TheMasses;
import com.first.themasses.curios.mysterious.KunZhong;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;
import java.util.UUID;

/**
 * 坤钟事件处理类
 * 负责：杀敌计数、伤害加成、成就监听、掉落强化、技能恢复限制
 */
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID)
public class KunZhongEvents {

    // 在类开头定义好 UUID (确保和 KunZhong.java 里的 SKILL_UUID 一致，或者直接用这里的)
    private static final UUID SKILL_SPEED_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID SKILL_DAMAGE_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567891");
    private static final UUID SKILL_ATK_SPEED_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567892");

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务端处理，且在 Tick 结束阶段
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        findKunZhong(player).ifPresent(stack -> {
            CompoundTag nbt = stack.getOrCreateTag();
            boolean isSkillActive = nbt.getBoolean("SkillActive");

            AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            AttributeInstance damageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
            AttributeInstance atkSpeedAttr = player.getAttribute(Attributes.ATTACK_SPEED);

            if (isSkillActive) {
                int roll = nbt.getInt("SkillRoll");
                if (roll < 1) roll = 1;

                // 1. 移动速度: roll * 10 (1000% ~ 10000%)
                updateModifier(speedAttr, SKILL_SPEED_ID, "KunZhongSkillSpeed", roll * 10.0, AttributeModifier.Operation.MULTIPLY_TOTAL);

                // 2. 攻击力: roll * 20
                updateModifier(damageAttr, SKILL_DAMAGE_ID, "KunZhongSkillDamage", roll * 20.0, AttributeModifier.Operation.ADDITION);

                // 3. 攻速: roll * 1.0 (100% ~ 1000%)
                updateModifier(atkSpeedAttr, SKILL_ATK_SPEED_ID, "KunZhongSkillAtkSpeed", roll * 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
            } else {
                // 技能关闭时，立即移除修改器
                if (speedAttr != null && speedAttr.getModifier(SKILL_SPEED_ID) != null) speedAttr.removeModifier(SKILL_SPEED_ID);
                if (damageAttr != null && damageAttr.getModifier(SKILL_DAMAGE_ID) != null) damageAttr.removeModifier(SKILL_DAMAGE_ID);
                if (atkSpeedAttr != null && atkSpeedAttr.getModifier(SKILL_ATK_SPEED_ID) != null) atkSpeedAttr.removeModifier(SKILL_ATK_SPEED_ID);
            }
        });
    }

    // --- 3. 成就监听：每获得一个成就增加 2% 随机属性系数 (摘下饰品则重置) ---
    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent event) {
        // 这里的 event.getEntity() 在新版本 Forge 中直接返回 Player
        Player player = event.getEntity();
        findKunZhong(player).ifPresent(stack -> {
            CompoundTag nbt = stack.getOrCreateTag();
            nbt.putFloat("AdvancementBonus", nbt.getFloat("AdvancementBonus") + 0.02f);
        });
    }

    // --- 4. 探索判定：50% 概率增加抢夺和时运等级 (摘下饰品则重置) ---
    @SubscribeEvent
    public static void onLooting(LootingLevelEvent event) {
        if (event.getDamageSource() != null && event.getDamageSource().getEntity() instanceof Player player) {
            findKunZhong(player).ifPresent(stack -> {
                // 50% 几率额外增加 2 级抢夺
                if (player.getRandom().nextFloat() < 0.5f) {
                    event.setLootingLevel(event.getLootingLevel() + 2);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        findKunZhong(player).ifPresent(stack -> {
            // 50% 几率判定，此处逻辑通常配合 LootTable，但可在 Break 阶段模拟时运提升
            if (player.getRandom().nextFloat() < 0.5f) {
                // 实际掉落增加建议在 LootTableEvent 或通过给工具添加临时魔法实现
            }
        });
    }

    // --- 5. 杀敌计数与阶梯最大生命值百分比伤害 (摘下饰品则重置) ---
    @SubscribeEvent
    public static void onEntityKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            findKunZhong(player).ifPresent(stack -> {
                CompoundTag nbt = stack.getOrCreateTag();
                // 使用 KillTracker 复合标签存储数据
                CompoundTag tracker = nbt.getCompound("KillTracker");

                String entityId = event.getEntity().getType().getDescriptionId();
                int currentKills = tracker.getInt(entityId);
                tracker.putInt(entityId, currentKills + 1);

                nbt.put("KillTracker", tracker);
            });
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 1. 攻击者逻辑 (玩家打怪)
        if (event.getSource().getEntity() instanceof Player player) {
            findKunZhong(player).ifPresent(stack -> {
                CompoundTag nbt = stack.getOrCreateTag();

                // --- [逻辑 A] 杀敌计数阶梯伤害 ---
                if (nbt.contains("KillTracker")) {
                    CompoundTag tracker = nbt.getCompound("KillTracker");
                    LivingEntity target = event.getEntity();
                    String entityId = target.getType().getDescriptionId();
                    int kills = tracker.getInt(entityId);

                    if (kills > 0) {
                        float maxHealth = target.getMaxHealth();
                        float extraDamage = 0;
                        if (kills >= 10000) { extraDamage = maxHealth * 1.0f; }
                        else if (kills >= 100) { extraDamage = maxHealth * 0.1f; }
                        else if (kills >= 1) { extraDamage = maxHealth * 0.01f; }

                        if (extraDamage > 0) {
                            event.setAmount(event.getAmount() + extraDamage);
                        }
                    }
                }

                // --- [奔跑判定增伤版] 逻辑 B ---
                if (nbt.getBoolean("SkillActive")) {
                    // 1. 判定玩家是否正在奔跑 (Sprinting)
                    if (player.isSprinting()) {

                        int roll = nbt.getInt("SkillRoll");
                        if (roll < 1) roll = 1;

                        // 2. 获取你技能提供的“百倍速度”属性
                        // 既然你 KunZhong.java 里给了 1000%~10000% 的速度，这里拿到的值会非常大（1.0 ~ 10.0）
                        double speedAttr = player.getAttributeValue(Attributes.MOVEMENT_SPEED);

                        // 3. 严格执行你的公式: (10% - 100%) * 速度
                        // 我们把 speedAttr 当作速度基数。
                        // 倍率计算 = (roll * 0.1) * speedAttr
                        double damageMultiplier = (roll * 0.1) * speedAttr;

                        float currentAmount = event.getAmount();
                        float bonus = (float) (currentAmount * damageMultiplier);

                        if (bonus > 0) {
                            event.setAmount(currentAmount + bonus);
                        }
                    }
                }
            });
        }
        // 2. 被攻击者逻辑 (玩家摔落免疫)
        if (event.getEntity() instanceof Player victim && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            findKunZhong(victim).ifPresent(stack -> {
                if (stack.getOrCreateTag().getInt("EquippedTicks") >= 648000) {
                    event.setCanceled(true);
                }
            });
        }
    }


    // --- 技能判定：技能期间无法恢复生命值 ---
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player player) {
            findKunZhong(player).ifPresent(stack -> {
                if (stack.getOrCreateTag().getBoolean("SkillActive")) {
                    event.setAmount(0f);
                    event.setCanceled(true);
                }
            });
        }
    }
    //死亡重置
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            // 查找饰品
            CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof KunZhong)
                    .ifPresent(slotResult -> {
                        ItemStack stack = slotResult.stack();
                        CompoundTag nbt = stack.getOrCreateTag();

                        // 1. 死了 CD 重置
                        nbt.putInt("SkillCD", 0);

                        // 2. 强制清理技能运行状态，防止复活后逻辑残留
                        if (nbt.getBoolean("SkillActive")) {
                            nbt.putBoolean("SkillActive", false);
                            nbt.remove("SkillTimer");
                            nbt.remove("SkillDrainedHealth");
                            player.getPersistentData().remove("KunZhongSkillImmuneHeal");
                        }
                    });
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player) {
            // 读取饰品在 curioTick 中实时写入的免疫标记
            if (player.getPersistentData().getBoolean("KunZhongImmune")) {
                MobEffect effect = event.getEffectInstance().getEffect();

                // 判定是否为需要拦截的四个效果
                if (effect == MobEffects.POISON ||
                    effect == MobEffects.HUNGER ||
                    effect == MobEffects.MOVEMENT_SLOWDOWN ||
                    effect == MobEffects.WEAKNESS) {

                    // 正确的 Result 引用：强制拒绝应用效果
                    event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                }
            }
        }
    }

    /**
     * 辅助方法：确保修改器存在且数值正确
     */
    private static void updateModifier(AttributeInstance inst, UUID uuid, String name, double value, AttributeModifier.Operation op) {
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(uuid);

        // 如果数值不对或者不存在，则重置（防止随机数变动属性没更新）
        if (existing == null || existing.getAmount() != value) {
            inst.removeModifier(uuid);
            inst.addTransientModifier(new AttributeModifier(uuid, name, value, op));
        }
    }

    /**
     * 辅助方法：快速获取玩家饰品栏里的坤钟
     */
    private static Optional<ItemStack> findKunZhong(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof KunZhong)
                .map(slotResult -> slotResult.stack());
    }
}