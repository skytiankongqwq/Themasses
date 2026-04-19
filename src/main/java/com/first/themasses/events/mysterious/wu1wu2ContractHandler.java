package com.first.themasses.events.mysterious;

import com.first.themasses.item.ModItems;
import com.first.themasses.util.SearchServant;
import com.first.themasses.util.wu1wu2DataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.reflect.Method;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "themasses")
public class wu1wu2ContractHandler {

    /**
     * 【新增】主动右键转化方法
     * 只要玩家拥有饰品，右键任何非玩家生物都会尝试建立契约
     */
    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (!wu1wu2EvolutionHandler.isServantConversionEnabled) return;

        // 仅在服务端处理，且必须是主手操作（防止触发两次）
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        Entity target = event.getTarget();

        if (player instanceof ServerPlayer serverPlayer && target instanceof LivingEntity livingTarget) {
            if (hasUnity(serverPlayer)) {
                // 如果已经是仆从了，就不重复转化
                if (livingTarget.getPersistentData().getBoolean("IsSentientServant")) {
                    return;
                }

                // 尝试转化
                tryContract(livingTarget, serverPlayer);

                // 成功或尝试后取消事件，防止触发实体的原本右键菜单（比如骑马、挤奶等）
                event.setCanceled(true);
            }
        }
    }

    /**
     * 被动触发（仇恨触发）：当怪物盯上玩家时自动转化
     */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!wu1wu2EvolutionHandler.isServantConversionEnabled) return;

        LivingEntity attacker = event.getEntity();
        LivingEntity target = event.getNewTarget();

        if (target instanceof ServerPlayer player && hasUnity(player)) {
            if (attacker.getPersistentData().getBoolean("IsSentientServant") ||
                    attacker.getPersistentData().getBoolean("HasGrantedUnityN")) {
                return;
            }

            event.setNewTarget(null);
            tryContract(attacker, player);
        }
    }

    /**
     * 核心契约转换逻辑
     */
    private static void tryContract(LivingEntity target, ServerPlayer master) {
        if (!(target.level() instanceof ServerLevel level) || !target.isAlive() || target instanceof Player) return;

        // 1. 尝试寻找 Goety 仆从转换 (这属于深度转化)
        String entityName = EntityType.getKey(target.getType()).getPath();
        String servantName = entityName.replace("hostile_", "") + "_servant";
        LivingEntity servant = null;

        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            String typeKey = EntityType.getKey(type).toString();
            if (typeKey.contains("goety") && typeKey.contains(servantName)) {
                servant = (LivingEntity) type.create(level);
                break;
            }
        }

        if (servant != null) {
            // --- 逻辑 A: 替换为 Goety 仆从 ---
            servant.moveTo(target.getX(), target.getY(), target.getZ());
            servant.getPersistentData().putBoolean("IsSentientServant", true);
            servant.getPersistentData().putBoolean("HasGrantedUnityN", true);

            trySetGoetyOwner(servant, master); // 注入 Goety 所有权

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                servant.setItemSlot(slot, target.getItemBySlot(slot).copy());
            }

            target.discard();
            level.addFreshEntity(servant);

            completeContract(servant, master);
            master.displayClientMessage(Component.literal("§d眷属契约：§f" + servant.getDisplayName().getString() + " §d已加入。"), true);

        } else {
            // --- 逻辑 B: 强制驯服原生物 ---
            // 针对原版可驯服生物 (狼、猫、鹦鹉等)
            if (target instanceof TamableAnimal tamable) {
                tamable.tame(master); // 调用原版驯服方法
                tamable.setOrderedToSit(false); // 确保它不会原地坐下
            }
            // 针对马类
            else if (target instanceof AbstractHorse horse) {
                horse.setTamed(true);
                horse.setOwnerUUID(master.getUUID());
            }

            // 统一打上眷属标记，确保享受你的 EvolutionHandler 加成
            target.getPersistentData().putBoolean("IsSentientServant", true);

            // 清除当前的仇恨目标
            if (target instanceof Mob mob) {
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
            }

            completeContract(target, master);
            master.displayClientMessage(Component.literal("§6眷属驯服：§f" + target.getDisplayName().getString() + " §6已效忠。"), true);
        }
    }
    /**
     * 提取公共的契约完成步骤：增加N值、注册UUID、全域刷新
     */
    private static void completeContract(LivingEntity entity, ServerPlayer master) {
        wu1wu2DataManager.incrementAllyCount(master);
        SearchServant.onServantJoin(master, entity);

        // 关键：立即触发 EvolutionHandler 的全域刷新（含周围扫描和血量补满）
        wu1wu2EvolutionHandler.refreshStats(master);

        // 如果是新驯服的，强制把血补满（视觉效果最好）
        entity.setHealth(entity.getMaxHealth());
    }

    private static void trySetGoetyOwner(LivingEntity servant, Player master) {
        try {
            Class<?> iOwnedClass = Class.forName("com.Polarice3.Goety.api.entities.IOwned");
            if (iOwnedClass.isInstance(servant)) {
                Method setOwnerMethod = iOwnedClass.getMethod("setTrueOwner", LivingEntity.class);
                setOwnerMethod.invoke(servant, master);
            }
        } catch (Exception ignored) {}
    }

    @SubscribeEvent
    public static void onDeathPrevention(LivingHurtEvent event) {
        if (!wu1wu2EvolutionHandler.isServantConversionEnabled) return;

        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() >= player.getHealth() && hasUnity(player)) {
            if (player.level() instanceof ServerLevel serverLevel) {
                SearchServant.getEnhancedServantData(player).ifPresent(data -> {
                    for (UUID uuid : data.servantDetails.keySet()) {
                        Entity entity = serverLevel.getEntity(uuid);
                        if (entity instanceof LivingEntity servant && servant.isAlive()) {
                            event.setCanceled(true);
                            player.setHealth(4.0F);
                            player.teleportTo(serverLevel, servant.getX(), servant.getY(), servant.getZ(), player.getYRot(), player.getXRot());
                            servant.hurt(player.damageSources().magic(), Float.MAX_VALUE);
                            player.displayClientMessage(Component.literal("§c眷属献祭了自己..."), true);
                            break;
                        }
                    }
                });
            }
        }
    }

    private static boolean hasUnity(Player p) {
        if (p == null) return false;
        return CuriosApi.getCuriosHelper().findFirstCurio(p, ModItems.WU1WU2.get()).isPresent();
    }
}