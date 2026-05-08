package com.first.themasses.events.mysterious;

import com.first.themasses.curios.mysterious.XIN_TIRIS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = "themasses")
public class XINTirisEvents {

    public static int calculateImmediateTotalLevel(Player player) {
        java.util.Set<String> immediateEffectIds = new java.util.HashSet<>();
        int immediateTotalLevel = 0;
        for (var effect : player.getActiveEffects()) {
            var effectId = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
            if (effectId != null) {
                String idStr = effectId.toString();
                if (!immediateEffectIds.contains(idStr)) {
                    immediateEffectIds.add(idStr);
                    immediateTotalLevel += (effect.getAmplifier() + 1);
                }
            }
        }
        return immediateTotalLevel;
    }

    public static java.util.Set<String> getImmediateEffectIds(Player player) {
        java.util.Set<String> immediateEffectIds = new java.util.HashSet<>();
        for (var effect : player.getActiveEffects()) {
            var effectId = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
            if (effectId != null) {
                immediateEffectIds.add(effectId.toString());
            }
        }
        return immediateEffectIds;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();

        // 1. 被动防御逻辑 (限伤 6% + 负面种类%，最大36%)
        if (victim instanceof Player player) {
            CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof XIN_TIRIS).ifPresent(slot -> {
                int types = 0;
                var tag = slot.stack().getTagElement("RecordedEffects");
                if (tag != null) {
                    types = tag.getAllKeys().size();
                }

                float limitPercent = Math.min(0.06F + (types * 0.01F), 0.36F);
                float maxAllowed = player.getMaxHealth() * limitPercent;
                if (event.getAmount() > maxAllowed) {
                    event.setAmount(maxAllowed);
                }
            });
        }

        // 2. 主动攻击倍率逻辑
        if (event.getSource().getEntity() instanceof Player attacker && event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
            if (victim.getPersistentData().getBoolean("XINTirisProcessed")) {
                return;
            }

            CuriosApi.getCuriosHelper().findFirstCurio(attacker, stack -> stack.getItem() instanceof XIN_TIRIS).ifPresent(slot -> {
                CompoundTag tag = slot.stack().getTagElement("RecordedEffects");

                if (tag != null && !tag.isEmpty()) {
                    // 历史buff种类数（去重后的低等级）
                    int historicalTypes = tag.getAllKeys().size();

                    // 即时buff统计（当前所有效果）
                    int immediateTotalLevel = calculateImmediateTotalLevel(attacker);
                    java.util.Set<String> immediateEffectIds = getImmediateEffectIds(attacker);

                    // 总种类数 = 历史 + 即时（排除重复）
                    int totalTypes = historicalTypes;
                    for (String id : immediateEffectIds) {
                        if (!tag.contains(id)) {
                            totalTypes++;
                        }
                    }

                    if (totalTypes > 0 ) {
                        float bonusDamage = (float) totalTypes * (immediateTotalLevel + 1);
                        victim.getPersistentData().putBoolean("XINTirisProcessed", true);
                        event.setAmount(event.getAmount() + bonusDamage);
                    }
                }
            });
        }
    }

    // 【关键修改 3】在伤害计算结束后，必须清除标记，否则下一刀就没伤害加成了
    @SubscribeEvent
    public static void onLivingDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        // 在 LivingDamageEvent（Hurt 之后）清除标记位，确保下一击正常
        event.getEntity().getPersistentData().remove("XINTirisProcessed");
    }
}
