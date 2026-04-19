package com.first.themasses.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class wu1wu2DataManager {
    private static final String NBT_KEY = "PermanentAllyCount";

    /**
     * 增加 N 值（契约数）
     * 写入到 PlayerPersisted 标签中，这是跨死亡存储的标准位置
     */
    public static void incrementAllyCount(Player player) {
        if (player == null) return;

        // 获取 Forge 提供的持久化 NBT 区域
        CompoundTag forgeData = player.getPersistentData();
        // 获取（如果没有则创建）"PlayerPersisted" 子标签
        CompoundTag persistentTag = forgeData.getCompound(Player.PERSISTED_NBT_TAG);

        int currentN = persistentTag.getInt(NBT_KEY);
        persistentTag.putInt(NBT_KEY, currentN + 1);

        // 必须写回，否则修改后的 persistentTag 可能不会被正确保存到 forgeData 中
        forgeData.put(Player.PERSISTED_NBT_TAG, persistentTag);
    }

    /**
     * 获取当前的 N 值
     */
    public static int getPermanentN(Player player) {
        if (player == null) return 0;

        CompoundTag forgeData = player.getPersistentData();
        if (forgeData.contains(Player.PERSISTED_NBT_TAG)) {
            return forgeData.getCompound(Player.PERSISTED_NBT_TAG).getInt(NBT_KEY);
        }
        return 0;
    }
}