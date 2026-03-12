package com.first.themasses.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
//计时系统
public class PlayerCooldownManager {

    // 冷却时间常量：60秒 * 20 ticks/秒 = 1200 ticks
    private static final int COOLDOWN_TICKS = 24000;

    // NBT 键值：用于存储上次使用不死图腾效果的游戏时间戳
    public static final String NBT_LAST_TOTEM_TICK = "TheMasses_LastTotemTick";

    public static boolean isTotemCooldownReady(Player player) {
        // 使用玩家的持久化 NBT 数据
        CompoundTag nbt = player.getPersistentData();
        long lastUseTick = nbt.getLong(NBT_LAST_TOTEM_TICK);
        long currentTick = player.level().getGameTime();

        // 如果时间戳为 0 (首次使用或新玩家)，则视为冷却完毕
        if (lastUseTick == 0) {
            return true;
        }

        // 判断当前时间与上次使用时间的差值是否大于冷却周期
        return currentTick - lastUseTick >= COOLDOWN_TICKS;
    }

    /**
     * 重置不死图腾效果的冷却时间。在效果被使用后调用。
     * @param player 目标玩家
     */
    public static void resetTotemCooldown(Player player) {
        CompoundTag nbt = player.getPersistentData();
        long currentTick = player.level().getGameTime();

        // 写入当前游戏时间作为上次使用的时间戳
        nbt.putLong(NBT_LAST_TOTEM_TICK, currentTick);
    }

    /**
     * 计算剩余冷却时间（秒）。
     * @param player 目标玩家
     * @return 剩余冷却时间（秒）
     */
    public static int getRemainingCooldownSeconds(Player player) {
        CompoundTag nbt = player.getPersistentData();
        long lastUseTick = nbt.getLong(NBT_LAST_TOTEM_TICK);
        long currentTick = player.level().getGameTime();

        if (lastUseTick == 0) return 0;

        long elapsed = currentTick - lastUseTick;
        long remainingTicks = COOLDOWN_TICKS - elapsed;

        // 转换为秒 (1秒 = 20 ticks)，并确保结果不为负
        return (int) Math.max(0, remainingTicks / 20);
    }
}