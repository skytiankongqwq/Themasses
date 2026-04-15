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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = "themasses")
public class wu1wu2EvolutionHandler {

    private static final String ENHANCED_TAG = "SentientEnhanced";
    private static final double MAX_HEALTH_LIMIT = 5152520.0;//wu1wu2isbaka

    @SubscribeEvent
    public static void onCombat(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();

        if (source instanceof Player attacker && hasUnity(attacker)) {
            int n = wu1wu2DataManager.getPermanentN(attacker);
            if (n > 0) {
                float multiplier = (float) Math.pow(1.1, n);
                event.setAmount(event.getAmount() * multiplier);
                applyEnemyDebuff(victim, n);
                victim.getPersistentData().putInt("SentientVulnerability", n);

                if (attacker instanceof ServerPlayer sp && attacker.level().getGameTime() % 20 == 0) {
                    sp.displayClientMessage(Component.literal("因果加成生效: " + n + " 层"), true);
                }
            }
        }

        if (victim instanceof Player player && hasUnity(player)) {
            int n = wu1wu2DataManager.getPermanentN(player);
            event.setAmount((float) (event.getAmount() * Math.pow(0.9, n)));
        }

        if (victim.getPersistentData().contains("SentientVulnerability")) {
            int n = victim.getPersistentData().getInt("SentientVulnerability");
            event.setAmount((float) (event.getAmount() * Math.pow(1.1, n)));
        }
    }

    @SubscribeEvent
    public static void onServantUpdate(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.player;

            if (hasUnity(player)) {
                if (player.level().getGameTime() % 40 == 0) {
                    int n = wu1wu2DataManager.getPermanentN(player);
                    if (n > 0) {
                        double multiplier = Math.pow(1.1, n);

                        // 1. 玩家属性倍率+基数加成
                        // 血量：20 * 1.1^n + n (上限限制)
                        double targetHp = Math.min(20.0 * multiplier + n, MAX_HEALTH_LIMIT);
                        setBase(player, Attributes.MAX_HEALTH, targetHp);
                        // 攻击：1 * 1.1^n + n
                        setBase(player, Attributes.ATTACK_DAMAGE,  multiplier + n);
                        // 其他纯基数加成
                        setBase(player, Attributes.ATTACK_SPEED, 4.0 + (n * 0.02)); // 玩家基础攻速约为4.0
                        setBase(player, Attributes.ARMOR, n * 1.0);
                        setBase(player, Attributes.ARMOR_TOUGHNESS, n * 1.0);
                        setBase(player, Attributes.LUCK, n * 1.0);
                        setBase(player, Attributes.KNOCKBACK_RESISTANCE, n * 0.01);

                        // 铁魔法 Mana 反射处理
                        applyIronsMana(player, n * 50.0);

                        // 2. 强化仆从 (仅限一次)
                        if (player.level() instanceof ServerLevel serverLevel) {
                            SearchServant.getEnhancedServantData(player).ifPresent(data -> {
                                data.servantDetails.keySet().forEach(uuid -> {
                                    Entity entity = serverLevel.getEntity(uuid);
                                    if (entity instanceof LivingEntity servant && servant.isAlive()) {
                                        if (!servant.getTags().contains(ENHANCED_TAG)) {
                                            boostServantOnce(servant, multiplier, n);
                                        }
                                    }
                                });
                            });
                        }
                    }
                }
            } else {
                // 3. 卸下饰品重置属性 (每2秒检查一次是否需要重置)
                if (player.level().getGameTime() % 40 == 0) {
                    resetPlayerAttributes(player);
                }
            }
        }
    }

    private static void setBase(LivingEntity entity, Attribute attr, double value) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    private static void resetPlayerAttributes(ServerPlayer player) {
        AttributeInstance hp = player.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && hp.getBaseValue() > 20.1) { // 简单判断是否处于加成状态
            hp.setBaseValue(20.0);
            setBase(player, Attributes.ATTACK_DAMAGE, 1.0);
            setBase(player, Attributes.ATTACK_SPEED, 4.0);
            setBase(player, Attributes.ARMOR, 0.0);
            setBase(player, Attributes.ARMOR_TOUGHNESS, 0.0);
            setBase(player, Attributes.LUCK, 0.0);
            setBase(player, Attributes.KNOCKBACK_RESISTANCE, 0.0);
            applyIronsMana(player, 0.0); // 重置法力上限
            System.out.println("【因果律】饰品已卸下，属性已重置。");
        }
    }

    private static void applyIronsMana(Player player, double bonus) {
        try {
            Attribute manaAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "max_mana"));
            if (manaAttr != null) {
                AttributeInstance inst = player.getAttribute(manaAttr);
                if (inst != null) {
                    // 基础值通常是 100
                    inst.setBaseValue(100.0 + bonus);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void boostServantOnce(LivingEntity servant, double m, int n) {
        double hpVal = (servant.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * m) + n;
        setBase(servant, Attributes.MAX_HEALTH, hpVal);

        double dmgVal = (servant.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * m) + n;
        setBase(servant, Attributes.ATTACK_DAMAGE, dmgVal);

        setBase(servant, Attributes.ARMOR, servant.getAttribute(Attributes.ARMOR).getBaseValue() + n);

        servant.setHealth(servant.getMaxHealth());
        servant.addTag(ENHANCED_TAG);
    }

    private static void applyEnemyDebuff(LivingEntity enemy, int n) {
        double mult = Math.pow(0.9, n);
        scaleAttr(enemy, Attributes.MAX_HEALTH, mult);
        scaleAttr(enemy, Attributes.ATTACK_DAMAGE, mult);
    }

    private static void scaleAttr(LivingEntity e, Attribute a, double m) {
        AttributeInstance i = e.getAttribute(a);
        if (i != null) i.setBaseValue(i.getBaseValue() * m);
    }

    private static boolean hasUnity(Player p) {
        if (p == null) return false;
        return CuriosApi.getCuriosHelper().findFirstCurio(p, stack -> {
            if (stack == null || stack.isEmpty()) return false;
            return stack.is(ModItems.WU1WU2.get());
        }).isPresent();
    }
}