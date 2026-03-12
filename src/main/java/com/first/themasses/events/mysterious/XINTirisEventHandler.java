package com.first.themasses.events.mysterious;

import com.first.themasses.curios.mysterious.XIN_TIRIS;
import com.first.themasses.item.ModItems; // 假设你的物品注册类名
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Collection;

@Mod.EventBusSubscriber(modid = "themasses")
public class XINTirisEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 攻击者必须是玩家
        if (event.getSource().getEntity() instanceof Player player) {

            // 检查玩家是否佩戴了 XIN_TIRIS 饰品
            CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.XIN_TIRIS.get()).ifPresent(triple -> {

                // --- 逻辑：计算药水伤害加成 ---
                Collection<MobEffectInstance> effects = player.getActiveEffects();
                int types = effects.size();
                int totalAmplifier = 0;
                for (MobEffectInstance effect : effects) {
                    totalAmplifier += (effect.getAmplifier() + 1);
                }

                // 最终加伤 = 种类数 * 总等级数
                float bonusDamage = (float) types * totalAmplifier;
                if (bonusDamage > 0) {
                    event.setAmount(event.getAmount() + bonusDamage);
                }

                // --- 逻辑：处理 100% 穿透 ---
                // 假设你有一个名为 PENETRATION 的效果，或者直接根据某种标记判定
                // if (player.hasEffect(ModEffects.PENETRATION.get())) {
                //    event.getSource().bypassArmor(); // 1.20.1 某些版本可能需要手动计算忽略护甲的伤害值
                // }
            });
        }
    }
}