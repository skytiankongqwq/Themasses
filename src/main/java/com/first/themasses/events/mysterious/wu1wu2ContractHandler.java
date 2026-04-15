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
        LivingEntity attacker = event.getEntity();
        LivingEntity target = event.getNewTarget();

        if (target instanceof ServerPlayer player && hasUnity(player)) {
            event.setNewTarget(null);
            tryContract(attacker, player);
        }
    }

    private static void tryContract(LivingEntity target, ServerPlayer master) {
        if (!(target.level() instanceof ServerLevel level)) return;
        if (target instanceof Player || !target.isAlive()) return;

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
            servant.moveTo(target.getX(), target.getY(), target.getZ());

            // 使用反射设置所有者，替代直接 import IOwned
            trySetGoetyOwner(servant, master);

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                servant.setItemSlot(slot, target.getItemBySlot(slot).copy());
            }

            target.discard();
            level.addFreshEntity(servant);

            wu1wu2DataManager.incrementAllyCount(master);
            SearchServant.onServantJoin(master, servant);

            System.out.println("【因果契约】生物已成功转化。");
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
                // 获取 setTrueOwner(LivingEntity) 方法
                Method setOwnerMethod = iOwnedClass.getMethod("setTrueOwner", LivingEntity.class);
                setOwnerMethod.invoke(servant, master);
            }
        } catch (ClassNotFoundException e) {
            // 未安装 Goety 模组，忽略
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onDeathPrevention(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() >= player.getHealth() && hasUnity(player)) {
            if (player.level() instanceof ServerLevel serverLevel) {
                SearchServant.getEnhancedServantData(player).ifPresent(data -> {
                    for (UUID uuid : data.servantDetails.keySet()) {
                        Entity entity = serverLevel.getEntity(uuid);
                        if (entity instanceof LivingEntity servant && servant.isAlive()) {
                            event.setCanceled(true);
                            player.setHealth(2.0F);
                            player.teleportTo(serverLevel, servant.getX(), servant.getY(), servant.getZ(), player.getYRot(), player.getXRot());
                            servant.hurt(player.damageSources().magic(), Float.MAX_VALUE);
                            break;
                        }
                    }
                });
            }
        }
    }

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