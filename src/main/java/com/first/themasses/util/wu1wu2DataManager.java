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
    }

    public static int getPermanentN(Player player) {
        if (player == null) return 0;
        return player.getPersistentData().getInt(NBT_KEY);
    }
}