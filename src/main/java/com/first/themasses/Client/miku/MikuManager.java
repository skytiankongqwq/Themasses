package com.first.themasses.Client.miku;

import com.first.themasses.networking.ModMessages;
import com.first.themasses.networking.PacketMikuEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MikuManager {

    private static final String NBT_ENABLED = "MikuEnabled";
    private static final String NBT_DURATION = "MikuDuration";

    private static final String TEXTURE = "themasses:textures/screens/miku.png";
    private static final String MOD_NBT_TAG = "themasses_miku";
    private static final byte PACKET_TYPE = PacketMikuEffect.TYPE_MIKU;

    // 必须保留缓存：客户端实体不持久化自定义 NBT，必须靠缓存记住开启状态！
    private static final ConcurrentHashMap<UUID, EffectConfig> EFFECT_CACHE = new ConcurrentHashMap<>();

    private MikuManager() {
    }

    public static EffectConfig getEffectConfig(Entity entity) {
        if (entity == null) {
            return new EffectConfig();
        }

        UUID uuid = entity.getUUID();
        EffectConfig cached = EFFECT_CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }

        CompoundTag tag = getTag(entity);
        EffectConfig config = new EffectConfig();
        config.enabled = tag.getBoolean(NBT_ENABLED);
        config.duration = tag.getInt(NBT_DURATION);
        EFFECT_CACHE.put(uuid, config);
        return config;
    }

    private static CompoundTag getTag(Entity entity) {
        return entity.getPersistentData().getCompound(MOD_NBT_TAG);
    }

    private static void saveTag(Entity entity, CompoundTag tag) {
        entity.getPersistentData().put(MOD_NBT_TAG, tag);
    }

    public static void activate(Entity entity, int durationSeconds) {
        if (entity == null) return;

        EffectConfig currentConfig = getEffectConfig(entity);
        boolean wasEnabled = currentConfig.enabled;

        // 【核心修复】如果当前已经处于开启状态，直接更新时间即可，绝不重复发包，彻底杜绝闪烁！
        CompoundTag tag = getTag(entity);
        tag.putBoolean(NBT_ENABLED, true);
        int durationTicks = Math.max(0, durationSeconds) * 20;
        tag.putInt(NBT_DURATION, durationTicks);
        saveTag(entity, tag);
        updateCache(entity.getUUID(), true, durationTicks);

        // 只有当它【之前确实是关闭状态】时，才向追踪的客户端发送网络包
        if (!entity.level().isClientSide && !wasEnabled) {
            ModMessages.sendToTracking(
                    new PacketMikuEffect(entity.getId(), PACKET_TYPE, true, durationSeconds), entity);
        }
    }

    public static void deactivate(Entity entity) {
        if (entity == null) return;

        boolean wasEnabled = getEffectConfig(entity).enabled;

        CompoundTag tag = getTag(entity);
        tag.putBoolean(NBT_ENABLED, false);
        saveTag(entity, tag);
        updateCache(entity.getUUID(), false, 0);

        if (!entity.level().isClientSide && wasEnabled) {
            ModMessages.sendToTracking(
                    new PacketMikuEffect(entity.getId(), PACKET_TYPE, false, 0), entity);
        }
    }

    public static boolean shouldRender(Entity entity) {
        return getEffectConfig(entity).enabled; // 纯粹由开关决定，绝不自主倒计时闪烁
    }

    public static int getRemainingTime(Entity entity) {
        EffectConfig config = getEffectConfig(entity);
        if (!config.enabled || config.duration <= 0) {
            return 0;
        }
        return config.duration / 20;
    }

    public static String getTexture() {
        return TEXTURE;
    }

    private static void updateCache(UUID uuid, boolean enabled, int duration) {
        if (uuid == null) return;
        EffectConfig config = new EffectConfig();
        config.enabled = enabled;
        config.duration = duration;
        config.texture = TEXTURE;
        EFFECT_CACHE.put(uuid, config);
    }

    public static final class EffectConfig {
        public boolean enabled;
        public int duration;
        public String texture = TEXTURE;

        public EffectConfig() {
        }
    }
}