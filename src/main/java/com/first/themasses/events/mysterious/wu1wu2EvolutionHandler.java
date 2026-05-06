package com.first.themasses.events.mysterious;

import com.first.themasses.item.ModItems;
import com.first.themasses.util.SearchServant;
import com.first.themasses.util.wu1wu2DataManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;
@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = "themasses")
public class wu1wu2EvolutionHandler {

    private static final String ENHANCED_TAG = "SentientEnhanced";
    private static final double MAX_HEALTH_LIMIT = 5152520.0;

    // --- 玩家属性专用 UUID 阵列 ---
    private static final UUID P_HP_UUID = UUID.fromString("f42a781b-5e6a-4b9d-8c12-3a4b5c6d7e8f");
    private static final UUID P_DMG_UUID = UUID.fromString("a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d");
    private static final UUID P_SPD_UUID = UUID.fromString("c9d8e7f6-a5b4-3c2d-1e0f-9a8b7c6d5e4f");
    private static final UUID P_ARM_UUID = UUID.fromString("11223344-5566-7788-99aa-bbccddeeff00");
    private static final UUID P_TOU_UUID = UUID.fromString("aabbccdd-eeff-1122-3344-556677889900");
    private static final UUID P_LUK_UUID = UUID.fromString("77777777-8888-9999-0000-aaaaaaaaaaaa");
    private static final UUID P_KBR_UUID = UUID.fromString("12345678-1234-1234-1234-123456789012");

    // --- 仆从属性专用 UUID ---
    private static final UUID SERVANT_HP_MOD_UUID = UUID.fromString("b0bafeef-dead-beef-cafe-000000000001");
    private static final UUID SERVANT_DMG_MOD_UUID = UUID.fromString("b0bafeef-dead-beef-cafe-000000000002");
    private static final UUID SERVANT_ARMOR_MOD_UUID = UUID.fromString("b0bafeef-dead-beef-cafe-000000000003");
    private static final UUID SERVANT_TOUGHNESS_MOD_UUID = UUID.fromString("b0bafeef-dead-beef-cafe-000000000004");

    public static boolean isServantConversionEnabled = true;

    public static void handleAbilityPacket(ServerPlayer player, int entityId) {
        if (entityId == -51) {
            isServantConversionEnabled = !isServantConversionEnabled;
            String msg = isServantConversionEnabled ? "§a仆从转化：已开启" : "§c仆从转化：已关闭";
            player.displayClientMessage(Component.literal(msg), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        CompoundTag oldForgeData = oldPlayer.getPersistentData();
        if (oldForgeData.contains(Player.PERSISTED_NBT_TAG)) {
            newPlayer.getPersistentData().put(Player.PERSISTED_NBT_TAG,
                    oldForgeData.getCompound(Player.PERSISTED_NBT_TAG).copy());
        }
    }

    // 【修复】登入时立刻同步数据
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refreshStats(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refreshStats(player);
        }
    }

    public static double getLog2Multiplier(int n) {
        return 0.05 * n + 0.005 * n * Math.log10(n + 1);
    }

    @SubscribeEvent
    public static void onCombat(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();

        if (source instanceof Player attacker && hasUnity(attacker)) {
            int n = wu1wu2DataManager.getPermanentN(attacker);
            if (n > 0) {
                float multiplier = (float) getLog2Multiplier(n);
                event.setAmount(event.getAmount() * (1.0F + multiplier));
                victim.getPersistentData().putInt("SentientVulnerability", n);
            }
        }

        if (victim instanceof Player player && hasUnity(player)) {
            int n = wu1wu2DataManager.getPermanentN(player);
            if (n > 0) {
                double damageReduction = getLog2Multiplier(n);
                event.setAmount((float) (event.getAmount() / (1.0 + damageReduction)));
            }
        }

        if (victim.getPersistentData().contains("SentientVulnerability")) {
            int n = victim.getPersistentData().getInt("SentientVulnerability");
            double vulnerability = getLog2Multiplier(n);
            event.setAmount((float) (event.getAmount() * (1.0 + vulnerability)));
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onBeingDamage(LivingDamageEvent event){
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();

        if (victim instanceof Player player && hasUnity(player)) {
            int n = wu1wu2DataManager.getPermanentN(player);
            if (n > 0) {
                float damageReduction = (float) getLog2Multiplier(n);
                float maxHealth = victim.getMaxHealth();
                float damagePercent = 100.0f / (damageReduction + 0.05f);
                float maxDamage = maxHealth * (damagePercent / 100.0f);
                maxDamage = Math.max(maxDamage, maxHealth * 0.01f);

                if (event.getAmount() > maxDamage) {
                    event.setAmount(maxDamage);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.player;
            if (player.level().getGameTime() % 40 == 0) {
                refreshStats(player);
            }
        }
    }

    /**
     * 【重磅修复】全局同步方法
     * 现在的逻辑：不仅检查列表，还提供一个“强制目标”选项
     */
    public static void refreshStats(ServerPlayer player) {
        if (!hasUnity(player)) {
            removeAllModifiers(player);
            return;
        }

        int n = wu1wu2DataManager.getPermanentN(player);
        double multiplier = getLog2Multiplier(n);

        // 1. 同步玩家自身
        applyPlayerModifiers(player, multiplier, n);

        // 2. 增强型全域扫描
        if (player.level() instanceof ServerLevel serverLevel) {
            // A. 扫描玩家周围 64 格
            serverLevel.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64)).forEach(e -> {
                if (isValidServant(e, player)) {
                    boostServant(e, multiplier, n);
                }
            });

            // B. 列表保底（处理不在玩家身边的仆从）
            SearchServant.getEnhancedServantData(player).ifPresent(data -> {
                for (UUID uuid : data.servantDetails.keySet()) {
                    Entity entity = serverLevel.getEntity(uuid);
                    if (entity instanceof LivingEntity servant && servant.isAlive()) {
                        boostServant(servant, multiplier, n);
                    }
                }
            });
        }
    }

    private static void applyPlayerModifiers(ServerPlayer player, double m, int n) {
        float oldMax = player.getMaxHealth();

        double hpBonus = Math.min(10.0 * m, MAX_HEALTH_LIMIT);
        updateModifier(player, Attributes.MAX_HEALTH, P_HP_UUID, "UnityHP", hpBonus);

        float newMax = player.getMaxHealth();

        // 【核心修复】计算上限差值。如果上限增加了，立刻将差值作为治疗量补给玩家，彻底解决血条不满
        if (newMax > oldMax) {
            player.heal(newMax - oldMax);
        }

        updateModifier(player, Attributes.ATTACK_DAMAGE, P_DMG_UUID, "UnityDMG", n * 0.05 + m);
        updateModifier(player, Attributes.ATTACK_SPEED, P_SPD_UUID, "UnitySPD", n * 0.01);
        updateModifier(player, Attributes.ARMOR, P_ARM_UUID, "UnityARM", n * 0.2);
        updateModifier(player, Attributes.ARMOR_TOUGHNESS, P_TOU_UUID, "UnityTOU", n * 0.2);
        updateModifier(player, Attributes.LUCK, P_LUK_UUID, "UnityLUK", n * 0.1);
        updateModifier(player, Attributes.KNOCKBACK_RESISTANCE, P_KBR_UUID, "UnityKBR", Math.min(n * 0.01, 1.0));
        applyIronsMana(player, n * 1.0);
    }

    /**
     * 【新增】兼容性判定逻辑
     */
    private static boolean isValidServant(LivingEntity entity, Player player) {
        // 1. 检查是否带了我们的特殊仆从标签
        if (entity.getPersistentData().getBoolean("IsSentientServant")) return true;

        // 2. 检查是否为原版驯服生物（狼、猫、马、鹦鹉等）
        if (entity instanceof OwnableEntity ownable) {
            return player.getUUID().equals(ownable.getOwnerUUID());
        }

        return false;
    }

    public static void boostServant(LivingEntity servant, double m, int n) {
        // 这里不再直接 return，由外部 isValidServant 过滤
        AttributeInstance hpInst = servant.getAttribute(Attributes.MAX_HEALTH);
        if (hpInst != null) {
            float oldMax = servant.getMaxHealth();
            double baseHp = hpInst.getBaseValue();

            // 公式：基础血量 * (1 + 倍率) + 固定加成
            double hpBonus = (baseHp * m) + (double)n;

            updateModifier(servant, Attributes.MAX_HEALTH, SERVANT_HP_MOD_UUID, "ServantHP", hpBonus);

            float newMax = servant.getMaxHealth();
            if (newMax > oldMax) {
                // 如果是新加成，直接补满；如果是老加成微调，补差价
                servant.heal(newMax - oldMax);
            }
        }

        AttributeInstance dmgInst = servant.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgInst != null) {
            // 攻击力也按比例 + 固定值
            double dmgBonus = (dmgInst.getBaseValue() * m) + (n * 0.5);
            updateModifier(servant, Attributes.ATTACK_DAMAGE, SERVANT_DMG_MOD_UUID, "ServantDMG", dmgBonus);
        }

        AttributeInstance armorInst = servant.getAttribute(Attributes.ARMOR);
        if (armorInst != null) {
            double armorBonus = (armorInst.getBaseValue() * m) + (n * 0.5);
            updateModifier(servant, Attributes.ARMOR, SERVANT_ARMOR_MOD_UUID, "ServantARMOR", armorBonus);
        }

        AttributeInstance toughnessInst = servant.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughnessInst != null) {
            double toughnessBonus = (toughnessInst.getBaseValue() * m) + (n * 0.5);
            updateModifier(servant, Attributes.ARMOR_TOUGHNESS, SERVANT_TOUGHNESS_MOD_UUID, "ServantTOUGHNESS", toughnessBonus);
        }
    }

    private static void updateModifier(LivingEntity entity, Attribute attr, UUID uuid, String name, double value) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst != null) {
            AttributeModifier existing = inst.getModifier(uuid);
            if (existing == null || Math.abs(existing.getAmount() - value) > 0.001) {
                if (existing != null) inst.removeModifier(uuid);
                if (value != 0) {
                    inst.addPermanentModifier(new AttributeModifier(uuid, name, value, AttributeModifier.Operation.ADDITION));
                }
            }
        }
    }

    private static void removeAllModifiers(ServerPlayer player) {
        removeModifier(player, Attributes.MAX_HEALTH, P_HP_UUID);
        removeModifier(player, Attributes.ATTACK_DAMAGE, P_DMG_UUID);
        removeModifier(player, Attributes.ATTACK_SPEED, P_SPD_UUID);
        removeModifier(player, Attributes.ARMOR, P_ARM_UUID);
        removeModifier(player, Attributes.ARMOR_TOUGHNESS, P_TOU_UUID);
        removeModifier(player, Attributes.LUCK, P_LUK_UUID);
        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, P_KBR_UUID);
        applyIronsMana(player, 0.0);
    }

    private static void removeModifier(LivingEntity entity, Attribute attr, UUID uuid) {
        AttributeInstance inst = entity.getAttribute(attr);
        if (inst != null && inst.getModifier(uuid) != null) inst.removeModifier(uuid);
    }

    private static void applyIronsMana(Player player, double bonus) {
        try {
            updateIronsAttr(player, "max_mana", 100.0 + bonus);
            updateIronsAttr(player, "spell_power", 1.0 + (bonus * 0.01));
            updateIronsAttr(player, "cooldown_reduction", Math.min(bonus * 0.001 + 1, 3));
            updateIronsAttr(player, "cast_time_reduction", Math.min(bonus * 0.001 + 1, 3));
        } catch (Exception ignored) {}
    }

    private static void updateIronsAttr(Player player, String path, double val) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", path));
        if (attr != null) {
            AttributeInstance inst = player.getAttribute(attr);
            if (inst != null) inst.setBaseValue(val);
        }
    }

    private static boolean hasUnity(Player p) {
        if (p == null) return false;
        return CuriosApi.getCuriosHelper().findFirstCurio(p, ModItems.WU1WU2.get()).isPresent();
    }
}