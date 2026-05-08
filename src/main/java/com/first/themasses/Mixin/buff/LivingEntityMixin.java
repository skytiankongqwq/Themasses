package com.first.themasses.Mixin.buff;

import com.first.themasses.item.ModItems;
import com.first.themasses.util.SearchServant;
import com.first.themasses.util.wu1wu2DataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

@SuppressWarnings("removal")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    @Unique
    private static final java.util.Map<UUID, Long> INVULNERABILITY_TICKS = new java.util.HashMap<>();

    /**
     * 修改 MobEffectInstance。
     * 尽量避免在方法内使用 ResourceLocation 频繁查询 getValue()
     */
    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance themasses$limitStarryLevel(MobEffectInstance instance) {
        if (instance == null) return null;

        MobEffect effect = instance.getEffect();
        // 技巧：直接对比 ID 的字符串，比去注册表里“拿对象再对比”要安全得多，且不依赖注册表加载顺序
        String effectName = ForgeRegistries.MOB_EFFECTS.getKey(effect).toString();

        if (effectName.equals("enigmaticdelicacy:astral_drunkenness")) {
            LivingEntity entity = (LivingEntity) (Object) this;

            // 检查饰品是否存在
            boolean hasCurio = CuriosApi.getCuriosHelper()
                    .findFirstCurio(entity, ModItems.LAOWANG237.get()).isPresent();

            // 如果有饰品且等级 >= 5级 (Amplifier 4)
            if (hasCurio && instance.getAmplifier() >= 4) {
                // 返回一个限制在 4 级 (Amplifier 3) 的新实例
                return new MobEffectInstance(
                        effect,
                        instance.getDuration(),
                        3,
                        instance.isAmbient(),
                        instance.isVisible(),
                        instance.showIcon()
                );
            }
        }
        return instance;
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void themasseslimitHealthByEvolution(float health, CallbackInfo ci) {
        if (IS_PROCESSING.get()) return;

        IS_PROCESSING.set(true);
        try {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!(entity instanceof ServerPlayer player)) return;

        boolean hasWu1wu2 = CuriosApi.getCuriosHelper()
                .findFirstCurio(player, ModItems.WU1WU2.get()).isPresent();

        if (!hasWu1wu2) return;

        int n = wu1wu2DataManager.getPermanentN(player);
        if (n <= 0) return;

        float damageReduction = (float) (0.05 * n + 0.005 * n * Math.log10(n + 1));
        float maxHealth = entity.getMaxHealth();
        float damagePercent = 100.0f / (damageReduction + 0.05f);
        float maxDamage = maxHealth * (damagePercent / 100.0f);
        maxDamage = Math.max(maxDamage, maxHealth * 0.01f);

        float currentHealth = entity.getHealth();
        float healthDifference = currentHealth - health;

            if (healthDifference > maxDamage && currentHealth > maxDamage) {
                UUID playerUUID = player.getUUID();
                long currentTick = player.level().getGameTime();

                if (INVULNERABILITY_TICKS.containsKey(playerUUID)) {
                    long invulnEndTick = INVULNERABILITY_TICKS.get(playerUUID);
                    if (currentTick < invulnEndTick) {
                        ci.cancel();
                        return;
                    } else {
                        INVULNERABILITY_TICKS.remove(playerUUID);
                    }
                }

                int invulnDuration = (int) Math.ceil(damageReduction) + 5;
                INVULNERABILITY_TICKS.put(playerUUID, currentTick + invulnDuration);
                entity.setHealth(currentHealth - maxDamage);
                ci.cancel();
            } else if (health <= 0 ) {
            if (player.level() instanceof ServerLevel serverLevel) {
                SearchServant.getEnhancedServantData(player).ifPresent(data -> {
                    for (UUID uuid : data.servantDetails.keySet()) {
                        Entity servantEntity = serverLevel.getEntity(uuid);
                        if (servantEntity instanceof LivingEntity servant && servant.isAlive()) {
                            ci.cancel();
                            player.teleportTo(serverLevel, servant.getX(), servant.getY(), servant.getZ(), player.getYRot(), player.getXRot());
                            servant.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
                            player.displayClientMessage(Component.literal("§c眷属献祭了自己..."), true);
                            return;
                        }
                    }
                });
            }
        }
        } finally {
            IS_PROCESSING.set(false);
        }
    }
}