package com.first.themasses.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class wu1wu2DataManager {
    private static final String NBT_KEY = "PermanentAllyCount";

    public static void incrementAllyCount(Player player) {
        if (player == null) return;
        CompoundTag nbt = player.getPersistentData();
        int currentN = nbt.getInt(NBT_KEY);
        nbt.putInt(NBT_KEY, currentN + 1);

        // 同步通知控制台，方便调试
        System.out.println("【因果律系统】数据已持久化。当前 n = " + (currentN + 1));
    }

    public static int getPermanentN(Player player) {
        if (player == null) return 0;
        return player.getPersistentData().getInt(NBT_KEY);
    }
}