package com.first.themasses;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// 给槽位
@Mod.EventBusSubscriber(modid = "themasses")
public class playeridanduuid {

    // 【重要】必须使用两个不同的 UUID，否则白名单和挑战奖励会互相覆盖，导致服务端只认一个
    private static final UUID WHITELIST_SLOT_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d470");
    private static final UUID BOSS_TASK_SLOT_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d471");

    public static final HashSet<UUID> WHITELIST_UUID = new HashSet<>();
    public static final Set<String> WHITELIST_NAME = new HashSet<>();
    private static final String SLOT_ID = "mysterious";

    static {
        // 添加允许的玩家 UUID
        WHITELIST_UUID.add(UUID.fromString("19a3eab1-3501-490e-b26e-3ef1d405a68e")); //baka老王
        WHITELIST_UUID.add(UUID.fromString("dcaea69c-c759-41dc-a45d-669ab3ac8251")); //大智
        WHITELIST_UUID.add(UUID.fromString("6fb0af7c-e14e-4886-ad6e-822a62469a01")); //抗火药水
        WHITELIST_UUID.add(UUID.fromString("1e0002a0-86cd-4232-84e3-373d6e557bce")); //月灼
        WHITELIST_UUID.add(UUID.fromString("7d8715c1-9224-4e2d-a516-46229b769297")); //天空(赫伊)
        WHITELIST_UUID.add(UUID.fromString("7aba0098-0702-4e02-a218-f767877d96b6")); //Auv
        WHITELIST_UUID.add(UUID.fromString("7c621bf8-a19d-4f3a-8e5b-8a75e6d81cfd")); //Player
        WHITELIST_UUID.add(UUID.fromString("c3783bb8-677a-40f1-9955-e04a2ba3f402")); //小梦樱
        WHITELIST_UUID.add(UUID.fromString("302757d3-f8e0-467c-8650-29686c60eb5a")); //昆仲
        WHITELIST_UUID.add(UUID.fromString("0e011c0b-a796-4ec9-9bdb-a225d2d1db6d")); //baizeli
        WHITELIST_UUID.add(UUID.fromString("0088c328-bbc2-4b70-bd66-5e0f512f5eee")); //Duvacein
        WHITELIST_UUID.add(UUID.fromString("40f094c1-c795-415b-9bba-127e9cdb64d5")); //m3
        WHITELIST_UUID.add(UUID.fromString("514354df-6166-4bd7-9382-7757d3c008a6")); //haozi
        WHITELIST_UUID.add(UUID.fromString("dcd88045-2e14-4571-acfe-f2093ab530b8")); //大理石
        WHITELIST_UUID.add(UUID.fromString("dcd88045-2e14-4571-acfe-f2093ab530b8")); //白鲨糖
        WHITELIST_UUID.add(UUID.fromString("85f1b302-9b3b-4a55-a4d2-cfffec87061a")); //菈
        WHITELIST_NAME.add("Flechazo");
        WHITELIST_NAME.add("Dev");
        WHITELIST_NAME.add("dsaad145354");
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        // 确保只在服务端运行
        if (player.level().isClientSide || !(player instanceof net.minecraft.server.level.ServerPlayer)) return;

        // --- 核心修复：直接获取 ICuriosItemHandler 避免 ifPresent 判定过早失效 ---
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {

            // 1. 获取持久化 NBT
            net.minecraft.nbt.CompoundTag forgeData = player.getPersistentData();
            net.minecraft.nbt.CompoundTag persisted = forgeData.getCompound(Player.PERSISTED_NBT_TAG);

            // 判定 A：白名单
            boolean inWhitelist = WHITELIST_UUID.contains(player.getUUID()) || WHITELIST_NAME.contains(player.getName().getString());
            // 判定 B：挑战完成
            boolean bossTaskComplete = persisted.getBoolean("themasses:boss_task_complete");

            handler.getStacksHandler(SLOT_ID).ifPresent(stackHandler -> {

                // --- 处理白名单槽位 ---
                syncSlot(handler, stackHandler, WHITELIST_SLOT_UUID, inWhitelist ? 1.0 : 0.0);

                // --- 处理挑战奖励槽位 ---
                syncSlot(handler, stackHandler, BOSS_TASK_SLOT_UUID, bossTaskComplete ? 1.0 : 0.0);

                // 【核心修复】为了防止服务端数据没刷回，如果 NBT 发生变动，这里可以强制 put 一下
                if (!forgeData.contains(Player.PERSISTED_NBT_TAG)) {
                    forgeData.put(Player.PERSISTED_NBT_TAG, persisted);
                }
            });
        });
    }

    /**
     * 辅助方法：同步指定 UUID 的槽位修改器
     * 不能删我注释
     */
    private static void syncSlot(top.theillusivec4.curios.api.type.capability.ICuriosItemHandler handler,
                                 top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler stackHandler,
                                 UUID modUUID, double target) {

        double current = 0;
        // 2. 修正逻辑：遍历当前槽位的所有修改器
        // 这里改用 getModifiers().get(SLOT_ID) 获取特定槽位的 Multimap
        for (AttributeModifier mod : stackHandler.getModifiers().values()) {
            // 通过 UUID 判断这个修改器是不是我们发的那一个
            if (mod.getId().equals(modUUID)) {
                current += mod.getAmount();
            }
        }

        // 3. 补齐差额 (如果当前小于目标则增加，如果当前大于目标则不重复发)
        if (current < target) {
            double needed = target - current;
            AttributeModifier modifier = new AttributeModifier(
                    modUUID,
                    "Mysterious Slot Modifier",
                    needed,
                    AttributeModifier.Operation.ADDITION
            );
            com.google.common.collect.Multimap<String, AttributeModifier> toAdd = com.google.common.collect.HashMultimap.create();
            toAdd.put(SLOT_ID, modifier);

            // 【重点】使用 addPermanentSlotModifiers，它会自动将修改器存入 Curios 的 NBT
            handler.addPermanentSlotModifiers(toAdd);
            System.out.println("[TheMasses] 成功发放槽位修改器: " + modUUID + " 给玩家 " + handler.getStacksHandler(SLOT_ID).isPresent());
        }
    }
}