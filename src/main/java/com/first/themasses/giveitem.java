package com.first.themasses;

import com.first.themasses.item.ModItems;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;
import java.util.function.Supplier;

//给物品
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class giveitem {
    private static final UUID MYSTERIOUS_SLOT_UUID = UUID.nameUUIDFromBytes("whitelist_mysterious_slot".getBytes());
    private static final String SLOT_ID = "mysterious";

    public static final Set<UUID> WHITELIST_UUID = new HashSet<>();
    public static final Set<String> WHITELIST_NAME = new HashSet<>();

    private static final Map<UUID, Long> dragonStartTime = new HashMap<>();
    private static final Set<UUID> killedDragon = new HashSet<>();
    private static final Set<UUID> killedWither = new HashSet<>();

    public static final List<PlayerReward> FIRST_JOIN_REWARDS = Arrays.asList(
            new PlayerReward(UUID.fromString("19a3eab1-3501-490e-b26e-3ef1d405a68e"), ModItems.LAOWANG237, 1),
            new PlayerReward(UUID.fromString("dcaea69c-c759-41dc-a45d-669ab3ac8251"), ModItems.DAZHI, 1),
            new PlayerReward(UUID.fromString("6fb0af7c-e14e-4886-ad6e-822a62469a01"), ModItems.GREEN_HAND_POTF, 1),
            new PlayerReward(UUID.fromString("1e0002a0-86cd-4232-84e3-373d6e557bce"), ModItems.LYUEZHUO, 1),
            new PlayerReward(UUID.fromString("7d8715c1-9224-4e2d-a516-46229b769297"), ModItems.XIN_TIRIS, 1),//天空
            new PlayerReward(UUID.fromString("7aba0098-0702-4e02-a218-f767877d96b6"), ModItems.AUVIOTRE_ITEM, 1),
            new PlayerReward(UUID.fromString("7c621bf8-a19d-4f3a-8e5b-8a75e6d81cfd"), ModItems.PLAYER__7, 1),
            new PlayerReward(UUID.fromString("c3783bb8-677a-40f1-9955-e04a2ba3f402"), ModItems.XIAOARIN, 1),
            new PlayerReward(UUID.fromString("302757d3-f8e0-467c-8650-29686c60eb5a"), ModItems.KUN_ZHONG, 1),
            new PlayerReward(UUID.fromString("0e011c0b-a796-4ec9-9bdb-a225d2d1db6d"), ModItems.BAI_ZE_LI, 1),
            new PlayerReward(UUID.fromString("0088c328-bbc2-4b70-bd66-5e0f512f5eee"), ModItems.DUVACEIN, 1),
            new PlayerReward(UUID.fromString("40f094c1-c795-415b-9bba-127e9cdb64d5"), ModItems.XIN_TIRIS, 1),
            new PlayerReward(UUID.fromString("514354df-6166-4bd7-9382-7757d3c008a6"), ModItems.HAOZI, 1),
            new PlayerReward(UUID.fromString("dcd88045-2e14-4571-acfe-f2093ab530b8"), ModItems.WU1WU2, 1),
            new PlayerReward(UUID.fromString("85f1b302-9b3b-4a55-a4d2-cfffec87061a"), ModItems.MIKULA, 1)


    );

    static {
        WHITELIST_UUID.add(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        WHITELIST_UUID.add(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        WHITELIST_NAME.add("Flechazo");
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        String playerName = sp.getName().getString();
        UUID playerUUID = sp.getUUID();

        // 1. 获取并初始化 NBT
        CompoundTag forgeData = sp.getPersistentData();
        CompoundTag persistedData = forgeData.getCompound(ServerPlayer.PERSISTED_NBT_TAG);

        // 判定：白名单玩家 OR 已通过挑战的玩家
        boolean isWhitelisted = WHITELIST_UUID.contains(playerUUID) ||
                WHITELIST_NAME.contains(playerName) ||
                persistedData.getBoolean("themasses:boss_task_complete");

        // --- A. Curios 槽位处理 ---
        CuriosApi.getCuriosInventory(sp).ifPresent(handler -> {
            var modifier = new AttributeModifier(MYSTERIOUS_SLOT_UUID, "Whitelist Mysterious Slot", 1.0, AttributeModifier.Operation.ADDITION);

            // 移除旧的防止冲突
            Multimap<String, AttributeModifier> r = HashMultimap.create();
            r.put(SLOT_ID, modifier);
            handler.removeSlotModifiers(r);

            if (isWhitelisted) {
                Multimap<String, AttributeModifier> add = HashMultimap.create();
                add.put(SLOT_ID, modifier);
                handler.addPermanentSlotModifiers(add);
            }
        });

        // --- B. 物品发放处理 (移出 Curios 闭包，确保 100% 执行) ---
        String rewardKey = "themasses:reward_given";

        // 如果没发过，则进入发放逻辑
        if (!persistedData.getBoolean(rewardKey)) {
            ItemStack rewardStack = ItemStack.EMPTY;

            // 1. 遍历匹配奖励
            for (PlayerReward pr : FIRST_JOIN_REWARDS) {
                if (pr.uuid().equals(playerUUID)) {
                    rewardStack = new ItemStack(pr.itemSupplier().get(), pr.count());
                    break;
                }
            }

            // 2. 特殊 ID 判定
            if (rewardStack.isEmpty() && "Dev".equals(playerName)) {rewardStack = new ItemStack(ModItems.WU1WU2.get(), 1);}

            // 3. 执行发放
            if (!rewardStack.isEmpty()) {
                // 确保物品能进背包，进不去就丢地上
                if (!sp.getInventory().add(rewardStack)) {
                    sp.drop(rewardStack, false);
                }

                // 标记已领取
                persistedData.putBoolean(rewardKey, true);

                // 【核心修复】将修改后的数据存回玩家 PersistentData，否则重启即消失
                forgeData.put(ServerPlayer.PERSISTED_NBT_TAG, persistedData);
            }
        }
    }


    // 1. 伤害龙时触发：挑战计时器启动
    @SubscribeEvent
    public static void onDamageDragon(LivingDamageEvent event) {
        // 判定受击者是否为末影龙，攻击者是否为玩家
        if (!(event.getEntity() instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;

        // 尝试启动计时，只有第一次伤害会放入（防止重复重置5分钟）
        if (!dragonStartTime.containsKey(sp.getUUID())) {
            dragonStartTime.put(sp.getUUID(), System.currentTimeMillis());
        }
    }

    // 2. 击杀目标时触发
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        // 必须是玩家完成的击杀
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;

        UUID id = sp.getUUID();
        long now = System.currentTimeMillis();

        // 检查是否有计时记录，没有说明挑战未启动
        if (!dragonStartTime.containsKey(id)) return;

        // 挑战超时检测
        long start = dragonStartTime.get(id);
        if (now - start > 5 * 60 * 1000) {
            killedDragon.remove(id);
            killedWither.remove(id);
            dragonStartTime.remove(id);
            return;
        }

        net.minecraft.world.entity.Entity target = event.getEntity();

        // 判定 A：击杀末影龙
        if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            killedDragon.add(id);

            // 判定 B：击杀凋灵 (建议使用 Wither 类确保兼容性)
        } else if (target instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
            if (killedDragon.contains(id)) {
                killedWither.add(id);
            }


            // 判定 C：击杀监守者 (Warden)
        } else if (target instanceof net.minecraft.world.entity.monster.warden.Warden) {
            if (killedDragon.contains(id) && killedWither.contains(id)) {
                rewardSlot(sp);

                // 清理进度
                killedDragon.remove(id);
                killedWither.remove(id);
                dragonStartTime.remove(id);
            }
        }
    }

    // 3. 发放奖励逻辑
    private static void rewardSlot(ServerPlayer sp) {
        CompoundTag forgeData = sp.getPersistentData();
        CompoundTag persisted = forgeData.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);

        if (!persisted.getBoolean("themasses:boss_task_complete")) {
            persisted.putBoolean("themasses:boss_task_complete", true);
            forgeData.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);

            // B. 发放槽位
            CuriosApi.getCuriosInventory(sp).ifPresent(handler -> {
                UUID UNIQUE_BOSS_REWARD_UUID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

                AttributeModifier modifier = new AttributeModifier(
                        UNIQUE_BOSS_REWARD_UUID,
                        "Boss Challenge Reward Slot",
                        1.0,
                        AttributeModifier.Operation.ADDITION
                );

                com.google.common.collect.Multimap<String, AttributeModifier> add = com.google.common.collect.HashMultimap.create();
                add.put("mysterious", modifier); // 这里写死字符串确保万无一失

                // 发放永久修改器
                handler.addPermanentSlotModifiers(add);
            });
        }
    }

    public record PlayerReward (UUID uuid, Supplier<? extends Item> itemSupplier, int count) {}
}