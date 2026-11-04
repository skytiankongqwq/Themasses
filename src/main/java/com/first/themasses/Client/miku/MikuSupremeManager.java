package com.first.themasses.Client.miku;

import com.first.themasses.networking.ModMessages;
import com.first.themasses.networking.PacketMikuEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MikuSupremeManager {

    private static final String NBT_ENABLED = "Enabled";
    private static final String NBT_DURATION = "Duration";

    private static final String TEXTURE = "themasses:textures/particle/sakura_1.png";
    private static final String MOD_NBT_TAG = "themasses_miku_supreme";
    private static final byte PACKET_TYPE = PacketMikuEffect.TYPE_SUPREME;

    private static final ConcurrentHashMap<UUID, EffectConfig> EFFECT_CACHE = new ConcurrentHashMap<>();

    private MikuSupremeManager() {
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

        boolean wasEnabled = getEffectConfig(entity).enabled;

        CompoundTag tag = getTag(entity);
        tag.putBoolean(NBT_ENABLED, true);
        int durationTicks = Math.max(0, durationSeconds) * 20;
        tag.putInt(NBT_DURATION, durationTicks);
        saveTag(entity, tag);
        updateCache(entity.getUUID(), true, durationTicks);

        // 只有当它之前确实是关闭状态时才发包，防止每秒 Tick 重复发包导致闪烁
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
        return getEffectConfig(entity).enabled;
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