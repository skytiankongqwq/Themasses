package com.first.themasses.Recipes.sword;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber
public class ZitanhuaFormula {

    // 进度记录：玩家UUID -> 当前击杀到的索引
    private static final Map<UUID, Integer> PROGRESS = new HashMap<>();

    // 定义击杀序列
    private static final List<EntityType<?>> TARGET_SEQUENCE = List.of(
            EntityType.BAT
    );

    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        // 1. 软依赖检查：如果没有 Enigmatic Legacy，直接跳过整个逻辑
        if (!ModList.get().isLoaded("enigmaticlegacy")) return;

        // 2. 判定攻击者是否为玩家
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.level().isClientSide) return;

            // 3. 核心判定：主手龙心，副手寰宇之心
            if (!checkRequiredHearts(player)) {
                PROGRESS.remove(player.getUUID()); // 没拿好心，进度直接清空
                return;
            }

            EntityType<?> victimType = event.getEntity().getType();
            int currentIndex = PROGRESS.getOrDefault(player.getUUID(), 0);

            // 4. 判定击杀顺序
            if (victimType == TARGET_SEQUENCE.get(currentIndex)) {
                // 击杀正确，进度+1
                int nextIndex = currentIndex + 1;

                if (nextIndex >= TARGET_SEQUENCE.size()) {
                    // 完成仪式！发放武器
                    giveZitanhua(player);
                    PROGRESS.remove(player.getUUID());
                } else {
                    PROGRESS.put(player.getUUID(), nextIndex);
                }
            } else {
                // 5. 关键：杀错了别的怪，进度中断
                PROGRESS.remove(player.getUUID());
            }
        }
    }

    /**
     * 软依赖方式检测手持物品
     */
    private static boolean checkRequiredHearts(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // 龙心 ID: enigmaticlegacy:abyssal_heart
        // 寰宇之心 ID: enigmaticlegacy:cosmic_heart
        String mainId = ForgeRegistries.ITEMS.getKey(mainHand.getItem()).toString();
        String offId = ForgeRegistries.ITEMS.getKey(offHand.getItem()).toString();

        return mainId.equals("enigmaticlegacy:abyssal_heart") &&
                offId.equals("enigmaticlegacy:cosmic_heart");
    }

    /**
     * 发放紫昙花武器
     */
    private static void giveZitanhua(Player player) {
        // 假设你的武器注册名是 themasses:zitanhua
        ResourceLocation weaponId = new ResourceLocation("themasses", "zitanhua");
        var weaponItem = ForgeRegistries.ITEMS.getValue(weaponId);

        if (weaponItem != null && weaponItem != Items.AIR) {
            ItemStack reward = new ItemStack(weaponItem);
            if (!player.getInventory().add(reward)) {
                player.drop(reward, false); // 背包满了就掉地上
            }
        }
    }
}