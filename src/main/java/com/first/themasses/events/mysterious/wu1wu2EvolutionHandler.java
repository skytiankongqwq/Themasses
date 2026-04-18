package com.first.themasses.events.mysterious;

import com.first.themasses.item.ModItems;
import com.first.themasses.util.SearchServant;
import com.first.themasses.util.wu1wu2DataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "themasses")
public class wu1wu2EvolutionHandler {

    private static final String ENHANCED_TAG = "SentientEnhanced";
    private static final double MAX_HEALTH_LIMIT = 5152520.0;

    // 专属的 UUID 修饰符，用于保证无论怎么刷新都不叠加、不丢失
    private static final UUID SERVANT_HP_MOD_UUID = UUID.fromString("f42a781b-5e6a-4b9d-8c12-3a4b5c6d7e8f");
    private static final UUID SERVANT_DMG_MOD_UUID = UUID.fromString("a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d");
    private static final UUID SERVANT_ARMOR_MOD_UUID = UUID.fromString("11223344-5566-7788-99aa-bbccddeeff00");

    // 用于随时开关仆从转化功能
    public static boolean isServantConversionEnabled = true;

    /**
     * 处理来自 PacketAbilityKey 的数据包
     * 请在你的 Packet 处理类中调用此方法
     */
    public static void handleAbilityPacket(ServerPlayer player, int entityId) {
        if (entityId == -51) {
            isServantConversionEnabled = !isServantConversionEnabled;
            String msg = isServantConversionEnabled ? "§a仆从转化：已开启" : "§c仆从转化：已关闭";
            player.displayClientMessage(Component.literal(msg), true);
        }
    }

    // 计算属性倍率：根据 N 值获取 Log2 的增长曲线
    private static double getLog2Multiplier(int n) {
        return 0.1 * n + 0.005 * n * Math.log10(n + 1);
    }

    @SubscribeEvent
    public static void onCombat(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();

        // 1. 玩家攻击敌人时，造成额外伤害并挂上易伤 Tag
        if (source instanceof Player attacker && hasUnity(attacker)) {
            int n = wu1wu2DataManager.getPermanentN(attacker);
            if (n > 0) {
                float multiplier = (float) getLog2Multiplier(n);
                event.setAmount(event.getAmount() * multiplier);
                applyEnemyDebuff(victim, n);
                victim.getPersistentData().putInt("SentientVulnerability", n);
            }
        }

        // 2. 玩家受击时，根据增长曲线提供减伤
        if (victim instanceof Player player && hasUnity(player)) {
            int n = wu1wu2DataManager.getPermanentN(player);
            if (n > 0) {
                double damageReduction = getLog2Multiplier(n);
                event.setAmount((float) (event.getAmount() / (1.0 + damageReduction)));
            }
        }

        // 3. 敌人身上有易伤 Tag 时，放大其受到的最终伤害
        if (victim.getPersistentData().contains("SentientVulnerability")) {
            int n = victim.getPersistentData().getInt("SentientVulnerability");
            double vulnerability = getLog2Multiplier(n);
            event.setAmount((float) (event.getAmount() * (1.0 + vulnerability)));
        }
    }

    @SubscribeEvent
    public static void onServantUpdate(TickEvent.PlayerTickEvent event) {
        // 确保在服务端运行且在 Tick 结束阶段执行，避免重复运算
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.player;

            if (hasUnity(player)) {
                // 每 40 tick (2秒) 刷新一次玩家和仆从的状态
                if (player.level().getGameTime() % 40 == 0) {
                    int n = wu1wu2DataManager.getPermanentN(player);
                    if (n > 0) {
                        double multiplier = getLog2Multiplier(n);

                        // 1. 玩家自身属性更新 (始终执行，保证倍率随 n 实时变化)
                        updatePlayerAttributes(player, multiplier, n);

                        // 2. 强化仆从逻辑
                        // 如果开关关闭，不再寻找新生物转化，但已存在的仆从仍需维持生命上限，否则会血崩
                        // 因此去掉 isServantConversionEnabled 的拦截，让现存仆从的属性得以稳固
                        if (player.level() instanceof ServerLevel serverLevel) {
                            SearchServant.getEnhancedServantData(player).ifPresent(data -> {
                                data.servantDetails.keySet().forEach(uuid -> {
                                    Entity entity = serverLevel.getEntity(uuid);
                                    if (entity instanceof LivingEntity servant && servant.isAlive()) {
                                        boostServantOnce(servant, multiplier, n);
                                    }
                                });
                            });
                        }
                    }
                }
            } else {
                // 若玩家摘下了饰品，重置玩家属性
                if (player.level().getGameTime() % 40 == 0) {
                    resetPlayerAttributes(player);
                }
            }
        }
    }

    private static void updatePlayerAttributes(ServerPlayer player, double multiplier, int n) {
        double targetHp = Math.min(20.0 * (multiplier +1.0), MAX_HEALTH_LIMIT);
        setBase(player, Attributes.MAX_HEALTH, targetHp);
        setBase(player, Attributes.ATTACK_DAMAGE, multiplier +n +1);
        setBase(player, Attributes.ATTACK_SPEED, 4.0 + (n * 0.02));
        setBase(player, Attributes.ARMOR, n * 1.0);
        setBase(player, Attributes.ARMOR_TOUGHNESS, n * 1.0);
        setBase(player, Attributes.LUCK, n * 1.0+1.0);
        setBase(player, Attributes.KNOCKBACK_RESISTANCE, n * 0.01);
        applyIronsMana(player, n * 1.0);
    }

    private static void setBase(LivingEntity entity, Attribute attr, double value) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    private static void resetPlayerAttributes(ServerPlayer player) {
        AttributeInstance hp = player.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && hp.getBaseValue() > 20.1) {
            hp.setBaseValue(20.0);
            setBase(player, Attributes.ATTACK_DAMAGE, 1.0);
            setBase(player, Attributes.ATTACK_SPEED, 4.0);
            setBase(player, Attributes.ARMOR, 0.0);
            setBase(player, Attributes.ARMOR_TOUGHNESS, 0.0);
            setBase(player, Attributes.LUCK, 0.0);
            setBase(player, Attributes.KNOCKBACK_RESISTANCE, 0.0);
            applyIronsMana(player, 0.0);
        }
    }

    // 兼容 Iron's Spellbooks 法力值
    private static void applyIronsMana(Player player, double bonus) {
        try {
            // 最大法力值
            Attribute manaAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "max_mana"));
            if (manaAttr != null) {
                AttributeInstance inst = player.getAttribute(manaAttr);
                if (inst != null) {
                    inst.setBaseValue(100.0 + bonus);
                }
            }

            // 通用法术强度 (Spell Power)
            Attribute spellPowerAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "spell_power"));
            if (spellPowerAttr != null) {
                AttributeInstance inst = player.getAttribute(spellPowerAttr);
                if (inst != null) {
                    inst.setBaseValue(bonus * 0.01 +1);
                }
            }

            // 通用法术冷却缩减 (Cooldown Reduction)
            Attribute cooldownAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "cooldown_reduction"));
            if (cooldownAttr != null) {
                AttributeInstance inst = player.getAttribute(cooldownAttr);
                if (inst != null) {
                    inst.setBaseValue(Math.min(bonus * 0.001 + 1, 3));
                }
            }

            // 通用法术吟唱缩减 (Cast Time Reduction)
            Attribute castTimeAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "cast_time_reduction"));
            if (castTimeAttr != null) {
                AttributeInstance inst = player.getAttribute(castTimeAttr);
                if (inst != null) {
                    inst.setBaseValue(Math.min(bonus * 0.001 + 1, 3));
                }
            }
        } catch (Exception ignored) {}
    }

    // ========== 修复核心：安全的修饰符附加逻辑 ==========
    private static void boostServantOnce(LivingEntity servant, double m, int n) {
        // 关键：打上标签，防止重复扫描
        if (!servant.getPersistentData().getBoolean(ENHANCED_TAG)) {
            servant.getPersistentData().putBoolean(ENHANCED_TAG, true);
        }

        // --- 1. 生命值加成 (修复重进变半颗心的问题) ---
        AttributeInstance hpInst = servant.getAttribute(Attributes.MAX_HEALTH);
        if (hpInst != null) {
            double pureBase = hpInst.getBaseValue();
            double expectedDiff = (pureBase * (m - 1.0)) + (double)n;

            AttributeModifier currentMod = hpInst.getModifier(SERVANT_HP_MOD_UUID);

            // 【重点】：仅当修饰符不存在，或者当前加成值与期望值不一致时，才进行替换。
            // 严禁每 2 秒移除了重加，这会导致血条瞬间被原版机制截断。
            if (expectedDiff > 0 && (currentMod == null || Math.abs(currentMod.getAmount() - expectedDiff) > 0.1)) {
                if (currentMod != null) {
                    hpInst.removeModifier(SERVANT_HP_MOD_UUID);
                }
                hpInst.addPermanentModifier(new AttributeModifier(SERVANT_HP_MOD_UUID, "SentientHP", expectedDiff, AttributeModifier.Operation.ADDITION));

                // 只有在上限真正发生改变时，才拉满血条，平时让它可以正常受伤和回血
                servant.setHealth(servant.getMaxHealth());
            }
        }

        // --- 2. 攻击力加成 ---
        AttributeInstance dmgInst = servant.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgInst != null) {
            double expectedDmgDiff = (dmgInst.getBaseValue() * (m - 1.0)) + (double)n;
            AttributeModifier currentDmgMod = dmgInst.getModifier(SERVANT_DMG_MOD_UUID);

            if (expectedDmgDiff > 0 && (currentDmgMod == null || Math.abs(currentDmgMod.getAmount() - expectedDmgDiff) > 0.1)) {
                if (currentDmgMod != null) {
                    dmgInst.removeModifier(SERVANT_DMG_MOD_UUID);
                }
                dmgInst.addPermanentModifier(new AttributeModifier(SERVANT_DMG_MOD_UUID, "SentientDMG", expectedDmgDiff, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    // 给敌人上削弱属性
    private static void applyEnemyDebuff(LivingEntity enemy, int n) {
        double mult = Math.pow(0.9, n);
        scaleAttr(enemy, Attributes.MAX_HEALTH, mult);
        scaleAttr(enemy, Attributes.ATTACK_DAMAGE, mult);
    }

    private static void scaleAttr(LivingEntity e, Attribute a, double m) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(i.getBaseValue() * m);
    }

    // 检查玩家是否佩戴了指定的饰品
    private static boolean hasUnity(Player p) {
        if (p == null) return false;
        return CuriosApi.getCuriosHelper().findFirstCurio(p, stack -> {
            if (stack == null || stack.isEmpty()) return false;
            return stack.is(ModItems.WU1WU2.get());
        }).isPresent();
    }
}