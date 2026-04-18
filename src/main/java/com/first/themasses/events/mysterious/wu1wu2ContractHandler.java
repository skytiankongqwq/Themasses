package com.first.themasses.events.mysterious;

import com.first.themasses.item.ModItems;
import com.first.themasses.util.SearchServant;
import com.first.themasses.util.wu1wu2DataManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.lang.reflect.Method;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "themasses")
public class wu1wu2ContractHandler {

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        // --- 核心修复：检查转化开关 ---
        // 引用 EvolutionHandler 里的开关状态，如果玩家关闭了，不再将怪物转化为仆从
        if (!wu1wu2EvolutionHandler.isServantConversionEnabled) {
            return;
        }

        LivingEntity attacker = event.getEntity();
        LivingEntity target = event.getNewTarget();

        // 当敌对生物试图将仇恨锁定到佩戴了饰品的玩家身上时，触发转化契约
        if (target instanceof ServerPlayer player && hasUnity(player)) {
            event.setNewTarget(null); // 清除仇恨
            tryContract(attacker, player); // 尝试将其转变为 Goety 仆从
        }
    }

    private static void tryContract(LivingEntity target, ServerPlayer master) {
        if (!(target.level() instanceof ServerLevel level)) return;
        if (target instanceof Player || !target.isAlive()) return;

        // 获取原实体的注册名，并尝试匹配为 Goety 的仆从类型
        String entityName = EntityType.getKey(target.getType()).getPath();
        String servantName = entityName.replace("hostile_", "") + "_servant";

        LivingEntity servant = null;
        // 遍历注册表寻找对应的仆从实体
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            String typeKey = EntityType.getKey(type).toString();
            if (typeKey.contains("goety") && typeKey.contains(servantName)) {
                servant = (LivingEntity) type.create(level);
                break;
            }
        }

        if (servant != null) {
            // 将仆从移动到原怪物的位置
            servant.moveTo(target.getX(), target.getY(), target.getZ());

            // 使用反射设置所有者，替代直接 import IOwned 防止无前置崩溃
            trySetGoetyOwner(servant, master);

            // 复制装备到新仆从身上
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                servant.setItemSlot(slot, target.getItemBySlot(slot).copy());
            }

            // 删除原怪物，生成新仆从
            target.discard();
            level.addFreshEntity(servant);

            // 增加玩家的 N 值，并将仆从加入扫描列表
            wu1wu2DataManager.incrementAllyCount(master);
            SearchServant.onServantJoin(master, servant);
        }
    }

    /**
     * 通过反射调用 Goety 的 setTrueOwner 方法
     */
    private static void trySetGoetyOwner(LivingEntity servant, Player master) {
        try {
            // 检查该实体是否实现了 IOwned 接口
            Class<?> iOwnedClass = Class.forName("com.Polarice3.Goety.api.entities.IOwned");
            if (iOwnedClass.isInstance(servant)) {
                // 获取 setTrueOwner(LivingEntity) 方法并注入所有者
                Method setOwnerMethod = iOwnedClass.getMethod("setTrueOwner", LivingEntity.class);
                setOwnerMethod.invoke(servant, master);
            }
        } catch (ClassNotFoundException e) {
            // 未安装 Goety 模组，忽略异常
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onDeathPrevention(LivingHurtEvent event) {
        // 替死逻辑加上开关判断。如果转化关闭，不再触发献祭仆从的替死效果
        if (!wu1wu2EvolutionHandler.isServantConversionEnabled) return;

        // 当玩家即将受到致死伤害时
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() >= player.getHealth() && hasUnity(player)) {
            if (player.level() instanceof ServerLevel serverLevel) {
                SearchServant.getEnhancedServantData(player).ifPresent(data -> {
                    for (UUID uuid : data.servantDetails.keySet()) {
                        Entity entity = serverLevel.getEntity(uuid);
                        // 寻找到存活的仆从进行替死
                        if (entity instanceof LivingEntity servant && servant.isAlive()) {
                            event.setCanceled(true); // 取消玩家的死亡事件
                            player.setHealth(2.0F); // 给玩家保留一格血
                            // 玩家瞬移到仆从位置，仆从承受巨额魔法伤害死亡
                            player.teleportTo(serverLevel, servant.getX(), servant.getY(), servant.getZ(), player.getYRot(), player.getXRot());
                            servant.hurt(player.damageSources().magic(), Float.MAX_VALUE);
                            break; // 替死一次即可
                        }
                    }
                });
            }
        }
    }

    // 检查玩家是否佩戴了指定的饰品
    private static boolean hasUnity(Player p) {
        if (p == null) return false;
        return CuriosApi.getCuriosHelper().findFirstCurio(p, stack -> {
            if (stack == null || stack.isEmpty()) return false;
            boolean match = stack.is(ModItems.WU1WU2.get());
            if (!match) match = stack.getItem() == ModItems.WU1WU2.get();
            return match;
        }).isPresent();
    }
}