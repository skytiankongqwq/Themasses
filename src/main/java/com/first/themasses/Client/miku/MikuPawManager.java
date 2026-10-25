package com.first.themasses.Client.miku;

import com.first.themasses.networking.ModMessages;
import com.first.themasses.networking.PacketMikuEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 猫爪特效管理器：管理实体碰撞箱上方猫爪特效的 NBT 状态。
 * <p>原类名 MikuSupremeManager（Dream Sakura），因移植后名称被樱花特效
 * 占用，改名为 MikuPawManager；时间源改为 {@link System#currentTimeMillis()}。</p>
 */
public final class MikuPawManager {

    private static final String NBT_ENABLED = "MikuSupremeEnabled";
    private static final String NBT_DURATION = "MikuSupremeDuration";
    private static final String NBT_START_TIME = "MikuSupremeStartTick";

    private static final String TEXTURE = "themasses:textures/font/cat_paw.png";
    private static final String MOD_NBT_TAG = "themasses_miku_paw";
    private static final byte PACKET_TYPE = PacketMikuEffect.TYPE_PAW;

    private static final ConcurrentHashMap<UUID, EffectConfig> EFFECT_CACHE = new ConcurrentHashMap<>();

    private MikuPawManager() {
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
        config.startTime = tag.getLong(NBT_START_TIME);
        config.texture = TEXTURE;
        EFFECT_CACHE.put(uuid, config);
        return config;
    }

    private static CompoundTag getTag(Entity entity) {
        return entity.getPersistentData().getCompound(MOD_NBT_TAG);
    }

    private static void saveTag(Entity entity, CompoundTag tag) {
        entity.getPersistentData().put(MOD_NBT_TAG, tag);
    }

    /**
     * @param durationSeconds 持续时间（秒）
     */
    public static void activate(Entity entity, int durationSeconds) {
        if (entity == null) return;

        boolean wasEnabled = getEffectConfig(entity).enabled;

        CompoundTag tag = getTag(entity);
        tag.putBoolean(NBT_ENABLED, true);
        tag.putInt(NBT_DURATION, Math.max(0, durationSeconds) * 20);
        tag.putLong(NBT_START_TIME, System.currentTimeMillis());
        saveTag(entity, tag);
        updateCache(entity.getUUID(), tag);

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
        updateCache(entity.getUUID(), tag);

        if (!entity.level().isClientSide && wasEnabled) {
            ModMessages.sendToTracking(
                    new PacketMikuEffect(entity.getId(), PACKET_TYPE, false, 0), entity);
        }
    }

    public static boolean shouldRender(Entity entity) {
        EffectConfig config = getEffectConfig(entity);
        if (!config.enabled) {
            return false;
        }
        if (config.duration > 0) {
            long elapsed = System.currentTimeMillis() - config.startTime;
            if (elapsed >= config.duration * 50L) {
                deactivate(entity);
                return false;
            }
        }
        return true;
    }

    public static int getRemainingTime(Entity entity) {
        EffectConfig config = getEffectConfig(entity);
        if (!config.enabled || config.duration <= 0) {
            return 0;
        }
        long remaining = config.duration * 50L - (System.currentTimeMillis() - config.startTime);
        return (int) Math.max(0L, remaining / 1000L);
    }

    public static String getTexture() {
        return TEXTURE;
    }

    public static void clearCache(UUID uuid) {
        if (uuid != null) {
            EFFECT_CACHE.remove(uuid);
        }
    }

    private static void updateCache(UUID uuid, CompoundTag tag) {
        if (uuid == null) return;

        EffectConfig config = new EffectConfig();
        config.enabled = tag.getBoolean(NBT_ENABLED);
        config.duration = tag.getInt(NBT_DURATION);
        config.startTime = tag.getLong(NBT_START_TIME);
        config.texture = TEXTURE;
        EFFECT_CACHE.put(uuid, config);
    }

    public static final class EffectConfig {
        public boolean enabled;
        public int duration;
        public long startTime;
        public String texture = TEXTURE;

        public EffectConfig() {
        }
    }
}
